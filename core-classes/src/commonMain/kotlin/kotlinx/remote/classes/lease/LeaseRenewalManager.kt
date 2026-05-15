package kotlinx.remote.classes.lease

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.remote.classes.InternalConcurrentHashMap
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.network.LeaseClient

abstract class LeaseRenewalManager(
    private val config: LeaseRenewalClientConfig = LeaseRenewalClientConfig(),
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val renewalClients = InternalConcurrentHashMap<String, LeaseRenewalClient>()

    protected abstract fun createLeaseClient(url: String): LeaseClient

    val onStubDeserialization: (Stub) -> Unit = { stub ->
        renewalClients.computeIfAbsent(stub.url) {
            LeaseRenewalClient(config, createLeaseClient(stub.url))
                .also { it.startRenewalJob(scope) }
        }.registerStub(stub)
    }

    open fun stop() {
        for (client in renewalClients.values) client.stopRenewalJob()
        renewalClients.clear()
        scope.cancel()
    }
}
