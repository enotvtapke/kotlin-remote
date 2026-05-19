package bench

import kotlinx.coroutines.runBlocking
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseReleaseRequest
import kotlinx.remote.classes.lease.LeaseRenewalRequest
import kotlinx.serialization.json.Json

private val SIZES = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000)

fun main() = runBlocking {
    println("=== Kotlin Remote lease / DGC benchmarks ===")
    println()

    println("--- (1) Server-side renewLeases(N) in-process (no network) ---")
    println("Measures cost of the synchronized hash-map renewal under the LeaseManager lock.")
    val pool = RemoteInstancesPool()
    val manager = LeaseManager(LeaseConfig(), pool)
    val maxN = SIZES.max()
    val populated = LongArray(maxN)
    for (i in populated.indices) populated[i] = manager.addInstanceWithLease(Any(), clientId = "c")

    for (n in SIZES) {
        val ids = populated.toList().subList(0, n)
        val req = LeaseRenewalRequest(ids, clientId = "c")
        repeat(20) { manager.renewLeases(req) }
        val iters = if (n <= 100) 5_000 else if (n <= 10_000) 200 else 20
        val t0 = System.nanoTime()
        repeat(iters) { manager.renewLeases(req) }
        val elapsed = System.nanoTime() - t0
        val perCallUs = elapsed / iters / 1000.0
        val perIdNs = elapsed.toDouble() / iters / n
        println(
            "  N=%-7d  per-renewal=%9.2f us  per-id=%7.1f ns  (iters=$iters)".format(
                n, perCallUs, perIdNs
            )
        )
    }

    println()
    println("--- (2) Renewal request body size and steady-state traffic ---")
    println("With default 10s renewal interval each tracked-server batch is sent 6 times / minute.")
    for (n in SIZES) {
        val ids = (1L..n).toList()
        val req = LeaseRenewalRequest(ids, clientId = "client-with-a-typical-uuid")
        val json = Json.encodeToString(LeaseRenewalRequest.serializer(), req)
        println(
            "  N=%-7d  body=%9d B  steady-state=%d B/min".format(n, json.length, json.length * 6)
        )
    }

    println()
    println("--- (3) Memory footprint per leased instance (server-side) ---")
    println("Cumulative used-heap measurement across growing pool sizes.")
    println("Includes the Any() user object reference. The LeaseManager + RemoteInstancesPool")
    println("hold all entries strongly, so they survive GC.")
    val rt = Runtime.getRuntime()
    fun usedHeap(): Long {
        repeat(6) { System.gc(); Thread.sleep(200) }
        return rt.totalMemory() - rt.freeMemory()
    }
    val mgrM = LeaseManager(LeaseConfig(), RemoteInstancesPool())
    val baseline = usedHeap()
    val checkpoints = intArrayOf(10_000, 100_000, 500_000)
    var added = 0
    var prevHeap = baseline
    var prevAdded = 0
    val sharedClientId = "client-uuid-typical"
    println("  baseline=${baseline / 1024} KB, single shared clientId per client (realistic)")
    for (target in checkpoints) {
        while (added < target) {
            mgrM.addInstanceWithLease(Any(), clientId = sharedClientId)
            added++
        }
        val cur = usedHeap()
        val deltaBytes = cur - prevHeap
        val deltaN = added - prevAdded
        val perStub = deltaBytes.toDouble() / deltaN
        println("  N=%-7d  used=%6d KB  +N=%-7d  +%6d KB  per-stub=${"%.1f".format(perStub)} B".format(
            added, cur / 1024, deltaN, deltaBytes / 1024
        ))
        prevHeap = cur
        prevAdded = added
    }
    // Touch mgrM to prevent dead-code elimination
    require(mgrM.leaseCount() == added)

    println()
    println("--- (4) cleanupExpiredInstances() cost ---")
    val pool4 = RemoteInstancesPool()
    val mgr4 = LeaseManager(LeaseConfig(), pool4)
    for (i in 0 until 100_000) mgr4.addInstanceWithLease(Any(), clientId = "c$i")
    // Pretend everything is expired immediately:
    val release = LeaseReleaseRequest((1L..100_000L).toList(), clientId = null)
    mgr4.releaseLeases(release)
    val t0 = System.nanoTime()
    val removed = mgr4.cleanupExpiredInstances()
    val elapsedUs = (System.nanoTime() - t0) / 1000.0
    println("  removed=$removed  elapsed=${"%.2f".format(elapsedUs)} us  per-id=${"%.1f".format(elapsedUs * 1000 / removed)} ns")
}
