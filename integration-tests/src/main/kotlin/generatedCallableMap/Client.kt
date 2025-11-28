package generatedCallableMap

import ClientContext
import kotlinx.coroutines.runBlocking
import kotlinx.remote.CallableMap
import kotlinx.remote.RemoteContext
import kotlinx.remote.genCallableMap

context(_: RemoteContext)
private suspend fun expression(a: Long, b: Long): Long {
    return a + multiply(a, b)
}

fun main(): Unit = runBlocking {
    CallableMap.putAll(genCallableMap())
    with(ClientContext) {
        println(expression(100, 600))
        multiplyStreaming(5, 6).collect { println(it) }
    }
}
