package bank.userService

import bank.remote.UserServiceConfig
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

object UserServiceDeps {
    private val userRepository: UserRepository = UserRepositoryImpl()
    private val userService: UserService = UserService(userRepository)

    @Remote
    context(_: RemoteContext<UserServiceConfig>)
    suspend fun userService(): UserService = userService
}
