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
    initCallableMap()
    with(object : RemoteContext {}) {
        println(expression(100, 600))
        multiplyStreaming(5, 6).collect { println(it) }
    }
}
