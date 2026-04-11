package kotlinx.remote.ktor

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.KtorDsl
import kotlinx.remote.classes.lease.LeaseReleaseRequest
import kotlinx.remote.classes.lease.LeaseRenewalRequest

@KtorDsl
fun Route.leaseRoutes(path: String = "/lease") {
    val leaseManager = application.attributes.getOrNull(KRemoteServerPluginAttributesKey)?.classes?.server?.leaseManager
        ?: error("Install KRemote plugin and specify classes server configuration to use lease routes")
    route(path) {
        post("/renew") {
            val request = call.receive<LeaseRenewalRequest>()
            val response = leaseManager.renewLeases(request)
            call.respond(response)
        }

        post("/release") {
            val request = call.receive<LeaseReleaseRequest>()
            leaseManager.releaseLeases(request)
            call.respond(mapOf("status" to "ok"))
        }
    }
}
