package social.launchers

import org.koin.core.context.startKoin
import social.di.followModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(followModule) }
    remoteServer(8105).start(wait = true)
}
