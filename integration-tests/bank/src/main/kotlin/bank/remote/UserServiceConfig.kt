package bank.remote

import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig

object UserServiceConfig : RemoteConfig {
    override val client: RemoteClient = remoteClient("http://localhost:8000")
}