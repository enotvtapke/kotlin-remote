import experiments.WrappedInt
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
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.network.leaseClient
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.leaseRoutes
import kotlinx.remote.ktor.remote
import kotlinx.remote.remoteClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val leaseRenewalClients = mutableMapOf<String, LeaseRenewalClient>()

fun getOrCreateLeaseRenewalClient(
    url: String,
    config: LeaseRenewalClientConfig = LeaseRenewalClientConfig(500)
): LeaseRenewalClient {
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

fun startLeaseOnStubDeserialization(config: LeaseRenewalClientConfig = LeaseRenewalClientConfig(5000)): (Stub) -> Unit =
    { stub ->
        getOrCreateLeaseRenewalClient(stub.url, config).registerStub(stub)
    }

data object ServerConfig : RemoteConfig {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModule {
                    callableMap = genCallableMap()
                    classes {
                        remoteClasses = genRemoteClassList()
                        client { }
                    }
                }
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(genCallableMap(), "/call")
}

fun remoteEmbeddedServer(
    nodeUrl: String = "http://localhost:8080",
    leaseConfig: LeaseConfig = LeaseConfig()
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    return embeddedServer(Netty, port = nodeUrl.split(":").last().toInt(), watchPaths = listOf()) {
        install(CallLogging)
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.application.environment.log.error("Unhandled exception during request", cause)
                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
            }
        }
        install(KRemote) {
            callableMap = genCallableMap()
            serializersModule = SerializersModule {
                polymorphic(Any::class) {
                    subclass(WrappedInt::class, WrappedInt.serializer())
                }
            }
            classes {
                remoteClasses = genRemoteClassList()
                server {
                    this.leaseConfig = leaseConfig
                    this.nodeUrl = nodeUrl
                }
            }
        }
        routing {
            remote("/call")
            leaseRoutes()
        }
    }
}
