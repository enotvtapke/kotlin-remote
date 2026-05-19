package cms.repository

import cms.model.Role
import cms.model.UserAcc

data class InternalUser(val login: String, val password: String, var role: Role) {
    fun toAcc() = UserAcc(login, role)
}

class UserStore {
    private val users: MutableMap<String, InternalUser> = mutableMapOf(
        "alice" to InternalUser("alice", "pw", Role.AUTHOR),
        "bob" to InternalUser("bob", "pw", Role.MODERATOR),
        "charlie" to InternalUser("charlie", "pw", Role.ADMIN),
    )

    fun authenticate(login: String, password: String): InternalUser? =
        users[login]?.takeIf { it.password == password }

    fun list(): List<UserAcc> = users.values.map { it.toAcc() }

    fun count(): Int = users.size

    fun promote(login: String, role: Role): UserAcc {
        val u = users[login] ?: error("User $login not found")
        u.role = role
        return u.toAcc()
    }
}
