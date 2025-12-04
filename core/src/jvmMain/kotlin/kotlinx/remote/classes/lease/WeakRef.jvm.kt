package kotlinx.remote.classes.lease

import java.lang.ref.WeakReference

actual class WeakRef<T : Any> actual constructor(referent: T) {
    private val ref = WeakReference(referent)
    actual fun get(): T? = ref.get()
}

