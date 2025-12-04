package kotlinx.remote.classes.lease

import kotlinx.serialization.Serializable

/**
 * Information about a lease for a remote object instance.
 *
 * @property instanceId The unique ID of the remote object instance
 * @property expirationTimeMs The timestamp (in milliseconds) when the lease expires
 * @property clientId Optional identifier for the client holding the lease
 */
@Serializable
data class LeaseInfo(
    val instanceId: Long,
    val expirationTimeMs: Long,
    val clientId: String? = null
)

/**
 * Configuration for lease-based garbage collection.
 */
data class LeaseConfig(
    /**
     * Default lease duration in milliseconds.
     * Clients should renew leases before this duration expires.
     */
    val leaseDurationMs: Long = DEFAULT_LEASE_DURATION_MS,

    /**
     * Interval at which the lease manager checks for expired leases.
     */
    val cleanupIntervalMs: Long = DEFAULT_CLEANUP_INTERVAL_MS,

    /**
     * Grace period after lease expiration before cleanup.
     * Provides tolerance for network delays in lease renewal.
     */
    val gracePeriodMs: Long = DEFAULT_GRACE_PERIOD_MS
) {
    companion object {
        const val DEFAULT_LEASE_DURATION_MS = 30_000L // 30 seconds
        const val DEFAULT_CLEANUP_INTERVAL_MS = 10_000L // 10 seconds
        const val DEFAULT_GRACE_PERIOD_MS = 5_000L // 5 seconds
    }
}

/**
 * Request to renew a lease for one or more remote object instances.
 */
@Serializable
data class LeaseRenewalRequest(
    val instanceIds: List<Long>,
    val clientId: String? = null
)

/**
 * Response from a lease renewal request.
 */
@Serializable
data class LeaseRenewalResponse(
    val renewedLeases: List<LeaseInfo>,
    val failedIds: List<Long> = emptyList()
)

/**
 * Request to explicitly release leases for remote object instances.
 */
@Serializable
data class LeaseReleaseRequest(
    val instanceIds: List<Long>,
    val clientId: String? = null
)

