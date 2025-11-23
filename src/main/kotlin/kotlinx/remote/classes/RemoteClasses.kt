package kotlinx.remote.classes

import java.util.concurrent.ConcurrentHashMap

object RemoteClasses {
    val remoteClasses = ConcurrentHashMap<Long, Any?>()
}