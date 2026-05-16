package social.launchers

import org.koin.core.context.startKoin
import social.di.feedModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(feedModule) }
    remoteServer(8106).start(wait = true)
}
