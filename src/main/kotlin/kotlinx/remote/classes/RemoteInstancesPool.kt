package kotlinx.remote.classes

import java.util.concurrent.ConcurrentHashMap

object RemoteInstancesPool {
    val instances = ConcurrentHashMap<Long, Any?>()
}