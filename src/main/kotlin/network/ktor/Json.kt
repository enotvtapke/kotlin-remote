package org.jetbrains.kotlinx.network.ktor

import kotlinx.serialization.json.Json
import network.serialization.remoteCallSerializersModule

val jsonWithRemoteCallSerializer = Json {
    serializersModule = remoteCallSerializersModule()
}