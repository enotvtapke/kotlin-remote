package pubsub.remote

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.remote
import kotlinx.remote.serialization.remoteSerializersModule
import kotlinx.serialization.json.Json

fun pubsubServer(port: Int): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
    embeddedServer(Netty, port = port, watchPaths = listOf()) {
        install(CallLogging)
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.application.environment.log.error("Unhandled exception", cause)
                call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Internal Server Error")
            }
        }
        install(ContentNegotiation) {
            json(Json { serializersModule = remoteSerializersModule(genCallableMap()) })
        }
        install(KRemote) { callableMap = genCallableMap() }
        routing { remote("/call") }
    }
