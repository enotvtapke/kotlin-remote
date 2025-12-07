package generatedBodies

import ServerConfig
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.routing.routing
import kotlinx.remote.*
import kotlinx.remote.CallableMapClass
import kotlinx.remote.genCallableMap
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import remoteSerializersModule
import kotlin.reflect.typeOf
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main() {
    val callableMap = CallableMapClass(initCallableMap())
    embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ServerContentNegotiation) {
            json(Json {
                serializersModule = SerializersModule {
                }.remoteSerializersModule(callableMap, SerializersModule { })
            })
        }
        install(KRemote) {
            this.callableMap = callableMap
        }
        routing {
            remote("/call")
        }
    }.start(wait = true)
}

fun initCallableMap(): Map<String, RemoteCallable> {
    val callableMap = mutableMapOf<String, RemoteCallable>()
    callableMap["generated.multiply"] = RemoteCallable(
        name = "multiply",
        returnType = RemoteType(typeOf<Long>()),
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
    callableMap["generated.multiplyStreaming"] = RemoteCallable(
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
    return callableMap
}