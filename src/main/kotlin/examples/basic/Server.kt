package org.jetbrains.kotlinx.examples.basic

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.logging.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import network.serialization.RpcCallSerializer
import network.serialization.rpcInternalKClass
import org.jetbrains.kotlinx.*
import org.jetbrains.kotlinx.network.RemoteCall
import org.jetbrains.kotlinx.network.RemoteServerImpl
import kotlin.reflect.typeOf

fun main() {
    CallableMap["multiply"] = RpcCallable(
        name = "multiply",
        returnType = RemoteType(typeOf<Long>()),
        invokator = RpcInvokator { args ->
            return@RpcInvokator with(ServerConfig.context) {
                multiply(args[0] as Long, args[1] as Long)
            }
        },
        parameters = arrayOf(
            RemoteParameter("lhs", RemoteType(typeOf<Long>()), false),
            RemoteParameter("rhs", RemoteType(typeOf<Long>()), false)
        ),
    )

    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            serializersModule = SerializersModule {
                contextual(RemoteCall::class, RpcCallSerializer(SerializersModule {}))
            }
        })
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
    install(CallLogging) {
        callIdMdc("call-id")
        format { call ->
            val status = call.response.status() ?: "Unhandled"
            "${status}: ${call.request.toLogString()} ${call.request.queryString()}"
        }
    }
    val logger = log
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error(cause.stackTraceToString())
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        post("/call") {
            val remoteCall = call.receive<RemoteCall>()
            call.respond(
                RemoteServerImpl.handleCall(remoteCall),
                TypeInfo(
                    CallableMap[remoteCall.callableName].returnType.kType.rpcInternalKClass<Any>(),
                    CallableMap[remoteCall.callableName].returnType.kType
                )
            )
        }
    }
}