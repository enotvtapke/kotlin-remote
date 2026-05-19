package cms

import cms.api.AdminApi
import cms.api.AuthorApi
import cms.api.GuestApi
import cms.api.ModeratorApi
import cms.di.cmsModule
import cms.di.dep
import cms.model.Role
import cms.model.atLeast
import cms.repository.ArticleRepository
import cms.repository.CommentRepository
import cms.repository.UserStore
import cms.server.AdminApiImpl
import cms.server.AuthorApiImpl
import cms.server.GuestApiImpl
import cms.server.ModeratorApiImpl
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.core.context.startKoin

fun main() {
    startCms()
}

fun startCms() {
    startKoin { modules(cmsModule) }
    val users = dep<UserStore>()
    fun roleValidator(min: Role): suspend io.ktor.server.application.ApplicationCall.(io.ktor.server.auth.UserPasswordCredential) -> UserIdPrincipal? = { creds ->
        val u = users.authenticate(creds.name, creds.password)
        if (u != null && u.role.atLeast(min)) UserIdPrincipal(creds.name) else null
    }
    val server = embeddedServer(Netty, port = 8080) {
        install(Krpc)
        install(Authentication) {
            basic("author") { validate(roleValidator(Role.AUTHOR)) }
            basic("moderator") { validate(roleValidator(Role.MODERATOR)) }
            basic("admin") { validate(roleValidator(Role.ADMIN)) }
        }
        routing {
            rpc("/call/guest") {
                rpcConfig { serialization { json() } }
                registerService<GuestApi> { GuestApiImpl(dep<ArticleRepository>(), dep<CommentRepository>()) }
            }
            authenticate("author") {
                rpc("/call/author") {
                    rpcConfig { serialization { json() } }
                    registerService<AuthorApi> { AuthorApiImpl(dep<ArticleRepository>(), dep<CommentRepository>()) }
                }
            }
            authenticate("moderator") {
                rpc("/call/moderator") {
                    rpcConfig { serialization { json() } }
                    registerService<ModeratorApi> { ModeratorApiImpl(dep<ArticleRepository>(), dep<CommentRepository>()) }
                }
            }
            authenticate("admin") {
                rpc("/call/admin") {
                    rpcConfig { serialization { json() } }
                    registerService<AdminApi> { AdminApiImpl(dep<ArticleRepository>(), dep<CommentRepository>(), dep<UserStore>()) }
                }
            }
        }
    }
    server.start(wait = false)
    println("CMS server started on http://localhost:8080")
    Runtime.getRuntime().addShutdownHook(Thread { server.stop(1000, 2000) })
    Thread.currentThread().join()
}
