package cms.client

import cms.*
import cms.model.Role
import cms.remote.AdminSession
import cms.remote.AuthorSession
import cms.remote.GuestSession
import cms.remote.ModeratorSession
import cms.startCms
import kotlinx.coroutines.runBlocking
import kotlinx.remote.asContext
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(): Unit = runBlocking {
    thread { startCms() }
    sleep(1000)

    val alice = AuthorSession("alice", "pw")
    val bob = ModeratorSession("bob", "pw")
    val charlie = AdminSession("charlie", "pw")

    println("=== Author (alice) creates and comments ===")
    val article = with(alice.asContext()) {
        val a = createArticle("alice", "Hello", "world")
        addComment("alice", a.id, "first comment by alice")
        a
    }
    println("Created: $article")

    println("=== Guest (anonymous) reads ===")
    with(GuestSession.asContext()) {
        println("articles: ${listArticles()}")
        println("comments: ${getComments(article.id)}")
    }

    println("=== Moderator (bob) pins + uses guest functions via hierarchy ===")
    with(bob.asContext()) {
        val pinned = pinArticle(article.id)
        println("pinned: $pinned")
        // getArticle is a Guest tier function; bob's ModeratorConfig satisfies it via subtyping.
        // Call goes through /call/moderator with bob's credentials.
        println("article via moderator session: ${getArticle(article.id)}")
    }

    println("=== Author (alice) tries to add another comment ===")
    with(alice.asContext()) { addComment("alice", article.id, "second comment by alice") }

    println("=== Admin (charlie) promotes alice and shows stats ===")
    with(charlie.asContext()) {
        val updated = promoteUser("alice", Role.MODERATOR)
        println("promoted: $updated")
        // siteStats requires AdminConfig; works.
        // listArticles requires GuestConfig; also works via subtyping.
        println("stats: ${siteStats()}")
        println("articles (via admin): ${listArticles()}")
    }

    println("=== Admin purges alice (cross-tier internal calls) ===")
    with(charlie.asContext()) {
        val removed = purgeUser("alice")
        println("purgeUser removed $removed items; stats: ${siteStats()}")
    }

    println("=== Demo: unauthorized request is rejected by Ktor auth ===")
    // bob's password is wrong here -- Ktor basic auth must reject.
    val impostor = ModeratorSession("bob", "WRONG_PASSWORD")
    try {
        with(impostor.asContext()) { pinArticle(article.id) }
        println("UNEXPECTED: call succeeded with wrong password")
    } catch (e: Exception) {
        println("got expected auth failure: ${e.message}")
    }

    println("=== Demo: author credentials cannot reach moderator route ===")
    // alice is AUTHOR, not MODERATOR -- the /call/moderator route rejects her.
    // We construct a moderator session with alice's credentials by hand to bypass the type system.
    val aliceMisusingModerator = ModeratorSession("alice", "pw")
    try {
        with(aliceMisusingModerator.asContext()) { pinArticle(article.id) }
        println("UNEXPECTED: alice could pin as moderator")
    } catch (e: Exception) {
        println("got expected auth failure: ${e.message}")
    }
    exitProcess(0)
}
