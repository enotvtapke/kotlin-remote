package kotlinx.remote.ktor

import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.*
import kotlinx.remote.*
import kotlinx.remote.serialization.asKClass

@KtorDsl
fun Route.remote(path: String) {
    post(path) {
        handleRemoteCall()
    }
}

suspend fun RoutingContext.handleRemoteCall() {
    val remoteCall = call.receive<RemoteCall>()
    val callableMap = call.application.attributes.getOrNull(KRemoteServerPluginAttributesKey)?.callableMap
        ?: error("KRemote Ktor plugin not installed")
    val callable = callableMap[remoteCall.callableName]
    call.respond(
        invokeCallable(callable, remoteCall),
        callable.returnTypeInfo(),
    )
}

fun RemoteCallable.returnTypeInfo(): TypeInfo = TypeInfo(
    returnType.kType.asKClass(),
    returnType.kType
)
