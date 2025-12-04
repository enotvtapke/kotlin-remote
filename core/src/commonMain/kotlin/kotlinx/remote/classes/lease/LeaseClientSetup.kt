package kotlinx.remote.classes.lease

import kotlinx.coroutines.CoroutineScope
import kotlinx.remote.network.LeaseClient

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

fun startLeaseRenewal(
    leaseClient: LeaseClient,
    coroutineScope: CoroutineScope,
    config: LeaseRenewalClientConfig = LeaseRenewalClientConfig()
) {
    setupLeaseRenewal(leaseClient, config)
    LeaseRenewalClient.startRenewalJob(coroutineScope)
}

suspend fun stopLeaseRenewal(releaseLeases: Boolean = true) {
    if (releaseLeases) {
        LeaseRenewalClient.shutdown()
    } else {
        LeaseRenewalClient.stopRenewalJob()
        LeaseRenewalClient.clear()
    }
}
