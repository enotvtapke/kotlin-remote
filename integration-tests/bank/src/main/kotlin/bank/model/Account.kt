package bank.model

import bank.model.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Account(
    val id: Long,
    val owner: User,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal
)
