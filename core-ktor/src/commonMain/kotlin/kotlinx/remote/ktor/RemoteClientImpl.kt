package kotlinx.remote.ktor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteResponse
import kotlinx.remote.returnTypeInfo
import kotlinx.remote.serialization.StackFrame
import kotlinx.remote.serialization.setStackTrace
import kotlinx.remote.serialization.stackTrace

internal class RemoteClientImpl(
    private val httpClient: HttpClient,
    private val path: String,
    private val callableMap: CallableMap
) : RemoteClient {
    override suspend fun call(call: RemoteCall): Any? {
        val localStackTrace = Exception().stackTrace()
        val post = httpClient.post(path) {
            setBody(call)
        }
        val callable = callableMap[call.callableName]
        when (val response = post.body<Any?>(callable.returnTypeInfo()) as RemoteResponse<*>) {
            is RemoteResponse.Success -> return response.value
            is RemoteResponse.Failure -> {
                throw mergeStackTraces(response.error, localStackTrace)
            }
        }
    }

    private fun mergeStackTraces(error: Throwable, localStackTrace: List<StackFrame>): Throwable {
        val remoteStackTrace = error.stackTrace()
        if (remoteStackTrace.isEmpty() || localStackTrace.isEmpty()) {
            return error
        }
        val filteredLocalStackTrace = localStackTrace.drop(3)
        val filteredRemoteCallStack = remoteStackTrace.takeWhile { it.methodName != "callWithUniqueName" }
        val mergedStackTrace = filteredRemoteCallStack + filteredLocalStackTrace
        return error.setStackTrace(mergedStackTrace)
    }
}

fun HttpClient.remoteClient(callableMap: CallableMap, path: String = "call"): RemoteClient =
    RemoteClientImpl(this, path, callableMap)
