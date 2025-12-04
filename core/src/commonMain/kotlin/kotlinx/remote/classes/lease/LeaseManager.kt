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

/**
 * Server-side manager for lease-based garbage collection of remote object instances.
 *
 * Lease-based GC works as follows:
 * 1. When a remote object is created and sent to a client, a lease is created
 * 2. The client must periodically renew the lease to keep the object alive
 * 3. When the lease expires (plus grace period), the object is eligible for cleanup
 * 4. A background cleanup task periodically removes expired instances
 */
object LeaseManager : SynchronizedObject() {
    private val leases = InternalConcurrentHashMap<Long, LeaseEntry>()
    private val config = atomic(LeaseConfig())
    
    private var cleanupJob: Job? = null
    
    /**
     * Time provider - can be overridden for testing.
     * Defaults to system time in milliseconds since epoch.
     */
    var timeProvider: TimeProvider = SystemTimeProvider
    
    /**
     * Internal entry tracking lease state for an instance.
     */
    private data class LeaseEntry(
        val instanceId: Long,
        var expirationTimeMs: Long,
        val clientIds: MutableSet<String> = mutableSetOf()
    )
    
    /**
     * Configure the lease manager with custom settings.
     */
    fun configure(newConfig: LeaseConfig) {
        config.value = newConfig
    }
    
    /**
     * Get the current configuration.
     */
    fun getConfig(): LeaseConfig = config.value
    
    /**
     * Create a new lease for a remote object instance.
     * Called when an object is serialized to be sent to a client.
     *
     * @param instanceId The ID of the remote object instance
     * @param clientId Optional identifier for the client
     * @return LeaseInfo with the lease details
     */
    fun createLease(instanceId: Long, clientId: String? = null): LeaseInfo = synchronized(this) {
        val currentConfig = config.value
        val expirationTime = timeProvider.currentTimeMillis() + currentConfig.leaseDurationMs
        
        val entry = leases.computeIfAbsent(instanceId) {
            LeaseEntry(instanceId, expirationTime)
        }
        
        // Update expiration time to the latest
        entry.expirationTimeMs = maxOf(entry.expirationTimeMs, expirationTime)
        
        // Track client if provided
        if (clientId != null) {
            entry.clientIds.add(clientId)
        }
        
        LeaseInfo(instanceId, entry.expirationTimeMs, clientId)
    }
    
    /**
     * Renew leases for multiple instances.
     *
     * @param request The renewal request containing instance IDs
     * @return Response with renewed leases and any failed IDs
     */
    fun renewLeases(request: LeaseRenewalRequest): LeaseRenewalResponse = synchronized(this) {
        val currentConfig = config.value
        val currentTime = timeProvider.currentTimeMillis()
        val renewedLeases = mutableListOf<LeaseInfo>()
        val failedIds = mutableListOf<Long>()
        
        for (instanceId in request.instanceIds) {
            val entry = leases[instanceId]
            if (entry != null && RemoteInstancesPool.instances.containsKey(instanceId)) {
                // Renew the lease
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
    
    /**
     * Release leases for multiple instances.
     * When all clients release their leases, the instance becomes eligible for cleanup.
     *
     * @param request The release request containing instance IDs
     */
    fun releaseLeases(request: LeaseReleaseRequest): Unit = synchronized(this) {
        for (instanceId in request.instanceIds) {
            val entry = leases[instanceId]
            if (entry != null) {
                if (request.clientId != null) {
                    entry.clientIds.remove(request.clientId)
                }
                // If no specific client or all clients released, mark for immediate expiration
                if (request.clientId == null || entry.clientIds.isEmpty()) {
                    entry.expirationTimeMs = 0L
                }
            }
        }
    }
    
    /**
     * Check if an instance has an active (non-expired) lease.
     */
    fun hasActiveLease(instanceId: Long): Boolean = synchronized(this) {
        val currentConfig = config.value
        val entry = leases[instanceId] ?: return false
        return entry.expirationTimeMs + currentConfig.gracePeriodMs > timeProvider.currentTimeMillis()
    }
    
    /**
     * Get lease information for an instance.
     */
    fun getLeaseInfo(instanceId: Long): LeaseInfo? = synchronized(this) {
        leases[instanceId]?.let { entry ->
            LeaseInfo(entry.instanceId, entry.expirationTimeMs)
        }
    }
    
    /**
     * Clean up expired instances.
     * Removes instances whose leases have expired (including grace period).
     *
     * @return Number of instances cleaned up
     */
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
    
    /**
     * Start the background cleanup job.
     * Should be called when the server starts.
     */
    fun startCleanupJob(coroutineScope: CoroutineScope) {
        stopCleanupJob()
        cleanupJob = coroutineScope.launch {
            while (true) {
                delay(config.value.cleanupIntervalMs)
                cleanupExpiredInstances()
            }
        }
    }
    
    /**
     * Stop the background cleanup job.
     * Should be called when the server shuts down.
     */
    fun stopCleanupJob() {
        cleanupJob?.cancel()
        cleanupJob = null
    }
    
    /**
     * Clear all leases and instances. Primarily for testing.
     */
    fun clear() = synchronized(this) {
        leases.clear()
        RemoteInstancesPool.instances.clear()
    }
    
    /**
     * Get the number of active leases.
     */
    fun leaseCount(): Int = synchronized(this) {
        leases.entries.size
    }
}
