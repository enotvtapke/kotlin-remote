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

<!GENERIC_REMOTE_FUNCTION!>@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun <T> multiply(lhs: T) = lhs<!>

fun box(): String = runBlocking {
    context(ClientContext) {
        val test1 = multiply(5L)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
