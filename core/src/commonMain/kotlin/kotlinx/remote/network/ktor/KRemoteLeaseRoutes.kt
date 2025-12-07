package kotlinx.remote.network.ktor

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.remote.classes.lease.LeaseReleaseRequest
import kotlinx.remote.classes.lease.LeaseRenewalRequest

fun Route.leaseRoutes(path: String = "/lease") {
    val leaseManager = application.attributes.getOrNull(KRemoteServerPluginAttributesKey)?.leaseManager
        ?: error("KRemote Ktor plugin not installed")
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

fun Route.remoteWithLeasing(callPath: String = "/call", leasePath: String = "/lease") {
    remote(callPath)
    leaseRoutes(leasePath)
}
