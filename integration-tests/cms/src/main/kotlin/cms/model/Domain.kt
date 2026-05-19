package cms.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class Role { GUEST, AUTHOR, MODERATOR, ADMIN }

fun Role.atLeast(other: Role): Boolean = this.ordinal >= other.ordinal

@Serializable
data class UserAcc(val login: String, val role: Role)

@Serializable
data class Article(
    val id: Long,
    val authorLogin: String,
    val title: String,
    val body: String,
    val pinned: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
)

@Serializable
data class Comment(
    val id: Long,
    val articleId: Long,
    val authorLogin: String,
    val text: String,
    val createdAt: Instant = Clock.System.now(),
)

@Serializable
data class SiteStats(val articles: Int, val comments: Int, val users: Int)
