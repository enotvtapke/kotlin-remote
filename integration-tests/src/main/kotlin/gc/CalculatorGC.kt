package gc

import ServerContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.wrapped

@RemoteSerializable
class CalculatorGC private constructor(private var init: Int) {
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
        suspend operator fun invoke(init: Int) = CalculatorGC(init)
    }
}

fun main(): Unit = runBlocking {
    context(ServerContext.wrapped) {
        val x = CalculatorGC(5)
        println(x.result())
        delay(5_000)
        println(x.result())
    }
}
