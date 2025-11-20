package kotlinx.remote

import kotlin.reflect.KType

data class RemoteType(
    val kType: KType
)

data class RemoteParameter(
    val name: String,
    val type: RemoteType,
    val isOptional: Boolean,
)

class RpcCallable(
    val name: String,
    val returnType: RemoteType,
    val invokator: RpcInvokator,
    val parameters: Array<out RemoteParameter>,
    val returnsStream: Boolean,
)

fun interface RpcInvokator {
    suspend fun call(parameters: Array<Any?>): Any?
}

