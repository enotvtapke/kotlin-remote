package kotlinx.remote.classes

import kotlinx.remote.CallableMapClass
import kotlinx.remote.RemoteIntrinsic
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.serialization.RpcCallSerializer
import kotlinx.remote.network.serialization.setupExceptionSerializers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.LONG
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlin.reflect.KClass

class RemoteSerializer<T : Any>(
    private val leaseManager: LeaseManager? = null,
    private val leaseRenewalClient: LeaseRenewalClient? = null,
    private val stubFabric: ((Long) -> T)? = null
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("id", LONG)

    override fun serialize(encoder: Encoder, value: T) {
        if (value is Stub) {
            encoder.encodeLong(value.id)
            return
        }
        val id =
            leaseManager?.addInstanceWithLease(value) ?: error("Cannot serialize `$value`. No lease manager provided.")
        encoder.encodeLong(id)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        val id = decoder.decodeLong()
        val instance = leaseManager?.getInstance(id)
        if (instance != null) {
            return instance as T
        } else {
            val stub =
                stubFabric?.invoke(id) ?: error("Cannot deserialize stub with id `$id`. No stub fabric provided.")
            leaseRenewalClient?.registerStub(stub as Stub)
                ?: error("Cannot deserialize stub `$stub`. No lease renewal client provided.")
            return stub
        }
    }
}

fun remoteSerializersModule(
    remoteClasses: List<Pair<KClass<Any>, (Long) -> Any>>,
    callableMap: CallableMapClass,
    leaseManager: LeaseManager?,
    leaseRenewalClient: LeaseRenewalClient,
    serializersModule: SerializersModule = SerializersModule { }
): SerializersModule {
    val classSerializersModule = remoteClassSerializersModule(remoteClasses, leaseManager, leaseRenewalClient)
    return serializersModule + classSerializersModule + SerializersModule {
        contextual(
            RemoteCall::class,
            RpcCallSerializer(callableMap, serializersModule + classSerializersModule)
        )
        setupExceptionSerializers()
    }
}

fun remoteClassSerializersModule(
    remoteClasses: List<Pair<KClass<Any>, (Long) -> Any>>,
    leaseManager: LeaseManager?,
    leaseRenewalClient: LeaseRenewalClient
): SerializersModule = SerializersModule {
    remoteClasses.forEach { (clazz, stubFabric) ->
        contextual(clazz, RemoteSerializer(
            leaseManager = leaseManager,
            leaseRenewalClient = leaseRenewalClient,
            stubFabric = stubFabric
        ))
    }
}

/**
 * The compiler plugin will replace every call to this function with generated code
 */
fun genRemoteClassList(): List<Pair<KClass<*>, (Long) -> Any?>> = RemoteIntrinsic
