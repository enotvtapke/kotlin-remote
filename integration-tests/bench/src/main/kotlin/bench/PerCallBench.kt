package bench

import kotlinx.coroutines.runBlocking
import kotlinx.remote.runWith

private const val WARMUP = 2000
private const val MEASURE = 10000
private const val LIST_SIZE = 100

fun main() = runBlocking {
    val port = 18080
    val server = benchServer(port).start(wait = false)
    Thread.sleep(500)

    val cfg = BenchConfig("localhost", port)

    println("=== Kotlin Remote per-call benchmark ===")
    println("warmup=$WARMUP  measure=$MEASURE  list_size=$LIST_SIZE")
    println()

    cfg.runWith {
        run("ping (no args, Unit return)", WARMUP, MEASURE) { ping() }
        run("echoLong(0)", WARMUP, MEASURE) { echoLong(0L) }
        run("echoString(small)", WARMUP, MEASURE) { echoString("hello") }
        run("echoString(1KB)", WARMUP, MEASURE) {
            echoString(BIG_STRING)
        }
        run("addLong(1,2)", WARMUP, MEASURE) { addLong(1, 2) }
        run("createTodo(req)", WARMUP, MEASURE) {
            createTodo(CreateTodoRequest("milk"))
        }
        run("listTodos(100)", WARMUP / 5, MEASURE / 5) {
            listTodos(LIST_SIZE)
        }
    }

    server.stop(1000, 2000)
}

private val BIG_STRING = "x".repeat(1024)

private suspend fun run(
    name: String,
    warmup: Int,
    measure: Int,
    block: suspend () -> Any?,
) {
    repeat(warmup) { block() }
    val samples = LongArray(measure)
    for (i in 0 until measure) {
        val t0 = System.nanoTime()
        block()
        samples[i] = System.nanoTime() - t0
    }
    val s = stats(samples)
    println("%-25s %s".format(name, s.formatUs()))
}
