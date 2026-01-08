package experiments

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext
import roomsChat.HostConfig

context(_: RemoteContext<RemoteConfig>)
suspend fun error1() {
    error()
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun error() {
    innerError()
}

fun innerError() {
    innerInnerError()
}

fun innerInnerError() {
    throw IllegalArgumentException("My Error")
}

fun main(): Unit = runBlocking {
//    context(Local) {
//        error()
//    }
    context(HostConfig.asContext()) {
//        try {
        error1()
//        } catch (e: Exception) {
//            println("Error: ${e.message}")
//        }
    }
}