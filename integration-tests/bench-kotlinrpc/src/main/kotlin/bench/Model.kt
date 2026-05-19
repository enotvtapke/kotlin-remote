package bench

import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    val id: Long,
    val title: String,
    val done: Boolean,
)

@Serializable
data class CreateTodoRequest(val title: String)
