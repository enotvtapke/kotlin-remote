package pubsub.remote

import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig

interface BusConfig : RemoteConfig
interface SubscriberConfig : RemoteConfig

object DefaultBus : BusConfig {
    override val client: RemoteClient = busClient("http://localhost:8090")
}

class SubscriberAt(url: String) : SubscriberConfig {
    override val client: RemoteClient = subscriberClient(url)
}
