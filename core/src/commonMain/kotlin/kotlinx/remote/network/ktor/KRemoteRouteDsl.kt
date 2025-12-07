package kotlinx.remote.network.ktor

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.remote.RemoteCallable
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.RemoteResponse
import kotlinx.remote.returnTypeInfo
import kotlinx.serialization.serializer

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
    if (callable.returnsStream) {
        call.response.header(HttpHeaders.ContentType, "application/x-ndjson")
        call.response.header(HttpHeaders.CacheControl, "no-cache")

        call.respondBytesWriter {
            val serializer = serializer(callable.returnType.kType)
            val result = callableMap[remoteCall.callableName].invokator.call(remoteCall.parameters) as Flow<Any?>
            result.collect { item ->
                writeStringUtf8(DefaultJson.encodeToString(serializer, item))
                writeStringUtf8("\n")
                flush()
            }
        }
    } else {
        call.respond(
            invokeCallable(callable, remoteCall),
            callable.returnTypeInfo(),
        )
    }
}

private suspend fun invokeCallable(callable: RemoteCallable, remoteCall: RemoteCall): RemoteResponse<*> {
    return try {
        RemoteResponse.Success(callable.invokator.call(remoteCall.parameters))
    } catch (e: Exception) {
        RemoteResponse.Failure(e)
    }
}
