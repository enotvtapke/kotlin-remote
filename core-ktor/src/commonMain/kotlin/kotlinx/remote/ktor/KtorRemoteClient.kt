package kotlinx.remote.ktor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.remote.*

internal class KtorRemoteClient(
    private val httpClient: HttpClient,
    private val path: String,
    private val callableMap: CallableMap
) : RemoteClient {
    override suspend fun call(call: RemoteCall): RemoteResponse<*> {
        val post = httpClient.post(path) {
            setBody(call)
        }
        val callable = callableMap[call.callableName]
        return post.body<Any?>(callable.returnTypeInfo()) as RemoteResponse<*>
    }
}

fun HttpClient.remoteClient(callableMap: CallableMap, path: String = "call"): RemoteClient =
    KtorRemoteClient(this, path, callableMap)
