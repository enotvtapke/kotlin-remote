package pubsub.client

import kotlinx.coroutines.runBlocking
import kotlinx.remote.asContext
import pubsub.listTopics
import pubsub.launchers.startAll
import pubsub.publish
import pubsub.remote.DefaultBus
import pubsub.subscribe
import pubsub.unsubscribe
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(): Unit = runBlocking {
    thread { startAll() }
    sleep(1500)

    val sub1 = "http://localhost:8091"
    val sub2 = "http://localhost:8092"
    val sub3 = "http://localhost:8093"

    with(DefaultBus.asContext()) {
        println("--- subscriptions ---")
        subscribe("news", sub1)
        subscribe("news", sub2)
        subscribe("weather", sub2)
        subscribe("weather", sub3)
        println("topics now: ${listTopics()}")

        println("--- publish news ---")
        publish("news", "Big news today!")

        println("--- publish weather ---")
        publish("weather", "Sunny tomorrow")

        println("--- sub2 leaves news ---")
        unsubscribe("news", sub2)

        println("--- publish news again ---")
        publish("news", "Followup news")
    }

    sleep(500)
    exitProcess(0)
}
