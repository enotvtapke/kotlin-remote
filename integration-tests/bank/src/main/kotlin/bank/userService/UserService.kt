package bank.userService

import bank.model.User
import bank.model.dto.UserRegisterDto
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.RemoteSerializable

@RemoteSerializable
class UserService(
    private val userRepository: UserRepository,
) {

    @Remote
    context(_: RemoteContext<RemoteConfig>)
    suspend fun login(login: String, password: String): User {
        return userRepository.login(login, password)
    }

    @Remote
    context(_: RemoteContext<RemoteConfig>)
    suspend fun register(dto: UserRegisterDto): User {
        return userRepository.register(dto)
    }
}
