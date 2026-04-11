package kotlinx.remote.serialization

import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteCall
import kotlinx.serialization.modules.SerializersModule

fun remoteSerializersModuleShort(
    callableMap: CallableMap,
): SerializersModule {
   return SerializersModule {
        include(throwableSerializers())
        contextual(
            RemoteCall::class,
            RemoteCallableSerializer(callableMap)
        )
    }
}
