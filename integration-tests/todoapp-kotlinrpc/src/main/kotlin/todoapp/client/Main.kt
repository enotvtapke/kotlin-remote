package todoapp.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.client.Krpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import todoapp.api.TodoService
import todoapp.model.CreateTodoRequest

fun main() = runBlocking {
    val httpClient = HttpClient(CIO) { install(Krpc) }
    val rpcClient = httpClient.rpc {
        url("ws://localhost:8000/api")
        rpcConfig { serialization { json() } }
    }
    val service = rpcClient.withService<TodoService>()
    service.createTodo(CreateTodoRequest("Buy milk"))
    service.createTodo(CreateTodoRequest("Sell cow"))
    println(service.todos())
    httpClient.close()
}
