package social.remote

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.remote
import kotlinx.remote.serialization.remoteSerializersModule
import kotlinx.serialization.json.Json

fun remoteServer(port: Int): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
    embeddedServer(Netty, port = port, watchPaths = listOf()) {
        install(KRemote) { callableMap = genCallableMap() }
        routing { remote("/call") }
    }
