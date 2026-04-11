package kotlinx.remote.ktor

import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.RemoteSerializer
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass

fun simpleRemoteClassSerializersModule(
    remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>,
    leaseConfig: LeaseConfig = LeaseConfig(),
    leaseRenewalClientConfig: LeaseRenewalClientConfig = LeaseRenewalClientConfig(),
    nodeUrl: String? = null,
): SerializersModule = SerializersModule {
    remoteClasses.forEach { (clazz, stubFabric) ->
        contextual(clazz, RemoteSerializer(
            leaseManager = LeaseManager(leaseConfig, RemoteInstancesPool()),
            nodeUrl = nodeUrl,
            onStubDeserialization = startLeaseOnStubDeserialization(leaseRenewalClientConfig),
            stubFabric = stubFabric
        ))
    }
}
