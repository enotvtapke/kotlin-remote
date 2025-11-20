package kotlinx.remote.network.ktor

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.logging.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*

internal val KRemoteServerPluginAttributesKey = AttributeKey<KRemoteConfigBuilder>("KRemoteServerPluginAttributesKey")

val KRemote: ApplicationPlugin<KRemoteConfigBuilder> = createApplicationPlugin(
    name = "KRemote",
    createConfiguration = { KRemoteConfigBuilder() },
) {
    application.install(ContentNegotiation) {
        json(jsonWithRemoteCallSerializer)
    }
    application.install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
    application.install(CallLogging) {
        callIdMdc("call-id")
        format { call ->
            val status = call.response.status() ?: "Unhandled"
            "${status}: ${call.request.toLogString()} ${call.request.queryString()}"
        }
    }
    val logger = application.log
    application.install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error(cause.stackTraceToString())
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    application.attributes.put(KRemoteServerPluginAttributesKey, pluginConfig)
}
