package manualFunctionCalling

import kotlinx.remote.LocalContext
import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteContext
import kotlinx.remote.call

context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) =
    if (ctx is LocalContext) {
        lhs / rhs
    } else {
        ctx.client.call<Long>(
            RemoteCall("multiply", arrayOf(lhs, rhs))
        )
    }
