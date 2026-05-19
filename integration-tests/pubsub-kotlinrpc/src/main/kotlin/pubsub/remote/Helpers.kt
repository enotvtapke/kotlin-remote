package pubsub.remote

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

const val BUS_PORT = 8090

fun rpcHttpClient(): HttpClient = HttpClient(CIO) { installKrpc() }

suspend fun rpcConn(httpClient: HttpClient, port: Int) =
    httpClient.rpc {
        url {
            protocol = URLProtocol.WS
            host = "localhost"
            this.port = port
            pathSegments = listOf("api")
        }
        rpcConfig { serialization { json() } }
    }

fun rpcServer(
    port: Int,
    register: KrpcRoute.() -> Unit,
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
    embeddedServer(Netty, port = port) {
        install(Krpc)
        routing {
            rpc("/api") {
                rpcConfig { serialization { json() } }
                register()
            }
        }
    }
