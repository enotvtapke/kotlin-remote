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
    
    private class StubEntry(
        val id: Long,
        stub: Stub
    ) {
        val weakRef = WeakRef(stub)
        var lastRenewedMs: Long = 0
    }
    
    fun configure(newConfig: LeaseRenewalClientConfig) {
        config.value = newConfig
    }
    
    fun getConfig(): LeaseRenewalClientConfig = config.value
    
    fun registerStub(stub: Stub): Unit = synchronized(this) {
        trackedStubs.computeIfAbsent(stub.id) {
            StubEntry(stub.id, stub)
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
    
    private fun collectGarbageAndGetActiveIds(): Pair<List<Long>, List<Long>> = synchronized(this) {
        val activeIds = mutableListOf<Long>()
        val collectedIds = mutableListOf<Long>()
        
        for (entry in trackedStubs.entries) {
            if (entry.value.weakRef.get() != null) {
                activeIds.add(entry.key)
            } else {
                collectedIds.add(entry.key)
            }
        }
        
        for (id in collectedIds) {
            trackedStubs.remove(id)
        }
        
        activeIds to collectedIds
    }
    
    suspend fun renewAllLeases(): LeaseRenewalResponse? {
        val renewal = renewalFunction ?: return null
        
        val (activeIds, collectedIds) = collectGarbageAndGetActiveIds()
        
        if (collectedIds.isNotEmpty()) {
            releaseFunction?.invoke(LeaseReleaseRequest(collectedIds, config.value.clientId))
        }
        
        if (activeIds.isEmpty()) return null
        
        val request = LeaseRenewalRequest(activeIds, config.value.clientId)
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
