package cms.api

import cms.model.Article
import cms.model.Comment
import cms.model.Role
import cms.model.SiteStats
import cms.model.UserAcc
import kotlinx.rpc.annotations.Rpc

@Rpc
interface GuestApi {
    suspend fun listArticles(): List<Article>
    suspend fun getArticle(id: Long): Article
    suspend fun getComments(articleId: Long): List<Comment>
    suspend fun searchArticles(query: String): List<Article>
}

@Rpc
interface AuthorApi {
    suspend fun createArticle(login: String, title: String, body: String): Article
    suspend fun updateMyArticle(login: String, id: Long, body: String): Article
    suspend fun deleteMyArticle(login: String, id: Long)
    suspend fun addComment(login: String, articleId: Long, text: String): Comment
}

@Rpc
interface ModeratorApi {
    suspend fun deleteAnyComment(id: Long)
    suspend fun pinArticle(id: Long): Article
    suspend fun removeArticleAndComments(id: Long)
}

@Rpc
interface AdminApi {
    suspend fun promoteUser(login: String, role: Role): UserAcc
    suspend fun siteStats(): SiteStats
    suspend fun purgeUser(login: String): Int
}
