package roomschat.server

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import roomschat.api.ChatMessage
import roomschat.api.ChatService

class ChatServiceImpl(override val coroutineContext: CoroutineContext) : ChatService {

    private val rooms = mutableMapOf<String, MutableSet<String>>()
    private val users = mutableMapOf<String, Channel<ChatMessage>>()

    override suspend fun login(userName: String) {
        if (users.containsKey(userName)) error("User '$userName' already exists")
        users[userName] = Channel(Channel.UNLIMITED)
    }

    override suspend fun logout(userName: String) {
        users.remove(userName)?.close() ?: error("User '$userName' not found")
        rooms.values.forEach { it.remove(userName) }
    }

    override suspend fun createRoom(name: String) {
        if (rooms.containsKey(name)) error("Room '$name' already exists")
        rooms[name] = mutableSetOf()
    }

    override suspend fun enterRoom(userName: String, roomName: String) {
        val room = rooms[roomName] ?: error("Room '$roomName' not found")
        if (!room.add(userName)) error("User '$userName' is already in room '$roomName'")
    }

    override suspend fun exitRoom(userName: String, roomName: String) {
        val room = rooms[roomName] ?: error("Room '$roomName' not found")
        if (!room.remove(userName)) error("User '$userName' not in room '$roomName'")
    }

    override suspend fun broadcast(userName: String, roomName: String, text: String) {
        val room = rooms[roomName] ?: error("Room '$roomName' not found")
        val msg = ChatMessage(userName, roomName, text)
        room.filter { it != userName }.forEach { users[it]?.trySend(msg) }
    }

    override suspend fun listRooms(): List<String> = rooms.keys.toList()

    override fun subscribe(userName: String): Flow<ChatMessage> {
        val ch = users[userName] ?: error("User '$userName' not found")
        return ch.consumeAsFlow()
    }
}
