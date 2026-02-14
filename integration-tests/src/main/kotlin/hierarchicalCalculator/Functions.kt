package hierarchicalCalculator

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
import kotlin.math.PI
import kotlin.math.abs

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
            }
        })
    }
    install(Logging) {
        level = LogLevel.BODY
    }
}.remoteClient(genCallableMap(), "/call")

interface ArithmeticConfig: RemoteConfig
object ArithmeticConfigImpl: ArithmeticConfig {
    override val client: RemoteClient = remoteClient("http://localhost:8001")
}

object TrigonometricConfig: ArithmeticConfig {
    override val client: RemoteClient = remoteClient("http://localhost:8002")
}

@Remote
context(_: RemoteContext<ArithmeticConfig>)
suspend infix fun Double.mul(rhs: Double) = this * rhs
@Remote
context(_: RemoteContext<ArithmeticConfig>)
suspend infix fun Double.divide(rhs: Double) = this / rhs
@Remote
context(_: RemoteContext<TrigonometricConfig>)
suspend fun sin(x: Double): Double {
    var (term, sum, n) = Triple(x, x, 1.0)
    while (abs(term) > 1e-5) {
        val divisor = (2.0 mul n) mul (2.0 mul n + 1.0)
        term = term mul -1.0 mul x mul x divide divisor
        sum = sum + term
        n++
    }
    return sum
}

fun main() = runBlocking {
    context(TrigonometricConfig.asContext()) {
        println(sin(PI / 6))
    }
}
