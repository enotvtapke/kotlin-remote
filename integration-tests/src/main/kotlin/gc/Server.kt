package gc

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import kotlinx.remote.CallableMap
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.genCallableMap
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.leaseRoutes
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.network.serialization.setupExceptionSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main() {
    CallableMap.putAll(genCallableMap())
    embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ServerContentNegotiation) {
            json(Json {
                serializersModule = SerializersModule {
                    setupExceptionSerializers()
                }
            })
        }
        install(KRemote) {
            enableLeasing = true
            leaseConfig = LeaseConfig(2000, 2000, 2000)
        }
        routing {
            remote("/call")
            leaseRoutes()
        }
    }.start(wait = true)
}
