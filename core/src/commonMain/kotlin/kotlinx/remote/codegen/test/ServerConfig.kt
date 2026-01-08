package kotlinx.remote.codegen.test

import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteClient
import kotlinx.remote.asContext

data object ServerConfig: RemoteConfig {
    override val client: RemoteClient = TestRpcClient
}
