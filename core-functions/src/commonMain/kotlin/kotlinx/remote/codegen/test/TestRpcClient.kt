package kotlinx.remote.codegen.test

import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteClient

// TODO This class is only needed for compiler plugin tests and should be moved
object TestRpcClient : RemoteClient {
    override suspend fun call(call: RemoteCall): Any {
        return 42L
    }
}
