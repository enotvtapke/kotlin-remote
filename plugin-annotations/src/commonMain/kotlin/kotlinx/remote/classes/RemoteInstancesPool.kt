package kotlinx.remote.classes

import kotlinx.remote.classes.RemoteInstancesPool.instances

object RemoteInstancesPool {
    val instances = InternalConcurrentHashMap<Long, Any>()

    @Suppress("UNUSED")
    fun getOrDefault(id: Long, default: Any?) = instances[id] ?: default
}

fun addInstance(value: Any): Long {
    val id = StubIdGenerator.nextId()
    instances[id] = value
    return id
}
