package bank.paymentService

import bank.accountService.Accounts
import bank.model.Account
import bank.model.Payment
import bank.model.User
import bank.model.dto.MakePaymentDto
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

interface PaymentRepository {
    fun createPayment(payment: MakePaymentDto): Payment
    fun userPayments(userId: Long): List<Payment>
}

class PaymentRepositoryImpl : PaymentRepository {
    override fun createPayment(payment: MakePaymentDto): Payment = transaction {
        val newPayment = Payments.insert {
            it[amount] = payment.amount
            it[payerAccount] = payment.payerAccountId
            it[payeeAccount] = payment.payeeAccountId
        }
        Payments.selectAll().where { Payments.id eq newPayment[Payments.id] }.single().toPayment()
    }

    override fun userPayments(userId: Long): List<Payment> = transaction {
        val payerAccounts = Accounts.alias("payer_accounts")
        val payeeAccounts = Accounts.alias("payee_accounts")

        Payments
            .innerJoin(payerAccounts, { Payments.payerAccount }, { payerAccounts[Accounts.id] })
            .innerJoin(payeeAccounts, { Payments.payeeAccount }, { payeeAccounts[Accounts.id] })
            .selectAll()
            .where { (payerAccounts[Accounts.owner] eq userId) or (payeeAccounts[Accounts.owner] eq userId) }
            .map { it.toPayment() }
    }
}

fun ResultRow.toPayment() = Payment(
    this[Payments.id].value,
    this[Payments.amount],
    Account(this[Payments.payerAccount].value, User(1, "", ""), 0.toBigDecimal()),
    Account(this[Payments.payeeAccount].value, User(1, "", ""), 0.toBigDecimal()),
    this[Payments.time].toKotlinLocalDateTime()
)
