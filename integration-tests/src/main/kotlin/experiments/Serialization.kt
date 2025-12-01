package experiments

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified T: Exception> exceptionSerializer(noinline exceptionFactory: (String?) -> T): KSerializer<T> {
    return ExceptionSerializer(T::class.simpleName!!, exceptionFactory)
}

class ExceptionSerializer<T : Exception>(
    name: String,
    private val exceptionFactory: (String?) -> T
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name) {
        element<String?>("message")
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
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): T {
        var message: String? = null

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> message = decodeNullableSerializableElement(
                        descriptor,
                        0,
                        serializer<String?>(),
                        null
                    )
                    DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        return exceptionFactory(message)
    }
}

@Serializable
sealed interface Response<T> {
    @Serializable
    @SerialName("success")
    data class Success<T>(val value: T): Response<T>
    @Serializable
    @SerialName("failure")
    data class Failure(val error: @Polymorphic Exception): Response<Nothing>
}

fun main() {
    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(Exception::class) {
                subclass(exceptionSerializer(::IllegalArgumentException))
                subclass(exceptionSerializer(::IllegalStateException))
            }
        }
    }
    val r: Response<String> = Response.Success("Yeah")
    val kType: KType = typeOf<Response<String>>()
    val serializer = serializer(kType)
    val rs = json.encodeToString(serializer, r)
    println(rs)
    println(json.decodeFromString(serializer, rs))
    val e = Response.Failure(IllegalArgumentException("What is?", IllegalStateException("What is what?")))
    val es = json.encodeToString(serializer, e)
    println(es)
    println(json.decodeFromString(serializer, es))
}