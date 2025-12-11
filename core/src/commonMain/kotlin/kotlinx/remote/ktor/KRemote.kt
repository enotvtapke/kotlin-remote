package kotlinx.remote.ktor

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val KRemoteServerPluginAttributesKey = AttributeKey<KRemoteConfig>("KRemoteServerPluginAttributesKey")

val KRemote: ApplicationPlugin<KRemoteConfigBuilder> = createApplicationPlugin(
    name = "KRemote",
    createConfiguration = { KRemoteConfigBuilder() },
) {
    val config = pluginConfig.build()
    application.attributes.put(KRemoteServerPluginAttributesKey, config)
    application.pluginOrNull(ContentNegotiation) ?: run {
        application.install(ContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModule(
                    callableMap = config.callableMap,
                    remoteClasses = config.classes?.remoteClasses,
                    leaseManager = config.classes?.server?.leaseManager,
                    nodeUrl = config.classes?.server?.nodeUrl,
                    onStubDeserialization = config.classes?.client?.onStubDeserialization,
                    serializersModule = config.serializersModule ?: SerializersModule { }
                )
            })
        }
    }
    config.classes?.server?.leaseManager?.startCleanupJob(CoroutineScope(Dispatchers.Default))
    application.monitor.subscribe(ApplicationStopped) {
        config.classes?.server?.leaseManager?.stopCleanupJob()
    }
}
