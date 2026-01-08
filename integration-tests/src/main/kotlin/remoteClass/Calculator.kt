package remoteClass

import ServerContext
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.wrapped

@RemoteSerializable
class Calculator private constructor(private var init: Int) {
    @Remote
    context(_: RemoteWrapper<RemoteContext>)
    suspend fun multiply(x: Int): Int {
        init *= x
        return init
    }

    @Remote
    context(_: RemoteWrapper<RemoteContext>)
    suspend fun result(): Int {
        return init
    }

    companion object {
        @Remote
        context(_: RemoteWrapper<RemoteContext>)
        suspend operator fun invoke(init: Int) = Calculator(init)
    }
}

fun main(): Unit = runBlocking {
    context(ServerContext.wrapped) {
        val x = Calculator(5)
        println(x.multiply(6))
        println(x.multiply(7))
        println(x.result())
    }
}
