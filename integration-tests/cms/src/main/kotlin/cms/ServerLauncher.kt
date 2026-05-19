package cms

import cms.di.cmsModule
import cms.remote.cmsServer
import org.koin.core.context.startKoin

fun main() {
    startCms()
}

fun startCms() {
    startKoin { modules(cmsModule) }
    val server = cmsServer(port = 8080)
    server.start(wait = false)
    println("CMS server started on http://localhost:8080")
    Runtime.getRuntime().addShutdownHook(Thread { server.stop(1000, 2000) })
    Thread.currentThread().join()
}
