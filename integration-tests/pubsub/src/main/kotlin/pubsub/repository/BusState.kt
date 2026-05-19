package pubsub.repository

import kotlinx.remote.RemoteContext
import pubsub.remote.SubscriberConfig

class BusState {
    private val subs: MutableMap<String, MutableList<Pair<String, RemoteContext<SubscriberConfig>>>> = mutableMapOf()

    fun subscribe(topic: String, url: String, ctx: RemoteContext<SubscriberConfig>) {
        subs.getOrPut(topic) { mutableListOf() }.add(url to ctx)
    }

    fun unsubscribe(topic: String, url: String) {
        subs[topic]?.removeAll { it.first == url }
    }

    fun subscribersFor(topic: String): List<Pair<String, RemoteContext<SubscriberConfig>>> =
        subs[topic]?.toList() ?: emptyList()

    fun topics(): List<String> = subs.keys.toList()
}
