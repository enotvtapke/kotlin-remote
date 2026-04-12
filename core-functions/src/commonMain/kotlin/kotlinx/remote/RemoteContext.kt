package kotlinx.remote

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Target(AnnotationTarget.FUNCTION)
annotation class Remote(val name: String = "")

interface RemoteConfig {
    val client: RemoteClient
}

sealed interface RemoteContext <out T: RemoteConfig>
object LocalContext: RemoteContext<Nothing>
class ConfiguredContext<T: RemoteConfig>(val config: T): RemoteContext<T>

fun <T: RemoteConfig> T.asContext() = ConfiguredContext(this)

@OptIn(ExperimentalContracts::class)
inline fun <T: RemoteConfig, R> T.runWith(block: context(RemoteContext<T>) () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return context(ConfiguredContext(this)) { block() }
}
