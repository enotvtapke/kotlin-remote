package examples.basic

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteParameter
import kotlinx.remote.RemoteType
import kotlinx.remote.RpcCallable
import kotlinx.remote.RpcInvokator
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlin.reflect.typeOf

fun main() {
    initCallableMap()
    embeddedServer(Netty, port = 8080) {
        install(KRemote)

        routing {
            remote("/call")
        }
    }.start(wait = true)
}

fun initCallableMap() {
    CallableMap["multiply"] = RpcCallable(
        name = "multiply",
        returnType = RemoteType(typeOf<Long>()),
        invokator = RpcInvokator { args ->
            return@RpcInvokator with(ServerConfig.context) {
                multiply(args[0] as Long, args[1] as Long)
            }
        },
        parameters = arrayOf(
            RemoteParameter("lhs", RemoteType(typeOf<Long>()), false),
            RemoteParameter("rhs", RemoteType(typeOf<Long>()), false)
        ),
        returnsStream = false,
    )
    CallableMap["multiplyStreaming"] = RpcCallable(
        name = "multiplyStreaming",
        returnType = RemoteType(typeOf<Long>()),
        invokator = RpcInvokator { args ->
            return@RpcInvokator with(ServerConfig.context) {
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