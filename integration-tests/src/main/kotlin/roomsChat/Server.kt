package roomsChat

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.routing.routing
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.leaseRoutes
import kotlinx.remote.ktor.remote

fun main() {
    val port = 8080
    embeddedServer(Netty, port, watchPaths = listOf()) {
        install(CallLogging)
        install(KRemote) {
            callableMap = genCallableMap()
            classes {
                remoteClasses = genRemoteClassList()
                server {
                    nodeUrl = "http://localhost:$port"
                }
            }
        }
        routing {
            remote("/call")
            leaseRoutes()
        }
    }.start(wait = true)
}
