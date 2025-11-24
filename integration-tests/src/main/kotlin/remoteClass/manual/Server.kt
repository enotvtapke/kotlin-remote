package remoteClass.manual

import remoteEmbeddedServer

fun main() {
    initCallableMap()
    remoteEmbeddedServer().start(wait = true)
}
