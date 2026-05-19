package social.server

import social.api.UserService
import social.model.UpdateProfileDto
import social.model.User
import social.repository.UserRepository

class UserServiceImpl(
    private val repo: UserRepository,
) : UserService {
    override suspend fun register(login: String, password: String, name: String): User = repo.register(login, password, name)
    override suspend fun login(login: String, password: String): User = repo.login(login, password)
    override suspend fun getUser(id: Long): User = repo.getUser(id)
    override suspend fun updateProfile(id: Long, dto: UpdateProfileDto): User = repo.updateProfile(id, dto)
    override suspend fun searchByName(query: String): List<User> = repo.searchByName(query)
}
