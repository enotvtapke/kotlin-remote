package kotlinx.remote.classes.lease

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.remote.classes.InternalConcurrentHashMap
import kotlinx.remote.classes.Stub
import kotlinx.remote.network.LeaseClient

class LeaseRenewalClient(private val config: LeaseRenewalClientConfig, private val leaseClient: LeaseClient, val onLeaseExpired: ((Long) -> Unit) = {}) : SynchronizedObject() {
    private val trackedStubs = InternalConcurrentHashMap<Long, StubEntry>()
    private var renewalJob: Job? = null

    private class StubEntry(
        val id: Long,
        stub: Stub,
        var lastRenewedMs: Long = 0,
    ) {
        val weakRef = WeakRef(stub)

    }

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
            leaseClient.releaseLeases(LeaseReleaseRequest(listOf(id), config.clientId))
        }
    }
    
    suspend fun unregisterStubIds(ids: List<Long>, releaseOnServer: Boolean = true) {
        synchronized(this) {
            for (id in ids) {
                trackedStubs.remove(id)
            }
        }
        if (releaseOnServer && ids.isNotEmpty()) {
            leaseClient.releaseLeases(LeaseReleaseRequest(ids, config.clientId))
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
        val (activeIds, collectedIds) = collectGarbageAndGetActiveIds()
        
        if (collectedIds.isNotEmpty()) {
            leaseClient.releaseLeases(LeaseReleaseRequest(collectedIds, config.clientId))
        }
        
        if (activeIds.isEmpty()) return null
        
        val request = LeaseRenewalRequest(activeIds, config.clientId)
        val response = leaseClient.renewLeases(request)
        
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
            onLeaseExpired.invoke(failedId)
        }
        
        return response
    }
    
    fun startRenewalJob(coroutineScope: CoroutineScope) {
        stopRenewalJob()
        renewalJob = coroutineScope.launch {
            while (true) {
                delay(config.renewalIntervalMs)
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
            leaseClient.releaseLeases(LeaseReleaseRequest(allIds, config.clientId))
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
