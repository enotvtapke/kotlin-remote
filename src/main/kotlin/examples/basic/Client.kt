package org.jetbrains.kotlinx.examples.basic

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlinx.CallableMap
import org.jetbrains.kotlinx.RemoteContext
import org.jetbrains.kotlinx.RpcCallable
import org.jetbrains.kotlinx.RpcInvokator
import org.jetbrains.kotlinx.RemoteParameter
import org.jetbrains.kotlinx.RemoteType
import kotlin.reflect.typeOf

context(_: RemoteContext)
private suspend fun expression(a: Long, b: Long): Long {
    return a + multiply(a, b)
}

fun main() = runBlocking {
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
    with(object : RemoteContext {}) {
        println(expression(1, 2))
    }
}
