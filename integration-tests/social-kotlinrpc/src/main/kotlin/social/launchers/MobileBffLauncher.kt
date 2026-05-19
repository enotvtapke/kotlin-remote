package social.launchers

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.withService
import org.koin.core.context.startKoin
import social.api.FeedService
import social.api.FollowService
import social.api.MobileBffService
import social.api.NotificationService
import social.api.PostService
import social.api.UserService
import social.di.mobileBffModule
import social.remote.FEED_PORT
import social.remote.FOLLOW_PORT
import social.remote.MOBILE_BFF_PORT
import social.remote.NOTIFICATION_PORT
import social.remote.POST_PORT
import social.remote.USER_PORT
import social.remote.rpcConn
import social.remote.rpcHttpClient
import social.remote.rpcServer
import social.server.MobileBffServiceImpl

fun main(): Unit = runBlocking {
    startKoin { modules(mobileBffModule) }
    val http = rpcHttpClient()
    val userS = rpcConn(http, USER_PORT).withService<UserService>()
    val postS = rpcConn(http, POST_PORT).withService<PostService>()
    val feedS = rpcConn(http, FEED_PORT).withService<FeedService>()
    val notificationS = rpcConn(http, NOTIFICATION_PORT).withService<NotificationService>()
    val followS = rpcConn(http, FOLLOW_PORT).withService<FollowService>()
    rpcServer(MOBILE_BFF_PORT) {
        registerService<MobileBffService> { MobileBffServiceImpl(userS, postS, feedS, notificationS, followS) }
    }.start(wait = true)
    Unit
}
