// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.remoteClient
import kotlinx.remote.codegen.test.ServerConfig
import kotlinx.remote.codegen.test.ClientContext
import kotlinx.remote.CallableMap
import kotlinx.remote.genCallableMap

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun <T> multiply(lhs: T) = lhs

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun <K: Number, P: List<Int>, T: Map<K, List<P>>> genericFunction(t: T) = t.entries.first().value.first()

fun box(): String = runBlocking {
    CallableMap.putAll(genCallableMap())
    context(ClientContext) {
        val test1 = multiply(5L)
        val test2 = genericFunction(mapOf(1 to listOf(listOf(2)))) as Long
        if (test1 == 42L && test2 == 42L) "OK" else "Fail: test1=$test1"
    }
}
