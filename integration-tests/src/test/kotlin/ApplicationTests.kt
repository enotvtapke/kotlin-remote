import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
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
import kotlinx.coroutines.runBlocking
import kotlinx.remote.*
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.classes.RemoteSerializer
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.network.LeaseClient
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.leaseRoutes
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.network.leaseClient
import kotlinx.remote.network.remoteClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class ApplicationTests {
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
            suspend fun <T : Int> numberMap(list: List<T>): List<Long> = list.map { it.toLong() }

            @Remote(ServerConfig::class)
            context(_: RemoteContext)
            suspend fun <K : Long, P : List<Int>, T : Map<K, List<P>>> genericFunction(t: T) =
                t.entries.first().value.first()

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
    class TestCalculator(private var init: Int) {
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
            suspend fun calculator(init: Int): TestCalculator {
                return TestCalculator(init)
            }

            context(ClientContext) {
                val x = calculator(5)
                assertEquals(30, x.multiply(6))
                assertEquals(210, x.multiply(7))
            }
        }

    object IdObject {
        @Remote(ServerConfig::class)
        context(_: RemoteContext)
        suspend fun id(x: Int): Int {
            return x
        }
    }

    @Test
    fun `static this parameter should not be serialized`() =
        testApplication {
            configureApplication()
            ServerConfig._client = testRemoteClient()

            context(ClientContext) {
                assertEquals(42, IdObject.id(42))
            }
        }

    @Test
    fun `cannot call stub methods in local context`() = runBlocking {
        context<_, Unit>(ServerContext) {
            val e = assertThrows<IllegalArgumentException> { TestCalculator.RemoteClassStub(1L).multiply(42) }
            assertEquals(
                "Method of the stub `RemoteClassStub` was called in a local context. This may be caused by lease expiration.",
                e.message
            )
        }
    }

    @Test
    fun `leased class is expired`() =
        testApplication {
            configureApplication(LeaseConfig(2000, 200, 0))
            ServerConfig._client = testRemoteClient()

            @Remote(ServerConfig::class)
            context(_: RemoteContext)
            suspend fun testCalculator(init: Int): TestCalculator {
                return TestCalculator(init)
            }

//            startLeaseRenewal(leaseClient.leaseClient(), CoroutineScope(Dispatchers.IO), LeaseRenewalClientConfig(3000))

            context(ClientContext) {
                val x = testCalculator(5)
                assertEquals(30, x.multiply(6))
                delay(5000)
                assertThrows<IllegalStateException> { println(x.multiply(7)) }
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

    private fun ApplicationTestBuilder.testLeaseClient(): LeaseClient {
        return createClient {
            defaultRequest {
                url("http://localhost:8080")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
            install(ContentNegotiation) {
                json()
            }
            install(Logging) {
                level = LogLevel.BODY
            }
        }.leaseClient()
    }

    fun remoteClassSerializersModule(leaseClient: LeaseClient) = SerializersModule {
        contextual(
            TestCalculator::class, RemoteSerializer(
                leaseManager = LeaseManager(LeaseConfig(1000, 200), RemoteInstancesPool()),
                leaseRenewalClient = LeaseRenewalClient(
                    LeaseRenewalClientConfig(2000),
                    leaseClient
                ),
                stubFabric = { TestCalculator.RemoteClassStub(it) }
            ))
    }

    private fun ApplicationTestBuilder.testRemoteClient(): RemoteClient {
        val callableMap = CallableMapClass(genCallableMap())
        return createClient {
            defaultRequest {
                url("http://localhost:80")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
            install(ClientContentNegotiation) {
                json(Json {
                    serializersModule = SerializersModule { }.remoteSerializersModule(callableMap,
                        remoteClassSerializersModule(leaseClient = this@testRemoteClient.testLeaseClient())
                    )
                })
            }
        }.remoteClient(callableMap, "/call")
    }

    private fun ApplicationTestBuilder.configureApplication(leaseConfig: LeaseConfig? = null) {
        val callableMap = CallableMapClass(genCallableMap())
        application {
            install(CallLogging)
            install(ServerContentNegotiation) {
                json(Json {
                    serializersModule =
                        SerializersModule { }.remoteSerializersModule(callableMap, remoteClassSerializersModule(leaseClient = this@configureApplication.testLeaseClient()))
                })
            }
            install(KRemote) {
                this.callableMap = callableMap
                this.leaseManager = LeaseManager(leaseConfig ?: LeaseConfig(1000, 200, 0), RemoteInstancesPool())
            }
            routing {
                remote("/call")
                if (leaseConfig != null) leaseRoutes("/lease")
            }
        }
    }
}

