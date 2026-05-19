package todoapp.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.URLProtocol
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import todoapp.api.TodoService
import todoapp.model.CreateTodoRequest

fun main() = runBlocking {
    val httpClient = HttpClient(CIO) { installKrpc() }
    val rpcClient = httpClient.rpc {
        url {
            protocol = URLProtocol.WS
            host = "localhost"
            port = 8000
            pathSegments = listOf("api")
        }
        rpcConfig { serialization { json() } }
    }
    val service = rpcClient.withService<TodoService>()
    service.createTodo(CreateTodoRequest("Buy milk"))
    service.createTodo(CreateTodoRequest("Sell cow"))
    println(service.todos())
    httpClient.close()
}
