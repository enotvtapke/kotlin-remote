package pubsub.launchers

import pubsub.api.BusApi
import pubsub.api.SubscriberApi
import pubsub.remote.BUS_PORT
import pubsub.remote.rpcServer
import pubsub.server.BusApiImpl
import pubsub.server.SubscriberApiImpl

fun main() {
    startAll()
}

fun startAll() {
    val busServer = rpcServer(BUS_PORT) { registerService<BusApi> { BusApiImpl() } }
    val subscriberPorts = listOf(8091, 8092, 8093)
    val subscriberServers = subscriberPorts.map { p ->
        rpcServer(p) { registerService<SubscriberApi> { SubscriberApiImpl() } }
    }
    (listOf(busServer) + subscriberServers).forEach { it.start(wait = false) }
    println("Started bus on $BUS_PORT and ${subscriberPorts.size} subscribers on ${subscriberPorts.joinToString()}")
    Runtime.getRuntime().addShutdownHook(Thread {
        (listOf(busServer) + subscriberServers).forEach { it.stop(1000, 2000) }
    })
    Thread.currentThread().join()
}
