package bench

import kotlinx.rpc.annotations.Rpc

@Rpc
interface BenchService {
    suspend fun ping()
    suspend fun echoLong(x: Long): Long
    suspend fun echoString(s: String): String
    suspend fun addLong(a: Long, b: Long): Long
    suspend fun createTodo(req: CreateTodoRequest): Todo
    suspend fun listTodos(n: Int): List<Todo>
}
