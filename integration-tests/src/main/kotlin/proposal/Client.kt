package proposal

import ClientContext
import kotlinx.coroutines.runBlocking
import kotlinx.remote.CallableMap
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.genCallableMap

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

context(_: RemoteContext)
private suspend fun power(base: Long, power: Int): Long {
    return generateSequence { base }
        .take(power)
        // Multiply is called on the server
        .fold(1L) { acc, x -> multiply(acc, x) }
}

fun main(): Unit = runBlocking {
    CallableMap.putAll(genCallableMap())
    with(ClientContext) {
        // Power is called on the client
        println(power(2, 10))
    }
}
