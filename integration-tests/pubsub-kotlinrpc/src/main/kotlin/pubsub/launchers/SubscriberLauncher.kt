package pubsub.launchers

import pubsub.api.SubscriberApi
import pubsub.remote.rpcServer
import pubsub.server.SubscriberApiImpl

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: error("Usage: SubscriberLauncher <port>")
    rpcServer(port) { registerService<SubscriberApi> { SubscriberApiImpl() } }.start(wait = true)
}
