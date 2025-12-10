package kotlinx.remote.ktor

import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.remote.CallerInfo
import kotlinx.remote.InjectedContext
import kotlinx.remote.RemoteCallable
import kotlinx.remote.RemoteCall
import kotlinx.remote.RemoteResponse
import kotlinx.remote.returnTypeInfo

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
    val protocol = call.request.headers["X-Forwarded-Proto"]
        ?: call.request.origin.scheme
    val host = call.request.headers["X-Forwarded-Host"]
        ?: "${call.request.origin.remoteHost}:${call.request.origin.remotePort}"
    call.respond(
        invokeCallable(callable, remoteCall, CallerInfo("$protocol://$host")),
        callable.returnTypeInfo(),
    )
}

private suspend fun invokeCallable(callable: RemoteCallable, remoteCall: RemoteCall, callerInfo: CallerInfo): RemoteResponse<*> {
    return try {
        context(InjectedContext(callerInfo)) {
            RemoteResponse.Success(callable.invokator.call(remoteCall.parameters))
        }
    } catch (e: Exception) {
        RemoteResponse.Failure(e)
    }
}
