package manualFunctionCalling

import kotlinx.remote.Local
import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.WrappedRemote
import kotlinx.remote.call

context(ctx: RemoteWrapper<RemoteContext>)
suspend fun multiply(lhs: Long, rhs: Long) =
    if (ctx is Local) {
        lhs / rhs
    } else {
        (ctx as WrappedRemote<RemoteContext>).context.client.call<Long>(
            RemoteCall("multiply", arrayOf(lhs, rhs))
        )
    }
