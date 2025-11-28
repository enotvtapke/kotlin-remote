package kotlinx.remote.network

import kotlinx.remote.network.serialization.RpcCallSerializer
import kotlinx.serialization.Serializable

@Serializable(with = RpcCallSerializer::class)
class RemoteCall(
    val callableName: String,
    val parameters: Array<Any?>,
)