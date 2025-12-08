package manualFunctionCalling

import kotlinx.remote.LocalContext
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.call

context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) =
    if (ctx == LocalContext) {
        lhs / rhs
    } else {
        ctx.client.call<Long>(
            RemoteCall("multiply", arrayOf(lhs, rhs))
        )
    }
