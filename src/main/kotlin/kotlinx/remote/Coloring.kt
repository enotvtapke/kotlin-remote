package kotlinx.remote

import kotlinx.remote.network.RemoteClient
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class Remote(val config: KClass<out RemoteConfig>)

interface RemoteContext

interface RemoteConfig {
    val context: RemoteContext
    val client: RemoteClient
}