import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.remote.CallableMapClass
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.genCallableMap
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.KRemoteServerPluginAttributesKey
import kotlinx.remote.network.ktor.leaseRoutes
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.network.leaseClient
import kotlinx.remote.network.remoteClient
import kotlinx.serialization.json.Json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

private val leaseRenewalClients = mutableMapOf<String, LeaseRenewalClient>()

fun getOrCreateLeaseRenewalClient(url: String, config: LeaseRenewalClientConfig = LeaseRenewalClientConfig(500)): LeaseRenewalClient {
    return leaseRenewalClients.getOrPut(url) {
        LeaseRenewalClient(
            config, HttpClient {
                defaultRequest {
                    url(url)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }
                install(ContentNegotiation) {
                    json()
                }
                install(Logging) {
                    level = LogLevel.BODY
                }
            }.leaseClient()
        ).also {
            it.startRenewalJob(CoroutineScope(Dispatchers.IO))
        }
    }
}

fun createOnStubDeserialization(config: LeaseRenewalClientConfig = LeaseRenewalClientConfig(500)): (Stub) -> Unit = { stub ->
    getOrCreateLeaseRenewalClient(stub.url, config).registerStub(stub)
}

data object ServerConfig : RemoteConfig {
    override val context = ServerContext
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModule(
                    remoteClasses = genRemoteClassList(),
                    callableMap = CallableMapClass(genCallableMap()),
                    leaseManager = null,
                    onStubDeserialization = createOnStubDeserialization(),
                )
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(CallableMapClass(genCallableMap()), "/call")
}

data object ServerContext : RemoteContext
data object ClientContext : RemoteContext

fun remoteEmbeddedServer(leaseConfig: LeaseConfig = LeaseConfig()): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    return embeddedServer(Netty, port = 8080, watchPaths = listOf()) {
        install(CallLogging)
        install(KRemote) {
            this.callableMap = CallableMapClass(genCallableMap())
            this.leaseConfig = leaseConfig
        }
        installRemoteServerContentNegotiation()
        routing {
            remote("/call")
            leaseRoutes()
        }
    }
}

fun Application.installRemoteServerContentNegotiation(
    nodeUrl: String = "http://localhost:8080",
    onStubDeserialization: ((Stub) -> Unit) = createOnStubDeserialization()
) {
    install(ServerContentNegotiation) {
        json(Json {
            val leaseManager = this@installRemoteServerContentNegotiation.attributes[KRemoteServerPluginAttributesKey].leaseManager
            val callableMap = this@installRemoteServerContentNegotiation.attributes[KRemoteServerPluginAttributesKey].callableMap
            serializersModule = remoteSerializersModule(
                remoteClasses = genRemoteClassList(),
                callableMap = callableMap,
                leaseManager = leaseManager,
                nodeUrl = nodeUrl,
                onStubDeserialization = onStubDeserialization
            )
        })
    }
}
