package kotlinx.remote.codegen.test

import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteResponse

// TODO This class is only needed for compiler plugin tests and should be moved
object TestRpcClient : RemoteClient {
    override suspend fun call(call: RemoteCall): RemoteResponse<*> {
        return RemoteResponse.Success(42L)
    }
}
