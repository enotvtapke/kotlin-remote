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
import kotlinx.remote.*
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.network.leaseClient
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.KRemoteServerPluginAttributesKey
import kotlinx.remote.ktor.leaseRoutes
import kotlinx.remote.ktor.remote
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

fun startLeaseOnStubDeserialization(config: LeaseRenewalClientConfig = LeaseRenewalClientConfig(5000)): (Stub) -> Unit = { stub ->
    getOrCreateLeaseRenewalClient(stub.url, config).registerStub(stub)
}

data object ServerContext : NonlocalContext() {
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
                    onStubDeserialization = startLeaseOnStubDeserialization(),
                )
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(CallableMapClass(genCallableMap()), "/call")
}

fun remoteEmbeddedServer(nodeUrl: String = "http://localhost:8080", leaseConfig: LeaseConfig = LeaseConfig()): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    return embeddedServer(Netty, port = nodeUrl.split(":").last().toInt(), watchPaths = listOf()) {
        install(CallLogging)
        install(KRemote) {
            this.callableMap = CallableMapClass(genCallableMap())
            this.leaseConfig = leaseConfig
        }
        installRemoteServerContentNegotiation(nodeUrl)
        routing {
            remote("/call")
            leaseRoutes()
        }
    }
}

fun Application.installRemoteServerContentNegotiation(
    nodeUrl: String = "http://localhost:8080",
    onStubDeserialization: ((Stub) -> Unit) = startLeaseOnStubDeserialization()
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
