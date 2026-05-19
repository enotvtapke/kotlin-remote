package bench

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.remoteClient
import kotlinx.remote.serialization.remoteSerializersModule
import kotlinx.serialization.json.Json

class BenchConfig(host: String, port: Int) : RemoteConfig {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://$host:$port")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json { serializersModule = remoteSerializersModule(genCallableMap()) })
        }
    }.remoteClient(genCallableMap(), "/call")
}
