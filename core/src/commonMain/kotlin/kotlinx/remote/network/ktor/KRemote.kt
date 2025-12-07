package kotlinx.remote.network.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.lease.LeaseManager

val KRemoteServerPluginAttributesKey = AttributeKey<KRemoteConfigBuilder>("KRemoteServerPluginAttributesKey")

val KRemote: ApplicationPlugin<KRemoteConfigBuilder> = createApplicationPlugin(
    name = "KRemote",
    createConfiguration = { KRemoteConfigBuilder() },
) {
    pluginConfig.callableMap // Checking that CallableMap is initialized
    val logger = application.log
    application.install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error(cause.stackTraceToString())
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    pluginConfig.leaseManager = LeaseManager(pluginConfig.leaseConfig, RemoteInstancesPool())
    application.attributes.put(KRemoteServerPluginAttributesKey, pluginConfig)
    val cleanupScope = CoroutineScope(SupervisorJob())
    pluginConfig.leaseManager.startCleanupJob(cleanupScope)
    application.monitor.subscribe(ApplicationStopped) {
        pluginConfig.leaseManager.stopCleanupJob()
    }
}
