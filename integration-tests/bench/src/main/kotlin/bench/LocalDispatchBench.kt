package bench

import kotlinx.coroutines.runBlocking
import kotlinx.remote.LocalContext
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.runWith

suspend fun mulPlain(a: Long, b: Long): Long = a * b

@Remote
context(_: RemoteContext<BenchConfig>)
suspend fun mulRemote(a: Long, b: Long): Long = a * b

private const val WARMUP = 200_000
private const val MEASURE = 1_000_000

fun main() = runBlocking {
    val port = 18082
    val server = benchServer(port).start(wait = false)
    Thread.sleep(500)
    val cfg = BenchConfig("localhost", port)

    println("=== Kotlin Remote local-dispatch benchmark ===")
    println("warmup=$WARMUP  measure=$MEASURE")
    println()

    bench("plain suspend fun mulPlain(2,3)", WARMUP, MEASURE) {
        mulPlain(2L, 3L)
    }

    context(LocalContext) {
        bench("@Remote mulRemote(2,3) in LocalContext", WARMUP, MEASURE) {
            mulRemote(2L, 3L)
        }
    }

    cfg.runWith {
        bench("@Remote mulRemote(2,3) in ConfiguredContext (network)", 2_000, 10_000) {
            mulRemote(2L, 3L)
        }
    }

    server.stop(1000, 2000)
}

private suspend inline fun bench(
    name: String,
    warmup: Int,
    measure: Int,
    crossinline block: suspend () -> Any?,
) {
    repeat(warmup) { block() }
    val t0 = System.nanoTime()
    var sink: Any? = null
    for (i in 0 until measure) {
        sink = block()
    }
    val elapsed = System.nanoTime() - t0
    val perCallNs = elapsed.toDouble() / measure
    println("%-55s n=$measure  per-call=%.1f ns  (total=%.2f ms, sink=$sink)".format(
        name, perCallNs, elapsed / 1_000_000.0
    ))
}
