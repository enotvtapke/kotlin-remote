package kotlinx.remote.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer

inline fun <reified T: Exception> exceptionSerializer(noinline exceptionFactory: (String?) -> T): KSerializer<T> {
    return ExceptionSerializer(T::class.simpleName!!, exceptionFactory)
}

class ExceptionSerializer<T : Exception>(
    name: String,
    private val exceptionFactory: (String?) -> T
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name) {
        element<String?>("message")
        element<String?>("stackTrace")
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            encodeNullableSerializableElement(
                descriptor,
                0,
                serializer<String?>(),
                value.message
            )
            encodeNullableSerializableElement(
                descriptor,
                1,
                serializer<String?>(),
                value.stackTraceToString()
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): T {
        var message: String? = null
        var stackTrace: String? = null

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> message = decodeNullableSerializableElement(
                        descriptor,
                        0,
                        serializer<String?>(),
                        null
                    )
                    1 -> stackTrace = decodeNullableSerializableElement(
                        descriptor,
                        1,
                        serializer<String?>(),
                        null
                    )
                    DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        val finalMessage = if (stackTrace != null) {
            val base = message ?: "Remote exception"
            "$base\n--- Remote stack trace ---\n$stackTrace--- End of remote stack trace ---"
        } else message

        return exceptionFactory(finalMessage)
    }
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