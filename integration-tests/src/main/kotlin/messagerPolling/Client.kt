package messagerPolling

import ClientContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.remote.CallableMap
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

private val loggedUsers = mutableMapOf<String, MutableList<String>>()

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun login(name: String) {
    loggedUsers[name] = mutableListOf()
}

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun logout(name: String) {
    loggedUsers.remove(name) ?: error("User $name not found")
}

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun send(from: String, to: String, message: String) {
    loggedUsers[to]?.add("[$from] $message") ?: error("User $to not found")
}

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun receive(user: String): String? {
    return (loggedUsers[user] ?: error("User $user not found")).removeFirstOrNull()
}

fun main(): Unit = runBlocking {
    CallableMap.init()
    context(ClientContext) {
        println("Enter your name:")
        val name = readln()
        println("Type 'logout' to exit, or 'userName message' to send message to another user:")
        login(name)
        val receiveJob = launch {
            while (true) {
                delay(1000)
                receive(name)?.let { if (it != "null") println(it) }
            }
        }
        launch(Dispatchers.IO) {
            while (true) {
                val input = readln()
                if (input == "logout") {
                    logout(name)
                    receiveJob.cancel()
                    break
                }
                val (to, message) = input.split(' ', limit = 2)
                send(name, to, message)
            }
        }
    }
}
