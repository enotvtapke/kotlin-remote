package bank.userService

import org.jetbrains.exposed.dao.id.LongIdTable

object Users: LongIdTable("users") {
    val name = varchar("name", 100)
    val login = varchar("login", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
}
