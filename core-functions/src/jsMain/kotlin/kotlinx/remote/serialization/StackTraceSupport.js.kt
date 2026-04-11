package kotlinx.remote.serialization

actual fun <T : Throwable> T.stackTrace(): List<StackFrame> {
    return emptyList()
}

actual fun <T : Throwable> T.setStackTrace(stackTrace: List<StackFrame>): T = this

