package social.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.remote.RemoteClient
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.remoteClient
import kotlinx.remote.serialization.remoteSerializersModule
import kotlinx.serialization.json.Json

fun remoteClient(url: String): RemoteClient = HttpClient {
    defaultRequest {
        url(url)
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json(Json { serializersModule = remoteSerializersModule(genCallableMap()) })
    }
}.remoteClient(genCallableMap(), "/call")
