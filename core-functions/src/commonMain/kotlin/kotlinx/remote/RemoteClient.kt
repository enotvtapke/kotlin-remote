package kotlinx.remote

import kotlinx.remote.serialization.StackFrame
import kotlinx.remote.serialization.setStackTrace
import kotlinx.remote.serialization.stackTrace

interface RemoteClient {
    suspend fun call(call: RemoteCall): RemoteResponse<*>
}

@Suppress("UNCHECKED_CAST")
suspend fun <T> RemoteClient.call(call: RemoteCall): T {
    return callUnwrap(call) as T
}

private suspend fun RemoteClient.callUnwrap(call: RemoteCall): Any? {
    val localStackTrace = Exception().stackTrace()
    val response = try {
        call(call)
    } catch (e: Exception) {
        throw RemoteException("Exception during making remote call '${call.callableName}'", e)
    }
    when (response) {
        is RemoteResponse.Success -> return response.value
        is RemoteResponse.Failure -> {
            throw mergeStackTraces(response.error, localStackTrace)
        }
    }
}

private val STACK_TRACE_SEPARATOR = StackFrame(
    className = "== Remote Call Boundary ==",
    methodName = "",
    fileName = null,
    lineNumber = -1
)

private fun mergeStackTraces(error: Throwable, localStackTrace: List<StackFrame>): Throwable {
    val remoteStackTrace = error.stackTrace()
    if (remoteStackTrace.isEmpty() || localStackTrace.isEmpty()) {
        return RemoteException("Stack trace altering is not supported on your Kotlin backend", error)
    }
    val filteredLocalStackTrace = localStackTrace.drop(3)
    val filteredRemoteCallStack = remoteStackTrace.takeWhile { it.methodName != "callWithUniqueName" }

    val mergedStackTrace = filteredRemoteCallStack + STACK_TRACE_SEPARATOR + filteredLocalStackTrace
    return error.setStackTrace(mergedStackTrace)
}
