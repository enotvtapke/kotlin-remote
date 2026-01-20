package todoapp.server

import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import todoapp.model.CreateTodoRequest
import todoapp.model.Todo
import todoapp.model.UpdateTodoRequest

class TodoRepository {

    fun readAll(): List<Todo> = transaction {
        Todos.selectAll()
            .orderBy(Todos.createdAt to SortOrder.DESC)
            .map { it.toTodo() }
    }

    fun create(req: CreateTodoRequest): Todo = transaction {
        val inserted = Todos.insert { row ->
            row[title] = req.title
            row[done] = false
        }
        val id = inserted[Todos.id]
        Todos.selectAll().where { Todos.id eq id }.single().toTodo()
    }

    fun update(id: Long, req: UpdateTodoRequest): Todo = transaction {
        val updated = Todos.update({ Todos.id eq id }) { row ->
            req.title?.let { row[title] = it }
            req.done?.let { row[done] = it }
        }
        if (updated == 0) throw IllegalArgumentException("No todo with id $id found")
        Todos.selectAll().where { Todos.id eq id }.single().toTodo()
    }

    fun delete(id: Long) = transaction {
        val deleted = Todos.deleteWhere { Todos.id eq id }
        if (deleted == 0) throw IllegalArgumentException("No todo with id $id found")
    }

    private fun ResultRow.toTodo(): Todo = Todo(
        id = this[Todos.id].value,
        title = this[Todos.title],
        done = this[Todos.done],
        createdAt = this[Todos.createdAt].toKotlinLocalDateTime()
    )
}
