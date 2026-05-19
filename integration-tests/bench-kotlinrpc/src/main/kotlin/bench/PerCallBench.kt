package bench

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.URLProtocol
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

private const val WARMUP = 2000
private const val MEASURE = 10000
private const val LIST_SIZE = 100

fun main() = runBlocking {
    val port = 18081
    val server = benchServer(port).start(wait = false)
    Thread.sleep(500)

    val http = HttpClient(CIO) { installKrpc() }
    val rpcClient = http.rpc {
        url {
            protocol = URLProtocol.WS
            host = "localhost"
            this.port = port
            pathSegments = listOf("api")
        }
        rpcConfig { serialization { json() } }
    }
    val svc = rpcClient.withService<BenchService>()

    println("=== kotlinx.rpc per-call benchmark ===")
    println("warmup=$WARMUP  measure=$MEASURE  list_size=$LIST_SIZE")
    println()

    run("ping (no args, Unit return)", WARMUP, MEASURE) { svc.ping() }
    run("echoLong(0)", WARMUP, MEASURE) { svc.echoLong(0L) }
    run("echoString(small)", WARMUP, MEASURE) { svc.echoString("hello") }
    run("echoString(1KB)", WARMUP, MEASURE) { svc.echoString(BIG_STRING) }
    run("addLong(1,2)", WARMUP, MEASURE) { svc.addLong(1, 2) }
    run("createTodo(req)", WARMUP, MEASURE) {
        svc.createTodo(CreateTodoRequest("milk"))
    }
    run("listTodos(100)", WARMUP / 5, MEASURE / 5) { svc.listTodos(LIST_SIZE) }

    http.close()
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
