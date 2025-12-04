package kotlinx.remote.classes

import kotlinx.remote.classes.RemoteInstancesPool.instances
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.LONG
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Base serializer for remote classes.
 *
 * When serializing a remote object:
 * - If the object is a Stub, only the ID is serialized
 * - Otherwise, the object is stored in RemoteInstancesPool and a lease is created
 *
 * When deserializing:
 * - If the object exists in RemoteInstancesPool, it is returned
 * - Otherwise, a Stub is created for client-side reference
 */
abstract class RemoteSerializer<T: Any> : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("id", LONG)

    override fun serialize(encoder: Encoder, value: T) {
        if (value is Stub) {
            encoder.encodeLong(value.id)
            return
        }
        val id = StubIdGenerator.nextId()
        instances[id] = value
        // Create a lease for the new instance so it can be garbage collected later
        LeaseManager.createLease(id)
        encoder.encodeLong(id)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        val id = decoder.decodeLong()
        LeaseRenewalClient.registerStubId(id)
        return instances[id]?.let { it as T } ?: createStub(id)
    }

    /**
     * Create a stub object that represents a remote instance on the client side.
     * The stub should track its ID for lease renewal purposes.
     */
    abstract fun createStub(id: Long): T
}