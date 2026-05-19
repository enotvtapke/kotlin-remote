package social.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Long, val login: String, val name: String, val bio: String = "")

@Serializable
data class UpdateProfileDto(val name: String? = null, val bio: String? = null)

@Serializable
data class Post(
    val id: Long,
    val authorId: Long,
    val text: String,
    val createdAt: Instant = Clock.System.now()
)

@Serializable
data class CreatePostDto(val authorId: Long, val text: String)

@Serializable
data class EditPostDto(val id: Long, val text: String)

@Serializable
data class Comment(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val text: String,
    val createdAt: Instant = Clock.System.now()
)

@Serializable
data class AddCommentDto(val postId: Long, val authorId: Long, val text: String)

@Serializable
data class Like(val postId: Long, val userId: Long, val createdAt: Instant = Clock.System.now())

@Serializable
data class Follow(val followerId: Long, val followeeId: Long, val createdAt: Instant = Clock.System.now())

@Serializable
data class FeedItem(val post: Post, val author: User, val likes: Int, val comments: Int)

@Serializable
data class Notification(
    val id: Long,
    val userId: Long,
    val kind: String,
    val payload: String,
    val read: Boolean = false,
    val createdAt: Instant = Clock.System.now()
)

@Serializable
data class SendNotificationDto(val userId: Long, val kind: String, val payload: String)

@Serializable
data class WebHomePage(
    val user: User,
    val feed: List<FeedItem>,
    val notifications: List<Notification>,
    val trending: List<FeedItem>,
    val followerCount: Int,
    val followingCount: Int,
)

@Serializable
data class WebProfilePage(
    val user: User,
    val posts: List<FeedItem>,
    val followerCount: Int,
    val followingCount: Int,
    val isFollowedByMe: Boolean,
)

@Serializable
data class MobileFeedItem(val postId: Long, val text: String, val authorName: String, val likes: Int)

@Serializable
data class MobileHomePage(val feed: List<MobileFeedItem>, val unreadNotifications: Int)

@Serializable
data class MobileProfile(val name: String, val postCount: Int, val followerCount: Int)
