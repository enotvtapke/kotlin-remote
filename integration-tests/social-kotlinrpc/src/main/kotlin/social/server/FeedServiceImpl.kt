package social.server

import social.api.CommentService
import social.api.FeedService
import social.api.FollowService
import social.api.LikeService
import social.api.PostService
import social.api.UserService
import social.model.FeedItem

class FeedServiceImpl(
    private val postService: PostService,
    private val userService: UserService,
    private val likeService: LikeService,
    private val commentService: CommentService,
    private val followService: FollowService,
) : FeedService {
    override suspend fun getFeed(userId: Long): List<FeedItem> {
        val followees = followService.getFollowing(userId)
        val posts = followees.flatMap { postService.listUserPosts(it) }
        return posts.map { p ->
            val author = userService.getUser(p.authorId)
            val likeCount = likeService.getPostLikes(p.id).size
            val commentCount = commentService.getPostComments(p.id).size
            FeedItem(p, author, likeCount, commentCount)
        }
    }

    override suspend fun getTrending(): List<FeedItem> {
        val posts = postService.allPosts()
        return posts.map { p ->
            val author = userService.getUser(p.authorId)
            val likeCount = likeService.getPostLikes(p.id).size
            val commentCount = commentService.getPostComments(p.id).size
            FeedItem(p, author, likeCount, commentCount)
        }.sortedByDescending { it.likes }
    }

    override suspend fun getUserFeed(userId: Long): List<FeedItem> {
        val posts = postService.listUserPosts(userId)
        val author = userService.getUser(userId)
        return posts.map { p ->
            val likeCount = likeService.getPostLikes(p.id).size
            val commentCount = commentService.getPostComments(p.id).size
            FeedItem(p, author, likeCount, commentCount)
        }
    }
}
