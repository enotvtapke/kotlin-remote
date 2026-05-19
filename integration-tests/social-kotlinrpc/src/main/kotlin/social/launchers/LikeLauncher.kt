package social.launchers

import org.koin.core.context.startKoin
import social.api.LikeService
import social.di.dep
import social.di.likeModule
import social.remote.LIKE_PORT
import social.remote.rpcServer
import social.repository.LikeRepository
import social.server.LikeServiceImpl

fun main(): Unit {
    startKoin { modules(likeModule) }
    rpcServer(LIKE_PORT) {
        registerService<LikeService> { LikeServiceImpl(dep<LikeRepository>()) }
    }.start(wait = true)
}
