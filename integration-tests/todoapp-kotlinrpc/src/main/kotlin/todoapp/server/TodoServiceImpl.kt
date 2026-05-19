package todoapp.server

import todoapp.api.TodoService
import todoapp.model.CreateTodoRequest
import todoapp.model.Todo
import todoapp.model.UpdateTodoRequest

class TodoServiceImpl(
    private val repository: TodoRepository
) : TodoService {
    override suspend fun createTodo(request: CreateTodoRequest): Todo = repository.create(request)
    override suspend fun updateTodo(id: Long, request: UpdateTodoRequest): Todo = repository.update(id, request)
    override suspend fun deleteTodo(id: Long) = repository.delete(id)
    override suspend fun todos(): List<Todo> = repository.readAll()
}
