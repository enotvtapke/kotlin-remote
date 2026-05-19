package cms.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.remote.RemoteClient
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.remoteClient
import kotlinx.remote.serialization.remoteSerializersModule
import kotlinx.serialization.json.Json

const val SERVER_BASE = "http://localhost:8080"

fun guestClient(): RemoteClient = HttpClient {
    defaultRequest {
        url(SERVER_BASE)
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json(Json { serializersModule = remoteSerializersModule(genCallableMap()) })
    }
}.remoteClient(genCallableMap(), "/call/guest")

fun authedClient(path: String, login: String, password: String): RemoteClient = HttpClient {
    defaultRequest {
        url(SERVER_BASE)
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json(Json { serializersModule = remoteSerializersModule(genCallableMap()) })
    }
    install(Auth) {
        basic {
            credentials { BasicAuthCredentials(username = login, password = password) }
            sendWithoutRequest { it.url.host == "localhost" }
        }
    }
}.remoteClient(genCallableMap(), path)
