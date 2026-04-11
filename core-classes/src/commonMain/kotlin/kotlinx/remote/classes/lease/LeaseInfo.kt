package kotlinx.remote.classes.lease

import kotlinx.serialization.Serializable

@Serializable
data class LeaseInfo(
    val instanceId: Long,
    val expirationTimeMs: Long,
    val clientId: String? = null
)

data class LeaseConfig(
    val leaseDurationMs: Long = DEFAULT_LEASE_DURATION_MS,
    val cleanupIntervalMs: Long = DEFAULT_CLEANUP_INTERVAL_MS,
    val gracePeriodMs: Long = DEFAULT_GRACE_PERIOD_MS
) {
    companion object {
        const val DEFAULT_LEASE_DURATION_MS = 30_000L
        const val DEFAULT_CLEANUP_INTERVAL_MS = 10_000L
        const val DEFAULT_GRACE_PERIOD_MS = 5_000L
    }
}

@Serializable
data class LeaseRenewalRequest(
    val instanceIds: List<Long>,
    val clientId: String? = null
)

@Serializable
data class LeaseRenewalResponse(
    val renewedLeases: List<LeaseInfo>,
    val failedIds: List<Long> = emptyList()
)

@Serializable
data class LeaseReleaseRequest(
    val instanceIds: List<Long>,
    val clientId: String? = null
)
