package kotlinx.remote.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.remote.classes.lease.LeaseReleaseRequest
import kotlinx.remote.classes.lease.LeaseRenewalRequest
import kotlinx.remote.classes.lease.LeaseRenewalResponse

/**
 * Client interface for lease operations.
 */
interface LeaseClient {
    /**
     * Renew leases for multiple remote object instances.
     */
    suspend fun renewLeases(request: LeaseRenewalRequest): LeaseRenewalResponse
    
    /**
     * Release leases for multiple remote object instances.
     */
    suspend fun releaseLeases(request: LeaseReleaseRequest)
}

/**
 * HTTP-based implementation of LeaseClient using Ktor.
 */
class LeaseClientImpl(
    private val httpClient: HttpClient,
    private val basePath: String
) : LeaseClient {
    
    override suspend fun renewLeases(request: LeaseRenewalRequest): LeaseRenewalResponse {
        return httpClient.post("$basePath/renew") {
            setBody(request)
        }.body()
    }
    
    override suspend fun releaseLeases(request: LeaseReleaseRequest) {
        httpClient.post("$basePath/release") {
            setBody(request)
        }
    }
}

/**
 * Create a LeaseClient from an HttpClient.
 *
 * @param path The base path for lease endpoints (default: "/lease")
 */
fun HttpClient.leaseClient(path: String = "/lease"): LeaseClient = LeaseClientImpl(this, path)

