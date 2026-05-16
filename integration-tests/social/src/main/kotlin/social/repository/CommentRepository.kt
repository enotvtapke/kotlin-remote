package social.repository

import java.util.concurrent.atomic.AtomicLong
import social.model.AddCommentDto
import social.model.Comment

class CommentRepository {
    private val nextId = AtomicLong(1)
    private val comments = mutableMapOf<Long, Comment>()

    fun addComment(dto: AddCommentDto): Comment {
        val c = Comment(nextId.getAndIncrement(), dto.postId, dto.authorId, dto.text)
        comments[c.id] = c
        return c
    }

    fun getPostComments(postId: Long): List<Comment> =
        comments.values.filter { it.postId == postId }.sortedBy { it.createdAt }

    fun deleteComment(id: Long) {
        comments.remove(id) ?: error("Comment $id not found")
    }

    fun countForPost(postId: Long): Int = comments.values.count { it.postId == postId }
}
