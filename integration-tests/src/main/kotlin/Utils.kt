import gc.CalculatorGC
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
import kotlinx.remote.CallableMapClass
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.RemoteSerializer
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.genCallableMap
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.network.leaseClient
import kotlinx.remote.network.remoteClient
import kotlinx.remote.network.serialization.RpcCallSerializer
import kotlinx.remote.network.serialization.setupExceptionSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

val leaseRenewalClient = LeaseRenewalClient(
    LeaseRenewalClientConfig(500), HttpClient {
        defaultRequest {
            url("http://localhost:8080")
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
)

data object ServerConfig : RemoteConfig {
    private val callableMap = CallableMapClass(genCallableMap())
    override val context = ServerContext
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                val remoteClassSerializersModule = SerializersModule {
                    contextual(
                        CalculatorGC::class, RemoteSerializer(
                            leaseRenewalClient = leaseRenewalClient,
                            stubFabric = { CalculatorGC.RemoteClassStub(it) }
                        )
                    )
                }
                serializersModule = SerializersModule{}.remoteSerializersModule(callableMap, remoteClassSerializersModule)
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(callableMap, "/call")
}

fun SerializersModule.remoteSerializersModule(
    callableMap: CallableMapClass,
    remoteClassSerializersModule: SerializersModule
): SerializersModule =
    this + remoteClassSerializersModule + SerializersModule {
        contextual(RemoteCall::class, RpcCallSerializer(callableMap, this@remoteSerializersModule + remoteClassSerializersModule))
        setupExceptionSerializers()
    }

data object ServerContext : RemoteContext
data object ClientContext : RemoteContext

fun remoteEmbeddedServer(): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val callableMap = CallableMapClass(genCallableMap())
    return embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ServerContentNegotiation) {
            json(Json {
                serializersModule = SerializersModule {
                }.remoteSerializersModule(callableMap, SerializersModule { })
            })
        }
        install(KRemote) {
            this.callableMap = callableMap
        }
        routing {
            remote("/call")
        }
    }
}
