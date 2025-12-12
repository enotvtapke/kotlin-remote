package kotlinx.remote.serialization

internal actual fun <T : Exception> T.stackTrace(): List<StackFrame> {
    return emptyList()
}

internal actual fun <T : Exception> T.setStackTrace(stackTrace: List<StackFrame>): T = this
