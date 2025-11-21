package callableMap

import ServerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
fun multiplyStreaming(lhs: Long, rhs: Long): Flow<Long> {
    return flow {
        repeat(50) {
            delay(50)
            emit(lhs * rhs)
        }
    }
}
