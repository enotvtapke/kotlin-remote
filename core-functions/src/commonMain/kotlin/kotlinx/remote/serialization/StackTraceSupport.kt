package kotlinx.remote.serialization

expect fun <T : Throwable> T.stackTrace(): List<StackFrame>

expect fun <T : Throwable> T.setStackTrace(stackTrace: List<StackFrame>): T
