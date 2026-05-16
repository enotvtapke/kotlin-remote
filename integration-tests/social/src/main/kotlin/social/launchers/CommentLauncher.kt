package social.launchers

import org.koin.core.context.startKoin
import social.di.commentModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(commentModule) }
    remoteServer(8103).start(wait = true)
}
