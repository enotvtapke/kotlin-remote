package kotlinx.remote

interface RemoteClient {
    suspend fun call(call: RemoteCall): Any?
}

@Suppress("UNCHECKED_CAST")
suspend fun <T> RemoteClient.call(call: RemoteCall): T {
    return call(call) as T
}
