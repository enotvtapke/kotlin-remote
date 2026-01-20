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