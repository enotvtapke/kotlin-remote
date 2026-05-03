package gc

import kotlinx.remote.classes.RemoteClassDescriptor
import kotlinx.remote.classes.lease.LeaseConfig
import remoteEmbeddedServer
import kotlin.reflect.KClass

fun manualGenRemoteClassList(): List<RemoteClassDescriptor<Any>> {
    return listOf(
        RemoteClassDescriptor(
            CalculatorGC::class as KClass<Any>,
            "gc.CalculatorGC",
        ) { id, url -> CalculatorGC.RemoteClassStub(id, url) }
    )
}

fun main() {
    remoteEmbeddedServer(leaseConfig = LeaseConfig(1000, 200, 0)).start(wait = true)
}
