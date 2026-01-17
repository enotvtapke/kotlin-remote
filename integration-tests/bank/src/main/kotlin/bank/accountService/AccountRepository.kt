package bank.accountService

import bank.model.Account
import bank.model.dto.CreateAccountDto
import bank.userService.Users
import bank.userService.toUser
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

interface AccountRepository {
    fun createAccount(dto: CreateAccountDto): Account
    fun accountById(id: Long): Account
    fun userAccounts(userId: Long): List<Account>
    fun transfer(fromAccountId: Long, toAccountId: Long, amount: BigDecimal)
}

class AccountRepositoryImpl : AccountRepository {
    override fun createAccount(dto: CreateAccountDto): Account = transaction {
        val newAccount = Accounts.insert {
            it[owner] = dto.ownerId
        }
        Accounts.innerJoin(Users).selectAll().where { Accounts.id eq newAccount[Accounts.id] }.single().toAccount()
    }

    override fun accountById(id: Long): Account = transaction {
        Accounts.innerJoin(Users).selectAll().where { Accounts.id eq id }.single().toAccount()
    }

    override fun userAccounts(userId: Long): List<Account> = transaction {
        Accounts.innerJoin(Users).selectAll().where { Users.id eq userId }.map { it.toAccount() }
    }

    override fun transfer(fromAccountId: Long, toAccountId: Long, amount: BigDecimal) = transaction {
        Accounts.update({ Accounts.id eq fromAccountId }) {
            it.update(balance, balance - amount)
        }
        Accounts.update({ Accounts.id eq toAccountId }) {
            it.update(balance, balance + amount)
        }
        Unit
    }

}

fun ResultRow.toAccount() = Account(this[Accounts.id].value, this.toUser(), this[Accounts.balance])
