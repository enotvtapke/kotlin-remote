package kotlinx.remote.serialization

internal expect fun <T : Exception> T.stackTrace(): List<StackFrame>

internal expect fun <T : Exception> T.setStackTrace(stackTrace: List<StackFrame>): T
