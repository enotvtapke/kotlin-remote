package pubsub.api

import kotlinx.rpc.annotations.Rpc

@Rpc
interface BusApi {
    suspend fun subscribe(topic: String, subscriberUrl: String)
    suspend fun unsubscribe(topic: String, subscriberUrl: String)
    suspend fun publish(topic: String, message: String)
    suspend fun listTopics(): List<String>
}

@Rpc
interface SubscriberApi {
    suspend fun deliver(myUrl: String, topic: String, message: String)
}
