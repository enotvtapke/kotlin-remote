package social.server

import social.api.CommentService
import social.model.AddCommentDto
import social.model.Comment
import social.repository.CommentRepository

class CommentServiceImpl(
    private val repo: CommentRepository,
) : CommentService {
    override suspend fun addComment(dto: AddCommentDto): Comment = repo.addComment(dto)
    override suspend fun getPostComments(postId: Long): List<Comment> = repo.getPostComments(postId)
    override suspend fun deleteComment(id: Long) = repo.deleteComment(id)
}
