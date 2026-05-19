package social.launchers

import org.koin.core.context.startKoin
import social.di.webBffModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(webBffModule) }
    remoteServer(8201).start(wait = true)
}
