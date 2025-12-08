package kotlinx.remote

@Target(AnnotationTarget.FUNCTION)
annotation class Remote

interface RemoteContext {
    val client: RemoteClient
}

object LocalContext: RemoteContext {
    override val client: RemoteClient by lazy { error("Local context does not have client.") }
}
