package kotlinx.remote.network.ktor

import kotlinx.remote.CallableMapClass
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager

class KRemoteConfigBuilder {
    var leaseConfig = LeaseConfig()
    lateinit var leaseManager: LeaseManager
    lateinit var callableMap: CallableMapClass
}
