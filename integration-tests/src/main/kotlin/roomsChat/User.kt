package roomsChat

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.remote.*
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.asContext
import kotlinx.serialization.json.Json
import remoteEmbeddedServer
import startLeaseOnStubDeserialization

data class User(val name: String, val remoteContext: RemoteContext<RemoteConfig>)

val rooms = mutableMapOf<String, Room>()
val users = mutableMapOf<String, User>()

@RemoteSerializable
class Room private constructor() {
    private val clients = mutableListOf<User>()

    @Remote
    context(_: RemoteContext<RemoteConfig>)
    suspend fun enter(userName: String) {
        if (clients.any { it.name == userName }) error("User $userName is already in this room")
        clients.add(users[userName] ?: error("User $userName not found"))
    }

    @Remote
    context(_: RemoteContext<RemoteConfig>)
    suspend fun exit(userName: String) {
        val user = users[userName] ?: error("User $userName not found")
        if (!clients.remove(user)) error("User $userName is not in this room")
    }

    @Remote
    context(_: RemoteContext<RemoteConfig>)
    suspend fun broadcast(fromUserName: String, message: String) {
        clients.filter { it.name != fromUserName }.forEach {
            context(it.remoteContext) { send("$fromUserName: $message") }
        }
    }

    companion object {
        @Remote
        context(_: RemoteContext<RemoteConfig>)
        suspend operator fun invoke(name: String): Room {
            if (rooms.containsKey(name)) error("Room '$name' already exists")
            val room = Room()
            rooms[name] = room
            return room
        }
    }
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun getRoom(name: String): Room {
    return rooms[name] ?: error("Room '$name' not found")
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun listRooms(): List<String> {
    return rooms.keys.toList()
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun send(message: String) {
    println(message)
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun login(userName: String, url: String) {
    if (users.containsKey(userName)) error("User '$userName' already exists")
    users[userName] = User(userName, ClientConfig(url).asContext())
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun logout(userName: String) {
    users.remove(userName) ?: error("User '$userName' not found")
}

val callableMap = genCallableMap()

class ClientConfig(private val url: String) : RemoteConfig {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url(this@ClientConfig.url)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModule(
                    remoteClasses = genRemoteClassList(),
                    callableMap = callableMap,
                    onStubDeserialization = startLeaseOnStubDeserialization(),
                )
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(callableMap)
}

data object HostConfig : RemoteConfig {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModule {
                    callableMap = genCallableMap()
                    classes {
                        remoteClasses = genRemoteClassList()
                        client { }
                    }
                }
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(callableMap)
}

fun printHelp() {
    println("""
        |Available commands:
        |  /create <room_name>  - Create a new room
        |  /enter <room_name>   - Enter an existing room
        |  /exit                - Exit the current room
        |  /rooms               - List all available rooms
        |  /logout              - Logout and exit the application
        |  /help                - Show this help message
        |  
        |  When in a room, type any message to broadcast it to other users
    """.trimMargin())
}

fun main(): Unit = runBlocking {
    context(HostConfig.asContext()) {
        println("Enter your port:")
        val port = readln()
        val clientUrl = "http://localhost:$port"

        println("Enter your username:")
        val userName = readln()

        remoteEmbeddedServer(clientUrl).start(wait = false)

        try {
            login(userName, clientUrl)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return@context
        }

        println("Logged in as '$userName'")
        printHelp()

        var currentRoom: Room? = null
        var currentRoomName: String? = null
        var running = true

        while (running) {
            val input = readlnOrNull() ?: break

            try {
                when {
                    input.startsWith("/create ") -> {
                        val roomName = input.removePrefix("/create ").trim()
                        if (roomName.isEmpty()) {
                            println("Error: Room name cannot be empty")
                            continue
                        }
                        Room(roomName)
                        println("Room '$roomName' created successfully")
                    }

                    input.startsWith("/enter ") -> {
                        val roomName = input.removePrefix("/enter ").trim()
                        if (roomName.isEmpty()) {
                            println("Error: Room name cannot be empty")
                            continue
                        }
                        if (currentRoom != null) {
                            println("Error: You are already in room '$currentRoomName'. Use /exit first")
                            continue
                        }
                        val room = getRoom(roomName)
                        room.enter(userName)
                        currentRoom = room
                        currentRoomName = roomName
                        println("Entered room '$roomName'")
                    }

                    input == "/exit" -> {
                        if (currentRoom == null) {
                            println("Error: You are not in any room")
                            continue
                        }
                        currentRoom.exit(userName)
                        println("Exited room '$currentRoomName'")
                        currentRoom = null
                        currentRoomName = null
                    }

                    input == "/rooms" -> {
                        val roomsList = listRooms()
                        if (roomsList.isEmpty()) {
                            println("No rooms available. Create one with /create <room_name>")
                        } else {
                            println("Available rooms:")
                            roomsList.forEach { println("  - $it") }
                        }
                    }

                    input == "/logout" -> {
                        if (currentRoom != null) {
                            try {
                                currentRoom.exit(userName)
                            } catch (_: Exception) {
                            }
                        }
                        logout(userName)
                        println("Logged out. Goodbye!")
                        running = false
                    }

                    input == "/help" -> {
                        printHelp()
                    }

                    input.startsWith("/") -> {
                        println("Unknown command. Type /help for available commands")
                    }

                    else -> {
                        if (currentRoom == null) {
                            println("You are not in any room. Use /enter <room_name> to join a room")
                            continue
                        }
                        currentRoom.broadcast(userName, input)
                        println("You: $input")
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
}
