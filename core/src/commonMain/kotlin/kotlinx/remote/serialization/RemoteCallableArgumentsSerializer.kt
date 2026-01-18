package kotlinx.remote.serialization

import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteType
import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteParameter
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
import kotlin.reflect.KTypeParameter

@Suppress("UNCHECKED_CAST")
internal fun KType.asKClass() = when (val t = classifier) {
    is KClass<*> -> t
    is KTypeParameter -> {
        throw IllegalArgumentException(
            "Captured type parameter $t from generic non-reified function. " +
                    "Such functionality cannot be supported because $t is erased, either specify serializer explicitly or make " +
                    "calling function inline with reified $t."
        )
    }

    else ->  throw IllegalArgumentException("Only KClass supported as classifier, got $t")
}

@Suppress("UNCHECKED_CAST")
private fun SerializersModule.buildSerializer(type: RemoteType): KSerializer<Any?> {
    return if (type.isPolymorphic) {
        val polymorphicSerializer = PolymorphicSerializer(type.kType.asKClass())
        (if (type.kType.isMarkedNullable) polymorphicSerializer.nullable else polymorphicSerializer) as KSerializer<Any?>
    } else {
        serializer(type.kType)
    }
}

private class RemoteCallableArgumentsSerializer(
    private val parameters: Array<RemoteParameter>,
    private val module: SerializersModule,
) : KSerializer<Array<Any?>> {
    private val callableSerializers = Array(parameters.size) { i ->
        module.buildSerializer(parameters[i].type)
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RemoteCallableArgumentsSerializer") {
        for (i in callableSerializers.indices) {
            element(
                elementName = parameters[i].name,
                descriptor = callableSerializers[i].descriptor,
                isOptional = parameters[i].isOptional,
            )
        }
    }

    override fun serialize(encoder: Encoder, value: Array<Any?>) {
        encoder.encodeStructure(descriptor) {
            for (i in parameters.indices) {
                encodeSerializableElement(descriptor, i, callableSerializers[i], value[i])
            }
        }
    }

    override fun deserialize(decoder: Decoder): Array<Any?> {
        return decoder.decodeStructure(descriptor) {
            val result = arrayOfNulls<Any?>(parameters.size)
            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                result[index] = decodeSerializableElement(descriptor, index, callableSerializers[index])
            }
            result
        }
    }
}


class RemoteCallableSerializer(private val callableMap: CallableMap, private val module: SerializersModule) : KSerializer<RemoteCall> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RpcCall") {
        element<String>("callableName")
        element("parameters", buildClassSerialDescriptor("Parameters"))
    }

    override fun serialize(encoder: Encoder, value: RemoteCall) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.callableName)

            // Find the callable and create serializer for arguments
            val callable = callableMap[value.callableName]
            val parametersSerializer = RemoteCallableArgumentsSerializer(callable.parameters, module)

            encodeSerializableElement(descriptor, 1, parametersSerializer, value.arguments)
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
                        val parametersSerializer = RemoteCallableArgumentsSerializer(callable.parameters, module)
                        parameters = decodeSerializableElement(descriptor, 1, parametersSerializer)
                    }
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            RemoteCall(
                callableName = callableName ?: error("Missing callableName"),
                arguments = parameters ?: error("Missing parameters")
            )
        }
    }
}
