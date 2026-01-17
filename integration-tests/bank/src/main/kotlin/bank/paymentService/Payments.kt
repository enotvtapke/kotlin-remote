package bank.paymentService

import bank.accountService.Accounts
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Payments: LongIdTable("payments") {
    val amount = decimal("amount", 20, 5)
    val payerAccount = reference("payer_account", Accounts)
    val payeeAccount = reference("payee_account", Accounts)
    val time = datetime("time").defaultExpression(CurrentDateTime)
}