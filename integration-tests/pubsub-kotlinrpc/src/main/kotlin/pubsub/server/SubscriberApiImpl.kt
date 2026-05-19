package pubsub.server

import pubsub.api.SubscriberApi

class SubscriberApiImpl : SubscriberApi {
    override suspend fun deliver(myUrl: String, topic: String, message: String) {
        println("[subscriber $myUrl] received on '$topic': $message")
    }
}
