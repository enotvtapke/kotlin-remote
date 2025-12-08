package manualFunctionCalling

import ServerContext
import kotlinx.coroutines.runBlocking
import kotlinx.remote.RemoteContext

context(_: RemoteContext)
private suspend fun expression(a: Long, b: Long): Long {
    return a + multiply(a, b)
}

fun main() = runBlocking {
    manualCallableMap()
    with(ServerContext) {
        println(expression(6, 1))
        multiplyStreaming(5, 6).collect { println(it) }
    }
}
