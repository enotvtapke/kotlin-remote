import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.remote.CallableMap
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.network.remoteClient
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApplicationTests {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            CallableMap.init()
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
                json()
            }
        }.remoteClient("/call")

    private fun ApplicationTestBuilder.configureApplication() {
        application {
            install(CallLogging)
            install(ServerContentNegotiation) {
                json()
            }
            install(KRemote)
            routing {
                remote("/call")
            }
        }
    }
}

