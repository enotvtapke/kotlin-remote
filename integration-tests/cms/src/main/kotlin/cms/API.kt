package cms

import cms.di.dep
import cms.model.Article
import cms.model.Comment
import cms.model.Role
import cms.model.SiteStats
import cms.model.UserAcc
import cms.remote.AdminConfig
import cms.remote.AuthorConfig
import cms.remote.GuestConfig
import cms.remote.ModeratorConfig
import cms.repository.ArticleRepository
import cms.repository.CommentRepository
import cms.repository.UserStore
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

// === Guest tier ===

@Remote context(_: RemoteContext<GuestConfig>)
suspend fun listArticles(): List<Article> = dep<ArticleRepository>().list()

@Remote context(_: RemoteContext<GuestConfig>)
suspend fun getArticle(id: Long): Article = dep<ArticleRepository>().get(id)

@Remote context(_: RemoteContext<GuestConfig>)
suspend fun getComments(articleId: Long): List<Comment> = dep<CommentRepository>().forArticle(articleId)

@Remote context(_: RemoteContext<GuestConfig>)
suspend fun searchArticles(query: String): List<Article> = dep<ArticleRepository>().search(query)

// === Author tier ===

@Remote context(_: RemoteContext<AuthorConfig>)
suspend fun createArticle(login: String, title: String, body: String): Article =
    dep<ArticleRepository>().create(login, title, body)

@Remote context(_: RemoteContext<AuthorConfig>)
suspend fun updateMyArticle(login: String, id: Long, body: String): Article =
    dep<ArticleRepository>().update(id, login, body)

@Remote context(_: RemoteContext<AuthorConfig>)
suspend fun deleteMyArticle(login: String, id: Long) = dep<ArticleRepository>().delete(id, login)

@Remote context(_: RemoteContext<AuthorConfig>)
suspend fun addComment(login: String, articleId: Long, text: String): Comment =
    dep<CommentRepository>().add(articleId, login, text)

// === Moderator tier ===

@Remote context(_: RemoteContext<ModeratorConfig>)
suspend fun deleteAnyComment(id: Long) = dep<CommentRepository>().delete(id)

@Remote context(_: RemoteContext<ModeratorConfig>)
suspend fun pinArticle(id: Long): Article = dep<ArticleRepository>().setPinned(id, true)

@Remote context(_: RemoteContext<ModeratorConfig>)
suspend fun removeArticleAndComments(id: Long) {
    val comments = getComments(id)
    comments.forEach { deleteAnyComment(it.id) }
    dep<ArticleRepository>().deleteAny(id)
}

// === Admin tier ===

@Remote context(_: RemoteContext<AdminConfig>)
suspend fun promoteUser(login: String, role: Role): UserAcc = dep<UserStore>().promote(login, role)

@Remote context(_: RemoteContext<AdminConfig>)
suspend fun siteStats(): SiteStats = SiteStats(
    articles = dep<ArticleRepository>().count(),
    comments = dep<CommentRepository>().count(),
    users = dep<UserStore>().count(),
)

@Remote context(_: RemoteContext<AdminConfig>)
suspend fun purgeUser(login: String): Int {
    val articles = dep<ArticleRepository>().byAuthor(login)
    articles.forEach { removeArticleAndComments(it.id) }
    val droppedComments = dep<CommentRepository>().deleteByAuthor(login)
    promoteUser(login, Role.GUEST)
    return articles.size + droppedComments
}
