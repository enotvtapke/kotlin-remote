package bank.remote

import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig

object PaymentServiceConfig : RemoteConfig {
    override val client: RemoteClient = remoteClient("http://localhost:8001")
}
