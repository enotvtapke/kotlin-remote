package kotlinx.remote.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.remote.CallableMap
import kotlinx.remote.network.serialization.rpcInternalKClass
import kotlinx.remote.returnTypeInfo
import kotlinx.serialization.serializer

interface RemoteClient {
    suspend fun call(call: RemoteCall): Any?
    fun callStreaming(call: RemoteCall, returnType: TypeInfo): Flow<Any?>
}

class RemoteClientImpl(private val httpClient: HttpClient, private val path: String) : RemoteClient {
    override suspend fun call(call: RemoteCall): Any? {
        val post = httpClient.post(path) {
            setBody(call)
        }
        return post.body(CallableMap[call.callableName].returnTypeInfo())
    }

    override fun callStreaming(
        call: RemoteCall,
        returnType: TypeInfo
    ): Flow<Any?> {
        return flow {
            httpClient.preparePost(path) {
                setBody(call)
            }.execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line()
                    if (line != null) {
                        emit(DefaultJson.decodeFromString(serializer(returnType.kotlinType!!), line))
                    }
                }
            }
        }
    }
}

inline fun <reified T> RemoteClient.callStreaming(call: RemoteCall) = callStreaming(call, typeInfo<T>()) as Flow<T>

suspend fun <T> RemoteClient.call(call: RemoteCall): T {
    when (val response = call(call) as RemoteResponse<T>) {
        is RemoteResponse.Success -> return response.value
        is RemoteResponse.Failure -> throw response.error
    }
}

fun HttpClient.remoteClient(path: String): RemoteClient = RemoteClientImpl(this, path)
