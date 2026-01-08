package proposalExamples

import ServerContext
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.wrapped

@Remote
context(_: RemoteWrapper<RemoteContext>)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

context(_: RemoteWrapper<RemoteContext>)
private suspend fun power(base: Long, power: Int): Long {
    return generateSequence { base }
        .take(power)
        // Multiply is called on the server
        .fold(1L) { acc, x -> multiply(acc, x) }
}

fun main(): Unit = runBlocking {
    with(ServerContext.wrapped) {
        // Power is called on the client
        println(power(2, 10))
    }
}
