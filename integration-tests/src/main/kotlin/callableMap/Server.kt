package callableMap

import kotlinx.remote.CallableMap
import remoteEmbeddedServer

fun main() {
    CallableMap.init()
    remoteEmbeddedServer().start(wait = true)
}
