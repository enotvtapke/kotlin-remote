package bank.paymentService

import bank.accountService.Accounts
import bank.api.Account
import bank.api.Payment
import bank.api.User
import java.math.BigDecimal
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class PaymentRepository {
    fun createPayment(amount: BigDecimal, payerAccountId: Long, payeeAccountId: Long): Payment = transaction {
        val inserted = Payments.insert {
            it[Payments.amount] = amount
            it[payerAccount] = payerAccountId
            it[payeeAccount] = payeeAccountId
        }
        Payments.selectAll().where { Payments.id eq inserted[Payments.id] }.single().toPayment()
    }

    fun userPayments(userId: Long): List<Payment> = transaction {
        val payer = Accounts.alias("payer_accounts")
        val payee = Accounts.alias("payee_accounts")
        Payments
            .innerJoin(payer, { Payments.payerAccount }, { payer[Accounts.id] })
            .innerJoin(payee, { Payments.payeeAccount }, { payee[Accounts.id] })
            .selectAll()
            .where { (payer[Accounts.owner] eq userId) or (payee[Accounts.owner] eq userId) }
            .map { it.toPayment() }
    }
}

private val placeholderUser = User(1, "", "")

fun ResultRow.toPayment() = Payment(
    this[Payments.id].value,
    this[Payments.amount],
    Account(this[Payments.payerAccount].value, placeholderUser, 0.toBigDecimal()),
    Account(this[Payments.payeeAccount].value, placeholderUser, 0.toBigDecimal()),
    this[Payments.time].toKotlinLocalDateTime()
)
