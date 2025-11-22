package generated

import ServerConfig
import kotlinx.remote.*
import remoteEmbeddedServer
import kotlin.reflect.typeOf

fun main() {
    initCallableMap()
    remoteEmbeddedServer().start(wait = true)
}

fun initCallableMap() {
    CallableMap["generated.multiply"] = RemoteCallable(
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
    CallableMap["generated.multiplyStreaming"] = RemoteCallable(
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