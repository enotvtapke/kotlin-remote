package remoteClass

import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.asContext

@RemoteSerializable
class Calculator private constructor(private var init: Int) {
    @Remote
    context(_: RemoteContext<RemoteConfig>)
    suspend fun multiply(x: Int): Int {
        init *= x
        return init
    }

    @Remote
    context(_: RemoteContext<RemoteConfig>)
    suspend fun result(): Int {
        return init
    }

    companion object {
        @Remote
        context(_: RemoteContext<RemoteConfig>)
        suspend operator fun invoke(init: Int) = Calculator(init)
    }
}

fun main(): Unit = runBlocking {
    context(ServerConfig.asContext()) {
        val x = Calculator(5)
        println(x.multiply(6))
        println(x.multiply(7))
        println(x.result())
    }
}
