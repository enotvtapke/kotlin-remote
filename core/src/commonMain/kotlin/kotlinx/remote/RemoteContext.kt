package kotlinx.remote

@Target(AnnotationTarget.FUNCTION)
annotation class Remote


interface RemoteContext {
    val client: RemoteClient
}

abstract class LocalContext: RemoteContext {
    final override val client: RemoteClient by lazy { error("Local context does not have client.") }
}

object DefaultLocalContext: LocalContext()
