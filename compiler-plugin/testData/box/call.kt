import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.codegen.test.ServerContext

@Remote
context(ctx: RemoteWrapper<RemoteContext>)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

fun box(): String = runBlocking {
    context(ServerContext) {
        val test1 = multiply(5, 6)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}

