package experiments

import ClientContext
import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun <T: Int> numberMap(list: List<T>): List<Long> = list.map { it.toLong() }

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun <K: Long, P: List<Int>, T: Map<K, List<P>>> genericFunction(t: T) = t.entries.first().value.first()

fun main(): Unit = runBlocking {
    with(ClientContext) {
        println(genericFunction(mapOf(1L to listOf(listOf(2)))))
        println(numberMap(listOf(1, 2)))
    }
}
