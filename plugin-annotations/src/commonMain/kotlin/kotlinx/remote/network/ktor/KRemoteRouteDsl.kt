package kotlinx.remote.network.ktor

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.remote.CallableMap
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.RemoteServerImpl
import kotlinx.remote.network.serialization.rpcInternalKClass
import kotlinx.serialization.serializer

@KtorDsl
fun Route.remote(path: String) {
    post(path) {
        handleRemoteCall()
    }
}

suspend fun RoutingContext.handleRemoteCall() {
    val remoteCall = call.receive<RemoteCall>()
    val callable = CallableMap[remoteCall.callableName]
    if (callable.returnsStream) {
        call.response.header(HttpHeaders.ContentType, "application/x-ndjson")
        call.response.header(HttpHeaders.CacheControl, "no-cache")

        call.respondBytesWriter {
            val serializer = serializer(callable.returnType.kType)
            val result = CallableMap[remoteCall.callableName].invokator.call(remoteCall.parameters) as Flow<Any?>
            result.collect { item ->
                writeStringUtf8(DefaultJson.encodeToString(serializer, item))
                writeStringUtf8("\n")
                flush()
            }
        }
    } else {
        call.respond(
            RemoteServerImpl.handleCall(remoteCall),
            TypeInfo(
                callable.returnType.kType.rpcInternalKClass<Any>(),
                callable.returnType.kType
            )
        )
    }
}
