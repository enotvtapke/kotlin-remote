package kotlinx.remote.serialization

actual fun <T : Throwable> T.stackTrace(): List<StackFrame> {
    return this.stackTrace.map {
        StackFrame(it.className, it.methodName, it.fileName, it.lineNumber)
    }
}

actual fun <T : Throwable> T.setStackTrace(stackTrace: List<StackFrame>): T {
    this.stackTrace = stackTrace.map {
        StackTraceElement(it.className, it.methodName, it.fileName, it.lineNumber)
    }.toTypedArray()
    return this
}
