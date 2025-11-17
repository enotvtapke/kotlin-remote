package org.jetbrains.kotlinx.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import network.serialization.RpcCallSerializer

interface RemoteClient {
    suspend fun call(call: RemoteCall, returnType: TypeInfo): Any?
}

class RemoteClientImpl(private val httpClient: HttpClient, private val path: String) : RemoteClient {
    override suspend fun call(call: RemoteCall, returnType: TypeInfo): Any? {
        val post = httpClient.post(path) {
            setBody(call)
        }
        return post.body(returnType)
    }
}

suspend inline fun <reified T> RemoteClient.call(call: RemoteCall) = call(call, typeInfo<T>()) as T

fun remoteClient(url: String, path: String, block: HttpClientConfig<*>.() -> Unit = {}): RemoteClient = HttpClient {
    defaultRequest {
        url(url)
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
    block()
}.let { RemoteClientImpl(it, path) }