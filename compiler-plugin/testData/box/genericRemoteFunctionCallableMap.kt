// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ServerConfig
import kotlinx.remote.asContext
import kotlinx.remote.genCallableMap
import kotlinx.serialization.Polymorphic

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun <T> multiply(lhs: @Polymorphic T): T = lhs

fun box(): String = runBlocking {
    genCallableMap()
    context(ServerConfig.asContext()) {
        val test1 = multiply(5L)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
