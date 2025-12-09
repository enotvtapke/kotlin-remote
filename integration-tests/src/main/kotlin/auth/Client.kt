package auth

import startLeaseOnStubDeserialization
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
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.RemoteClient
import kotlinx.serialization.json.Json

data object AuthServerContext : RemoteContext {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModule(
                    remoteClasses = genRemoteClassList(),
                    callableMap = CallableMapClass(genCallableMap()),
                    leaseManager = null,
                    onStubDeserialization = startLeaseOnStubDeserialization(),
                )
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
    }.remoteClient(CallableMapClass(genCallableMap()), "/callAuth")
}

@Remote
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

fun main(): Unit = runBlocking {
    with(AuthServerContext) {
        println(multiply(100, 600))
    }
}
