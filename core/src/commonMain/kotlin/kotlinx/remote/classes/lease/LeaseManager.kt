package kotlinx.remote.classes.lease

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.remote.classes.InternalConcurrentHashMap
import kotlinx.remote.classes.RemoteInstancesPool

object LeaseManager : SynchronizedObject() {
    private val leases = InternalConcurrentHashMap<Long, LeaseEntry>()
    private val config = atomic(LeaseConfig())
    private var cleanupJob: Job? = null
    var timeProvider: TimeProvider = SystemTimeProvider
    
    private data class LeaseEntry(
        val instanceId: Long,
        var expirationTimeMs: Long,
        val clientIds: MutableSet<String> = mutableSetOf()
    )
    
    fun configure(newConfig: LeaseConfig) {
        config.value = newConfig
    }
    
    fun getConfig(): LeaseConfig = config.value
    
    fun createLease(instanceId: Long, clientId: String? = null): LeaseInfo = synchronized(this) {
        val currentConfig = config.value
        val expirationTime = timeProvider.currentTimeMillis() + currentConfig.leaseDurationMs
        
        val entry = leases.computeIfAbsent(instanceId) {
            LeaseEntry(instanceId, expirationTime)
        }
        
        entry.expirationTimeMs = maxOf(entry.expirationTimeMs, expirationTime)
        
        if (clientId != null) {
            entry.clientIds.add(clientId)
        }
        
        LeaseInfo(instanceId, entry.expirationTimeMs, clientId)
    }
    
    fun renewLeases(request: LeaseRenewalRequest): LeaseRenewalResponse = synchronized(this) {
        val currentConfig = config.value
        val currentTime = timeProvider.currentTimeMillis()
        val renewedLeases = mutableListOf<LeaseInfo>()
        val failedIds = mutableListOf<Long>()
        
        for (instanceId in request.instanceIds) {
            val entry = leases[instanceId]
            if (entry != null && RemoteInstancesPool.instances.containsKey(instanceId)) {
                entry.expirationTimeMs = currentTime + currentConfig.leaseDurationMs
                if (request.clientId != null) {
                    entry.clientIds.add(request.clientId)
                }
                renewedLeases.add(LeaseInfo(instanceId, entry.expirationTimeMs, request.clientId))
            } else {
                failedIds.add(instanceId)
            }
        }
        
        LeaseRenewalResponse(renewedLeases, failedIds)
    }
    
    fun releaseLeases(request: LeaseReleaseRequest): Unit = synchronized(this) {
        for (instanceId in request.instanceIds) {
            val entry = leases[instanceId]
            if (entry != null) {
                if (request.clientId != null) {
                    entry.clientIds.remove(request.clientId)
                }
                if (request.clientId == null || entry.clientIds.isEmpty()) {
                    entry.expirationTimeMs = 0L
                }
            }
        }
    }
    
    fun hasActiveLease(instanceId: Long): Boolean = synchronized(this) {
        val currentConfig = config.value
        val entry = leases[instanceId] ?: return false
        return entry.expirationTimeMs + currentConfig.gracePeriodMs > timeProvider.currentTimeMillis()
    }
    
    fun getLeaseInfo(instanceId: Long): LeaseInfo? = synchronized(this) {
        leases[instanceId]?.let { entry ->
            LeaseInfo(entry.instanceId, entry.expirationTimeMs)
        }
    }
    
    fun cleanupExpiredInstances(): Int = synchronized(this) {
        val currentConfig = config.value
        val currentTime = timeProvider.currentTimeMillis()
        val expiredIds = mutableListOf<Long>()
        
        for (entry in leases.entries) {
            val expirationWithGrace = entry.value.expirationTimeMs + currentConfig.gracePeriodMs
            if (expirationWithGrace <= currentTime) {
                expiredIds.add(entry.key)
            }
        }
        
        for (id in expiredIds) {
            leases.remove(id)
            RemoteInstancesPool.instances.remove(id)
        }
        
        expiredIds.size
    }
    
    fun startCleanupJob(coroutineScope: CoroutineScope) {
        stopCleanupJob()
        cleanupJob = coroutineScope.launch {
            while (true) {
                delay(config.value.cleanupIntervalMs)
                cleanupExpiredInstances()
            }
        }
    }
    
    fun stopCleanupJob() {
        cleanupJob?.cancel()
        cleanupJob = null
    }
    
    fun clear() = synchronized(this) {
        leases.clear()
        RemoteInstancesPool.instances.clear()
    }
    
    fun leaseCount(): Int = synchronized(this) {
        leases.entries.size
    }
}
