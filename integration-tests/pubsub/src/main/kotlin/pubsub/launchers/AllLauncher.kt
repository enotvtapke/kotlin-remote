package pubsub.launchers

import org.koin.core.context.startKoin
import pubsub.di.busModule
import pubsub.di.subscriberModule
import pubsub.remote.pubsubServer

fun main() {
    startAll()
}

fun startAll() {
    startKoin { modules(busModule, subscriberModule) }
    val ports = listOf(8090, 8091, 8092, 8093)
    val servers = ports.map { pubsubServer(it).also { s -> s.start(wait = false) } }
    println("Started bus on ${ports.first()} and ${ports.drop(1).size} subscribers on ${ports.drop(1).joinToString()}")
    Runtime.getRuntime().addShutdownHook(Thread {
        servers.forEach { it.stop(1000, 2000) }
    })
    Thread.currentThread().join()
}
