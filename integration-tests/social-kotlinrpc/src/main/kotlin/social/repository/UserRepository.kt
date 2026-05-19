package social.repository

import java.util.concurrent.atomic.AtomicLong
import social.model.UpdateProfileDto
import social.model.User

class UserRepository {
    private val nextId = AtomicLong(1)
    private val users = mutableMapOf<Long, User>()
    private val passwords = mutableMapOf<String, String>()

    fun register(login: String, password: String, name: String): User {
        if (passwords.containsKey(login)) error("User '$login' already exists")
        val user = User(nextId.getAndIncrement(), login, name)
        users[user.id] = user
        passwords[login] = password
        return user
    }

    fun login(login: String, password: String): User {
        if (passwords[login] != password) error("Invalid credentials")
        return users.values.first { it.login == login }
    }

    fun getUser(id: Long): User = users[id] ?: error("User $id not found")

    fun updateProfile(id: Long, dto: UpdateProfileDto): User {
        val u = getUser(id)
        val updated = u.copy(name = dto.name ?: u.name, bio = dto.bio ?: u.bio)
        users[id] = updated
        return updated
    }

    fun searchByName(query: String): List<User> =
        users.values.filter { it.name.contains(query, ignoreCase = true) }
}
