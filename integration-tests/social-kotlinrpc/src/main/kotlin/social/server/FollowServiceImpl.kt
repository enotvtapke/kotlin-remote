package social.server

import social.api.FollowService
import social.model.Follow
import social.repository.FollowRepository

class FollowServiceImpl(
    private val repo: FollowRepository,
) : FollowService {
    override suspend fun follow(followerId: Long, followeeId: Long): Follow = repo.follow(followerId, followeeId)
    override suspend fun unfollow(followerId: Long, followeeId: Long) = repo.unfollow(followerId, followeeId)
    override suspend fun getFollowers(userId: Long): List<Long> = repo.getFollowers(userId)
    override suspend fun getFollowing(userId: Long): List<Long> = repo.getFollowing(userId)
    override suspend fun isFollowing(followerId: Long, followeeId: Long): Boolean = repo.isFollowing(followerId, followeeId)
}
