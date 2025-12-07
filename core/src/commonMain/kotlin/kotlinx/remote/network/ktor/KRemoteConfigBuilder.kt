package kotlinx.remote.network.ktor

import kotlinx.remote.CallableMapClass
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager

class KRemoteConfigBuilder {
    var leaseManager: LeaseManager = LeaseManager(LeaseConfig(), RemoteInstancesPool())
    lateinit var callableMap: CallableMapClass
}
