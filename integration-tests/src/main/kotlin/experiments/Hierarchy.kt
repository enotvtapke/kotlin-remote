package experiments.hierarchy

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
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.serialization.json.Json

open class B: RemoteConfig {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
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
                }
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(genCallableMap(), "/call")
}

object SubB: B()

@Remote
context(_: RemoteContext<B>)
suspend fun b(): Int {
    return 1
}

@Remote
context(_: RemoteContext<SubB>)
suspend fun subB(): Int {
    return b() + 2
}

fun main() = runBlocking {
    context(SubB.asContext(), "") {
        println(subB())
    }
    context(B().asContext(), "") {
        println(b())
    }
}