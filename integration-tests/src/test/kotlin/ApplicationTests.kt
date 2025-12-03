import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.remote.*
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.network.remoteClient
import kotlinx.remote.network.serialization.setupExceptionSerializers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class ApplicationTests {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            CallableMap.putAll(genCallableMap())
        }
    }

    @Test
    fun `simple local call`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(ctx: RemoteContext)
            suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

            context(ServerContext) {
                assertEquals(100, multiply(10, 10))
            }
        }

    @Test
    fun `simple non local call`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(ctx: RemoteContext)
            suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

            context(ClientContext) {
                assertEquals(100, multiply(10, 10))
            }
        }

    @Test
    fun `erroneous call`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(ctx: RemoteContext)
            suspend fun multiply(lhs: Long, rhs: Long): Long = throw IllegalStateException("Exception")

            context(ClientContext) {
                val exception = assertThrows<IllegalStateException> { multiply(10, 10) }
                val msg = Path("./src/test/kotlin/exceptionMessage.txt").readText()
                assertEquals(msg, exception.message)
            }
        }

    @Test
    fun `generic function`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(_: RemoteContext)
            suspend fun <T: Int> numberMap(list: List<T>): List<Long> = list.map { it.toLong() }

            @Remote(ServerConfig::class)
            context(_: RemoteContext)
            suspend fun <K: Long, P: List<Int>, T: Map<K, List<P>>> genericFunction(t: T) = t.entries.first().value.first()

            context(ClientContext) {
                assertEquals(listOf(2), genericFunction(mapOf(1L to listOf(listOf(2)))))
                assertEquals(listOf(1L, 2L), numberMap(listOf(1, 2)))
            }
        }

    @Test
    fun `direct recursion`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(ctx: RemoteContext)
            suspend fun power(base: Long, p: Long): Long {
                if (p == 0L) return 1L
                return base * power(base, p - 1L)
            }

            context(ClientContext) {
                assertEquals(1024L, power(2, 10))
            }
        }

    @Test
    fun `extension function call`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(ctx: RemoteContext)
            suspend fun Long.multiply(rhs: Long) = this * rhs

            context(ClientContext) {
                assertEquals(100, 10L.multiply(10))
            }
        }

    @Test
    fun `function with additional context parameters call`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(_: RemoteContext, x: Int)
            suspend fun Long.multiply(rhs: Long) = this * rhs * x

            context(ClientContext, 10) {
                assertEquals(1000, 10L.multiply(10))
            }
        }

    @Test
    fun `streaming local call`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(ctx: RemoteContext)
            suspend fun multiplyStreaming(lhs: Long, rhs: Long): Flow<Long> {
                return flow {
                    repeat(50) {
                        delay(10)
                        emit(lhs * rhs)
                    }
                }
            }

            context(ServerContext) {
                val res = multiplyStreaming(10, 10).toList()
                assertEquals(List(50) { 100L }, res)
            }
        }

    @Test
    fun `streaming non local call`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(ctx: RemoteContext)
            suspend fun multiplyStreaming(lhs: Long, rhs: Long): Flow<Long> {
                return flow {
                    repeat(50) {
                        delay(10)
                        emit(lhs * rhs)
                    }
                }
            }

            context(ClientContext) {
                val res = multiplyStreaming(10, 10).toList()
                assertEquals(List(50) { 100L }, res)
            }
        }

    @RemoteSerializable
    @Serializable(with = Calculator.RemoteClassSerializer::class)
    class Calculator(private var init: Int) {
        @Remote(ServerConfig::class)
        context(_: RemoteContext)
        suspend fun multiply(x: Int): Int {
            init *= x
            return init
        }
    }

    @Test
    fun `remote class`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(ctx: RemoteContext)
            suspend fun calculator(init: Int): Calculator {
                return Calculator(init)
            }

            context(ClientContext) {
                val x = calculator(5)
                assertEquals(30, x.multiply(6))
                assertEquals(210, x.multiply(7))
            }
        }

    private data object ServerConfig : RemoteConfig {
        override val context = ServerContext

        var _client: RemoteClient? = null
        override val client: RemoteClient
            get() = _client ?: throw IllegalStateException("Client not initialized")
    }

    private data object ServerContext : RemoteContext
    private data object ClientContext : RemoteContext

    private fun ApplicationTestBuilder.testRemoteClient(): RemoteClient =
        createClient {
            defaultRequest {
                url("http://localhost:80")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
            install(ClientContentNegotiation) {
                json(Json {
                    serializersModule = SerializersModule {
                        setupExceptionSerializers()
                    }
                })
            }
        }.remoteClient("/call")

    private fun ApplicationTestBuilder.configureApplication() {
        application {
            install(CallLogging)
            install(ServerContentNegotiation) {
                json(Json {
                    serializersModule = SerializersModule {
                        setupExceptionSerializers()
                    }
                })
            }
            install(KRemote)
            routing {
                remote("/call")
            }
        }
    }
}

