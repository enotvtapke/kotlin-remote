package social.launchers

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.withService
import org.koin.core.context.startKoin
import social.api.CommentService
import social.api.FeedService
import social.api.FollowService
import social.api.LikeService
import social.api.PostService
import social.api.UserService
import social.di.feedModule
import social.remote.COMMENT_PORT
import social.remote.FEED_PORT
import social.remote.FOLLOW_PORT
import social.remote.LIKE_PORT
import social.remote.POST_PORT
import social.remote.USER_PORT
import social.remote.rpcConn
import social.remote.rpcHttpClient
import social.remote.rpcServer
import social.server.FeedServiceImpl

fun main(): Unit = runBlocking {
    startKoin { modules(feedModule) }
    val http = rpcHttpClient()
    val postS = rpcConn(http, POST_PORT).withService<PostService>()
    val userS = rpcConn(http, USER_PORT).withService<UserService>()
    val likeS = rpcConn(http, LIKE_PORT).withService<LikeService>()
    val commentS = rpcConn(http, COMMENT_PORT).withService<CommentService>()
    val followS = rpcConn(http, FOLLOW_PORT).withService<FollowService>()
    rpcServer(FEED_PORT) {
        registerService<FeedService> { FeedServiceImpl(postS, userS, likeS, commentS, followS) }
    }.start(wait = true)
    Unit
}
