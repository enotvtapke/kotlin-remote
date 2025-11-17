package org.jetbrains.kotlinx.examples.basic

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType

import org.jetbrains.kotlinx.RemoteConfig
import org.jetbrains.kotlinx.RemoteContext
import org.jetbrains.kotlinx.network.RemoteClient
import org.jetbrains.kotlinx.network.remoteClient
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import network.serialization.RpcCallSerializer
import org.jetbrains.kotlinx.network.RemoteCall

data object ServerConfig : RemoteConfig {
    override val context = ServerContext
    override val remoteClient: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = SerializersModule {
                    contextual(RemoteCall::class, RpcCallSerializer(SerializersModule {}))
                }
            })
        }
    }.remoteClient()

}

data object ServerContext : RemoteContext
