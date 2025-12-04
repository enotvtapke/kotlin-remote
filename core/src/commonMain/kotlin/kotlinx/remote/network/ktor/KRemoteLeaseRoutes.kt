package kotlinx.remote.network.ktor

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseReleaseRequest
import kotlinx.remote.classes.lease.LeaseRenewalRequest

/**
 * Install routes for lease-based garbage collection.
 *
 * This adds the following endpoints:
 * - POST {path}/renew - Renew leases for remote object instances
 * - POST {path}/release - Explicitly release leases for remote object instances
 *
 * @param path The base path for lease endpoints
 */
fun Route.leaseRoutes(path: String = "/lease") {
    route(path) {
        /**
         * Renew leases for one or more remote object instances.
         * Clients should call this periodically to keep their remote objects alive.
         */
        post("/renew") {
            val request = call.receive<LeaseRenewalRequest>()
            val response = LeaseManager.renewLeases(request)
            call.respond(response)
        }
        
        /**
         * Explicitly release leases for remote object instances.
         * Clients can call this when they no longer need the remote objects,
         * allowing immediate cleanup instead of waiting for lease expiration.
         */
        post("/release") {
            val request = call.receive<LeaseReleaseRequest>()
            LeaseManager.releaseLeases(request)
            call.respond(mapOf("status" to "ok"))
        }
    }
}

/**
 * Install both remote call and lease routes.
 * Convenience function that combines remote() and leaseRoutes().
 *
 * @param callPath The path for remote call endpoint (default: "/call")
 * @param leasePath The path for lease endpoints (default: "/lease")
 */
fun Route.remoteWithLeasing(callPath: String = "/call", leasePath: String = "/lease") {
    remote(callPath)
    leaseRoutes(leasePath)
}
