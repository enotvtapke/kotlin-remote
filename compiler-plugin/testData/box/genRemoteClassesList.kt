// TARGET_BACKEND: JVM

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.codegen.test.ClientContext
import kotlinx.remote.CallableMap
import kotlinx.remote.genCallableMap
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.RemoteSerializable

@RemoteSerializable
class Calculator(private var init: Long = 0) {
    @Remote
    context(ctx: RemoteContext)
    suspend fun multiply(x: Long): Long {
        init *= x
        return init
    }
}

fun box(): String = runBlocking {
    genRemoteClassList()
    context(ClientContext) {
        val c = Calculator(1)
        val test1 = c.multiply(5)
        if (test1 == 42L) "OK" else "Fail: test1=$test1"
    }
}
