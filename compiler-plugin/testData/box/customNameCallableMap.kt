package box

// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.genCallableMap
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ServerConfig
import kotlinx.remote.asContext

@Remote("customName")
context(ctx: RemoteContext<RemoteConfig>)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

fun box(): String = runBlocking {
    val callableMap = genCallableMap()
    val keys = callableMap.callableMap.keys
    if ("customName" !in keys) return@runBlocking "Fail: expected 'customName' in keys, got $keys"
    if ("box.multiply" in keys) return@runBlocking "Fail: 'box.multiply' should not be in keys when custom name is provided"
    context(ServerConfig.asContext()) {
        val test1 = multiply(5, 6)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
