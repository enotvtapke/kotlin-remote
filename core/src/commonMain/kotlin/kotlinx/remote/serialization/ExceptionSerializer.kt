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

inline fun <reified T: Exception> exceptionSerializer(noinline exceptionFactory: (String?, Throwable?) -> T): KSerializer<T> {
    return ExceptionSerializer(T::class.simpleName!!, exceptionFactory)
}

@Serializable
data class StackFrame(val className: String, val methodName: String, val fileName: String?, val lineNumber: Int)

class ExceptionSerializer<T : Exception>(
    name: String,
    private val exceptionFactory: (String?, Throwable?) -> T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        name,
        ExceptionSurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: T) {
        val stackTrace = value.stackTrace()
        val cause = value.cause as? Exception
        val surrogate = ExceptionSurrogate(
            if (stackTrace.isEmpty()) "${value.message}\nRemote trace\n---\n${value.stackTraceToString()}---" else value.message,
            stackTrace,
            cause
        )
        encoder.encodeSerializableValue(ExceptionSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): T {
        val surrogate = decoder.decodeSerializableValue(ExceptionSurrogate.serializer())
        return exceptionFactory(surrogate.message, surrogate.cause).setStackTrace(surrogate.stackTrace)
    }

    @Serializable
    @SerialName("Exception")
    private data class ExceptionSurrogate(val message: String?, val stackTrace: List<StackFrame>, val cause: @Polymorphic Exception? = null)
}

fun SerializersModuleBuilder.setupExceptionSerializers() {
    polymorphic(Exception::class) {
        subclass(exceptionSerializer(::RuntimeException))
        subclass(exceptionSerializer(::IllegalArgumentException))
        subclass(exceptionSerializer(::IllegalStateException))
        subclass(exceptionSerializer(::UnsupportedOperationException))
        subclass(exceptionSerializer { message, _ -> IndexOutOfBoundsException(message)})
        subclass(exceptionSerializer { message, _ -> ArithmeticException(message) })
        subclass(exceptionSerializer { message, _ -> NumberFormatException(message) })
        subclass(exceptionSerializer { message, _ -> NullPointerException(message) })
        subclass(exceptionSerializer { message, _ -> ClassCastException(message) })
        subclass(exceptionSerializer { message, _ -> NoSuchElementException(message) })
        subclass(exceptionSerializer { message, _ -> ConcurrentModificationException(message) })
    }
}