package bank.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserRegisterDto(val login: String, val password: String, val name: String)
