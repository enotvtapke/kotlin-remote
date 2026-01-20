import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
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
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.classes.Stub
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseRenewalClient
import kotlinx.remote.classes.lease.LeaseRenewalClientConfig
import kotlinx.remote.classes.network.LeaseClient
import kotlinx.remote.classes.network.leaseClient
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.leaseRoutes
import kotlinx.remote.ktor.remote
import kotlinx.remote.serialization.UnregisteredRemoteException
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.Ignore
import kotlin.test.assertEquals

class ApplicationTests {
    @Test
    fun `simple local call`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
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
            context(_: RemoteContext<RemoteConfig>)
            suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

            context(testServerRemoteContext().asContext()) {
                assertEquals(100, multiply(10, 10))
            }
        }

    @Test
    fun `erroneous call`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun multiply(lhs: Long, rhs: Long): Long = throw IllegalStateException("My exception")

            context(testServerRemoteContext().asContext()) {
                val exception = assertThrows<IllegalStateException> { multiply(10, 10) }
                assertEquals("My exception", exception.message)
                assertEquals(
                    $$"""
                    ApplicationTests$erroneous call$1.invokeSuspend$multiply
                    ApplicationTests$erroneous call$1.access$invokeSuspend$multiply
                    ApplicationTests$erroneous call$1.invokeSuspend
                    ApplicationTests$erroneous call$1.invoke
                    ApplicationTests$erroneous call$1.invoke
                    """.trimIndent(),
                    exception.stackTrace.take(5).joinToString("\n") { "${it.className}.${it.methodName}" }
                )
            }
        }

    @Test
    fun `indirect erroneous call`() =
        testApplication {
            configureApplication()

            fun erroneous(): Long = throw IllegalStateException("My exception")

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun multiply(lhs: Long, rhs: Long): Long {
                val x = lhs * rhs
                erroneous()
                return x
            }

            context(testServerRemoteContext().asContext()) {
                val exception = assertThrows<IllegalStateException> { multiply(10, 10) }
                assertEquals("My exception", exception.message)
                assertEquals(
                    $$"""
                    ApplicationTests$indirect erroneous call$1.invokeSuspend$erroneous
                    ApplicationTests$indirect erroneous call$1.invokeSuspend$multiply
                    ApplicationTests$indirect erroneous call$1.access$invokeSuspend$multiply
                    ApplicationTests$indirect erroneous call$1.invokeSuspend
                    ApplicationTests$indirect erroneous call$1.invoke
                    """.trimIndent(),
                    exception.stackTrace.take(5).joinToString("\n") { "${it.className}.${it.methodName}" }
                )
            }
        }

    @Test
    fun `erroneous call with cause`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun multiply(lhs: Long, rhs: Long): Long = throw IllegalArgumentException(
                "My exception",
                IllegalArgumentException("My cause1", IllegalArgumentException("My cause2"))
            )

            context(testServerRemoteContext().asContext()) {
                val exception = assertThrows<IllegalArgumentException> { multiply(10, 10) }
                assertEquals("My exception", exception.message)
                assertEquals("My cause1", exception.cause?.message)
                assertEquals("My cause2", exception.cause?.cause?.message)
            }
        }

    private class MyException(message: String, cause: Throwable?) : Exception(message, cause)

    @Test
    fun `unknown exception call`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun multiply(lhs: Long, rhs: Long): Long =
                throw MyException("Message", IllegalArgumentException("My cause1"))

            context(testServerRemoteContext().asContext()) {
                val exception = assertThrows<UnregisteredRemoteException> { multiply(10, 10) }
                assertEquals("MyException: Message", exception.message)
                assertEquals("My cause1", exception.cause?.message)
            }
        }

    @Test
    fun `generic function`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun <T : Int> numberMap(list: List<T>): List<Long> = list.map { it.toLong() }

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun <K : Long, P : List<Int>, T : Map<K, List<P>>> genericFunction(t: T): P =
                t.entries.first().value.first()

            context(testServerRemoteContext().asContext()) {
                assertEquals(listOf(2), genericFunction(mapOf(1L to listOf(listOf(2)))))
                assertEquals(listOf(1L, 2L), numberMap(listOf(1, 2)))
            }
        }

    @Serializable
    data class WrappedInt(val value: Int)

    @Test
    fun `generic function argument using polymorphic`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun <T> string(x: @Polymorphic T): String = x.toString()

            context(testServerRemoteContext().asContext()) {
                assertEquals("WrappedInt(value=2)", string(WrappedInt(2)))
            }
        }

    @Serializable
    @Polymorphic
    open class A {
        override fun toString(): String {
            return "ssss"
        }
    }

    @Serializable
    data class SubA(val x: String) : A()

    @Test
    @Ignore(
        "Fails because SubA serialized as A. List<T> is not considered polymorphic by the compiler plugin. If I" +
                "add @Polymorphic annotation to List<T>, it wont work as well, because only T should be polymorphic."
    )
    fun `generic function list argument using polymorphic`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun <T : A> string(x: List<T>): String {
                val z = x.first() is SubA
                return x.toString()
            }

            context(testServerRemoteContext().asContext()) {
                assertEquals("[SubA(x=str)]", string(listOf(SubA("str"))))
            }
        }

    @Test
    fun `generic function return value using polymorphic`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun <T> string(x: @Polymorphic T): @Polymorphic T = x

            context(testServerRemoteContext().asContext()) {
                assertEquals(WrappedInt(2), string(WrappedInt(2)))
            }
        }

    @Test
    fun `direct recursion`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun power(base: Long, p: Long): Long {
                if (p == 0L) return 1L
                return base * power(base, p - 1L)
            }

            context(testServerRemoteContext().asContext()) {
                assertEquals(1024L, power(2, 10))
            }
        }

    @Test
    fun `extension function call`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun Long.multiply(rhs: Long) = this * rhs

            context(testServerRemoteContext().asContext()) {
                assertEquals(100, 10L.multiply(10))
            }
        }

    @Test
    fun `function with additional context parameters call`() =
        testApplication {
            configureApplication()

            @Remote
            context(_: RemoteContext<RemoteConfig>, x: Int)
            suspend fun Long.multiply(rhs: Long) = this * rhs * x

            context(testServerRemoteContext().asContext(), 10) {
                assertEquals(1000, 10L.multiply(10))
            }
        }

    @RemoteSerializable
    class TestCalculator(private var init: Int) {
        @Remote
        context(_: RemoteContext<RemoteConfig>)
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
            context(_: RemoteContext<RemoteConfig>)
            suspend fun calculator(init: Int): TestCalculator {
                return TestCalculator(init)
            }

            context(testServerRemoteContext().asContext()) {
                val x = calculator(5)
                assertEquals(30, x.multiply(6))
                assertEquals(210, x.multiply(7))
            }
        }

    object IdObject {
        @Remote
        context(_: RemoteContext<RemoteConfig>)
        suspend fun id(x: Int): Int {
            return x
        }
    }

    @Test
    fun `static this parameter should not be serialized`() =
        testApplication {
            configureApplication()

            context(testServerRemoteContext().asContext()) {
                assertEquals(42, IdObject.id(42))
            }
        }

    @Test
    fun `cannot call stub methods in local context`() = runBlocking {
        context(LocalContext) {
            val e = assertThrows<IllegalArgumentException> {
                TestCalculator.RemoteClassStub(1L, "http://localhost:80").multiply(42)
            }
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
            context(_: RemoteContext<RemoteConfig>)
            suspend fun testCalculator(init: Int): TestCalculator {
                return TestCalculator(init)
            }

            context(testServerRemoteContext().asContext()) {
                val x = testCalculator(5)
                assertEquals(30, x.multiply(6))
                delay(200)
                val e = assertThrows<IllegalArgumentException> { x.multiply(7) }
                assertEquals(
                    "Method of the stub `RemoteClassStub` was called in a local context. This may be caused by lease expiration.",
                    e.message?.lines()?.firstOrNull()
                )
            }
        }

    @Test
    fun `leased class is not expired when renewal is active`() =
        testApplication {
            configureApplication(LeaseConfig(100, 50, 0))

            @Remote
            context(_: RemoteContext<RemoteConfig>)
            suspend fun testCalculator(init: Int): TestCalculator {
                return TestCalculator(init)
            }

            context(testServerRemoteContext(LeaseRenewalClientConfig(renewalIntervalMs = 50)).asContext()) {
                val x = testCalculator(5)
                assertEquals(30, x.multiply(6))
                delay(200)
                assertEquals(210, x.multiply(7))
            }
        }

    @Test
    fun `context hierarchy`() =
        testApplication {
            configureApplication()

            open class BaseConfig : RemoteConfig {
                override val client: RemoteClient = testRemoteClient()
            }

            class SubBaseConfig : BaseConfig()

            @Remote
            context(_: RemoteContext<BaseConfig>)
            suspend fun b(): Int {
                return 1
            }

            @Remote
            context(_: RemoteContext<SubBaseConfig>)
            suspend fun subB(): Int {
                return b() + 2
            }

            context(SubBaseConfig().asContext(), "") {
                assertEquals(3, subB())
            }
            context(BaseConfig().asContext(), "") {
                assertEquals(1, b())
            }
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

    private fun ApplicationTestBuilder.testServerRemoteContext(leaseRenewalClientConfig: LeaseRenewalClientConfig = LeaseRenewalClientConfig()): RemoteConfig {
        return object : RemoteConfig {
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
            install(ContentNegotiation) {
                json(Json {
                    serializersModule = remoteSerializersModule {
                        serializersModule = SerializersModule {
                            polymorphic(Any::class) {
                                subclass(WrappedInt::class, WrappedInt.serializer())
                            }
                            polymorphic(A::class) {
                                subclass(SubA.serializer())
                            }
                        }
                        callableMap = genCallableMap()
                        classes {
                            remoteClasses = genRemoteClassList()
                            client {
                                this.onStubDeserialization = onStubDeserialization
                            }
                        }
                    }
                })
            }
        }.remoteClient(genCallableMap(), "/call")
    }

    private fun ApplicationTestBuilder.configureApplication(
        leaseConfig: LeaseConfig = LeaseConfig(),
        leaseRenewalClientConfig: LeaseRenewalClientConfig = LeaseRenewalClientConfig()
    ) {
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
                callableMap = genCallableMap()
                serializersModule = SerializersModule {
                    polymorphic(Any::class) {
                        subclass(WrappedInt.serializer())
                    }
                    polymorphic(A::class) {
                        subclass(SubA.serializer())
                    }
                }
                classes {
                    remoteClasses = genRemoteClassList()
                    server {
                        this.leaseConfig = leaseConfig
                        nodeUrl = "http://localhost:80"
                    }
                    client {
                        this.onStubDeserialization = onStubDeserialization
                    }
                }
            }

            routing {
                remote("/call")
                leaseRoutes("/lease")
            }
        }
    }
}

