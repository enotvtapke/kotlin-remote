package pubsub.launchers

import pubsub.api.BusApi
import pubsub.remote.BUS_PORT
import pubsub.remote.rpcServer
import pubsub.server.BusApiImpl

fun main() {
    rpcServer(BUS_PORT) { registerService<BusApi> { BusApiImpl() } }.start(wait = true)
}
