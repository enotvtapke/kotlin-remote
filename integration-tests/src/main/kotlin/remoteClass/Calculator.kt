package remoteClass

import ClientContext
import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.RemoteSerializable

@RemoteSerializable
class Calculator private constructor(private var init: Int) {
    @Remote(ServerConfig::class)
    context(_: RemoteContext)
    suspend fun multiply(x: Int): Int {
        init *= x
        return init
    }

    @Remote(ServerConfig::class)
    context(_: RemoteContext)
    suspend fun result(): Int {
        return init
    }

    companion object {
        @Remote(ServerConfig::class)
        context(_: RemoteContext)
        suspend operator fun invoke(init: Int) = Calculator(init)
    }
}

fun main(): Unit = runBlocking {
    context(ClientContext) {
        val x = Calculator(5)
        println(x.multiply(6))
        println(x.multiply(7))
        println(x.result())
    }
}
