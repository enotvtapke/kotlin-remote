package kotlinx.remote.classes.lease

internal expect class WeakRef<T : Any>(referent: T) {
    fun get(): T?
}

