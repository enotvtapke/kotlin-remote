package manual

import kotlinx.coroutines.runBlocking
import kotlinx.remote.RemoteContext

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
