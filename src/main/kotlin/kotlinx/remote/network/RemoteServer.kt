package kotlinx.remote.network

import kotlinx.remote.CallableMap

interface RemoteServer {
    suspend fun handleCall(call: RemoteCall): Any?
}

object RemoteServerImpl : RemoteServer {
    override suspend fun handleCall(call: RemoteCall): Any? {
        return CallableMap[call.callableName].invokator.call(call.parameters)
    }
}