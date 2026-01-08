package proposalExamples

import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

context(_: RemoteContext<RemoteConfig>)
private suspend fun power(base: Long, power: Int): Long {
    return generateSequence { base }
        .take(power)
        // Multiply is called on the server
        .fold(1L) { acc, x -> multiply(acc, x) }
}

fun main(): Unit = runBlocking {
    with(ServerConfig.asContext()) {
        // Power is called on the client
        println(power(2, 10))
    }
}
