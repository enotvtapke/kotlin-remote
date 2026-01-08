import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.codegen.test.ServerContext
import kotlinx.remote.genCallableMap

object IdObject {
    @Remote
    context(_: RemoteWrapper<RemoteContext>)
    suspend fun id(x: Int): Int {
        return x
    }
}

fun box(): String = runBlocking {
    genCallableMap()

    context(ServerContext) {
        val test1 = IdObject.id(5).toLong()
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}

