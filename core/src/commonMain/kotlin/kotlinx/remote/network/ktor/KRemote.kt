package kotlinx.remote.network.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.remote.classes.lease.LeaseManager

internal val KRemoteServerPluginAttributesKey = AttributeKey<KRemoteConfigBuilder>("KRemoteServerPluginAttributesKey")

val KRemote: ApplicationPlugin<KRemoteConfigBuilder> = createApplicationPlugin(
    name = "KRemote",
    createConfiguration = { KRemoteConfigBuilder() },
) {
    val logger = application.log
    application.install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error(cause.stackTraceToString())
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    application.attributes.put(KRemoteServerPluginAttributesKey, pluginConfig)
    
    // Configure and start lease-based GC if enabled
    if (pluginConfig.enableLeasing) {
        LeaseManager.configure(pluginConfig.leaseConfig)
        
        // Create a coroutine scope for the cleanup job
        val cleanupScope = CoroutineScope(SupervisorJob())
        LeaseManager.startCleanupJob(cleanupScope)
        
        logger.info("KRemote: Lease-based GC enabled with config: ${pluginConfig.leaseConfig}")
        
        // Stop cleanup job when application stops
        application.monitor.subscribe(ApplicationStopped) {
            LeaseManager.stopCleanupJob()
            logger.info("KRemote: Lease cleanup job stopped")
        }
    }
}
