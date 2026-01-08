package kotlinx.remote

@Target(AnnotationTarget.FUNCTION)
annotation class Remote

interface RemoteConfig {
    val client: RemoteClient
}

sealed interface RemoteContext <out T: RemoteConfig>
object LocalContext: RemoteContext<Nothing>
class ConfiguredContext<T: RemoteConfig>(val config: T): RemoteContext<T>

fun <T: RemoteConfig> T.asContext() = ConfiguredContext(this)
