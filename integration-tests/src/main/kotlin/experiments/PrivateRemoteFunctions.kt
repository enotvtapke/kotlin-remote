package experiments

import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext

@Remote
context(_: RemoteContext<RemoteConfig>)
private suspend fun one(): Int {
    return 1
}

fun main(): Unit = runBlocking {
    context(ServerConfig.asContext()) {
        println(one())
    }
}