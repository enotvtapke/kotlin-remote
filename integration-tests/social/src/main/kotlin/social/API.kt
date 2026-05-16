package social

import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext
import social.di.dep
import social.model.*
import social.remote.*
import social.repository.*

// === Users ===

@Remote context(_: RemoteContext<UserServiceConfig>)
suspend fun register(login: String, password: String, name: String): User =
    dep<UserRepository>().register(login, password, name)

@Remote context(_: RemoteContext<UserServiceConfig>)
suspend fun login(login: String, password: String): User =
    dep<UserRepository>().login(login, password)

@Remote context(_: RemoteContext<UserServiceConfig>)
suspend fun getUser(id: Long): User = dep<UserRepository>().getUser(id)

@Remote context(_: RemoteContext<UserServiceConfig>)
suspend fun updateProfile(id: Long, dto: UpdateProfileDto): User =
    dep<UserRepository>().updateProfile(id, dto)

@Remote context(_: RemoteContext<UserServiceConfig>)
suspend fun searchByName(query: String): List<User> = dep<UserRepository>().searchByName(query)

// === Posts ===

@Remote context(_: RemoteContext<PostServiceConfig>)
suspend fun createPost(dto: CreatePostDto): Post = dep<PostRepository>().createPost(dto)

@Remote context(_: RemoteContext<PostServiceConfig>)
suspend fun getPost(id: Long): Post = dep<PostRepository>().getPost(id)

@Remote context(_: RemoteContext<PostServiceConfig>)
suspend fun listUserPosts(authorId: Long): List<Post> = dep<PostRepository>().listUserPosts(authorId)

@Remote context(_: RemoteContext<PostServiceConfig>)
suspend fun deletePost(id: Long) = dep<PostRepository>().deletePost(id)

@Remote context(_: RemoteContext<PostServiceConfig>)
suspend fun editPost(dto: EditPostDto): Post = dep<PostRepository>().editPost(dto)

@Remote context(_: RemoteContext<PostServiceConfig>)
suspend fun allPosts(): List<Post> = dep<PostRepository>().allPosts()

// === Comments ===

@Remote context(_: RemoteContext<CommentServiceConfig>)
suspend fun addComment(dto: AddCommentDto): Comment = dep<CommentRepository>().addComment(dto)

@Remote context(_: RemoteContext<CommentServiceConfig>)
suspend fun getPostComments(postId: Long): List<Comment> = dep<CommentRepository>().getPostComments(postId)

@Remote context(_: RemoteContext<CommentServiceConfig>)
suspend fun deleteComment(id: Long) = dep<CommentRepository>().deleteComment(id)

// === Likes ===

@Remote context(_: RemoteContext<LikeServiceConfig>)
suspend fun likePost(postId: Long, userId: Long): Like = dep<LikeRepository>().likePost(postId, userId)

@Remote context(_: RemoteContext<LikeServiceConfig>)
suspend fun unlikePost(postId: Long, userId: Long) = dep<LikeRepository>().unlikePost(postId, userId)

@Remote context(_: RemoteContext<LikeServiceConfig>)
suspend fun getPostLikes(postId: Long): List<Like> = dep<LikeRepository>().getPostLikes(postId)

@Remote context(_: RemoteContext<LikeServiceConfig>)
suspend fun getUserLikes(userId: Long): List<Like> = dep<LikeRepository>().getUserLikes(userId)

// === Follows ===

@Remote context(_: RemoteContext<FollowServiceConfig>)
suspend fun follow(followerId: Long, followeeId: Long): Follow =
    dep<FollowRepository>().follow(followerId, followeeId)

@Remote context(_: RemoteContext<FollowServiceConfig>)
suspend fun unfollow(followerId: Long, followeeId: Long) =
    dep<FollowRepository>().unfollow(followerId, followeeId)

@Remote context(_: RemoteContext<FollowServiceConfig>)
suspend fun getFollowers(userId: Long): List<Long> = dep<FollowRepository>().getFollowers(userId)

@Remote context(_: RemoteContext<FollowServiceConfig>)
suspend fun getFollowing(userId: Long): List<Long> = dep<FollowRepository>().getFollowing(userId)

@Remote context(_: RemoteContext<FollowServiceConfig>)
suspend fun isFollowing(followerId: Long, followeeId: Long): Boolean =
    dep<FollowRepository>().isFollowing(followerId, followeeId)

// === Feed ===

@Remote context(_: RemoteContext<FeedServiceConfig>)
suspend fun getFeed(userId: Long): List<FeedItem> {
    val followees = context(FollowServiceConfig.asContext()) { getFollowing(userId) }
    val posts = followees.flatMap { context(PostServiceConfig.asContext()) { listUserPosts(it) } }
    return posts.map { p ->
        val author = context(UserServiceConfig.asContext()) { getUser(p.authorId) }
        val likeCount = context(LikeServiceConfig.asContext()) { getPostLikes(p.id) }.size
        val commentCount = context(CommentServiceConfig.asContext()) { getPostComments(p.id) }.size
        FeedItem(p, author, likeCount, commentCount)
    }
}

@Remote context(_: RemoteContext<FeedServiceConfig>)
suspend fun getTrending(): List<FeedItem> {
    val posts = context(PostServiceConfig.asContext()) { allPosts() }
    return posts.map { p ->
        val author = context(UserServiceConfig.asContext()) { getUser(p.authorId) }
        val likeCount = context(LikeServiceConfig.asContext()) { getPostLikes(p.id) }.size
        val commentCount = context(CommentServiceConfig.asContext()) { getPostComments(p.id) }.size
        FeedItem(p, author, likeCount, commentCount)
    }.sortedByDescending { it.likes }
}

@Remote context(_: RemoteContext<FeedServiceConfig>)
suspend fun getUserFeed(userId: Long): List<FeedItem> {
    val posts = context(PostServiceConfig.asContext()) { listUserPosts(userId) }
    val author = context(UserServiceConfig.asContext()) { getUser(userId) }
    return posts.map { p ->
        val likeCount = context(LikeServiceConfig.asContext()) { getPostLikes(p.id) }.size
        val commentCount = context(CommentServiceConfig.asContext()) { getPostComments(p.id) }.size
        FeedItem(p, author, likeCount, commentCount)
    }
}

// === Notifications ===

@Remote context(_: RemoteContext<NotificationServiceConfig>)
suspend fun sendNotification(dto: SendNotificationDto): Notification =
    dep<NotificationRepository>().sendNotification(dto)

@Remote context(_: RemoteContext<NotificationServiceConfig>)
suspend fun getNotifications(userId: Long): List<Notification> =
    dep<NotificationRepository>().getNotifications(userId)

@Remote context(_: RemoteContext<NotificationServiceConfig>)
suspend fun markAsRead(id: Long): Notification = dep<NotificationRepository>().markAsRead(id)

@Remote context(_: RemoteContext<NotificationServiceConfig>)
suspend fun markAllAsRead(userId: Long): Int = dep<NotificationRepository>().markAllAsRead(userId)

// === Search ===

@Remote context(_: RemoteContext<SearchServiceConfig>)
suspend fun searchPosts(query: String): List<Post> {
    val posts = context(PostServiceConfig.asContext()) { allPosts() }
    return posts.filter { it.text.contains(query, ignoreCase = true) }
}

@Remote context(_: RemoteContext<SearchServiceConfig>)
suspend fun searchUsers(query: String): List<User> =
    context(UserServiceConfig.asContext()) { searchByName(query) }

@Remote context(_: RemoteContext<SearchServiceConfig>)
suspend fun searchByHashtag(tag: String): List<Post> {
    val needle = "#$tag"
    val posts = context(PostServiceConfig.asContext()) { allPosts() }
    return posts.filter { it.text.contains(needle, ignoreCase = true) }
}
