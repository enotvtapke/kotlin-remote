package kotlinx.remote.network

import kotlinx.remote.CallableMap

interface RemoteServer {
    suspend fun handleCall(call: RemoteCall): RemoteResponse<Any?>
}

object RemoteServerImpl : RemoteServer {
    override suspend fun handleCall(call: RemoteCall): RemoteResponse<Any?> {
        return try {
            RemoteResponse.Success(CallableMap[call.callableName].invokator.call(call.parameters))
        } catch (e: Exception) {
            RemoteResponse.Failure(e)
        }
    }
}