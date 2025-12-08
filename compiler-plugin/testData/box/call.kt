import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ClientContext

@Remote
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

fun box(): String = runBlocking {
    context(ClientContext) {
        val test1 = multiply(5, 6)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}

