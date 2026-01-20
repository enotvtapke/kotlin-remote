package todoapp.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateTodoRequest(
    val title: String
)

@Serializable
data class UpdateTodoRequest(
    val title: String? = null,
    val done: Boolean? = null
)
