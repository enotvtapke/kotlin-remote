package roomschat.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import roomschat.api.ChatService

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(Krpc)
        routing {
            rpc("/api") {
                rpcConfig { serialization { json() } }
                registerService<ChatService> { ctx -> ChatServiceImpl(ctx) }
            }
        }
    }.start(wait = true)
}
