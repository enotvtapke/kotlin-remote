package cms.client

import cms.api.AdminApi
import cms.api.AuthorApi
import cms.api.GuestApi
import cms.api.ModeratorApi
import cms.model.Role
import cms.remote.rpcConn
import cms.remote.rpcHttpClient
import cms.startCms
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.withService
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(): Unit = runBlocking {
    thread { startCms() }
    sleep(1500)

    val guestHttp = rpcHttpClient()
    val aliceHttp = rpcHttpClient("alice", "pw")
    val bobHttp = rpcHttpClient("bob", "pw")
    val charlieHttp = rpcHttpClient("charlie", "pw")
    val impostorHttp = rpcHttpClient("bob", "WRONG_PASSWORD")
    val aliceAsModeratorHttp = rpcHttpClient("alice", "pw")

    val guestApi: GuestApi = rpcConn(guestHttp, "/call/guest").withService<GuestApi>()
    val aliceAuthor: AuthorApi = rpcConn(aliceHttp, "/call/author").withService<AuthorApi>()
    // bob needs both stubs to call guest functions and moderator functions
    val bobGuest: GuestApi = rpcConn(bobHttp, "/call/guest").withService<GuestApi>()
    val bobMod: ModeratorApi = rpcConn(bobHttp, "/call/moderator").withService<ModeratorApi>()
    // charlie needs three stubs: guest, moderator, admin
    val charlieGuest: GuestApi = rpcConn(charlieHttp, "/call/guest").withService<GuestApi>()
    val charlieAdmin: AdminApi = rpcConn(charlieHttp, "/call/admin").withService<AdminApi>()

    println("=== Author (alice) creates and comments ===")
    val article = aliceAuthor.createArticle("alice", "Hello", "world")
    aliceAuthor.addComment("alice", article.id, "first comment by alice")
    println("Created: $article")

    println("=== Guest (anonymous) reads ===")
    println("articles: ${guestApi.listArticles()}")
    println("comments: ${guestApi.getComments(article.id)}")

    println("=== Moderator (bob) pins + uses separate guest stub ===")
    val pinned = bobMod.pinArticle(article.id)
    println("pinned: $pinned")
    // bob has to use the guest stub to call getArticle; the call goes over the network
    // even though the moderator and guest routes are on the same JVM.
    println("article via bob's guest stub: ${bobGuest.getArticle(article.id)}")

    println("=== Author (alice) tries to add another comment ===")
    aliceAuthor.addComment("alice", article.id, "second comment by alice")

    println("=== Admin (charlie) promotes alice and shows stats ===")
    val updated = charlieAdmin.promoteUser("alice", Role.MODERATOR)
    println("promoted: $updated")
    println("stats: ${charlieAdmin.siteStats()}")
    println("articles (via charlie's guest stub): ${charlieGuest.listArticles()}")

    println("=== Admin purges alice (cross-tier internal calls bypass stubs) ===")
    val removed = charlieAdmin.purgeUser("alice")
    println("purgeUser removed $removed items; stats: ${charlieAdmin.siteStats()}")

    println("=== Demo: unauthorized request is rejected by Ktor auth ===")
    try {
        val impostorMod: ModeratorApi = rpcConn(impostorHttp, "/call/moderator").withService<ModeratorApi>()
        impostorMod.pinArticle(article.id)
        println("UNEXPECTED: call succeeded with wrong password")
    } catch (e: Exception) {
        println("got expected auth failure: ${e.message?.take(120) ?: e.javaClass.simpleName}")
    }

    println("=== Demo: author credentials cannot reach moderator route ===")
    try {
        val aliceMod: ModeratorApi = rpcConn(aliceAsModeratorHttp, "/call/moderator").withService<ModeratorApi>()
        aliceMod.pinArticle(article.id)
        println("UNEXPECTED: alice could pin as moderator")
    } catch (e: Exception) {
        println("got expected auth failure: ${e.message?.take(120) ?: e.javaClass.simpleName}")
    }

    exitProcess(0)
}
