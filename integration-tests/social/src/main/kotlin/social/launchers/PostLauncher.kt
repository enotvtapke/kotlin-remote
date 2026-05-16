package social.launchers

import org.koin.core.context.startKoin
import social.di.postModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(postModule) }
    remoteServer(8102).start(wait = true)
}
