package kotlinx.remote.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.network.leaseClient
import kotlinx.remote.classes.remoteClassSerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass

fun ktorRemoteClassSerializersModule(
    remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>,
    leaseConfig: LeaseConfig = LeaseConfig(),
    leaseRenewalClientConfig: LeaseRenewalClientConfig = LeaseRenewalClientConfig(),
    nodeUrl: String? = null,
): SerializersModule = remoteClassSerializersModule(
    remoteClasses = remoteClasses,
    leaseManager = LeaseManager(leaseConfig, RemoteInstancesPool()),
    nodeUrl = nodeUrl,
    onStubDeserialization = startLeaseOnStubDeserialization(leaseRenewalClientConfig)
)

private fun <T> memo(f: (String) -> T): (String) -> T {
    val memoMap = mutableMapOf<String, T>()
    return {
        memoMap.getOrPut(it) { f(it) }
    }
}

fun startLeaseOnStubDeserialization(config: LeaseRenewalClientConfig): (Stub) -> Unit {
    val memoGetLeaseRenewalClient = memo { url ->
        LeaseRenewalClient(
            config, HttpClient {
                defaultRequest {
                    url(url)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }
            }.leaseClient()
        ).also {
            it.startRenewalJob(CoroutineScope(Dispatchers.Default))
        }
    }
    return { stub ->
        memoGetLeaseRenewalClient(stub.url).registerStub(stub)
    }
}
