package manual

import ServerConfig
import experiments.exceptionSerializer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import kotlinx.remote.*
import kotlinx.remote.network.RemoteResponse
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.reflect.typeOf
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main() {
    initCallableMap()
    CallableMap.putAll(genCallableMap())
    embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ServerContentNegotiation) {
            json(Json {
                serializersModule = SerializersModule {
                    polymorphic(Exception::class) {
                        subclass(exceptionSerializer(::IllegalArgumentException))
                        subclass(exceptionSerializer(::IllegalStateException))
                        subclass(exceptionSerializer(::ArithmeticException))
                    }
                }
            })
        }
        install(KRemote)
        routing {
            remote("/call")
        }
    }.start(wait = true)
}

fun initCallableMap() {
    CallableMap["multiply"] = RemoteCallable(
        name = "multiply",
        returnType = RemoteType(typeOf<RemoteResponse<Long>>()),
        invokator = RemoteInvokator { args ->
            return@RemoteInvokator with(ServerConfig.context) {
                multiply(args[0] as Long, args[1] as Long)
            }
        },
        parameters = arrayOf(
            RemoteParameter("lhs", RemoteType(typeOf<Long>()), false),
            RemoteParameter("rhs", RemoteType(typeOf<Long>()), false)
        ),
        returnsStream = false,
    )
    CallableMap["multiplyStreaming"] = RemoteCallable(
        name = "multiplyStreaming",
        returnType = RemoteType(typeOf<Long>()),
        invokator = RemoteInvokator { args ->
            return@RemoteInvokator with(ServerConfig.context) {
                multiplyStreaming(args[0] as Long, args[1] as Long)
            }
        },
        parameters = arrayOf(
            RemoteParameter("lhs", RemoteType(typeOf<Long>()), false),
            RemoteParameter("rhs", RemoteType(typeOf<Long>()), false)
        ),
        returnsStream = true,
    )
}