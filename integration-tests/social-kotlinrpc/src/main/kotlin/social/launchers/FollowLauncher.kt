package social.launchers

import org.koin.core.context.startKoin
import social.api.FollowService
import social.di.dep
import social.di.followModule
import social.remote.FOLLOW_PORT
import social.remote.rpcServer
import social.repository.FollowRepository
import social.server.FollowServiceImpl

fun main(): Unit {
    startKoin { modules(followModule) }
    rpcServer(FOLLOW_PORT) {
        registerService<FollowService> { FollowServiceImpl(dep<FollowRepository>()) }
    }.start(wait = true)
}
