package bench

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json

class BenchServiceImpl : BenchService {
    override suspend fun ping() {}
    override suspend fun echoLong(x: Long): Long = x
    override suspend fun echoString(s: String): String = s
    override suspend fun addLong(a: Long, b: Long): Long = a + b
    override suspend fun createTodo(req: CreateTodoRequest): Todo = Todo(1, req.title, false)
    override suspend fun listTodos(n: Int): List<Todo> = buildList {
        var seed = 0L
        repeat(n) {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            add(Todo(it.toLong(), "item-$it-${seed.toString(16)}", it and 1 == 0))
        }
    }
}

fun benchServer(port: Int) =
    embeddedServer(Netty, port = port, watchPaths = emptyList()) {
        install(Krpc)
        routing {
            rpc("/api") {
                rpcConfig { serialization { json() } }
                registerService<BenchService> { BenchServiceImpl() }
            }
        }
    }
