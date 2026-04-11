package kotlinx.remote.serialization

internal actual fun <T : Throwable> T.stackTrace(): List<StackFrame> {
    return emptyList()
}

internal actual fun <T : Throwable> T.setStackTrace(stackTrace: List<StackFrame>): T = this
