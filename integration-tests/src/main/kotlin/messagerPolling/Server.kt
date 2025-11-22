package messagerPolling

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteParameter
import kotlinx.remote.RemoteType
import kotlinx.remote.RemoteCallable
import kotlinx.remote.RemoteInvokator
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlin.reflect.typeOf

fun main() {
    CallableMap.init()
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        install(KRemote)
        routing {
            remote("/call")
        }
    }.start(wait = true)
}
