package kotlinx.remote.classes.lease

actual class WeakRef<T : Any> actual constructor(referent: T) {
    // TODO: Implement weak references for WasmJS when available
    private var strongRef: T? = referent
    actual fun get(): T? = strongRef
}


