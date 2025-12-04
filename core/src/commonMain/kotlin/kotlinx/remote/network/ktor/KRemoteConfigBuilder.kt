package kotlinx.remote.network.ktor

import kotlinx.remote.classes.lease.LeaseConfig

/**
 * Configuration builder for the KRemote plugin.
 */
class KRemoteConfigBuilder {
    /**
     * Enable lease-based garbage collection for remote class instances.
     * When enabled, the LeaseManager cleanup job will be started automatically.
     */
    var enableLeasing: Boolean = false
    
    /**
     * Configuration for lease-based garbage collection.
     * Only used when enableLeasing is true.
     */
    var leaseConfig: LeaseConfig = LeaseConfig()
}