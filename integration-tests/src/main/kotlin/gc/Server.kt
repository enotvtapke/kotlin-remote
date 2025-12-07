package gc

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import kotlinx.remote.CallableMapClass
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.RemoteSerializer
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.genCallableMap
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.leaseRoutes
import kotlinx.remote.network.ktor.remote
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import remoteSerializersModule
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main() {
    val leaseManager = LeaseManager(LeaseConfig(2000, 200, 0), RemoteInstancesPool())
    val remoteClassSerializersModule =
        SerializersModule { contextual(CalculatorGC::class, RemoteSerializer(leaseManager = leaseManager)) }
    val callableMap = CallableMapClass(genCallableMap())
    val remoteSerializersModule = SerializersModule{}.remoteSerializersModule(callableMap, remoteClassSerializersModule)
    embeddedServer(Netty, port = 8080, watchPaths = listOf()) {
        install(CallLogging)
        install(ServerContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModule
            })
        }
        install(KRemote) {
            this.callableMap = callableMap
            this.leaseManager = leaseManager
        }
        routing {
            remote("/call")
            leaseRoutes()
        }
    }.start(wait = true)
}
