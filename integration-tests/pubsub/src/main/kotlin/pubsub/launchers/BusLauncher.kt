package pubsub.launchers

import org.koin.core.context.startKoin
import pubsub.di.busModule
import pubsub.remote.pubsubServer

fun main() {
    startKoin { modules(busModule) }
    pubsubServer(8090).start(wait = true)
}
