package social.launchers

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.withService
import org.koin.core.context.startKoin
import social.api.CommentService
import social.api.FeedService
import social.api.FollowService
import social.api.LikeService
import social.api.MobileBffService
import social.api.NotificationService
import social.api.PostService
import social.api.SearchService
import social.api.UserService
import social.api.WebBffService
import social.di.commentModule
import social.di.dep
import social.di.feedModule
import social.di.followModule
import social.di.likeModule
import social.di.mobileBffModule
import social.di.notificationModule
import social.di.postModule
import social.di.searchModule
import social.di.userModule
import social.di.webBffModule
import social.remote.COMMENT_PORT
import social.remote.FEED_PORT
import social.remote.FOLLOW_PORT
import social.remote.LIKE_PORT
import social.remote.MOBILE_BFF_PORT
import social.remote.NOTIFICATION_PORT
import social.remote.POST_PORT
import social.remote.SEARCH_PORT
import social.remote.USER_PORT
import social.remote.WEB_BFF_PORT
import social.remote.rpcConn
import social.remote.rpcHttpClient
import social.remote.rpcServer
import social.repository.CommentRepository
import social.repository.FollowRepository
import social.repository.LikeRepository
import social.repository.NotificationRepository
import social.repository.PostRepository
import social.repository.UserRepository
import social.server.CommentServiceImpl
import social.server.FeedServiceImpl
import social.server.FollowServiceImpl
import social.server.LikeServiceImpl
import social.server.MobileBffServiceImpl
import social.server.NotificationServiceImpl
import social.server.PostServiceImpl
import social.server.SearchServiceImpl
import social.server.UserServiceImpl
import social.server.WebBffServiceImpl

fun main() {
    startAll()
}

fun startAll() = runBlocking {
    startKoin {
        modules(
            userModule, postModule, commentModule, likeModule,
            followModule, feedModule, notificationModule, searchModule,
            webBffModule, mobileBffModule,
        )
    }
    val leafServers = listOf(
        rpcServer(USER_PORT) { registerService<UserService> { UserServiceImpl(dep<UserRepository>()) } },
        rpcServer(POST_PORT) { registerService<PostService> { PostServiceImpl(dep<PostRepository>()) } },
        rpcServer(COMMENT_PORT) { registerService<CommentService> { CommentServiceImpl(dep<CommentRepository>()) } },
        rpcServer(LIKE_PORT) { registerService<LikeService> { LikeServiceImpl(dep<LikeRepository>()) } },
        rpcServer(FOLLOW_PORT) { registerService<FollowService> { FollowServiceImpl(dep<FollowRepository>()) } },
        rpcServer(NOTIFICATION_PORT) { registerService<NotificationService> { NotificationServiceImpl(dep<NotificationRepository>()) } },
    )
    leafServers.forEach { it.start(wait = false) }
    val http = rpcHttpClient()
    val postS = rpcConn(http, POST_PORT).withService<PostService>()
    val userS = rpcConn(http, USER_PORT).withService<UserService>()
    val likeS = rpcConn(http, LIKE_PORT).withService<LikeService>()
    val commentS = rpcConn(http, COMMENT_PORT).withService<CommentService>()
    val followS = rpcConn(http, FOLLOW_PORT).withService<FollowService>()
    val notificationS = rpcConn(http, NOTIFICATION_PORT).withService<NotificationService>()
    val orchestrators = listOf(
        rpcServer(FEED_PORT) { registerService<FeedService> { FeedServiceImpl(postS, userS, likeS, commentS, followS) } },
        rpcServer(SEARCH_PORT) { registerService<SearchService> { SearchServiceImpl(postS, userS) } },
    )
    orchestrators.forEach { it.start(wait = false) }
    val feedS = rpcConn(http, FEED_PORT).withService<FeedService>()
    val bffServers = listOf(
        rpcServer(WEB_BFF_PORT) { registerService<WebBffService> { WebBffServiceImpl(userS, feedS, notificationS, followS) } },
        rpcServer(MOBILE_BFF_PORT) { registerService<MobileBffService> { MobileBffServiceImpl(userS, postS, feedS, notificationS, followS) } },
    )
    bffServers.forEach { it.start(wait = false) }
    println("All 10 services started.")
    Runtime.getRuntime().addShutdownHook(Thread {
        (leafServers + orchestrators + bffServers).forEach { it.stop(1000, 2000) }
    })
    Thread.currentThread().join()
}
