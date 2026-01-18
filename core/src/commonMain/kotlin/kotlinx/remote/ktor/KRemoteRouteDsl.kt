package kotlinx.remote.ktor

import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.remote.*

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

private suspend fun invokeCallable(callable: RemoteCallable, remoteCall: RemoteCall): RemoteResponse<*> {
    return try {
        context(LocalContext) {
            RemoteResponse.Success(callable.invokator.callWithUniqueName(remoteCall.arguments))
        }
    } catch (e: Exception) {
        RemoteResponse.Failure(e)
    }
}
