package social.server

import social.api.LikeService
import social.model.Like
import social.repository.LikeRepository

class LikeServiceImpl(
    private val repo: LikeRepository,
) : LikeService {
    override suspend fun likePost(postId: Long, userId: Long): Like = repo.likePost(postId, userId)
    override suspend fun unlikePost(postId: Long, userId: Long) = repo.unlikePost(postId, userId)
    override suspend fun getPostLikes(postId: Long): List<Like> = repo.getPostLikes(postId)
    override suspend fun getUserLikes(userId: Long): List<Like> = repo.getUserLikes(userId)
}
