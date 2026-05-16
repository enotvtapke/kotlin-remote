package social.client

import kotlinx.coroutines.runBlocking
import kotlinx.remote.RemoteContext
import social.*
import social.launchers.startAll
import social.model.AddCommentDto
import social.model.CreatePostDto
import social.model.SendNotificationDto
import social.remote.*
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.system.exitProcess

context(_ : RemoteContext<UserServiceConfig>, _ : RemoteContext<PostServiceConfig>, _ : RemoteContext<FollowServiceConfig>, _ : RemoteContext<LikeServiceConfig>, _ : RemoteContext<CommentServiceConfig>, _ : RemoteContext<NotificationServiceConfig>, _ : RemoteContext<FeedServiceConfig>, _ : RemoteContext<SearchServiceConfig>)
suspend fun testClient() {
    val alice = register("alice", "pw", "Alice")
    val bob = register("bob", "pw", "Bob")

    val p1 = createPost(CreatePostDto(alice.id, "Hello #world"))
    createPost(CreatePostDto(alice.id, "Second post by Alice"))

    follow(bob.id, alice.id)
    likePost(p1.id, bob.id)
    addComment(AddCommentDto(p1.id, bob.id, "Nice!"))
    sendNotification(SendNotificationDto(alice.id, "like", "Bob liked your post"))

    val feed = getFeed(bob.id)
    println("Bob's feed: ${feed.joinToString("\n")}")

    val found = searchByHashtag("world")
    println("Posts with #world: $found")
}

fun main() = runBlocking {
    thread {
        startAll()
    }
    sleep(1000)
    context(
        SearchServiceContext,
        FeedServiceContext,
        UserServiceContext,
        PostServiceContext,
        FollowServiceContext,
        LikeServiceContext,
        CommentServiceContext,
        NotificationServiceContext
    ) {
        testClient()
    }
    exitProcess(0)
    Unit
}
