package kotlinx.remote.classes.lease

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.remote.classes.InternalConcurrentHashMap
import kotlinx.remote.classes.Stub

/**
 * Client-side manager for tracking and renewing leases for remote object stubs.
 *
 * This component tracks all Stub instances that the client holds references to
 * and periodically sends renewal requests to the server to keep the remote objects alive.
 *
 * Usage:
 * 1. Configure with a RemoteClient or custom renewal function
 * 2. Call registerStub() when receiving a new stub from a remote call
 * 3. Call unregisterStub() when done with a stub (optional, for explicit cleanup)
 * 4. Call startRenewalJob() to begin automatic lease renewal
 * 5. Call stopRenewalJob() when shutting down
 */
object LeaseRenewalClient : SynchronizedObject() {
    private val trackedStubs = InternalConcurrentHashMap<Long, StubEntry>()
    private val config = atomic(LeaseRenewalClientConfig())
    
    private var renewalJob: Job? = null
    
    /**
     * Function to call for lease renewal. Should be set before starting renewal.
     */
    var renewalFunction: (suspend (LeaseRenewalRequest) -> LeaseRenewalResponse)? = null
    
    /**
     * Function to call for explicit lease release. Optional.
     */
    var releaseFunction: (suspend (LeaseReleaseRequest) -> Unit)? = null
    
    /**
     * Callback invoked when a stub's lease fails to renew (instance was garbage collected).
     */
    var onLeaseExpired: ((Long) -> Unit)? = null
    
    private data class StubEntry(
        val id: Long,
        val weakRef: Any, // The stub itself or a weak reference to it
        var lastRenewedMs: Long = 0
    )
    
    /**
     * Configure the lease renewal client.
     */
    fun configure(newConfig: LeaseRenewalClientConfig) {
        config.value = newConfig
    }
    
    /**
     * Get the current configuration.
     */
    fun getConfig(): LeaseRenewalClientConfig = config.value
    
    /**
     * Register a stub for automatic lease renewal.
     * Should be called when a stub is created/received from a remote call.
     *
     * @param stub The stub to track
     */
    fun registerStub(stub: Stub): Unit = synchronized(this) {
        trackedStubs.computeIfAbsent(stub.id) {
            StubEntry(stub.id, stub)
        }
    }
    
    /**
     * Register a stub ID for automatic lease renewal.
     *
     * @param id The stub ID to track
     */
    fun registerStubId(id: Long): Unit = synchronized(this) {
        trackedStubs.computeIfAbsent(id) {
            StubEntry(id, Unit) // No reference, just track the ID
        }
    }
    
    /**
     * Unregister a stub from automatic lease renewal.
     * Optionally sends a release request to the server.
     *
     * @param stub The stub to stop tracking
     * @param releaseOnServer Whether to send a release request to the server
     */
    suspend fun unregisterStub(stub: Stub, releaseOnServer: Boolean = true) {
        unregisterStubId(stub.id, releaseOnServer)
    }
    
    /**
     * Unregister a stub ID from automatic lease renewal.
     *
     * @param id The stub ID to stop tracking
     * @param releaseOnServer Whether to send a release request to the server
     */
    suspend fun unregisterStubId(id: Long, releaseOnServer: Boolean = true) {
        synchronized(this) {
            trackedStubs.remove(id)
        }
        if (releaseOnServer) {
            releaseFunction?.invoke(LeaseReleaseRequest(listOf(id), config.value.clientId))
        }
    }
    
    /**
     * Unregister multiple stub IDs at once.
     *
     * @param ids The stub IDs to stop tracking
     * @param releaseOnServer Whether to send a release request to the server
     */
    suspend fun unregisterStubIds(ids: List<Long>, releaseOnServer: Boolean = true) {
        synchronized(this) {
            for (id in ids) {
                trackedStubs.remove(id)
            }
        }
        if (releaseOnServer && ids.isNotEmpty()) {
            releaseFunction?.invoke(LeaseReleaseRequest(ids, config.value.clientId))
        }
    }
    
    /**
     * Perform a single renewal cycle for all tracked stubs.
     * Called automatically by the renewal job, but can also be called manually.
     *
     * @return The renewal response, or null if no stubs to renew or renewal function not set
     */
    suspend fun renewAllLeases(): LeaseRenewalResponse? {
        val renewal = renewalFunction ?: return null
        val stubIds = synchronized(this) { trackedStubs.keys.toList() }
        if (stubIds.isEmpty()) return null
        
        val request = LeaseRenewalRequest(stubIds, config.value.clientId)
        val response = renewal(request)
        
        // Handle failed renewals (instances that were garbage collected)
        synchronized(this) {
            for (failedId in response.failedIds) {
                trackedStubs.remove(failedId)
            }
            
            // Update last renewed time for successful renewals
            val currentTime = SystemTimeProvider.currentTimeMillis()
            for (lease in response.renewedLeases) {
                trackedStubs[lease.instanceId]?.lastRenewedMs = currentTime
            }
        }
        
        for (failedId in response.failedIds) {
            onLeaseExpired?.invoke(failedId)
        }
        
        return response
    }
    
    /**
     * Start the automatic lease renewal job.
     *
     * @param coroutineScope The coroutine scope to use for the renewal job
     */
    fun startRenewalJob(coroutineScope: CoroutineScope) {
        stopRenewalJob()
        renewalJob = coroutineScope.launch {
            while (true) {
                delay(config.value.renewalIntervalMs)
                try {
                    renewAllLeases()
                } catch (e: Exception) {
                    // Log error but continue - network issues shouldn't stop renewal attempts
                    // In production, you might want a more sophisticated error handling strategy
                }
            }
        }
    }
    
    /**
     * Stop the automatic lease renewal job.
     */
    fun stopRenewalJob() {
        renewalJob?.cancel()
        renewalJob = null
    }
    
    /**
     * Release all tracked stubs and stop renewal.
     * Should be called when the client is shutting down.
     */
    suspend fun shutdown() {
        stopRenewalJob()
        val allIds = synchronized(this) {
            val ids = trackedStubs.keys.toList()
            trackedStubs.clear()
            ids
        }
        if (allIds.isNotEmpty()) {
            releaseFunction?.invoke(LeaseReleaseRequest(allIds, config.value.clientId))
        }
    }
    
    /**
     * Clear all tracked stubs without sending release requests.
     * Primarily for testing.
     */
    fun clear(): Unit = synchronized(this) {
        trackedStubs.clear()
    }
    
    /**
     * Get the number of currently tracked stubs.
     */
    fun trackedCount(): Int = synchronized(this) {
        trackedStubs.entries.size
    }
    
    /**
     * Check if a specific stub ID is being tracked.
     */
    fun isTracking(id: Long): Boolean = synchronized(this) {
        trackedStubs.containsKey(id)
    }
}

/**
 * Configuration for the client-side lease renewal.
 */
data class LeaseRenewalClientConfig(
    /**
     * Interval between renewal requests in milliseconds.
     * Should be significantly less than the server's lease duration.
     */
    val renewalIntervalMs: Long = DEFAULT_RENEWAL_INTERVAL_MS,
    
    /**
     * Optional client identifier for lease tracking.
     */
    val clientId: String? = null
) {
    companion object {
        const val DEFAULT_RENEWAL_INTERVAL_MS = 10_000L // 10 seconds (1/3 of default lease duration)
    }
}
