package kotlinx.remote.classes

/**
 * Interface for client-side stub objects that represent remote instances.
 *
 * Stubs are created when a remote object reference is deserialized on the client.
 * They contain the instance ID needed to communicate with the actual object on the server.
 *
 * When used with lease-based GC, stubs should be registered with the LeaseRenewalClient
 * to automatically renew their leases.
 */
interface Stub {
    /**
     * The unique identifier for this remote instance.
     * This ID is used in all remote calls and lease operations.
     */
    val id: Long
}