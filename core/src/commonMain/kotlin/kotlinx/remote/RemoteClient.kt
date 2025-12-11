package kotlinx.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

interface RemoteClient {
    suspend fun call(call: RemoteCall): Any?
}

class RemoteClientImpl(private val httpClient: HttpClient, private val path: String, private val callableMap: CallableMap) : RemoteClient {
    override suspend fun call(call: RemoteCall): Any? {
        val post = httpClient.post(path) {
            setBody(call)
        }
        return post.body(callableMap[call.callableName].returnTypeInfo())
    }
}

suspend fun <T> RemoteClient.call(call: RemoteCall): T {
    when (val response = call(call) as RemoteResponse<T>) {
        is RemoteResponse.Success -> return response.value
        is RemoteResponse.Failure -> throw response.error
    }
}

fun HttpClient.remoteClient(callableMap: CallableMap, path: String = "call"): RemoteClient = RemoteClientImpl(this, path, callableMap)
