package remoteClass.updated

import ClientContext
import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.CallableMap
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.RemoteInstancesPool.instances
import kotlinx.remote.classes.StubIdGenerator
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind.LONG
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.serializersModule
import kotlinx.serialization.modules.SerializersModule
import remoteClass.updated.Calculator.CalculatorStub

interface Descriptor {
    fun createStub(id: Long): Any
}

interface Stub {
    val id: Long
}

interface Stubable {
    fun createStub(id: Long): Any
}

@Serializable(with = CalculatorSerializer::class)
open class Calculator(private var init: Int): Stubable {
    @Remote(ServerConfig::class)
    context(_: RemoteContext)
    open suspend fun multiply(x: Int): Int {
        init *= x
        return init
    }

    @Remote(ServerConfig::class)
    context(_: RemoteContext)
    open suspend fun result(): Int {
        return init
    }

    override fun createStub(id: Long): Any {
        return CalculatorStub(id)
    }

    class CalculatorStub(override val id: Long): Calculator(0), Stub

//    object DescriptorImpl : Descriptor {
//        override fun createStub(id: Long): Any = CalculatorStub(id)
//    }
}

class CalculatorSerializer : KSerializer<Calculator> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("id", LONG)

    override fun serialize(encoder: Encoder, value: Calculator) {
        if (value is Stub) {
            encoder.encodeLong(value.id)
            return
        }
        val id = StubIdGenerator.nextId()
        instances[id] = value
        encoder.encodeLong(id)
    }

    override fun deserialize(decoder: Decoder): Calculator {
        val id = decoder.decodeLong()
        return instances[id]?.let { it as Calculator } ?: CalculatorStub(id)
    }
    companion object {
        fun stub(id: Long): CalculatorStub = CalculatorStub(id)
    }
}

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

    override fun deserialize(decoder: Decoder): T {
        val id = decoder.decodeLong()
        return instances[id]?.let { it as T } ?: createStub(id)
    }

    abstract fun createStub(id: Long): T
}

@Serializable(with = BoxSerializer::class)
open class Box<T>(val value: T?) {
    class BoxStub<T>(override val id: Long): Box<T>(null), Stub
}

class BoxSerializer<T>(
    private val serializer: KSerializer<T>
): RemoteSerializer<Box<T>>() {
    override fun createStub(id: Long): Box<T> {
        return Box.BoxStub(id)
    }
}

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun calculator(init: Int): Calculator {
    return Calculator(init)
}

fun main(): Unit = runBlocking {
    val x = Json {
        serializersModule = SerializersModule {}
    }
    val b = Box("a")
    val ss = x.encodeToString(b)
    println(ss)
    println(x.decodeFromString<Box<String>>(ss))
//    val s = Box.serializer(Int.serializer())
//    Json.encodeToString(s, b)
}
