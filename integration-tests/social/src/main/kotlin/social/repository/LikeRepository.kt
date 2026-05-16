package social.repository

import social.model.Like

class LikeRepository {
    private val likes = mutableSetOf<Like>()

    fun likePost(postId: Long, userId: Long): Like {
        if (likes.any { it.postId == postId && it.userId == userId }) error("Already liked")
        val l = Like(postId, userId)
        likes.add(l)
        return l
    }

    fun unlikePost(postId: Long, userId: Long) {
        val removed = likes.removeAll { it.postId == postId && it.userId == userId }
        if (!removed) error("Like not found")
    }

    fun getPostLikes(postId: Long): List<Like> = likes.filter { it.postId == postId }

    fun getUserLikes(userId: Long): List<Like> = likes.filter { it.userId == userId }

    fun countForPost(postId: Long): Int = likes.count { it.postId == postId }
}
