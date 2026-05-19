package pubsub

import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext
import pubsub.di.dep
import pubsub.remote.BusConfig
import pubsub.remote.SubscriberAt
import pubsub.remote.SubscriberConfig
import pubsub.repository.BusState

// === Bus tier ===

@Remote context(_: RemoteContext<BusConfig>)
suspend fun subscribe(topic: String, subscriberUrl: String) {
    dep<BusState>().subscribe(topic, subscriberUrl, SubscriberAt(subscriberUrl).asContext())
    println("[bus] $subscriberUrl subscribed to '$topic'")
}

@Remote context(_: RemoteContext<BusConfig>)
suspend fun unsubscribe(topic: String, subscriberUrl: String) {
    dep<BusState>().unsubscribe(topic, subscriberUrl)
    println("[bus] $subscriberUrl unsubscribed from '$topic'")
}

@Remote context(_: RemoteContext<BusConfig>)
suspend fun publish(topic: String, message: String) {
    val subs = dep<BusState>().subscribersFor(topic)
    println("[bus] publishing '$topic' to ${subs.size} subscriber(s)")
    subs.forEach { (url, ctx) ->
        context(ctx) { deliver(url, topic, message) }
    }
}

@Remote context(_: RemoteContext<BusConfig>)
suspend fun listTopics(): List<String> = dep<BusState>().topics()

// === Subscriber tier ===

@Remote context(_: RemoteContext<SubscriberConfig>)
suspend fun deliver(myUrl: String, topic: String, message: String) {
    println("[subscriber $myUrl] received on '$topic': $message")
}
