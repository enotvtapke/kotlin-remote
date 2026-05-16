package social.launchers

import org.koin.core.context.startKoin
import social.di.userModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(userModule) }
    remoteServer(8101).start(wait = true)
}
