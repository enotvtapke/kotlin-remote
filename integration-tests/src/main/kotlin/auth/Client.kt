package auth

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
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.ktor.simpleRemoteClassSerializersModule
import kotlinx.remote.serialization.remoteSerializersModuleShort
import kotlinx.remote.asContext
import kotlinx.remote.ktor.remoteClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus

data object AuthServerConfig : RemoteConfig {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModuleShort(genCallableMap()) +
                        simpleRemoteClassSerializersModule(genRemoteClassList())
            })
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
    }.remoteClient(genCallableMap(), "/callAuth")
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

fun main(): Unit = runBlocking {
    with(AuthServerConfig.asContext()) {
        println(multiply(100, 600))
    }
}
