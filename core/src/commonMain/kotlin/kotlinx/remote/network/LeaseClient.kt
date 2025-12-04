package kotlinx.remote.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.remote.classes.lease.LeaseReleaseRequest
import kotlinx.remote.classes.lease.LeaseRenewalRequest
import kotlinx.remote.classes.lease.LeaseRenewalResponse

interface LeaseClient {
    suspend fun renewLeases(request: LeaseRenewalRequest): LeaseRenewalResponse
    suspend fun releaseLeases(request: LeaseReleaseRequest)
}

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

fun HttpClient.leaseClient(path: String = "/lease"): LeaseClient = LeaseClientImpl(this, path)
