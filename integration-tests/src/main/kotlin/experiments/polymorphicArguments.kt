package experiments

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.remote.*
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.ktor.remoteClient
import kotlinx.remote.ktor.ktorRemoteClassSerializersModule
import kotlinx.remote.serialization.remoteSerializersModule
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun <T> string(x: @Polymorphic T, y: Int = 3): String = x.toString()

@Serializable
data class WrappedInt(val value: Int)

data object GenericServerConfig : RemoteConfig {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = SerializersModule {
                    polymorphic(Any::class) {
                        subclass(WrappedInt::class, WrappedInt.serializer())
                    }
                } + remoteSerializersModule(genCallableMap()) + ktorRemoteClassSerializersModule(genRemoteClassList())
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(genCallableMap(), "/call")
}

fun main() = runBlocking {
    context(GenericServerConfig.asContext()) {
        println(string(WrappedInt(1)))
    }
}