package todoapp.client

import kotlinx.coroutines.runBlocking
import kotlinx.remote.asContext
import todoapp.model.CreateTodoRequest
import todoapp.remote.ServerConfig
import todoapp.server.createTodo
import todoapp.server.todos

fun main() = runBlocking {
    context(ServerConfig.asContext()) {
        createTodo(CreateTodoRequest("Buy milk"))
        createTodo(CreateTodoRequest("Sell cow"))
        val todos = todos()
        println(todos)
    }
    Unit
}
