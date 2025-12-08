package gc

import ClientContext
import ServerConfig
import getOrCreateLeaseRenewalClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.RemoteSerializable

@RemoteSerializable
class CalculatorGC private constructor(private var init: Int) {
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
        suspend operator fun invoke(init: Int) = CalculatorGC(init)
    }
}

fun main(): Unit = runBlocking {
    val leaseRenewalClient = getOrCreateLeaseRenewalClient("http://localhost:8080")
    context(ClientContext) {
        val x = CalculatorGC(5)
        println(x.result())
        delay(5_000)
        println(x.result())
    }
    leaseRenewalClient.shutdown()
}
