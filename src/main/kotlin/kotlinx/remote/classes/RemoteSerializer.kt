package kotlinx.remote.classes

import kotlinx.remote.classes.RemoteInstancesPool.instances
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind.LONG
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class RemoteSerializer<T> : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("id", LONG)

    override fun serialize(encoder: Encoder, value: T) {
        if (value is Stub) {
            encoder.encodeLong(value.id)
            return
        }
        val id = StubIdGenerator.nextId()
        instances[id] = value
        encoder.encodeLong(id)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        val id = decoder.decodeLong()
        return instances[id]?.let { it as T } ?: createStub(id)
    }

    abstract fun createStub(id: Long): T
}