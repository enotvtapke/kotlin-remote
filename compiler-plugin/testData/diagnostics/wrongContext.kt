// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteConfig
import kotlinx.remote.codegen.test.ServerConfig

<!WRONG_REMOTE_FUNCTION_CONTEXT!>@Remote
context(_: RemoteConfig)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs<!>

fun box(): String = runBlocking {
    context(ServerConfig) {
        val test1 = multiply(5, 6)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
