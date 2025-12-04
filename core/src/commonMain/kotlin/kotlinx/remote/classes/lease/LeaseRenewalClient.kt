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

object LeaseRenewalClient : SynchronizedObject() {
    private val trackedStubs = InternalConcurrentHashMap<Long, StubEntry>()
    private val config = atomic(LeaseRenewalClientConfig())
    private var renewalJob: Job? = null
    
    var renewalFunction: (suspend (LeaseRenewalRequest) -> LeaseRenewalResponse)? = null
    var releaseFunction: (suspend (LeaseReleaseRequest) -> Unit)? = null
    var onLeaseExpired: ((Long) -> Unit)? = null
    
    private data class StubEntry(
        val id: Long,
        val weakRef: Any,
        var lastRenewedMs: Long = 0
    )
    
    fun configure(newConfig: LeaseRenewalClientConfig) {
        config.value = newConfig
    }
    
    fun getConfig(): LeaseRenewalClientConfig = config.value
    
    fun registerStub(stub: Stub): Unit = synchronized(this) {
        trackedStubs.computeIfAbsent(stub.id) {
            StubEntry(stub.id, stub)
        }
    }
    
    fun registerStubId(id: Long): Unit = synchronized(this) {
        trackedStubs.computeIfAbsent(id) {
            StubEntry(id, Unit)
        }
    }
    
    suspend fun unregisterStub(stub: Stub, releaseOnServer: Boolean = true) {
        unregisterStubId(stub.id, releaseOnServer)
    }
    
    suspend fun unregisterStubId(id: Long, releaseOnServer: Boolean = true) {
        synchronized(this) {
            trackedStubs.remove(id)
        }
        if (releaseOnServer) {
            releaseFunction?.invoke(LeaseReleaseRequest(listOf(id), config.value.clientId))
        }
    }
    
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
    
    suspend fun renewAllLeases(): LeaseRenewalResponse? {
        val renewal = renewalFunction ?: return null
        val stubIds = synchronized(this) { trackedStubs.keys.toList() }
        if (stubIds.isEmpty()) return null
        
        val request = LeaseRenewalRequest(stubIds, config.value.clientId)
        val response = renewal(request)
        
        synchronized(this) {
            for (failedId in response.failedIds) {
                trackedStubs.remove(failedId)
            }
            
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
    
    fun startRenewalJob(coroutineScope: CoroutineScope) {
        stopRenewalJob()
        renewalJob = coroutineScope.launch {
            while (true) {
                delay(config.value.renewalIntervalMs)
                try {
                    renewAllLeases()
                } catch (_: Exception) {
                    // TODO Log error
                }
            }
        }
    }
    
    fun stopRenewalJob() {
        renewalJob?.cancel()
        renewalJob = null
    }
    
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
    
    fun clear(): Unit = synchronized(this) {
        trackedStubs.clear()
    }
    
    fun trackedCount(): Int = synchronized(this) {
        trackedStubs.entries.size
    }
    
    fun isTracking(id: Long): Boolean = synchronized(this) {
        trackedStubs.containsKey(id)
    }
}

data class LeaseRenewalClientConfig(
    val renewalIntervalMs: Long = DEFAULT_RENEWAL_INTERVAL_MS,
    val clientId: String? = null
) {
    companion object {
        const val DEFAULT_RENEWAL_INTERVAL_MS = 10_000L
    }
}
