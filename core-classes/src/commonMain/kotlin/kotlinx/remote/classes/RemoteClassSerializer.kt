package kotlinx.remote.classes

import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass

class RemoteClassSerializer<T : Any>(
    serialName: String,
    private val leaseManager: LeaseManager? = null,
    private val nodeUrl: String? = null,
    private val onStubDeserialization: ((Stub) -> Unit)? = null,
    private val stubFabric: ((Long, String) -> T)? = null
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        serialName,
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
        if (surrogate.url == nodeUrl) {  // TODO This is safety risk. One client can get remote class instance of another client by providing fake id.
            val instance = leaseManager?.getInstance(surrogate.id)
            if (instance != null) {
                return instance as T
            }
        }
        val stub = stubFabric?.invoke(surrogate.id, surrogate.url)
            ?: error("Cannot deserialize stub with id `${surrogate.id}`. No stub fabric provided.")
        onStubDeserialization?.invoke(stub as Stub)
        return stub
    }

    @Serializable
    @SerialName("Stub")
    private class StubSurrogate(val id: Long, val url: String)
}

fun remoteClassSerializersModule(
    remoteClasses: List<RemoteClassDescriptor<Any>>,
    leaseManager: LeaseManager? = null,
    nodeUrl: String? = null,
    onStubDeserialization: ((Stub) -> Unit)? = null
): SerializersModule = SerializersModule {
    remoteClasses.forEach { descriptor ->
        contextual(descriptor.clazz, RemoteClassSerializer(
            serialName = descriptor.serialName,
            leaseManager = leaseManager,
            nodeUrl = nodeUrl,
            onStubDeserialization = onStubDeserialization,
            stubFabric = descriptor.stubFabric
        ))
    }
}

data class RemoteClassDescriptor<T : Any>(
    val clazz: KClass<T>,
    val serialName: String,
    val stubFabric: (Long, String) -> T,
)

/**
 * The compiler plugin will replace every call to this function with generated code
 */
fun genRemoteClassList(): List<RemoteClassDescriptor<Any>> = RemoteIntrinsic

private val RemoteIntrinsic: Nothing
    get() = error("Intrinsic function should have been replaced by compiler plugin")
