package bank.model

import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Long, val login: String, val name: String)
