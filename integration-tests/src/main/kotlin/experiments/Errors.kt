package experiments

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import roomsChat.HostContext

context(_: RemoteContext)
suspend fun error1() {
    error()
}

@Remote
context(_: RemoteContext)
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
//    context(DefaultLocalContext) {
//        error()
//    }
    context(HostContext) {
//        try {
        error1()
//        } catch (e: Exception) {
//            println("Error: ${e.message}")
//        }
    }
}