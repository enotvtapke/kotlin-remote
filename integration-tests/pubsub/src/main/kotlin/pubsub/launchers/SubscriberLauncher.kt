package pubsub.launchers

import org.koin.core.context.startKoin
import pubsub.di.subscriberModule
import pubsub.remote.pubsubServer

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: error("Usage: SubscriberLauncher <port>")
    startKoin { modules(subscriberModule) }
    pubsubServer(port).start(wait = true)
}
