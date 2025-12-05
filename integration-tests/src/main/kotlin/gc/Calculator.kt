package gc

import ClientContext
import ServerConfig
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.remote.CallableMap
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.classes.lease.stopLeaseRenewal
import kotlinx.remote.genCallableMap
import kotlinx.serialization.Serializable

@RemoteSerializable
@Serializable(with = Calculator.RemoteClassSerializer::class)
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

val leaseClient = HttpClient {
    defaultRequest {
        url("http://localhost:8080")
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json()
    }
    install(Logging) {
        level = LogLevel.BODY
    }
}

fun main(): Unit = runBlocking {
    CallableMap.putAll(genCallableMap())
//    startLeaseRenewal(leaseClient.leaseClient(), this, LeaseRenewalClientConfig(3000))
    context(ClientContext) {
        val x = Calculator(5)
        println(x.multiply(6))
        println(x.multiply(7))
        println(x.result())
        delay(10_000)
        val y = Calculator(42)
        println(y.result())
        println(x.result())
    }
    stopLeaseRenewal()
}
