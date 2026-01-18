package kotlinx.remote

import io.ktor.util.reflect.*
import kotlinx.remote.serialization.asKClass
import kotlin.reflect.KType

data class RemoteType(
    val kType: KType,
    val isPolymorphic: Boolean = false,
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
    val parameters: Array<RemoteParameter>,
)

fun interface RemoteInvokator {
    context(_: LocalContext)
    suspend fun callWithUniqueName(parameters: Array<Any?>): Any?
}

fun RemoteCallable.returnTypeInfo(): TypeInfo = TypeInfo(
    returnType.kType.asKClass(),
    returnType.kType
)