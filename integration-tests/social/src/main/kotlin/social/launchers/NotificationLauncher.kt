package social.launchers

import org.koin.core.context.startKoin
import social.di.notificationModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(notificationModule) }
    remoteServer(8107).start(wait = true)
}
