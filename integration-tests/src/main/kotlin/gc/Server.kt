package gc

import kotlinx.remote.classes.lease.LeaseConfig
import remoteEmbeddedServer
import kotlin.reflect.KClass

fun manualGenRemoteClassList(): List<Pair<KClass<Any>, (Long) -> Any>> {
    return listOf(
        CalculatorGC::class as KClass<Any> to { CalculatorGC.RemoteClassStub(it) }
    )
}

fun main() {
    remoteEmbeddedServer(LeaseConfig(1000, 200, 0)).start(wait = true)
}
