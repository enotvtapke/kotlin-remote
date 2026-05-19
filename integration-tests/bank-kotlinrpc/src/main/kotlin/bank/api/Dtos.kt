package bank.api

import bank.api.serializers.BigDecimalSerializer
import java.math.BigDecimal
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Long, val login: String, val name: String)

@Serializable
data class Account(
    val id: Long,
    val owner: User,
    @Serializable(with = BigDecimalSerializer::class) val balance: BigDecimal
)

@Serializable
data class Payment(
    val id: Long,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val payerAccount: Account,
    val payeeAccount: Account,
    val time: LocalDateTime,
)

@Serializable
data class UserRegisterDto(val login: String, val password: String, val name: String)

@Serializable
data class CreateAccountDto(val ownerId: Long)

@Serializable
data class MakePaymentDto(
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val payerAccountId: Long,
    val payeeAccountId: Long
)
