package kotlinx.remote.classes.lease

expect class WeakRef<T : Any>(referent: T) {
    fun get(): T?
}

