package examples.basic

import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.remoteClient

data object ServerConfig : RemoteConfig {
    override val context = ServerContext
    override val client: RemoteClient = remoteClient("http://localhost:8080", "/call")
}

data object ServerContext : RemoteContext
