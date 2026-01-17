package bank.remote

import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig

object AccountServiceConfig : RemoteConfig {
    override val client: RemoteClient = remoteClient("http://localhost:8002")
}
