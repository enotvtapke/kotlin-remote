package todoapp.remote

import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig

object ServerConfig : RemoteConfig {
    override val client: RemoteClient = remoteClient("http://localhost:8000")
}