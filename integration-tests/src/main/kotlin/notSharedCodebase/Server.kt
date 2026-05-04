package notSharedCodebase

import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import remoteEmbeddedServer

object ServerRemoteServiceImpl: RemoteService {
    @Remote("myOrderPizzaFunction")
    context(_: RemoteContext<RemoteConfig>)
    suspend override fun orderPizza(count: Int): Boolean {
        return count < 7
    }
}

fun main() {
    remoteEmbeddedServer().start(wait = true)
}
