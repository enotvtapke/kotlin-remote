package auth

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.remote.CallableMapClass
import kotlinx.remote.genCallableMap
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.handleRemoteCall
import kotlinx.remote.network.ktor.remote
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import remoteSerializersModule

fun main() {
    authRemoteEmbeddedServer().start(wait = true)
}

fun authRemoteEmbeddedServer(): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val callableMapClass = CallableMapClass(genCallableMap())
    return embeddedServer(Netty, port = 8080) {
        install(Authentication) {
            basic("auth-basic") {
                realm = "Access to the Super Secret Base"
                validate { credentials ->
                    if (credentials.name == "user" && credentials.password == "password") {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }
        install(CallLogging)
        install(ContentNegotiation) {
            json(Json {
                serializersModule = SerializersModule {}.remoteSerializersModule(callableMapClass, SerializersModule { })
            })
        }
        install(KRemote) {
            callableMap = callableMapClass
        }
        routing {
            authenticate("auth-basic") {
                post("/callAuth") {
                    val principal = call.principal<UserIdPrincipal>()
                    println("Welcome to the protected route, ${principal?.name}!")
                    handleRemoteCall()
                }
            }
            remote("/call")
        }
    }
}
