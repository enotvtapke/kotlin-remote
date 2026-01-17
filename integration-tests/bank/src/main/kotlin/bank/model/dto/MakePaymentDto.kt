package bank.model.dto

import bank.model.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class MakePaymentDto(
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val payerAccountId: Long,
    val payeeAccountId: Long
)