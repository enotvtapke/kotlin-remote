package manualFunctionCalling

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.remote.*
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.serialization.json.Json

context(_: RemoteContext)
private suspend fun expression(a: Long, b: Long): Long {
    return a + multiply(a, b)
}

data object ManualServerContext : RemoteContext {
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                serializersModule = remoteSerializersModule {
                    callableMap = CallableMap(manualCallableMap())
                }
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }.remoteClient(CallableMap(manualCallableMap()), "/call")
}

fun main() = runBlocking {
    with(ManualServerContext) {
        println(expression(6, 1))
    }
}
