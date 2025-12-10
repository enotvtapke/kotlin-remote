package kotlinx.remote

@Target(AnnotationTarget.FUNCTION)
annotation class Remote

data class CallerInfo(val url: String)

sealed interface RemoteContext {
    val client: RemoteClient
    val callerInfo: CallerInfo
}

abstract class NonlocalContext: RemoteContext {
    final override val callerInfo: CallerInfo by lazy { error("Nonlocal context does not have caller info.") }
}

abstract class LocalContext: RemoteContext {
    final override val client: RemoteClient by lazy { error("Local context does not have client.") }
}

object DefaultLocalContext: LocalContext() {
    override val callerInfo: CallerInfo by lazy { error("Default local context does not have caller info.") }
}

class InjectedContext(override val callerInfo: CallerInfo): LocalContext()
