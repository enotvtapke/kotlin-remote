package kotlinx.remote.classes

import kotlinx.remote.CallableMapClass
import kotlinx.remote.RemoteIntrinsic
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.serialization.RpcCallSerializer
import kotlinx.remote.network.serialization.setupExceptionSerializers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlin.reflect.KClass

class RemoteSerializer<T : Any>(
    private val leaseManager: LeaseManager? = null,
    private val nodeUrl: String? = null,
    private val onStubDeserialization: ((Stub) -> Unit) = { },
    private val stubFabric: ((Long, String) -> T)? = null
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RemoteRef") {
        element("id", Long.serializer().descriptor)
        element("url", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: T) {
        val composite = encoder.beginStructure(descriptor)
        if (value is Stub) {
            composite.encodeLongElement(descriptor, 0, value.id)
            composite.encodeStringElement(descriptor, 1, value.url)
        } else {
            val id = leaseManager?.addInstanceWithLease(value) 
                ?: error("Cannot serialize `$value`. No lease manager provided.")
            val url = nodeUrl 
                ?: error("Cannot serialize `$value`. No node URL provided.")
            composite.encodeLongElement(descriptor, 0, id)
            composite.encodeStringElement(descriptor, 1, url)
        }
        composite.endStructure(descriptor)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        val composite = decoder.beginStructure(descriptor)
        var id: Long? = null
        var url: String? = null
        
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> id = composite.decodeLongElement(descriptor, 0)
                1 -> url = composite.decodeStringElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        composite.endStructure(descriptor)
        
        requireNotNull(id) { "Missing 'id' field in remote reference" }
        requireNotNull(url) { "Missing 'url' field in remote reference" }
        
        val instance = leaseManager?.getInstance(id)
        if (instance != null) {
            return instance as T
        } else {
            val stub = stubFabric?.invoke(id, url) 
                ?: error("Cannot deserialize stub with id `$id`. No stub fabric provided.")
            onStubDeserialization.invoke(stub as Stub)
            return stub
        }
    }
}

fun remoteSerializersModule(
    remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>,
    callableMap: CallableMapClass,
    leaseManager: LeaseManager?,
    nodeUrl: String? = null,
    onStubDeserialization: ((Stub) -> Unit) = {  },
    serializersModule: SerializersModule = SerializersModule { }
): SerializersModule {
    val classSerializersModule = remoteClassSerializersModule(remoteClasses, leaseManager, nodeUrl, onStubDeserialization)
    return serializersModule + classSerializersModule + SerializersModule {
        contextual(
            RemoteCall::class,
            RpcCallSerializer(callableMap, serializersModule + classSerializersModule)
        )
        setupExceptionSerializers()
    }
}

fun remoteClassSerializersModule(
    remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>,
    leaseManager: LeaseManager?,
    nodeUrl: String? = null,
    onStubDeserialization: ((Stub) -> Unit) = { }
): SerializersModule = SerializersModule {
    remoteClasses.forEach { (clazz, stubFabric) ->
        contextual(clazz, RemoteSerializer(
            leaseManager = leaseManager,
            nodeUrl = nodeUrl,
            onStubDeserialization = onStubDeserialization,
            stubFabric = stubFabric
        ))
    }
}

/**
 * The compiler plugin will replace every call to this function with generated code
 */
fun genRemoteClassList(): List<Pair<KClass<Any>, (Long, String) -> Any>> = RemoteIntrinsic
