package social.launchers

import org.koin.core.context.startKoin
import social.di.likeModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(likeModule) }
    remoteServer(8104).start(wait = true)
}
