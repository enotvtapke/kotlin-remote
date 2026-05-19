package social.client

import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.withService
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
import social.launchers.startAll
import social.model.AddCommentDto
import social.model.CreatePostDto
import social.model.SendNotificationDto
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

suspend fun testClient(
    userService: UserService,
    postService: PostService,
    followService: FollowService,
    likeService: LikeService,
    commentService: CommentService,
    notificationService: NotificationService,
    feedService: FeedService,
    searchService: SearchService,
    webBffService: WebBffService,
    mobileBffService: MobileBffService,
) {
    val alice = userService.register("alice", "pw", "Alice")
    val bob = userService.register("bob", "pw", "Bob")

    val p1 = postService.createPost(CreatePostDto(alice.id, "Hello #world"))
    postService.createPost(CreatePostDto(alice.id, "Second post by Alice"))

    followService.follow(bob.id, alice.id)
    likeService.likePost(p1.id, bob.id)
    commentService.addComment(AddCommentDto(p1.id, bob.id, "Nice!"))
    notificationService.sendNotification(SendNotificationDto(alice.id, "like", "Bob liked your post"))

    val feed = feedService.getFeed(bob.id)
    println("Bob's feed: ${feed.joinToString("\n")}")

    val found = searchService.searchByHashtag("world")
    println("Posts with #world: $found")

    val webHome = webBffService.getWebHomePage(bob.id)
    println("Web home: $webHome")

    val webProfile = webBffService.getWebProfilePage(alice.id, bob.id)
    println("Web profile: $webProfile")

    val mobileHome = mobileBffService.getMobileHomePage(bob.id, 0, 5)
    println("Mobile home: $mobileHome")

    val mobileProfile = mobileBffService.getMobileProfile(alice.id)
    println("Mobile profile: $mobileProfile")
}

fun main(): Unit = runBlocking {
    thread { startAll() }
    sleep(2000)
    val http = rpcHttpClient()
    testClient(
        rpcConn(http, USER_PORT).withService<UserService>(),
        rpcConn(http, POST_PORT).withService<PostService>(),
        rpcConn(http, FOLLOW_PORT).withService<FollowService>(),
        rpcConn(http, LIKE_PORT).withService<LikeService>(),
        rpcConn(http, COMMENT_PORT).withService<CommentService>(),
        rpcConn(http, NOTIFICATION_PORT).withService<NotificationService>(),
        rpcConn(http, FEED_PORT).withService<FeedService>(),
        rpcConn(http, SEARCH_PORT).withService<SearchService>(),
        rpcConn(http, WEB_BFF_PORT).withService<WebBffService>(),
        rpcConn(http, MOBILE_BFF_PORT).withService<MobileBffService>(),
    )
    exitProcess(0)
}
