import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ClientContext
import kotlinx.remote.CallableMap
import kotlinx.remote.genCallableMap

object IdObject {
    @Remote
    context(_: RemoteContext)
    suspend fun id(x: Int): Int {
        return x
    }
}

fun box(): String = runBlocking {
    genCallableMap()

    context(ClientContext) {
        val test1 = IdObject.id(5).toLong()
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}

