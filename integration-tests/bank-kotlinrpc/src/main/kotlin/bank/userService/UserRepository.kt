package bank.userService

import bank.api.User
import bank.api.UserRegisterDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

class UserRepository {
    fun login(login: String, password: String): User = transaction {
        val row = Users.selectAll().where { Users.login eq login }.single()
        if (!BCrypt.checkpw(password, row[Users.passwordHash])) error("Invalid password")
        row.toUser()
    }

    fun register(dto: UserRegisterDto): User = transaction {
        val inserted = Users.insert {
            it[name] = dto.name
            it[login] = dto.login
            it[passwordHash] = BCrypt.hashpw(dto.password, BCrypt.gensalt())
        }
        Users.selectAll().where { Users.id eq inserted[Users.id] }.single().toUser()
    }
}

fun ResultRow.toUser() = User(this[Users.id].value, this[Users.login], this[Users.name])
