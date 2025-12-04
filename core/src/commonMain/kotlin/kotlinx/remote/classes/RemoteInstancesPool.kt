package kotlinx.remote.classes

import kotlinx.remote.classes.RemoteInstancesPool.instances
import kotlinx.remote.classes.lease.LeaseManager

object RemoteInstancesPool {
    val instances = InternalConcurrentHashMap<Long, Any>()

    @Suppress("UNUSED")
    fun getOrDefault(id: Long, default: Any?) = instances[id] ?: default
    
    fun removeInstance(id: Long): Any? = instances.remove(id)
    
    fun hasInstance(id: Long): Boolean = instances.containsKey(id)
    
    fun instanceCount(): Int = instances.entries.size
    
    fun clear() {
        instances.clear()
    }
}

fun addInstance(value: Any, clientId: String? = null): Long {
    val id = StubIdGenerator.nextId()
    instances[id] = value
    LeaseManager.createLease(id, clientId)
    return id
}

fun addInstanceWithoutLease(value: Any): Long {
    val id = StubIdGenerator.nextId()
    instances[id] = value
    return id
}
