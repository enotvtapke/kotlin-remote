package pubsub.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import pubsub.api.BusApi
import pubsub.api.SubscriberApi

class BusApiImpl : BusApi {
    private val subs: MutableMap<String, MutableList<String>> = mutableMapOf()
    private val stubCache: MutableMap<String, SubscriberApi> = mutableMapOf()
    private val httpClient: HttpClient = HttpClient(CIO) { installKrpc() }

    private suspend fun stubFor(url: String): SubscriberApi = stubCache.getOrPut(url) {
        val parsed = URLBuilder().takeFrom(url)
        httpClient.rpc {
            url {
                protocol = URLProtocol.WS
                host = parsed.host
                port = parsed.port
                pathSegments = listOf("api")
            }
            rpcConfig { serialization { json() } }
        }.withService<SubscriberApi>()
    }

    override suspend fun subscribe(topic: String, subscriberUrl: String) {
        subs.getOrPut(topic) { mutableListOf() }.add(subscriberUrl)
        stubFor(subscriberUrl)
        println("[bus] $subscriberUrl subscribed to '$topic'")
    }

    override suspend fun unsubscribe(topic: String, subscriberUrl: String) {
        subs[topic]?.remove(subscriberUrl)
        println("[bus] $subscriberUrl unsubscribed from '$topic'")
    }

    override suspend fun publish(topic: String, message: String) {
        val urls = subs[topic]?.toList() ?: emptyList()
        println("[bus] publishing '$topic' to ${urls.size} subscriber(s)")
        urls.forEach { url -> stubFor(url).deliver(url, topic, message) }
    }

    override suspend fun listTopics(): List<String> = subs.keys.toList()
}
