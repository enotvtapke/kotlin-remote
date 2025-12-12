package kotlinx.remote.serialization

internal actual fun <T : Exception> T.stackTrace(): List<StackFrame> {
    return this.stackTrace.map {
        StackFrame(it.className, it.methodName, it.fileName, it.lineNumber)
    }
}

internal actual fun <T : Exception> T.setStackTrace(stackTrace: List<StackFrame>): T {
    this.stackTrace = stackTrace.map {
        StackTraceElement(it.className, it.methodName, it.fileName, it.lineNumber)
    }.toTypedArray()
    return this
}
