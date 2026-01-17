package bank.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.remote.RemoteClient
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.genCallableMap
import kotlinx.remote.remoteClient
import kotlinx.remote.serialization.throwableSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

fun remoteClient(url: String): RemoteClient = HttpClient {
    defaultRequest {
        url(url)
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json(Json {
            serializersModule = remoteSerializersModule {
                serializersModule = SerializersModule {
                    polymorphic(Throwable::class) {
                        subclass(SerializationException::class, throwableSerializer(::SerializationException))
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