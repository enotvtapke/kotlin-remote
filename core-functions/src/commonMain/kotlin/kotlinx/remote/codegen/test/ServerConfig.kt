package kotlinx.remote.codegen.test

import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteClient
import kotlinx.remote.asContext

// TODO This class is only needed for compiler plugin tests and should be moved
data object ServerConfig: RemoteConfig {
    override val client: RemoteClient = TestRpcClient
}
