package kotlinx.remote.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

inline fun <reified T: Exception> exceptionSerializer(noinline exceptionFactory: (String?) -> T): KSerializer<T> {
    return ExceptionSerializer(T::class.simpleName!!, exceptionFactory)
}

@Serializable
data class StackFrame(val className: String, val methodName: String, val fileName: String?, val lineNumber: Int)

class ExceptionSerializer<T : Exception>(
    name: String,
    private val exceptionFactory: (String?) -> T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        name,
        ExceptionSurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: T) {
        val stackTrace = value.stackTrace()
        val surrogate = ExceptionSurrogate(
            if (stackTrace.isEmpty()) "${value.message}\nRemote trace\n---\n${value.stackTraceToString()}---" else value.message,
            stackTrace
        )
        encoder.encodeSerializableValue(ExceptionSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): T {
        val surrogate = decoder.decodeSerializableValue(ExceptionSurrogate.serializer())
        return exceptionFactory(surrogate.message).setStackTrace(surrogate.stackTrace)
    }

    @Serializable
    @SerialName("Exception")
    private data class ExceptionSurrogate(val message: String?, val stackTrace: List<StackFrame>)
}

fun SerializersModuleBuilder.setupExceptionSerializers() {
    polymorphic(Exception::class) {
        subclass(exceptionSerializer(::RuntimeException))
        subclass(exceptionSerializer(::IllegalArgumentException))
        subclass(exceptionSerializer(::IllegalStateException))
        subclass(exceptionSerializer(::IndexOutOfBoundsException))
        subclass(exceptionSerializer(::UnsupportedOperationException))
        subclass(exceptionSerializer(::ArithmeticException))
        subclass(exceptionSerializer(::NumberFormatException))
        subclass(exceptionSerializer(::NullPointerException))
        subclass(exceptionSerializer(::ClassCastException))
        subclass(exceptionSerializer(::NoSuchElementException))
        subclass(exceptionSerializer(::ConcurrentModificationException))
    }
}