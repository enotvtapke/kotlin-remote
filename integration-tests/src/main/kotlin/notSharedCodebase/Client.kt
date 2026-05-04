package notSharedCodebase

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext

object ClientRemoteServiceImpl: RemoteService {
    @Remote("myOrderPizzaFunction")
    context(_: RemoteContext<RemoteConfig>)
    suspend override fun orderPizza(count: Int): Boolean {
        error("Unimplemented")
    }
}

fun main() = runBlocking {
    context(ServerConfig.asContext()) {
        println(ClientRemoteServiceImpl.orderPizza(5))
        println(ClientRemoteServiceImpl.orderPizza(10))
    }
    Unit
}