//package remoteClass.generatedMethods
//
//import ClientContext
//import ServerConfig
//import kotlinx.coroutines.runBlocking
//import kotlinx.remote.CallableMap
//import kotlinx.remote.Remote
//import kotlinx.remote.RemoteContext
//import kotlinx.remote.classes.RemoteInstancesPool.instances
//import kotlinx.remote.classes.StubIdGenerator
//import kotlinx.remote.genCallableMap
//import kotlinx.serialization.KSerializer
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.descriptors.PrimitiveKind.LONG
//import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
//import kotlinx.serialization.descriptors.SerialDescriptor
//import kotlinx.serialization.encoding.Decoder
//import kotlinx.serialization.encoding.Encoder
//
//@Serializable(with = Calculator.CalculatorSerializer::class)
//open class Calculator(private var init: Int) {
//    @Remote(ServerConfig::class)
//    context(_: RemoteContext)
//    open suspend fun multiply(x: Int): Int {
//        init *= x
//        return init
//    }
//
//    @Remote(ServerConfig::class)
//    context(_: RemoteContext)
//    open suspend fun result(): Int {
//        return init
//    }
//
//    class CalculatorStub(val id: Long): Calculator(0)
//
//    object CalculatorSerializer : KSerializer<Calculator> {
//        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("id", LONG)
//
//        override fun serialize(encoder: Encoder, value: Calculator) {
//            if (value is CalculatorStub) {
//                encoder.encodeLong(value.id)
//                return
//            }
//            val id = StubIdGenerator.nextId()
//            instances[id] = value
//            encoder.encodeLong(id)
//        }
//
//        override fun deserialize(decoder: Decoder): Calculator {
//            val id = decoder.decodeLong()
//            return instances[id]?.let { it as Calculator } ?: CalculatorStub(id)
//        }
//    }
//}
//
//@Remote(ServerConfig::class)
//context(ctx: RemoteContext)
//suspend fun calculator(init: Int): Calculator {
//    return Calculator(init)
//}
//
//fun main(): Unit = runBlocking {
//    CallableMap.putAll(genCallableMap())
//    context(ClientContext) {
//        val x = calculator(5)
//        println(x.multiply(6))
//        println(x.multiply(7))
//        println(x.result())
//    }
//}
