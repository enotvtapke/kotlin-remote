package bench

import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

@Remote
context(_: RemoteContext<BenchConfig>)
suspend fun ping() {
}

@Remote
context(_: RemoteContext<BenchConfig>)
suspend fun echoLong(x: Long): Long = x

@Remote
context(_: RemoteContext<BenchConfig>)
suspend fun echoString(s: String): String = s

@Remote
context(_: RemoteContext<BenchConfig>)
suspend fun createTodo(req: CreateTodoRequest): Todo =
    Todo(1, req.title, false)

@Remote
context(_: RemoteContext<BenchConfig>)
suspend fun listTodos(n: Int): List<Todo> = buildList {
    var seed = 0L
    repeat(n) {
        seed = seed * 6364136223846793005L + 1442695040888963407L
        add(Todo(it.toLong(), "item-$it-${seed.toString(16)}", it and 1 == 0))
    }
}

@Remote
context(_: RemoteContext<BenchConfig>)
suspend fun addLong(a: Long, b: Long): Long = a + b
