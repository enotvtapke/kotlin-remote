package kotlinx.remote.codegen.test

import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteClient

object TestRpcClient : RemoteClient {
    override suspend fun call(
        call: RemoteCall,
    ): Any? {
        return 42L
    }
}
