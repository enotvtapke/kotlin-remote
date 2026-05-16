package social.launchers

import org.koin.core.context.startKoin
import social.di.*
import social.remote.remoteServer

fun main() {
    startAll()
}

fun startAll() {
    startKoin {
        modules(
            userModule, postModule, commentModule, likeModule,
            followModule, feedModule, notificationModule, searchModule
        )
    }
    val ports = listOf(8101, 8102, 8103, 8104, 8105, 8106, 8107, 8108)
    val servers = ports.map { remoteServer(it).also { s -> s.start(wait = false) } }
    println("All 8 services started on ports ${ports.joinToString()}. Press Ctrl+C to stop.")
    Runtime.getRuntime().addShutdownHook(Thread {
        servers.forEach { it.stop(1000, 2000) }
    })
    Thread.currentThread().join()
}
