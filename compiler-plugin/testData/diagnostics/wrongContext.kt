// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.codegen.test.ServerContext
import kotlinx.remote.codegen.test.ServerRemoteContext

<!WRONG_REMOTE_FUNCTION_CONTEXT!>@Remote
context(_: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs<!>

fun box(): String = runBlocking {
    context(ServerRemoteContext) {
        val test1 = multiply(5, 6)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
