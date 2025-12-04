package kotlinx.remote.classes.lease

import kotlinx.coroutines.CoroutineScope
import kotlinx.remote.network.LeaseClient

/**
 * Wire up the LeaseRenewalClient with a LeaseClient for automatic lease renewal.
 *
 * This sets up the renewal and release functions on the LeaseRenewalClient
 * to use the provided LeaseClient for network operations.
 *
 * @param leaseClient The client for making lease network calls
 * @param config Optional configuration for the renewal client
 */
fun setupLeaseRenewal(
    leaseClient: LeaseClient,
    config: LeaseRenewalClientConfig = LeaseRenewalClientConfig()
) {
    LeaseRenewalClient.configure(config)
    LeaseRenewalClient.renewalFunction = { request ->
        leaseClient.renewLeases(request)
    }
    LeaseRenewalClient.releaseFunction = { request ->
        leaseClient.releaseLeases(request)
    }
}

/**
 * Start automatic lease renewal for remote stubs.
 *
 * @param leaseClient The client for making lease network calls
 * @param coroutineScope The coroutine scope for the renewal job
 * @param config Optional configuration for the renewal client
 */
fun startLeaseRenewal(
    leaseClient: LeaseClient,
    coroutineScope: CoroutineScope,
    config: LeaseRenewalClientConfig = LeaseRenewalClientConfig()
) {
    setupLeaseRenewal(leaseClient, config)
    LeaseRenewalClient.startRenewalJob(coroutineScope)
}

/**
 * Stop automatic lease renewal and optionally release all tracked leases.
 *
 * @param releaseLeases Whether to send release requests for all tracked stubs
 */
suspend fun stopLeaseRenewal(releaseLeases: Boolean = true) {
    if (releaseLeases) {
        LeaseRenewalClient.shutdown()
    } else {
        LeaseRenewalClient.stopRenewalJob()
        LeaseRenewalClient.clear()
    }
}

