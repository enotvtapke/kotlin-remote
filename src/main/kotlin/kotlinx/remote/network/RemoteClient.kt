package kotlinx.remote.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.serializer
import kotlinx.remote.network.ktor.HEARTBEAT_JSON
import kotlinx.remote.network.ktor.jsonWithRemoteCallSerializer

interface RemoteClient {
    suspend fun call(call: RemoteCall, returnType: TypeInfo): Any?
    suspend fun callStreaming(call: RemoteCall, returnType: TypeInfo): Flow<Any?>
}

class RemoteClientImpl(private val httpClient: HttpClient, private val path: String) : RemoteClient {
    override suspend fun call(call: RemoteCall, returnType: TypeInfo): Any? {
        val post = httpClient.post(path) {
            setBody(call)
        }
        return post.body(returnType)
    }

    override suspend fun callStreaming(
        call: RemoteCall,
        returnType: TypeInfo
    ): Flow<Any?> {
        val serializer = jsonWithRemoteCallSerializer.serializersModule.serializer(returnType.kotlinType!!)
        return flow {
            httpClient.preparePost(path) {
                setBody(call)
                timeout {
                    socketTimeoutMillis = 10000
                }
            }.execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line()
                    if (line == HEARTBEAT_JSON) {
                        println("Client received and ignored heartbeat.")
                        continue
                    }
                    if (line?.isNotBlank() == true) {
                        val item = jsonWithRemoteCallSerializer.decodeFromString(serializer, line)
                        emit(item)
                    }
                }
            }
        }
    }
}

suspend inline fun <reified T> RemoteClient.callStreaming(call: RemoteCall) = callStreaming(call, typeInfo<T>()) as Flow<T>

suspend inline fun <reified T> RemoteClient.call(call: RemoteCall) = call(call, typeInfo<T>()) as T

fun HttpClient.remoteClient(path: String): RemoteClient = RemoteClientImpl(this, path)

fun HttpClientConfig<*>.configureRemote(url: String) {
    defaultRequest {
        url(url)
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
    }
    install(Logging) {
        level = LogLevel.BODY
    }
    install(ContentNegotiation) {
        json(jsonWithRemoteCallSerializer)
    }
}
