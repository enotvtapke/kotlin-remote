package social.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.URLProtocol
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.KrpcRoute
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json

const val USER_PORT = 8101
const val POST_PORT = 8102
const val COMMENT_PORT = 8103
const val LIKE_PORT = 8104
const val FOLLOW_PORT = 8105
const val FEED_PORT = 8106
const val NOTIFICATION_PORT = 8107
const val SEARCH_PORT = 8108
const val WEB_BFF_PORT = 8201
const val MOBILE_BFF_PORT = 8202

fun rpcHttpClient(): HttpClient = HttpClient(CIO) { installKrpc() }

suspend fun rpcConn(httpClient: HttpClient, servicePort: Int) =
    httpClient.rpc {
        url {
            protocol = URLProtocol.WS
            host = "localhost"
            port = servicePort
            pathSegments = listOf("api")
        }
        rpcConfig { serialization { json() } }
    }

fun rpcServer(
    servicePort: Int,
    register: KrpcRoute.() -> Unit,
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
    embeddedServer(Netty, port = servicePort) {
        install(Krpc)
        routing {
            rpc("/api") {
                rpcConfig { serialization { json() } }
                register()
            }
        }
    }
