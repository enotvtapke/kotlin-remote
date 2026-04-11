package kotlinx.remote.classes.network

import kotlinx.remote.classes.lease.LeaseReleaseRequest
import kotlinx.remote.classes.lease.LeaseRenewalRequest
import kotlinx.remote.classes.lease.LeaseRenewalResponse

interface LeaseClient {
    suspend fun renewLeases(request: LeaseRenewalRequest): LeaseRenewalResponse
    suspend fun releaseLeases(request: LeaseReleaseRequest)
}
