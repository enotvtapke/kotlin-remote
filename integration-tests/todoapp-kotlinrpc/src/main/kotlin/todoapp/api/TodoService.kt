package todoapp.api

import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import todoapp.model.CreateTodoRequest
import todoapp.model.Todo
import todoapp.model.UpdateTodoRequest

@Rpc
interface TodoService : RemoteService {
    suspend fun createTodo(request: CreateTodoRequest): Todo
    suspend fun updateTodo(id: Long, request: UpdateTodoRequest): Todo
    suspend fun deleteTodo(id: Long)
    suspend fun todos(): List<Todo>
}
