package kotlinx.remote.classes

import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteIntrinsic
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.ktor.KRemoteConfigBuilder
import kotlinx.remote.serialization.RemoteCallableSerializer
import kotlinx.remote.serialization.setupThrowableSerializers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlin.reflect.KClass

class RemoteSerializer<T : Any>(
    private val leaseManager: LeaseManager? = null,
    private val nodeUrl: String? = null,
    private val onStubDeserialization: ((Stub) -> Unit)? = null,
    private val stubFabric: ((Long, String) -> T)? = null
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        "kotlinx.remote.classes.RemoteSerializable",
        StubSurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: T) {
        val surrogate = if (value is Stub) {
            StubSurrogate(value.id, value.url)
        } else {
            val id = leaseManager?.addInstanceWithLease(value) 
                ?: error("Cannot serialize `$value`. No lease manager provided.")
            val url = nodeUrl
                ?: error("Cannot serialize `$value`. No node URL provided.")
            StubSurrogate(id, url)
        }
        encoder.encodeSerializableValue(StubSurrogate.serializer(), surrogate)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        val surrogate = decoder.decodeSerializableValue(StubSurrogate.serializer())
        val instance = leaseManager?.getInstance(surrogate.id)
        if (instance != null) {
            return instance as T
        } else {
            val stub = stubFabric?.invoke(surrogate.id, surrogate.url)
                ?: error("Cannot deserialize stub with id `${surrogate.id}`. No stub fabric provided.")
            onStubDeserialization?.invoke(stub as Stub)
            return stub
        }
    }

    @Serializable
    @SerialName("Stub")
    private class StubSurrogate(val id: Long, val url: String)
}

fun remoteSerializersModule(block: KRemoteConfigBuilder.() -> Unit): SerializersModule {
    val config = KRemoteConfigBuilder().apply(block).build()
    return remoteSerializersModule(
        callableMap = config.callableMap,
        remoteClasses = config.classes?.remoteClasses,
        leaseManager = config.classes?.server?.leaseManager,
        nodeUrl = config.classes?.server?.nodeUrl,
        onStubDeserialization = config.classes?.client?.onStubDeserialization,
        serializersModule = config.serializersModule ?: SerializersModule { }
    )
}

fun remoteSerializersModule(
    callableMap: CallableMap,
    remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>? = null,
    leaseManager: LeaseManager? = null,
    nodeUrl: String? = null,
    onStubDeserialization: ((Stub) -> Unit)? = null,
    serializersModule: SerializersModule = SerializersModule { }
): SerializersModule {
    val classSerializersModule = remoteClassSerializersModule(remoteClasses ?: listOf(), leaseManager, nodeUrl, onStubDeserialization)
    return serializersModule + classSerializersModule + SerializersModule {
        contextual(
            RemoteCall::class,
            RemoteCallableSerializer(callableMap, serializersModule + classSerializersModule)
        )
        setupThrowableSerializers()
    }
}

fun remoteClassSerializersModule(
    remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>,
    leaseManager: LeaseManager? = null,
    nodeUrl: String? = null,
    onStubDeserialization: ((Stub) -> Unit)? = null
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
