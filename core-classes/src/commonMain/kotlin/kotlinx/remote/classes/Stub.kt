package kotlinx.remote.classes

// TODO Add equals and hash code that uses id and url in generated children of Stub interface
interface Stub {
    val id: Long
    val url: String
}

fun checkIsNotStubForRemoteClassMethod(value: Any) { // used in compiler plugin
    require(value !is Stub) {
        "Method of the stub `${value::class.simpleName}` was called in a local context. This may be caused by lease expiration."
    }
}
