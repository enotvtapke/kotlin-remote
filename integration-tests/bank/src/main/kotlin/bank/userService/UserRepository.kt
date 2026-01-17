package bank.userService

import bank.exceptions.SecurityException
import bank.model.User
import bank.model.dto.UserRegisterDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

interface UserRepository {
    fun login(login: String, password: String): User
    fun register(user: UserRegisterDto): User
}

class UserRepositoryImpl : UserRepository {
    override fun login(login: String, password: String): User  = transaction {
        val row = Users.selectAll().where { Users.login eq login }.single()
        val storedHash = row[Users.passwordHash]
        if (!BCrypt.checkpw(password, storedHash)) throw SecurityException("Invalid password")
        row.toUser()
    }

    override fun register(user: UserRegisterDto): User = transaction {
        val newUser = Users.insert {
            it[name] = user.name
            it[login] = user.login
            it[passwordHash] = BCrypt.hashpw(user.password, BCrypt.gensalt())
        }
        Users.selectAll().where { Users.id eq newUser[Users.id] }.single().toUser()
    }
}

fun ResultRow.toUser() = User(this[Users.id].value, this[Users.name], this[Users.login])
