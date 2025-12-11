package kotlinx.remote.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.remote.CallableMap
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.network.leaseClient
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass

class KRemoteConfigBuilder {
    class KRemoteClassesConfigBuilder {
        var remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>? = null
        private var clientConfig: KRemoteClassesClientConfig? = null
        private var serverConfig: KRemoteClassesServerConfig? = null
        fun client(block: KRemoteClassesClientConfigBuilder.() -> Unit) {
            val builder = KRemoteClassesClientConfigBuilder()
            clientConfig = builder.apply {
                block()
            }.build()
        }

        fun server(block: KRemoteClassesServerConfigBuilder.() -> Unit) {
            val builder = KRemoteClassesServerConfigBuilder()
            serverConfig = builder.apply {
                block()
            }.build()
        }

        class KRemoteClassesClientConfigBuilder {
            var leaseRenewalClientConfig: LeaseRenewalClientConfig? = null
            var onStubDeserialization: ((Stub) -> Unit)? = null
            fun build(): KRemoteClassesClientConfig {
                return KRemoteClassesClientConfig(
                    onStubDeserialization ?: startLeaseOnStubDeserialization(
                        leaseRenewalClientConfig ?: LeaseRenewalClientConfig()
                    )
                )
            }
        }

        class KRemoteClassesServerConfigBuilder {
            var leaseConfig: LeaseConfig? = null
            var leaseManager: LeaseManager? = null
            var nodeUrl: String? = null

            fun build(): KRemoteClassesServerConfig {
                return KRemoteClassesServerConfig(
                    leaseManager ?: LeaseManager(leaseConfig ?: LeaseConfig(), RemoteInstancesPool()),
                    nodeUrl ?: error("Specify node url for classes server config")
                )
            }
        }

        fun build(): KRemoteClassesConfig {
            return KRemoteClassesConfig(
                remoteClasses = remoteClasses ?: error("Specify remote classes for classes config (use genRemoteClassList())"),
                client = clientConfig,
                server = serverConfig
            )
        }
    }

    fun classes(block: KRemoteClassesConfigBuilder.() -> Unit) {
        val builder = KRemoteClassesConfigBuilder()
        classesConfig = builder.apply {
            block()
        }.build()
    }

    var classesConfig: KRemoteClassesConfig? = null

    var callableMap: CallableMap? = null

    var serializersModule: SerializersModule? = null

    fun build(): KRemoteConfig {
        return KRemoteConfig(
            callableMap ?: error("Specify callable map for KRemoteConfig (use genCallableMap())"),
            serializersModule,
            classesConfig
        )
    }
}

data class KRemoteConfig(
    val callableMap: CallableMap,
    var serializersModule: SerializersModule?,
    val classes: KRemoteClassesConfig? = null,
)

data class KRemoteClassesConfig(
    val remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>,
    val client: KRemoteClassesClientConfig?,
    val server: KRemoteClassesServerConfig?,
)

data class KRemoteClassesClientConfig(
    val onStubDeserialization: ((Stub) -> Unit)
)

data class KRemoteClassesServerConfig(
    val leaseManager: LeaseManager,
    val nodeUrl: String,
)

private fun <T> memo(f: (String) -> T): (String) -> T {
    val memoMap = mutableMapOf<String, T>()
    return {
        memoMap.getOrPut(it) { f(it) }
    }
}

private fun startLeaseOnStubDeserialization(config: LeaseRenewalClientConfig): (Stub) -> Unit {
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
