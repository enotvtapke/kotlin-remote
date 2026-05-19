package social.launchers

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.withService
import org.koin.core.context.startKoin
import social.api.FeedService
import social.api.FollowService
import social.api.NotificationService
import social.api.UserService
import social.api.WebBffService
import social.di.webBffModule
import social.remote.FEED_PORT
import social.remote.FOLLOW_PORT
import social.remote.NOTIFICATION_PORT
import social.remote.USER_PORT
import social.remote.WEB_BFF_PORT
import social.remote.rpcConn
import social.remote.rpcHttpClient
import social.remote.rpcServer
import social.server.WebBffServiceImpl

fun main(): Unit = runBlocking {
    startKoin { modules(webBffModule) }
    val http = rpcHttpClient()
    val userS = rpcConn(http, USER_PORT).withService<UserService>()
    val feedS = rpcConn(http, FEED_PORT).withService<FeedService>()
    val notificationS = rpcConn(http, NOTIFICATION_PORT).withService<NotificationService>()
    val followS = rpcConn(http, FOLLOW_PORT).withService<FollowService>()
    rpcServer(WEB_BFF_PORT) {
        registerService<WebBffService> { WebBffServiceImpl(userS, feedS, notificationS, followS) }
    }.start(wait = true)
    Unit
}
