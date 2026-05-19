package social.repository

import social.model.Follow

class FollowRepository {
    private val follows = mutableSetOf<Follow>()

    fun follow(followerId: Long, followeeId: Long): Follow {
        if (followerId == followeeId) error("Cannot follow yourself")
        if (follows.any { it.followerId == followerId && it.followeeId == followeeId }) {
            error("Already following")
        }
        val f = Follow(followerId, followeeId)
        follows.add(f)
        return f
    }

    fun unfollow(followerId: Long, followeeId: Long) {
        val removed = follows.removeAll { it.followerId == followerId && it.followeeId == followeeId }
        if (!removed) error("Not following")
    }

    fun getFollowers(userId: Long): List<Long> =
        follows.filter { it.followeeId == userId }.map { it.followerId }

    fun getFollowing(userId: Long): List<Long> =
        follows.filter { it.followerId == userId }.map { it.followeeId }

    fun isFollowing(followerId: Long, followeeId: Long): Boolean =
        follows.any { it.followerId == followerId && it.followeeId == followeeId }
}
