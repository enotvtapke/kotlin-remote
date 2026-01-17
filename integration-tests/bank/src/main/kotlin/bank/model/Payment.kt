package bank.model

import bank.model.serializers.BigDecimalSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Payment(
    val id: Long,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val payerAccount: Account,
    val payeeAccount: Account,
    val time: LocalDateTime,
)
