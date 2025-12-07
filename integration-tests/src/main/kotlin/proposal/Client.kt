package proposal

import ClientContext
import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

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
    with(ClientContext) {
        // Power is called on the client
        println(power(2, 10))
    }
}
