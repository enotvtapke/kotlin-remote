package messagerPolling

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.remote.CallableMap
import kotlinx.remote.genCallableMap
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.network.serialization.setupExceptionSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

fun main() {
    CallableMap.putAll(genCallableMap())
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
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
    }.start(wait = true)
}
