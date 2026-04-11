package kotlinx.remote.serialization

internal expect fun <T : Throwable> T.stackTrace(): List<StackFrame>

internal expect fun <T : Throwable> T.setStackTrace(stackTrace: List<StackFrame>): T
