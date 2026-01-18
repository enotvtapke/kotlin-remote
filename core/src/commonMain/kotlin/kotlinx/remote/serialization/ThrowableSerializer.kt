package kotlinx.remote.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

inline fun <reified T : Throwable> throwableSerializer(noinline throwableFactory: (String?, Throwable?) -> T): KSerializer<T> {
    return ThrowableSerializer(T::class.simpleName!!, throwableFactory)
}

@Serializable
data class StackFrame(val className: String, val methodName: String, val fileName: String?, val lineNumber: Int)

class ThrowableSerializer<T : Throwable>(
    name: String,
    private val throwableFactory: (String?, Throwable?) -> T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        name,
        ThrowableSurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: T) {
        val stackTrace = value.stackTrace()
        val cause = value.cause
        val surrogate = ThrowableSurrogate(
            if (stackTrace.isEmpty()) "${value.message}\nRemote trace\n---\n${value.stackTraceToString()}---" else value.message,
            stackTrace,
            cause
        )
        encoder.encodeSerializableValue(ThrowableSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): T {
        val surrogate = decoder.decodeSerializableValue(ThrowableSurrogate.serializer())
        return throwableFactory(surrogate.message, surrogate.cause).setStackTrace(surrogate.stackTrace)
    }

    @Serializable
    @SerialName("Throwable")
    private data class ThrowableSurrogate(
        val message: String?,
        val stackTrace: List<StackFrame>,
        val cause: @Polymorphic Throwable? = null
    )
}

class UnregisteredRemoteException(message: String?, cause: Throwable?) : Exception(message, cause)

fun SerializersModuleBuilder.setupThrowableSerializers() {
    polymorphic(Throwable::class) {
        subclass(throwableSerializer(::RuntimeException))
        subclass(throwableSerializer(::IllegalArgumentException))
        subclass(throwableSerializer(::IllegalStateException))
        subclass(throwableSerializer(::UnsupportedOperationException))
        subclass(throwableSerializer { message, _ -> IndexOutOfBoundsException(message) })
        subclass(throwableSerializer { message, _ -> ArithmeticException(message) })
        subclass(throwableSerializer { message, _ -> NumberFormatException(message) })
        subclass(throwableSerializer { message, _ -> NullPointerException(message) })
        subclass(throwableSerializer { message, _ -> ClassCastException(message) })
        subclass(throwableSerializer { message, _ -> NoSuchElementException(message) })
        subclass(throwableSerializer { message, _ -> ConcurrentModificationException(message) })
        polymorphicDefaultDeserializer(Throwable::class) {
            ThrowableSerializer(it ?: "Unknown exception name") { msg, cause ->
                UnregisteredRemoteException("$it: $msg", cause)
            }
        }
        polymorphicDefaultSerializer(Throwable::class) {
            ThrowableSerializer(it::class.simpleName ?: "Unknown exception name") { _, _ ->
                error("Not reachable")
            }
        }
    }
}