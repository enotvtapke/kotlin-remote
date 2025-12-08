package gc

import kotlinx.remote.classes.lease.LeaseConfig
import remoteEmbeddedServer
import kotlin.reflect.KClass

fun manualGenRemoteClassList(): List<Pair<KClass<Any>, (Long, String) -> Any>> {
    return listOf(
        CalculatorGC::class as KClass<Any> to { id, url -> CalculatorGC.RemoteClassStub(id, url) }
    )
}

fun main() {
    remoteEmbeddedServer(LeaseConfig(1000, 200, 0)).start(wait = true)
}
