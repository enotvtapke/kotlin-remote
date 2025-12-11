package manualFunctionCalling

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import kotlinx.remote.*
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.remote
import kotlin.reflect.typeOf

fun main() {
    val callableMap = CallableMap(manualCallableMap())
    embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(KRemote) {
            this.callableMap = callableMap
        }
//        installRemoteServerContentNegotiation()
        routing {
            remote("/call")
        }
    }.start(wait = true)
}

fun manualCallableMap(): Map<String, RemoteCallable> {
    val callableMap = mutableMapOf<String, RemoteCallable>()
    callableMap["multiply"] = RemoteCallable(
        name = "multiply",
        returnType = RemoteType(typeOf<RemoteResponse<Long>>()),
        invokator = RemoteInvokator { args ->
            return@RemoteInvokator context(DefaultLocalContext) {
                multiply(args[0] as Long, args[1] as Long)
            }
        },
        parameters = arrayOf(
            RemoteParameter("lhs", RemoteType(typeOf<Long>()), false),
            RemoteParameter("rhs", RemoteType(typeOf<Long>()), false)
        ),
    )
    return callableMap
}