package kotlinx.remote.serialization

import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteCallable
import kotlinx.remote.RemoteType
import kotlinx.remote.RemoteCall
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
fun <T : Any> KType.rpcInternalKClass(): KClass<T> {
    val classifier = classifier ?: error("Expected denotable type, found $this")
    val classifierClass = classifier as? KClass<*> ?: error("Expected class type, found $this")

    return classifierClass as KClass<T>
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerializersModule.buildContextualInternal(type: KType): KSerializer<Any?>? {
    val result = getContextual(
        kClass = type.rpcInternalKClass(),
        typeArgumentsSerializers = type.arguments.mapIndexed { i, typeArgument ->
            val typeArg = typeArgument.type
                ?: error("Unexpected star projection type at index $i in type arguments list of '$type'")

            buildContextualInternal(typeArg) ?: serializer(typeArg)
        }
    )

    @Suppress("UNCHECKED_CAST")
    return if (type.isMarkedNullable) result?.nullable else result as? KSerializer<Any?>
}

@Suppress("UNCHECKED_CAST")
private fun buildPolymorphicSerializer(type: KType): KSerializer<Any?> {
    val baseClass = type.rpcInternalKClass<Any>()
    val serializer = PolymorphicSerializer(baseClass)
    return if (type.isMarkedNullable) {
        serializer.nullable as KSerializer<Any?>
    } else {
        serializer as KSerializer<Any?>
    }
}

private fun SerializersModule.buildContextual(type: KType): KSerializer<Any?> {
    return buildContextualInternal(type) ?: serializer(type)
}

fun SerializersModule.buildSerializer(type: RemoteType): KSerializer<Any?> {
    return if (type.isPolymorphic) {
        buildPolymorphicSerializer(type.kType)
    } else {
        buildContextual(type.kType)
    }
}

private class CallableParametersSerializer(
    private val callable: RemoteCallable,
    private val module: SerializersModule,
) : KSerializer<Array<Any?>> {
    private val callableSerializers = Array(callable.parameters.size) { i ->
        module.buildSerializer(callable.parameters[i].type)
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CallableParametersSerializer") {
        for (i in callableSerializers.indices) {
            val param = callable.parameters[i]
            element(
                elementName = param.name,
                descriptor = callableSerializers[i].descriptor,
                isOptional = param.isOptional,
            )
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Array<Any?>,
    ) {
        encoder.encodeStructure(descriptor) {
            for (i in callable.parameters.indices) {
                encodeSerializableElement(descriptor, i, callableSerializers[i], value[i])
            }
        }
    }

    override fun deserialize(decoder: Decoder): Array<Any?> {
        return decoder.decodeStructure(descriptor) {
            val result = arrayOfNulls<Any?>(callable.parameters.size)
            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) {
                    break
                }

                result[index] = decodeSerializableElement(descriptor, index, callableSerializers[index])
            }

            result
        }
    }
}


class RpcCallSerializer(private val callableMap: CallableMap, private val module: SerializersModule) : KSerializer<RemoteCall> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RpcCall") {
        element<String>("callableName")
        element("parameters", buildClassSerialDescriptor("Parameters"))
    }

    override fun serialize(encoder: Encoder, value: RemoteCall) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.callableName)

            // Find the callable and create serializer for parameters
            val callable = callableMap[value.callableName]
            val parametersSerializer = CallableParametersSerializer(callable, module)

            encodeSerializableElement(descriptor, 1, parametersSerializer, value.parameters)
        }
    }

    override fun deserialize(decoder: Decoder): RemoteCall {
        return decoder.decodeStructure(descriptor) {
            var callableName: String? = null
            var parameters: Array<Any?>? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> callableName = decodeStringElement(descriptor, 0)
                    1 -> {
                        // We need the callable name first to create the parameters serializer
                        val name = callableName ?: error("Callable name must be decoded before parameters")
                        val callable = callableMap[name]
                        val parametersSerializer = CallableParametersSerializer(callable, module)
                        parameters = decodeSerializableElement(descriptor, 1, parametersSerializer)
                    }
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            RemoteCall(
                callableName = callableName ?: error("Missing callableName"),
                parameters = parameters ?: error("Missing parameters")
            )
        }
    }
}
