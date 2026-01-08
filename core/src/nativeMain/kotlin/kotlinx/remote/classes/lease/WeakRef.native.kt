package kotlinx.remote.classes.lease

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

@OptIn(ExperimentalNativeApi::class)
actual class WeakRef<T : Any> actual constructor(referent: T) {
    private val ref = WeakReference(referent)
    actual fun get(): T? = ref.get()
}

