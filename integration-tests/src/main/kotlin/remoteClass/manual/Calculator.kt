package remoteClass.manual

import ClientContext
import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.*
import kotlinx.remote.classes.RemoteInstancesPool.instances
import kotlinx.remote.classes.addInstance
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.call
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.LONG
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.typeOf

@Serializable(with = Calculator.CalculatorSerializer::class)
open class Calculator(private var init: Int) {
    context(ctx: RemoteContext)
    suspend fun multiply(x: Int): Int {
        if (ctx == ServerConfig.context) {
            init *= x
            return init
        } else {
            return ServerConfig.client.call<Int>(
                RemoteCall("multiply", arrayOf(this, x))
            )
        }
    }

    class CalculatorStub(val id: Long): Calculator(0)

    object CalculatorSerializer : KSerializer<Calculator> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("id", LONG)

        override fun serialize(encoder: Encoder, value: Calculator) {
            if (value is CalculatorStub) {
                encoder.encodeLong(value.id)
                return
            }
            encoder.encodeLong(addInstance(value))
        }

        override fun deserialize(decoder: Decoder): Calculator {
            val id = decoder.decodeLong()
            return instances.getOrDefault(id, CalculatorStub(id)) as Calculator
        }
    }
}

context(ctx: RemoteContext)
suspend fun calculator(init: Int): Calculator {
    if (ctx == ServerConfig.context) {
        return Calculator(init)
    } else {
        return ServerConfig.client.call<Calculator>(
            RemoteCall("calculator", arrayOf(init))
        )
    }
}

fun main(): Unit = runBlocking {
    initCallableMap()
    context(ClientContext) {
        val x = calculator(5)
        println(x.multiply(6))
        println(x.multiply(7))
    }
}

fun initCallableMap() {
    CallableMap["calculator"] = RemoteCallable(
        name = "calculator",
        returnType = RemoteType(typeOf<Calculator>()),
        invokator = RemoteInvokator { args ->
            return@RemoteInvokator with(ServerConfig.context) {
                calculator(args[0] as Int)
            }
        },
        parameters = arrayOf(
            RemoteParameter("init", RemoteType(typeOf<Int>()), false),
        ),
        returnsStream = false,
    )
    CallableMap["multiply"] = RemoteCallable(
        name = "multiply",
        returnType = RemoteType(typeOf<Int>()),
        invokator = RemoteInvokator { args ->
            return@RemoteInvokator with(ServerConfig.context) {
                (args[0] as Calculator).multiply(args[1] as Int)
            }
        },
        parameters = arrayOf(
            RemoteParameter("this", RemoteType(typeOf<Calculator>()), false),
            RemoteParameter("x", RemoteType(typeOf<Int>()), false),
        ),
        returnsStream = false,
    )
}