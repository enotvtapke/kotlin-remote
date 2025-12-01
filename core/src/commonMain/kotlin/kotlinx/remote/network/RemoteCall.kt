package kotlinx.remote.network

import kotlinx.remote.network.serialization.RpcCallSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = RpcCallSerializer::class)
class RemoteCall(
    val callableName: String,
    val parameters: Array<Any?>,
)

@Serializable
sealed interface RemoteResponse<T> {
    @Serializable
    @SerialName("success")
    data class Success<T>(val value: T): RemoteResponse<T>
    @Serializable
    @SerialName("failure")
    data class Failure(val error: @Polymorphic Exception): RemoteResponse<Any?>
}
