package experiments

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.genCallableMap
import kotlinx.remote.remoteClient
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
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
                serializersModule = remoteSerializersModule {
                    serializersModule = SerializersModule {
                        polymorphic(Any::class) {
                            subclass(WrappedInt::class, WrappedInt.serializer())
                        }
                    }
                    callableMap = genCallableMap()
                    classes {
                        remoteClasses = genRemoteClassList()
                        client { }
                    }
                }
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