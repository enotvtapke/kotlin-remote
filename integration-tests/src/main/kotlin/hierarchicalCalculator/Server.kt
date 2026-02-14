package hierarchicalCalculator

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.remote

fun main() {
    embeddedServer(Netty, port = 8002) {
        install(CallLogging)
        install(KRemote) {
            callableMap = genCallableMap()
        }
        routing {
            remote("/call")
        }
    }.start(wait = true)
}