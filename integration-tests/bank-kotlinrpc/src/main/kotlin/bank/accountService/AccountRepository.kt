package bank.accountService

import bank.api.Account
import bank.api.CreateAccountDto
import bank.userService.Users
import bank.userService.toUser
import java.math.BigDecimal
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class AccountRepository {
    fun createAccount(dto: CreateAccountDto): Account = transaction {
        val inserted = Accounts.insert { it[owner] = dto.ownerId }
        Accounts.innerJoin(Users).selectAll().where { Accounts.id eq inserted[Accounts.id] }.single().toAccount()
    }

    fun accountById(id: Long): Account = transaction {
        Accounts.innerJoin(Users).selectAll().where { Accounts.id eq id }.single().toAccount()
    }

    fun userAccounts(userId: Long): List<Account> = transaction {
        Accounts.innerJoin(Users).selectAll().where { Users.id eq userId }.map { it.toAccount() }
    }

    fun transfer(fromAccountId: Long, toAccountId: Long, amount: BigDecimal) = transaction {
        Accounts.update({ Accounts.id eq fromAccountId }) { it.update(balance, balance - amount) }
        Accounts.update({ Accounts.id eq toAccountId }) { it.update(balance, balance + amount) }
        Unit
    }
}

fun ResultRow.toAccount() = Account(this[Accounts.id].value, this.toUser(), this[Accounts.balance])
