package cms.server

import cms.api.GuestApi
import cms.model.Article
import cms.model.Comment
import cms.repository.ArticleRepository
import cms.repository.CommentRepository

class GuestApiImpl(
    private val articles: ArticleRepository,
    private val comments: CommentRepository,
) : GuestApi {
    override suspend fun listArticles(): List<Article> = articles.list()
    override suspend fun getArticle(id: Long): Article = articles.get(id)
    override suspend fun getComments(articleId: Long): List<Comment> = comments.forArticle(articleId)
    override suspend fun searchArticles(query: String): List<Article> = articles.search(query)
}
