package experiments

import ServerContext
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

@Remote
context(_: RemoteContext)
suspend fun <T: Int> numberMap(list: List<T>): List<Long> = list.map { it.toLong() }

@Remote
context(_: RemoteContext)
suspend fun <K: Long, P: List<Int>, T: Map<K, List<P>>> genericFunction(t: T) = t.entries.first().value.first()

fun main(): Unit = runBlocking {
    with(ServerContext) {
        println(genericFunction(mapOf(1L to listOf(listOf(2)))))
        println(numberMap(listOf(1, 2)))
    }
}
