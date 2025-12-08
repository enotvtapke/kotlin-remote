package manualFunctionCalling

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.remote.LocalContext
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.call
import kotlinx.remote.network.callStreaming

context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) =
    if (ctx == LocalContext) {
        lhs / rhs
    } else {
        ctx.client.call<Long>(
            RemoteCall("multiply", arrayOf(lhs, rhs))
        )
    }

context(ctx: RemoteContext)
suspend fun multiplyStreaming(lhs: Long, rhs: Long): Flow<Long> {
    if (ctx == LocalContext) {
        return flow {
            repeat(50) {
                delay(50)
                emit(lhs * rhs)
            }
        }
    } else {
        return ctx.client.callStreaming<Long>(RemoteCall("multiplyStreaming", arrayOf(lhs, rhs)))
    }
}
