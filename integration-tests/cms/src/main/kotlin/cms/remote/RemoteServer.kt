package cms.remote

import cms.di.dep
import cms.model.Role
import cms.model.atLeast
import cms.repository.UserStore
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.handleRemoteCall
import kotlinx.remote.ktor.remote
import kotlinx.remote.serialization.remoteSerializersModule
import kotlinx.serialization.json.Json

fun cmsServer(port: Int = 8080): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val users = dep<UserStore>()
    fun roleValidator(min: Role): suspend io.ktor.server.application.ApplicationCall.(io.ktor.server.auth.UserPasswordCredential) -> UserIdPrincipal? = { creds ->
        val u = users.authenticate(creds.name, creds.password)
        if (u != null && u.role.atLeast(min)) UserIdPrincipal(creds.name) else null
    }
    return embeddedServer(Netty, port = port, watchPaths = listOf()) {
        install(CallLogging)
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.application.environment.log.error("Unhandled exception", cause)
                call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Internal Server Error")
            }
        }
        install(ContentNegotiation) {
            json(Json { serializersModule = remoteSerializersModule(genCallableMap()) })
        }
        install(Authentication) {
            basic("author") { validate(roleValidator(Role.AUTHOR)) }
            basic("moderator") { validate(roleValidator(Role.MODERATOR)) }
            basic("admin") { validate(roleValidator(Role.ADMIN)) }
        }
        install(KRemote) { callableMap = genCallableMap() }
        routing {
            remote("/call/guest")
            authenticate("author") { post("/call/author") { handleRemoteCall() } }
            authenticate("moderator") { post("/call/moderator") { handleRemoteCall() } }
            authenticate("admin") { post("/call/admin") { handleRemoteCall() } }
        }
    }
}
