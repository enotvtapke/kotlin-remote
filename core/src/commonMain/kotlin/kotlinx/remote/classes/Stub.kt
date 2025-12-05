package kotlinx.remote.classes

interface Stub {
    val id: Long
}

fun checkIsNotStubForRemoteClassMethod(value: Any) { // used in compiler plugin
    require(value !is Stub) {
        "Method of the stub `${value::class.qualifiedName}` was called in a local context. This may be caused by lease expiration."
    }
}
