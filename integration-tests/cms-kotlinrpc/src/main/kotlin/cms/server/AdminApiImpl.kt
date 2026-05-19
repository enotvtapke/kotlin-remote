package cms.server

import cms.api.AdminApi
import cms.model.Role
import cms.model.SiteStats
import cms.model.UserAcc
import cms.repository.ArticleRepository
import cms.repository.CommentRepository
import cms.repository.UserStore

class AdminApiImpl(
    private val articles: ArticleRepository,
    private val comments: CommentRepository,
    private val users: UserStore,
) : AdminApi {
    override suspend fun promoteUser(login: String, role: Role): UserAcc = users.promote(login, role)
    override suspend fun siteStats(): SiteStats =
        SiteStats(articles = articles.count(), comments = comments.count(), users = users.count())
    override suspend fun purgeUser(login: String): Int {
        // Same bypass as in ModeratorApiImpl.removeArticleAndComments.
        val list = articles.byAuthor(login)
        list.forEach {
            comments.deleteForArticle(it.id)
            articles.deleteAny(it.id)
        }
        val droppedComments = comments.deleteByAuthor(login)
        users.promote(login, Role.GUEST)
        return list.size + droppedComments
    }
}
