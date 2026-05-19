package cms.server

import cms.api.AuthorApi
import cms.model.Article
import cms.model.Comment
import cms.repository.ArticleRepository
import cms.repository.CommentRepository

class AuthorApiImpl(
    private val articles: ArticleRepository,
    private val comments: CommentRepository,
) : AuthorApi {
    override suspend fun createArticle(login: String, title: String, body: String): Article =
        articles.create(login, title, body)
    override suspend fun updateMyArticle(login: String, id: Long, body: String): Article =
        articles.update(id, login, body)
    override suspend fun deleteMyArticle(login: String, id: Long) = articles.delete(id, login)
    override suspend fun addComment(login: String, articleId: Long, text: String): Comment =
        comments.add(articleId, login, text)
}
