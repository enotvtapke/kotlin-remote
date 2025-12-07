package kotlinx.remote.classes

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic

class RemoteInstancesPool {
    private val instances = InternalConcurrentHashMap<Long, Any>()

    private val idCounter: AtomicLong = atomic(0L)

    fun getOrDefault(id: Long, default: Any?) = instances[id] ?: default

    fun addInstance(value: Any): Long {
        return idCounter.incrementAndGet().also {
            instances[it] = value
        }
    }
    
    fun remove(id: Long): Any? = instances.remove(id)
    
    fun containsKey(id: Long): Boolean = instances.containsKey(id)
    
    fun instanceCount(): Int = instances.entries.size
    
    fun clear() {
        instances.clear()
    }
}
