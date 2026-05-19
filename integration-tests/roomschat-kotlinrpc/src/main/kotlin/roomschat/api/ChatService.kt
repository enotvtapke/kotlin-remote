package roomschat.api

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(val fromUser: String, val room: String, val text: String)

@Rpc
interface ChatService : RemoteService {
    suspend fun login(userName: String)
    suspend fun logout(userName: String)
    suspend fun createRoom(name: String)
    suspend fun enterRoom(userName: String, roomName: String)
    suspend fun exitRoom(userName: String, roomName: String)
    suspend fun broadcast(userName: String, roomName: String, text: String)
    suspend fun listRooms(): List<String>
    fun subscribe(userName: String): Flow<ChatMessage>
}
