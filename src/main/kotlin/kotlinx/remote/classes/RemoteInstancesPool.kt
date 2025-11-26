package kotlinx.remote.classes

import kotlinx.remote.classes.RemoteInstancesPool.instances
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

object RemoteInstancesPool {
    val instances = ConcurrentHashMap<Long, Any?>()

    @Suppress("UNUSED")
    fun getOrDefault(id: Long, default: Any?) = instances.getOrDefault(id, default)
}

fun addInstance(value: Any?): Long {
    val id = StubIdGenerator.nextId()
    instances[id] = value
    return id
}