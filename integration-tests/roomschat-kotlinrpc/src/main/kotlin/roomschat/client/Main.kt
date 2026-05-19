package roomschat.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.client.Krpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import roomschat.api.ChatService

fun printHelp() {
    println(
        """
        |Available commands:
        |  /create <room_name>  - Create a new room
        |  /enter <room_name>   - Enter an existing room
        |  /exit                - Exit the current room
        |  /rooms               - List all available rooms
        |  /logout              - Logout and exit the application
        |  /help                - Show this help message
        |
        |  When in a room, type any message to broadcast it to other users
        """.trimMargin()
    )
}

fun main(): Unit = runBlocking {
    println("Enter your username:")
    val userName = readln()

    val httpClient = HttpClient(CIO) { install(Krpc) }
    val rpcClient = httpClient.rpc {
        url("ws://localhost:8080/api")
        rpcConfig { serialization { json() } }
    }
    val service = rpcClient.withService<ChatService>()

    try {
        service.login(userName)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        httpClient.close()
        return@runBlocking
    }
    println("Logged in as '$userName'")
    printHelp()

    val subscription = launch(Dispatchers.IO) {
        try {
            service.subscribe(userName).collect { msg -> println("${msg.fromUser}: ${msg.text}") }
        } catch (_: Exception) {
        }
    }

    var currentRoom: String? = null
    var running = true

    while (running) {
        val input = readlnOrNull() ?: break
        try {
            when {
                input.startsWith("/create ") -> {
                    val name = input.removePrefix("/create ").trim()
                    if (name.isEmpty()) { println("Error: Room name cannot be empty"); continue }
                    service.createRoom(name)
                    println("Room '$name' created successfully")
                }
                input.startsWith("/enter ") -> {
                    val name = input.removePrefix("/enter ").trim()
                    if (name.isEmpty()) { println("Error: Room name cannot be empty"); continue }
                    if (currentRoom != null) { println("Error: You are already in room '$currentRoom'. Use /exit first"); continue }
                    service.enterRoom(userName, name)
                    currentRoom = name
                    println("Entered room '$name'")
                }
                input == "/exit" -> {
                    if (currentRoom == null) { println("Error: You are not in any room"); continue }
                    service.exitRoom(userName, currentRoom!!)
                    println("Exited room '$currentRoom'")
                    currentRoom = null
                }
                input == "/rooms" -> {
                    val list = service.listRooms()
                    if (list.isEmpty()) println("No rooms available. Create one with /create <room_name>")
                    else { println("Available rooms:"); list.forEach { println("  - $it") } }
                }
                input == "/logout" -> {
                    if (currentRoom != null) {
                        try { service.exitRoom(userName, currentRoom!!) } catch (_: Exception) {}
                    }
                    service.logout(userName)
                    println("Logged out. Goodbye!")
                    running = false
                }
                input == "/help" -> printHelp()
                input.startsWith("/") -> println("Unknown command. Type /help for available commands")
                else -> {
                    if (currentRoom == null) { println("You are not in any room. Use /enter <room_name> to join a room"); continue }
                    service.broadcast(userName, currentRoom!!, input)
                    println("You: $input")
                }
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    subscription.cancel()
    httpClient.close()
}
