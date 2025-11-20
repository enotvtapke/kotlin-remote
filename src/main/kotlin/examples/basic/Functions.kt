package examples.basic

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.call
import kotlinx.remote.network.callStreaming

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) =
    if (ctx == ServerConfig.context) {
        lhs * rhs
    } else {
        ServerConfig.client.call<Long>(
            RemoteCall("multiply", arrayOf(lhs, rhs))
        )
    }


@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiplyStreaming(lhs: Long, rhs: Long): Flow<Long> {
    if (ctx == ServerConfig.context) {
        return flow {
            repeat(50) {
                delay(50)
                emit(lhs * rhs)
            }
        }
    } else {
        return ServerConfig.client.callStreaming<Long>(RemoteCall("multiplyStreaming", arrayOf(lhs, rhs)))
    }
}

class Heh {
    @Remote(ServerConfig::class)
    fun aas() {

    }
}