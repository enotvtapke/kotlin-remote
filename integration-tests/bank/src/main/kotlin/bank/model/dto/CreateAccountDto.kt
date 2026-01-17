package bank.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountDto(
    val ownerId: Long,
)
