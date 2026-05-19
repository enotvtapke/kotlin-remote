package cms.server

import cms.api.ModeratorApi
import cms.model.Article
import cms.repository.ArticleRepository
import cms.repository.CommentRepository

class ModeratorApiImpl(
    private val articles: ArticleRepository,
    private val comments: CommentRepository,
) : ModeratorApi {
    override suspend fun deleteAnyComment(id: Long) = comments.delete(id)
    override suspend fun pinArticle(id: Long): Article = articles.setPinned(id, true)
    override suspend fun removeArticleAndComments(id: Long) {
        // Cross-tier composition has to bypass the GuestApi/ModeratorApi service boundary
        // here, because Kotlin RPC does not propagate "local execution" through service stubs.
        // We talk to the repositories directly instead.
        comments.deleteForArticle(id)
        articles.deleteAny(id)
    }
}
