package kotlinx.remote.ktor

import kotlinx.remote.CallableMap
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass

class KRemoteConfigBuilder {
    class KRemoteClassesConfigBuilder {
        class KRemoteClassesDeserializationConfigBuilder {
            var leaseRenewalClientConfig: LeaseRenewalClientConfig? = null
            var onStubDeserialization: ((Stub) -> Unit)? = null

            internal fun build(): KRemoteClassesDeserializationConfig {
                return KRemoteClassesDeserializationConfig(
                    onStubDeserialization ?: startLeaseOnStubDeserialization(
                        leaseRenewalClientConfig ?: LeaseRenewalClientConfig()
                    )
                )
            }
        }

        class KRemoteClassesSerializationConfigBuilder {
            var leaseConfig: LeaseConfig? = null
            var leaseManager: LeaseManager? = null
            var nodeUrl: String? = null

            internal fun build(): KRemoteClassesSerializationConfig {
                return KRemoteClassesSerializationConfig(
                    leaseManager ?: LeaseManager(leaseConfig ?: LeaseConfig(), RemoteInstancesPool()),
                    nodeUrl ?: error("Specify node url for classes server config")
                )
            }
        }

        var remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>? = null
        private var clientConfig: KRemoteClassesDeserializationConfig? = null
        private var serverConfig: KRemoteClassesSerializationConfig? = null

        fun deserialization(block: KRemoteClassesDeserializationConfigBuilder.() -> Unit) {
            val builder = KRemoteClassesDeserializationConfigBuilder()
            clientConfig = builder.apply {
                block()
            }.build()
        }

        fun serialization(block: KRemoteClassesSerializationConfigBuilder.() -> Unit) {
            val builder = KRemoteClassesSerializationConfigBuilder()
            serverConfig = builder.apply {
                block()
            }.build()
        }

        internal fun build(): KRemoteClassesConfig {
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

    private var classesConfig: KRemoteClassesConfig? = null

    var callableMap: CallableMap? = null

    var serializersModule: SerializersModule? = null

    internal fun build(): KRemoteConfig {
        return KRemoteConfig(
            callableMap ?: error("Specify callable map for KRemoteConfig (use genCallableMap())"),
            serializersModule,
            classesConfig
        )
    }
}

internal data class KRemoteConfig(
    val callableMap: CallableMap,
    var serializersModule: SerializersModule?,
    val classes: KRemoteClassesConfig? = null,
)

internal data class KRemoteClassesConfig(
    val remoteClasses: List<Pair<KClass<Any>, (Long, String) -> Any>>,
    val client: KRemoteClassesDeserializationConfig?,
    val server: KRemoteClassesSerializationConfig?,
)

internal data class KRemoteClassesDeserializationConfig(
    val onStubDeserialization: ((Stub) -> Unit)
)

internal data class KRemoteClassesSerializationConfig(
    val leaseManager: LeaseManager,
    val nodeUrl: String,
)
