package cms.repository

import cms.model.Article
import java.util.concurrent.atomic.AtomicLong

class ArticleRepository {
    private val nextId = AtomicLong(1)
    private val articles = mutableMapOf<Long, Article>()

    fun list(): List<Article> = articles.values.sortedByDescending { it.pinned }.toList()

    fun get(id: Long): Article = articles[id] ?: error("Article $id not found")

    fun create(authorLogin: String, title: String, body: String): Article {
        val a = Article(nextId.getAndIncrement(), authorLogin, title, body)
        articles[a.id] = a
        return a
    }

    fun update(id: Long, requesterLogin: String, body: String): Article {
        val a = get(id)
        if (a.authorLogin != requesterLogin) error("Cannot edit foreign article")
        val updated = a.copy(body = body)
        articles[id] = updated
        return updated
    }

    fun delete(id: Long, requesterLogin: String) {
        val a = get(id)
        if (a.authorLogin != requesterLogin) error("Cannot delete foreign article")
        articles.remove(id)
    }

    fun deleteAny(id: Long) {
        articles.remove(id) ?: error("Article $id not found")
    }

    fun setPinned(id: Long, pinned: Boolean): Article {
        val a = get(id)
        val updated = a.copy(pinned = pinned)
        articles[id] = updated
        return updated
    }

    fun search(query: String): List<Article> =
        articles.values.filter { it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true) }

    fun count(): Int = articles.size

    fun byAuthor(login: String): List<Article> = articles.values.filter { it.authorLogin == login }
}
