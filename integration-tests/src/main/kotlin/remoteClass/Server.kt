package remoteClass

import remoteEmbeddedServer

fun main() {
    initCallableMap()
    remoteEmbeddedServer().start(wait = true)
}
