package social.api

import kotlinx.rpc.annotations.Rpc
import social.model.AddCommentDto
import social.model.Comment
import social.model.CreatePostDto
import social.model.EditPostDto
import social.model.FeedItem
import social.model.Follow
import social.model.Like
import social.model.Notification
import social.model.Post
import social.model.MobileHomePage
import social.model.MobileProfile
import social.model.SendNotificationDto
import social.model.UpdateProfileDto
import social.model.User
import social.model.WebHomePage
import social.model.WebProfilePage

@Rpc
interface UserService {
    suspend fun register(login: String, password: String, name: String): User
    suspend fun login(login: String, password: String): User
    suspend fun getUser(id: Long): User
    suspend fun updateProfile(id: Long, dto: UpdateProfileDto): User
    suspend fun searchByName(query: String): List<User>
}

@Rpc
interface PostService {
    suspend fun createPost(dto: CreatePostDto): Post
    suspend fun getPost(id: Long): Post
    suspend fun listUserPosts(authorId: Long): List<Post>
    suspend fun deletePost(id: Long)
    suspend fun editPost(dto: EditPostDto): Post
    suspend fun allPosts(): List<Post>
}

@Rpc
interface CommentService {
    suspend fun addComment(dto: AddCommentDto): Comment
    suspend fun getPostComments(postId: Long): List<Comment>
    suspend fun deleteComment(id: Long)
}

@Rpc
interface LikeService {
    suspend fun likePost(postId: Long, userId: Long): Like
    suspend fun unlikePost(postId: Long, userId: Long)
    suspend fun getPostLikes(postId: Long): List<Like>
    suspend fun getUserLikes(userId: Long): List<Like>
}

@Rpc
interface FollowService {
    suspend fun follow(followerId: Long, followeeId: Long): Follow
    suspend fun unfollow(followerId: Long, followeeId: Long)
    suspend fun getFollowers(userId: Long): List<Long>
    suspend fun getFollowing(userId: Long): List<Long>
    suspend fun isFollowing(followerId: Long, followeeId: Long): Boolean
}

@Rpc
interface FeedService {
    suspend fun getFeed(userId: Long): List<FeedItem>
    suspend fun getTrending(): List<FeedItem>
    suspend fun getUserFeed(userId: Long): List<FeedItem>
}

@Rpc
interface NotificationService {
    suspend fun sendNotification(dto: SendNotificationDto): Notification
    suspend fun getNotifications(userId: Long): List<Notification>
    suspend fun markAsRead(id: Long): Notification
    suspend fun markAllAsRead(userId: Long): Int
}

@Rpc
interface SearchService {
    suspend fun searchPosts(query: String): List<Post>
    suspend fun searchUsers(query: String): List<User>
    suspend fun searchByHashtag(tag: String): List<Post>
}

@Rpc
interface WebBffService {
    suspend fun getWebHomePage(userId: Long): WebHomePage
    suspend fun getWebProfilePage(userId: Long, viewerId: Long): WebProfilePage
}

@Rpc
interface MobileBffService {
    suspend fun getMobileHomePage(userId: Long, offset: Int = 0, limit: Int = 10): MobileHomePage
    suspend fun getMobileProfile(userId: Long): MobileProfile
}
