// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.codegen.test.ServerConfig
import kotlinx.remote.asContext

<!WRONG_REMOTE_FUNCTION_CONTEXT!>@Remote
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs<!>

fun box(): String = runBlocking {
    context(ServerConfig.asContext()) {
        val test1 = multiply(5, 6)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
