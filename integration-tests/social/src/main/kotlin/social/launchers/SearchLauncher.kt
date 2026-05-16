package social.launchers

import org.koin.core.context.startKoin
import social.di.searchModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(searchModule) }
    remoteServer(8108).start(wait = true)
}
