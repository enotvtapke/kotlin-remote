package social.server

import social.api.FeedService
import social.api.FollowService
import social.api.MobileBffService
import social.api.NotificationService
import social.api.PostService
import social.api.UserService
import social.model.MobileFeedItem
import social.model.MobileHomePage
import social.model.MobileProfile

class MobileBffServiceImpl(
    private val userService: UserService,
    private val postService: PostService,
    private val feedService: FeedService,
    private val notificationService: NotificationService,
    private val followService: FollowService,
) : MobileBffService {
    override suspend fun getMobileHomePage(userId: Long, offset: Int, limit: Int): MobileHomePage {
        val feed = feedService.getFeed(userId).drop(offset).take(limit)
        val unread = notificationService.getNotifications(userId).count { !it.read }
        return MobileHomePage(
            feed = feed.map { MobileFeedItem(it.post.id, it.post.text, it.author.name, it.likes) },
            unreadNotifications = unread,
        )
    }

    override suspend fun getMobileProfile(userId: Long): MobileProfile {
        val user = userService.getUser(userId)
        val postCount = postService.listUserPosts(userId).size
        val followerCount = followService.getFollowers(userId).size
        return MobileProfile(user.name, postCount, followerCount)
    }
}
