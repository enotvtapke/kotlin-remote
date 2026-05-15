package kotlinx.remote.ktor

import kotlinx.remote.classes.RemoteClassDescriptor
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.remoteClassSerializersModule
import kotlinx.serialization.modules.SerializersModule

fun ktorRemoteClassSerializersModule(
    remoteClasses: List<RemoteClassDescriptor<Any>>,
    leaseConfig: LeaseConfig = LeaseConfig(),
    leaseRenewalClientConfig: LeaseRenewalClientConfig = LeaseRenewalClientConfig(),
    nodeUrl: String? = null,
): SerializersModule = remoteClassSerializersModule(
    remoteClasses = remoteClasses,
    leaseManager = LeaseManager(leaseConfig, RemoteInstancesPool()),
    nodeUrl = nodeUrl,
    onStubDeserialization = KtorLeaseRenewalManager(leaseRenewalClientConfig).onStubDeserialization
)
