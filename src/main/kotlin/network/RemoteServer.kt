package org.jetbrains.kotlinx.network

import org.jetbrains.kotlinx.CallableMap

interface RemoteServer {
    suspend fun handleCall(call: RemoteCall): Any?
}

object RemoteServerImpl : RemoteServer {
    override suspend fun handleCall(call: RemoteCall): Any? {
        return CallableMap[call.callableName].invokator.call(call.parameters)
    }
}