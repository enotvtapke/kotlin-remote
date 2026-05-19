package cms.repository

import cms.model.Comment
import java.util.concurrent.atomic.AtomicLong

class CommentRepository {
    private val nextId = AtomicLong(1)
    private val comments = mutableMapOf<Long, Comment>()

    fun add(articleId: Long, authorLogin: String, text: String): Comment {
        val c = Comment(nextId.getAndIncrement(), articleId, authorLogin, text)
        comments[c.id] = c
        return c
    }

    fun forArticle(articleId: Long): List<Comment> =
        comments.values.filter { it.articleId == articleId }.sortedBy { it.createdAt }

    fun delete(id: Long) {
        comments.remove(id) ?: error("Comment $id not found")
    }

    fun deleteForArticle(articleId: Long) {
        comments.entries.removeAll { it.value.articleId == articleId }
    }

    fun deleteByAuthor(login: String): Int {
        val keys = comments.entries.filter { it.value.authorLogin == login }.map { it.key }
        keys.forEach { comments.remove(it) }
        return keys.size
    }

    fun count(): Int = comments.size
}
