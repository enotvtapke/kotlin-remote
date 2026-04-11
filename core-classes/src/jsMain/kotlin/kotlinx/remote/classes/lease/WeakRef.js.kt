package kotlinx.remote.classes.lease

actual class WeakRef<T : Any> actual constructor(referent: T) {
    private val ref = JsWeakRef(referent)
    actual fun get(): T? = ref.deref()
}

@JsName("WeakRef")
private external class JsWeakRef<T : Any>(target: T) {
    fun deref(): T?
}
