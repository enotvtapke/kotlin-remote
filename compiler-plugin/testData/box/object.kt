import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ServerConfig
import kotlinx.remote.asContext
import kotlinx.remote.genCallableMap

object IdObject {
    @Remote
    context(_: RemoteContext<RemoteConfig>)
    suspend fun id(x: Int): Int {
        return x
    }
}

fun box(): String = runBlocking {
    genCallableMap()

    context(ServerConfig.asContext()) {
        val test1 = IdObject.id(5).toLong()
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}

