package todoapp.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    val id: Long,
    val title: String,
    val done: Boolean,
    val createdAt: LocalDateTime
)

@Serializable
data class CreateTodoRequest(
    val title: String
)

@Serializable
data class UpdateTodoRequest(
    val title: String? = null,
    val done: Boolean? = null
)
