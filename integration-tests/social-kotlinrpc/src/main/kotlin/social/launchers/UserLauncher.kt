package social.launchers

import org.koin.core.context.startKoin
import social.api.UserService
import social.di.dep
import social.di.userModule
import social.remote.USER_PORT
import social.remote.rpcServer
import social.repository.UserRepository
import social.server.UserServiceImpl

fun main(): Unit {
    startKoin { modules(userModule) }
    rpcServer(USER_PORT) {
        registerService<UserService> { UserServiceImpl(dep<UserRepository>()) }
    }.start(wait = true)
}
