package todoapp.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.remote.RemoteClient
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.genCallableMap
import kotlinx.remote.remoteClient
import kotlinx.remote.serialization.throwableSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
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
                callableMap = genCallableMap()
                classes {
                    remoteClasses = genRemoteClassList()
                    client { }
                }
            }.addAdditionalSerializers()
        })
    }
    install(Logging) {
        level = LogLevel.BODY
    }
}.remoteClient(genCallableMap(), "/call")

fun SerializersModule.addAdditionalSerializers(): SerializersModule = this + SerializersModule {
    polymorphic(Throwable::class) {
        subclass(SerializationException::class, throwableSerializer(::SerializationException))
    }
}