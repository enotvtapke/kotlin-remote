package pubsub.client

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.withService
import pubsub.api.BusApi
import pubsub.launchers.startAll
import pubsub.remote.BUS_PORT
import pubsub.remote.rpcConn
import pubsub.remote.rpcHttpClient
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(): Unit = runBlocking {
    thread { startAll() }
    sleep(2000)

    val http = rpcHttpClient()
    val bus: BusApi = rpcConn(http, BUS_PORT).withService<BusApi>()

    val sub1 = "ws://localhost:8091"
    val sub2 = "ws://localhost:8092"
    val sub3 = "ws://localhost:8093"

    println("--- subscriptions ---")
    bus.subscribe("news", sub1)
    bus.subscribe("news", sub2)
    bus.subscribe("weather", sub2)
    bus.subscribe("weather", sub3)
    println("topics now: ${bus.listTopics()}")

    println("--- publish news ---")
    bus.publish("news", "Big news today!")

    println("--- publish weather ---")
    bus.publish("weather", "Sunny tomorrow")

    println("--- sub2 leaves news ---")
    bus.unsubscribe("news", sub2)

    println("--- publish news again ---")
    bus.publish("news", "Followup news")

    sleep(500)
    exitProcess(0)
}
