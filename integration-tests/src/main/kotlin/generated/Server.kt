package generated

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteParameter
import kotlinx.remote.RemoteType
import kotlinx.remote.RemoteCallable
import kotlinx.remote.RemoteInvokator
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
    CallableMap["multiply"] = RemoteCallable(
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