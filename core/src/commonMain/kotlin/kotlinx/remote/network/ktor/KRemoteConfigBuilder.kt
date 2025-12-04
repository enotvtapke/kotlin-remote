package kotlinx.remote.network.ktor

import kotlinx.remote.classes.lease.LeaseConfig

class KRemoteConfigBuilder {
    var enableLeasing: Boolean = false
    var leaseConfig: LeaseConfig = LeaseConfig()
}
