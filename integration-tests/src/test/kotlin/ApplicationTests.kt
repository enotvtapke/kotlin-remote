import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.remote.*
import kotlinx.remote.LocalContext
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.classes.network.LeaseClient
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.ktor.KRemote
import kotlinx.remote.network.ktor.KRemoteServerPluginAttributesKey
import kotlinx.remote.network.ktor.leaseRoutes
import kotlinx.remote.network.ktor.remote
import kotlinx.remote.classes.network.leaseClient
import kotlinx.remote.network.remoteClient
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class ApplicationTests {
    @Test
    fun `simple local call`() =
        testApplication {
            configureApplication()
            ServerContext._client = testRemoteClient()

            @Remote
            context(ctx: RemoteContext)
            suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

            context(LocalContext) {
                assertEquals(100, multiply(10, 10))
            }
        }

    @Test
    fun `simple non local call`() =
        testApplication {
            configureApplication()

            @Remote
            context(ctx: RemoteContext)
            suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

            context(testServerRemoteContext()) {
                assertEquals(100, multiply(10, 10))
            }
        }

    @Test
    fun `erroneous call`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext)
            suspend fun multiply(lhs: Long, rhs: Long): Long = throw IllegalStateException("My exception")

            context(testServerRemoteContext()) {
                val exception = assertThrows<IllegalStateException> { multiply(10, 10) }
                assertEquals(
                    "My exception --- Remote stack trace ---",
                    exception.message?.lines()?.take(2)?.joinToString(" ")
                )
            }
        }

    @Test
    fun `generic function`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext)
            suspend fun <T : Int> numberMap(list: List<T>): List<Long> = list.map { it.toLong() }

            @Remote
            context(_: RemoteContext)
            suspend fun <K : Long, P : List<Int>, T : Map<K, List<P>>> genericFunction(t: T) =
                t.entries.first().value.first()

            context(testServerRemoteContext()) {
                assertEquals(listOf(2), genericFunction(mapOf(1L to listOf(listOf(2)))))
                assertEquals(listOf(1L, 2L), numberMap(listOf(1, 2)))
            }
        }

    @Test
    fun `direct recursion`() =
        testApplication {
            configureApplication()

            @Remote
            context(ctx: RemoteContext)
            suspend fun power(base: Long, p: Long): Long {
                if (p == 0L) return 1L
                return base * power(base, p - 1L)
            }

            context(testServerRemoteContext()) {
                assertEquals(1024L, power(2, 10))
            }
        }

    @Test
    fun `extension function call`() =
        testApplication {
            configureApplication()

            @Remote
            context(ctx: RemoteContext)
            suspend fun Long.multiply(rhs: Long) = this * rhs

            context(testServerRemoteContext()) {
                assertEquals(100, 10L.multiply(10))
            }
        }

    @Test
    fun `function with additional context parameters call`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext, x: Int)
            suspend fun Long.multiply(rhs: Long) = this * rhs * x

            context(testServerRemoteContext(), 10) {
                assertEquals(1000, 10L.multiply(10))
            }
        }

    @RemoteSerializable
    class TestCalculator(private var init: Int) {
        @Remote
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

            @Remote
            context(ctx: RemoteContext)
            suspend fun calculator(init: Int): TestCalculator {
                return TestCalculator(init)
            }

            context(testServerRemoteContext()) {
                val x = calculator(5)
                assertEquals(30, x.multiply(6))
                assertEquals(210, x.multiply(7))
            }
        }

    object IdObject {
        @Remote
        context(_: RemoteContext)
        suspend fun id(x: Int): Int {
            return x
        }
    }

    @Test
    fun `static this parameter should not be serialized`() =
        testApplication {
            configureApplication()

            context(testServerRemoteContext()) {
                assertEquals(42, IdObject.id(42))
            }
        }

    @Test
    fun `cannot call stub methods in local context`() = runBlocking {
        context(LocalContext) {
            val e = assertThrows<IllegalArgumentException> { TestCalculator.RemoteClassStub(1L, "http://localhost:80").multiply(42) }
            assertEquals(
                "Method of the stub `RemoteClassStub` was called in a local context. This may be caused by lease expiration.",
                e.message
            )
        }
    }

    @Test
    fun `leased class is expired`() =
        testApplication {
            configureApplication(LeaseConfig(100, 50, 0))

            @Remote
            context(_: RemoteContext)
            suspend fun testCalculator(init: Int): TestCalculator {
                return TestCalculator(init)
            }

            context(testServerRemoteContext()) {
                val x = testCalculator(5)
                assertEquals(30, x.multiply(6))
                delay(200)
                val e = assertThrows<IllegalArgumentException> { x.multiply(7) }
                assertEquals("Method of the stub `RemoteClassStub` was called in a local context. This may be caused by lease expiration.", e.message?.lines()?.firstOrNull())
            }
        }

    @Test
    fun `leased class is not expired when renewal is active`() =
        testApplication {
            configureApplication(LeaseConfig(100, 50, 0))

            @Remote
            context(_: RemoteContext)
            suspend fun testCalculator(init: Int): TestCalculator {
                return TestCalculator(init)
            }

            context(testServerRemoteContext(LeaseRenewalClientConfig(renewalIntervalMs = 50))) {
                val x = testCalculator(5)
                assertEquals(30, x.multiply(6))
                delay(200)
                assertEquals(210, x.multiply(7))
            }
        }

    private data object ServerContext : RemoteContext {
        var _client: RemoteClient? = null
        override val client: RemoteClient
            get() = _client ?: throw IllegalStateException("Client not initialized")
    }

    private fun ApplicationTestBuilder.testLeaseClient(): LeaseClient {
        return createClient {
            defaultRequest {
                url("http://localhost:80")
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

    private fun ApplicationTestBuilder.testServerRemoteContext(leaseRenewalClientConfig: LeaseRenewalClientConfig = LeaseRenewalClientConfig()): RemoteContext {
        return object : RemoteContext {
            override val client: RemoteClient
                get() = testRemoteClient(leaseRenewalClientConfig)
        }
    }

    private fun ApplicationTestBuilder.testRemoteClient(leaseRenewalClientConfig: LeaseRenewalClientConfig = LeaseRenewalClientConfig()): RemoteClient {
        val leaseRenewalClients = mutableMapOf<String, LeaseRenewalClient>()
        val onStubDeserialization: (Stub) -> Unit = { stub ->
            val client = leaseRenewalClients.getOrPut(stub.url) {
                LeaseRenewalClient(
                    leaseRenewalClientConfig,
                    this@testRemoteClient.testLeaseClient(),
                ).also {
                    it.startRenewalJob(CoroutineScope(Dispatchers.IO))
                }
            }
            client.registerStub(stub)
        }
        
        return createClient {
            defaultRequest {
                url("http://localhost:80")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
            install(ClientContentNegotiation) {
                json(Json {
                    serializersModule = remoteSerializersModule(
                        remoteClasses = genRemoteClassList(),
                        callableMap = CallableMapClass(genCallableMap()),
                        leaseManager = null,
                        onStubDeserialization = onStubDeserialization
                    )
                })
            }
        }.remoteClient(CallableMapClass(genCallableMap()), "/call")
    }

    private fun ApplicationTestBuilder.configureApplication(leaseConfig: LeaseConfig = LeaseConfig(), leaseRenewalClientConfig: LeaseRenewalClientConfig = LeaseRenewalClientConfig()) {
        val leaseRenewalClients = mutableMapOf<String, LeaseRenewalClient>()
        val onStubDeserialization: (Stub) -> Unit = { stub ->
            val client = leaseRenewalClients.getOrPut(stub.url) {
                LeaseRenewalClient(
                    leaseRenewalClientConfig,
                    this@configureApplication.testLeaseClient(),
                ).also {
                    it.startRenewalJob(CoroutineScope(Dispatchers.IO))
                }
            }
            client.registerStub(stub)
        }
        
        application {
            install(CallLogging)
            install(KRemote) {
                this.callableMap = CallableMapClass(genCallableMap())
                this.leaseConfig = leaseConfig
            }
            install(ServerContentNegotiation) {
                json(Json {
                    val leaseManager = this@application.attributes[KRemoteServerPluginAttributesKey].leaseManager
                    val callableMap = this@application.attributes[KRemoteServerPluginAttributesKey].callableMap
                    serializersModule = remoteSerializersModule(
                        remoteClasses = genRemoteClassList(),
                        callableMap = callableMap,
                        leaseManager = leaseManager,
                        nodeUrl = "http://localhost:80",
                        onStubDeserialization = onStubDeserialization
                    )
                })
            }

            routing {
                remote("/call")
                leaseRoutes("/lease")
            }
        }
    }
}

