package org.jetbrains.kotlinx.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.util.reflect.*

interface RemoteClient {
    suspend fun call(call: RemoteCall, returnType: TypeInfo): Any?
}

class RemoteClientImpl(private val httpClient: HttpClient): RemoteClient  {
    override suspend fun call(call: RemoteCall, returnType: TypeInfo): Any? {
        val post = httpClient.post("/call") {
            setBody(call)
        }
        return post.body(returnType)
    }
}

suspend inline fun <reified T> RemoteClient.call(call: RemoteCall) = call(call, typeInfo<T>()) as T

fun HttpClient.remoteClient() = RemoteClientImpl(this)