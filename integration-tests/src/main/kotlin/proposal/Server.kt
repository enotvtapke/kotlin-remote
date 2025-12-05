package proposal

import kotlinx.remote.CallableMap
import kotlinx.remote.genCallableMap
import remoteEmbeddedServer

fun main() {
    CallableMap.putAll(genCallableMap())
    remoteEmbeddedServer().start(wait = true)
}
