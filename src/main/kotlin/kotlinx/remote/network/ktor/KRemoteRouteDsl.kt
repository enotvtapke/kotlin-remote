package kotlinx.remote.network.ktor

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.serializer
import kotlinx.remote.network.serialization.rpcInternalKClass
import kotlinx.remote.CallableMap
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.RemoteServerImpl

const val HEARTBEAT_JSON = "{\"type\": \"heartbeat\"}"

@KtorDsl
fun Route.remote(path: String) {
    post(path) {
        val remoteCall = call.receive<RemoteCall>()
        val callable = CallableMap[remoteCall.callableName]
        if (callable.returnsStream) {
            call.response.header(HttpHeaders.ContentType, "application/x-ndjson")
            call.response.header(HttpHeaders.CacheControl, "no-cache")

            call.respondBytesWriter {
                val heartbeatJob = CoroutineScope(currentCoroutineContext()).launch {
                    while (isActive) {
                        writeStringUtf8(HEARTBEAT_JSON + "\n")
                        flush()

                        println("Server sent heartbeat.")
                        delay(5000)
                    }
                }

                val s = serializer(callable.returnType.kType)

                val result = CallableMap[remoteCall.callableName].invokator.call(remoteCall.parameters) as Flow<Any?>
                result.collect { item ->
                    writeStringUtf8(jsonWithRemoteCallSerializer.encodeToString(s, item))
                    writeStringUtf8("\n")
                    flush()
                }
                heartbeatJob.cancel()
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
}
