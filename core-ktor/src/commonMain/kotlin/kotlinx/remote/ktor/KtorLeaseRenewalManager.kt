package kotlinx.remote.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.lease.LeaseRenewalManager
import kotlinx.remote.classes.network.LeaseClient
import kotlinx.remote.ktor.classes.leaseClient

class KtorLeaseRenewalManager(
    private val config: LeaseRenewalClientConfig = LeaseRenewalClientConfig(),
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) { json() }
        defaultRequest {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
    }
) : LeaseRenewalManager(config) {
    override fun createLeaseClient(url: String): LeaseClient = httpClient.leaseClient("$url/${config.leasePath}")

    override fun stop() {
        super.stop()
        httpClient.close()
    }
}
