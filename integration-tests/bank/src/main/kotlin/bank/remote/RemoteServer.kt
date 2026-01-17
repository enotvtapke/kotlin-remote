package bank.remote

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
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.KRemoteConfigBuilder
import kotlinx.remote.ktor.leaseRoutes
import kotlinx.remote.ktor.remote
import kotlinx.remote.serialization.throwableSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

fun remoteEmbeddedServer(
    nodeUrl: String,
    leaseConfig: LeaseConfig = LeaseConfig()
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    return embeddedServer(Netty, port = nodeUrl.split(":").last().toInt(), watchPaths = listOf()) {
        install(CallLogging)
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.application.environment.log.error("Unhandled exception during request", cause)
                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
            }
        }
        val config: KRemoteConfigBuilder.() -> Unit = {
            callableMap = genCallableMap()
            serializersModule = SerializersModule {
                polymorphic(Throwable::class) {
                    subclass(SerializationException::class, throwableSerializer(::SerializationException))
                }
            }
            classes {
                remoteClasses = genRemoteClassList()
                server {
                    this.leaseConfig = leaseConfig
                    this.nodeUrl = nodeUrl
                }
            }
        }
        val module = remoteSerializersModule(config).addPolymorphicRemoteClasses()
        install(ContentNegotiation) {
            json(Json { serializersModule = module })
        }
        install(KRemote, config)
        routing {
            remote("/call")
            leaseRoutes()
        }
    }
}
