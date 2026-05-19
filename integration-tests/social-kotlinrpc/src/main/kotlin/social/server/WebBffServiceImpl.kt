package social.server

import social.api.FeedService
import social.api.FollowService
import social.api.NotificationService
import social.api.UserService
import social.api.WebBffService
import social.model.WebHomePage
import social.model.WebProfilePage

class WebBffServiceImpl(
    private val userService: UserService,
    private val feedService: FeedService,
    private val notificationService: NotificationService,
    private val followService: FollowService,
) : WebBffService {
    override suspend fun getWebHomePage(userId: Long): WebHomePage {
        val user = userService.getUser(userId)
        val feed = feedService.getFeed(userId)
        val notifications = notificationService.getNotifications(userId).take(10)
        val trending = feedService.getTrending().take(5)
        val followerCount = followService.getFollowers(userId).size
        val followingCount = followService.getFollowing(userId).size
        return WebHomePage(user, feed, notifications, trending, followerCount, followingCount)
    }

    override suspend fun getWebProfilePage(userId: Long, viewerId: Long): WebProfilePage {
        val user = userService.getUser(userId)
        val posts = feedService.getUserFeed(userId)
        val followerCount = followService.getFollowers(userId).size
        val followingCount = followService.getFollowing(userId).size
        val isFollowedByMe = followService.isFollowing(viewerId, userId)
        return WebProfilePage(user, posts, followerCount, followingCount, isFollowedByMe)
    }
}
