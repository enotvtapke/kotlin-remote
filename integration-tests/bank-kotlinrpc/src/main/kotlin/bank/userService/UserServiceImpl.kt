package bank.userService

import bank.api.User
import bank.api.UserRegisterDto
import bank.api.UserService
import kotlin.coroutines.CoroutineContext

class UserServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val repository: UserRepository
) : UserService {
    override suspend fun login(login: String, password: String): User = repository.login(login, password)
    override suspend fun register(dto: UserRegisterDto): User = repository.register(dto)
}
