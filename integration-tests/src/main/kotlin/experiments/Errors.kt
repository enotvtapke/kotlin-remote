package experiments

import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.RemoteWrapper
import kotlinx.remote.wrapped
import roomsChat.HostContext

context(_: RemoteWrapper<RemoteContext>)
suspend fun error1() {
    error()
}

@Remote
context(_: RemoteWrapper<RemoteContext>)
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
    context(HostContext.wrapped) {
//        try {
        error1()
//        } catch (e: Exception) {
//            println("Error: ${e.message}")
//        }
    }
}