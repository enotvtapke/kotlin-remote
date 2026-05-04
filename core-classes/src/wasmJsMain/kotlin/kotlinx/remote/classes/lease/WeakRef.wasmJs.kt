@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlinx.remote.classes.lease

internal actual class WeakRef<T : Any> actual constructor(referent: T) {
    private val ref = JsWeakRef(referent.toJsReference())
    actual fun get(): T? = ref.deref()?.get()
}

@JsName("WeakRef")
private external class JsWeakRef<T : JsAny>(target: T) : JsAny {
    fun deref(): T?
}
