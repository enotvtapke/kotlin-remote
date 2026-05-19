package social.launchers

import org.koin.core.context.startKoin
import social.di.mobileBffModule
import social.remote.remoteServer

fun main() {
    startKoin { modules(mobileBffModule) }
    remoteServer(8202).start(wait = true)
}
