package kotlinx.remote.classes

import kotlinx.remote.classes.RemoteInstancesPool.instances
import kotlinx.remote.classes.lease.LeaseManager

object RemoteInstancesPool {
    val instances = InternalConcurrentHashMap<Long, Any>()

    @Suppress("UNUSED")
    fun getOrDefault(id: Long, default: Any?) = instances[id] ?: default
    
    /**
     * Remove an instance by its ID.
     * @return The removed instance, or null if not found
     */
    fun removeInstance(id: Long): Any? = instances.remove(id)
    
    /**
     * Check if an instance exists.
     */
    fun hasInstance(id: Long): Boolean = instances.containsKey(id)
    
    /**
     * Get the number of stored instances.
     */
    fun instanceCount(): Int = instances.entries.size
    
    /**
     * Clear all instances. Primarily for testing.
     */
    fun clear() {
        instances.clear()
    }
}

/**
 * Add an instance to the pool and create a lease for it.
 * The lease will be automatically managed by the LeaseManager.
 *
 * @param value The object instance to store
 * @param clientId Optional client identifier for lease tracking
 * @return The unique ID assigned to this instance
 */
fun addInstance(value: Any, clientId: String? = null): Long {
    val id = StubIdGenerator.nextId()
    instances[id] = value
    LeaseManager.createLease(id, clientId)
    return id
}

/**
 * Add an instance to the pool without creating a lease.
 * Use this for instances that should not be garbage collected.
 *
 * @param value The object instance to store
 * @return The unique ID assigned to this instance
 */
fun addInstanceWithoutLease(value: Any): Long {
    val id = StubIdGenerator.nextId()
    instances[id] = value
    return id
}
