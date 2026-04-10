package kotlinx.remote.serialization

import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteCall
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.remoteClassSerializersModule
import kotlinx.remote.ktor.KRemoteConfigBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlin.reflect.KClass

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
    val classSerializersModule =
        remoteClassSerializersModule(remoteClasses ?: listOf(), leaseManager, nodeUrl, onStubDeserialization)
    return SerializersModule {
        include(classSerializersModule)
        include(serializersModule)
        include(throwableSerializers())
        contextual(
            RemoteCall::class,
            RemoteCallableSerializer(callableMap, serializersModule + classSerializersModule)
        )
    }
}