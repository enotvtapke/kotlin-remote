package kotlinx.remote.network.ktor

import kotlinx.serialization.json.Json
import kotlinx.remote.network.serialization.remoteCallSerializersModule

val jsonWithRemoteCallSerializer = Json {
    serializersModule = remoteCallSerializersModule()
}