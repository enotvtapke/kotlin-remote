package kotlinx.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.remote.serialization.StackFrame
import kotlinx.remote.serialization.setStackTrace
import kotlinx.remote.serialization.stackTrace

interface RemoteClient {
    suspend fun call(call: RemoteCall): Any?
}

class RemoteClientImpl(
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
                throw mergeStackTraces(response.error, localStackTrace, call.callableName)
            }
        }
    }

    private fun mergeStackTraces(error: Exception, localStackTrace: List<StackFrame>, callableName: String): Exception {
        if (error.stackTrace().isEmpty()) return error
        val remoteStackTrace = error.stackTrace()
        if (remoteStackTrace.isEmpty() || localStackTrace.isEmpty()) {
            return error
        }
        val methodName = callableName.substringAfterLast('.')
        val containerName = callableName.substringBeforeLast('.')
        val normalizedContainerName = containerName
            .replace(Regex("\\$\\d+"), ".<anonymous>")
            .replace('$', '.')
        fun StackFrame.matchesCallable() = this.methodName == methodName &&
                (this.className == normalizedContainerName || this.className.startsWith("$normalizedContainerName."))
        val filteredLocalStackTrace = localStackTrace.reversed().takeWhile { !it.matchesCallable() }.reversed()
        val filteredRemoteCallStack = remoteStackTrace.reversed().dropWhile { !it.matchesCallable() }.reversed()
        val mergedStackTrace = filteredRemoteCallStack + filteredLocalStackTrace
        return error.setStackTrace(mergedStackTrace)
    }
}

suspend fun <T> RemoteClient.call(call: RemoteCall): T {
    return call(call) as T
}

fun HttpClient.remoteClient(callableMap: CallableMap, path: String = "call"): RemoteClient =
    RemoteClientImpl(this, path, callableMap)
