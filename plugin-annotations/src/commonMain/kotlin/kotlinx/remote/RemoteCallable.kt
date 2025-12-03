package kotlinx.remote

import io.ktor.util.reflect.*
import kotlinx.remote.network.serialization.rpcInternalKClass
import kotlin.reflect.KType

data class RemoteType(
    val kType: KType
)

data class RemoteParameter(
    val name: String,
    val type: RemoteType,
    val isOptional: Boolean,
)

data class RemoteCallable(
    val name: String,
    val returnType: RemoteType,
    val invokator: RemoteInvokator,
    val parameters: Array<out RemoteParameter>,
    val returnsStream: Boolean,
)

fun interface RemoteInvokator {
    suspend fun call(parameters: Array<Any?>): Any?
}

fun RemoteCallable.returnTypeInfo(): TypeInfo = TypeInfo(
    returnType.kType.rpcInternalKClass<Any>(),
    returnType.kType
)