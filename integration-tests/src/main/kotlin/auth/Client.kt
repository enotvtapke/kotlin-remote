package auth

import ClientContext
import ServerContext
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.remote.*
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.remoteClient

data object AuthServerConfig : RemoteConfig {
    override val context = ServerContext
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json()
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(username = "user", password = "password")
                }

                sendWithoutRequest { request ->
                    request.url.host == "localhost"
                }
            }
        }
    }.remoteClient("/callAuth")
}

@Remote(AuthServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

fun main(): Unit = runBlocking {
    CallableMap.putAll(genCallableMap())
    with(ClientContext) {
        println(multiply(100, 600))
    }
}
