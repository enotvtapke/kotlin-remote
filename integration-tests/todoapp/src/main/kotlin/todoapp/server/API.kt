package todoapp.server

import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import todoapp.model.CreateTodoRequest
import todoapp.model.Todo
import todoapp.model.UpdateTodoRequest
import todoapp.remote.ServerConfig

@Remote
context(_: RemoteContext<ServerConfig>)
suspend fun createTodo(request: CreateTodoRequest): Todo = Dependencies.repository.create(request)

@Remote
context(_: RemoteContext<ServerConfig>)
suspend fun updateTodo(id: Long, request: UpdateTodoRequest): Todo = Dependencies.repository.update(id, request)

@Remote
context(_: RemoteContext<ServerConfig>)
suspend fun deleteTodo(id: Long) = Dependencies.repository.delete(id)

@Remote
context(_: RemoteContext<ServerConfig>)
suspend fun todos(): List<Todo> = Dependencies.repository.readAll()