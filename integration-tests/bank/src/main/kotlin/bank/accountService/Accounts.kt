package bank.accountService

import bank.userService.Users
import org.jetbrains.exposed.dao.id.LongIdTable

object Accounts: LongIdTable("accounts") {
    val balance = decimal("balance", 20, 5).default(0.toBigDecimal())
    val owner = reference("owner", Users)
}
