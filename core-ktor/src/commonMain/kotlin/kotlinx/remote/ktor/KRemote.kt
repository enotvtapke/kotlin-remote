package kotlinx.remote.ktor

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.remote.classes.remoteClassSerializersModule
import kotlinx.remote.serialization.remoteSerializersModuleShort
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

internal val KRemoteServerPluginAttributesKey = AttributeKey<KRemoteConfig>("KRemoteServerPluginAttributesKey")

val KRemote: ApplicationPlugin<KRemoteConfigBuilder> = createApplicationPlugin(
    name = "KRemote",
    createConfiguration = { KRemoteConfigBuilder() },
) {
    val config = pluginConfig.build()
    application.attributes.put(KRemoteServerPluginAttributesKey, config)
    application.pluginOrNull(ContentNegotiation) ?: run {
        application.install(ContentNegotiation) {
            json(Json {
                serializersModule = (config.serializersModule ?: SerializersModule { }) + remoteSerializersModuleShort(
                    callableMap = config.callableMap,
                ) + remoteClassSerializersModule(
                    remoteClasses = config.classes?.remoteClasses ?: listOf(),
                    leaseManager = config.classes?.server?.leaseManager,
                    nodeUrl = config.classes?.server?.nodeUrl,
                    onStubDeserialization = config.classes?.client?.onStubDeserialization,
                )
            })
        }
    }
    config.classes?.server?.leaseManager?.startCleanupJob(CoroutineScope(Dispatchers.Default))
    application.monitor.subscribe(ApplicationStopped) {
        config.classes?.server?.leaseManager?.stopCleanupJob()
    }
}
