package org.jetbrains.kotlinx.examples.basic

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.jetbrains.kotlinx.*
import org.jetbrains.kotlinx.network.ktor.KRemote
import org.jetbrains.kotlinx.network.ktor.remote
import kotlin.reflect.typeOf

fun main() {
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
    )

    embeddedServer(Netty, port = 8080) {
        install(KRemote)

        routing {
            remote("/call")
        }
    }.start(wait = true)
}
