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
import kotlinx.remote.classes.simpleRemoteClassSerializersModule
import kotlinx.remote.serialization.remoteSerializersModuleShort
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus

open class B: RemoteConfig {
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