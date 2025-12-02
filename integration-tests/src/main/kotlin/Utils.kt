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
import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.genCallableMap
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.network.remoteClient
import kotlinx.remote.network.serialization.setupExceptionSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

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
                serializersModule = SerializersModule {
                    setupExceptionSerializers()
                }
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient("/call")
}

data object ServerContext : RemoteContext
data object ClientContext : RemoteContext

fun remoteEmbeddedServer(): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    CallableMap.putAll(genCallableMap())
    return embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ServerContentNegotiation) {
            json(Json {
                serializersModule = SerializersModule {
                    setupExceptionSerializers()
                }
            })
        }
        install(KRemote)
        routing {
            remote("/call")
        }
    }
}
