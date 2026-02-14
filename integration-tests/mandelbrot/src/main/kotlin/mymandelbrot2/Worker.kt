package mymandelbrot2

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext
import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.RemoteSerializable
import kotlinx.remote.classes.genRemoteClassList
import kotlinx.remote.classes.lease.LeaseConfig
import kotlinx.remote.classes.lease.LeaseManager
import kotlinx.remote.classes.remoteSerializersModule
import kotlinx.remote.genCallableMap
import kotlinx.remote.ktor.KRemote
import kotlinx.remote.ktor.KRemoteConfigBuilder
import kotlinx.remote.ktor.KRemoteConfigBuilder.KRemoteClassesConfigBuilder.KRemoteClassesServerConfigBuilder
import kotlinx.remote.ktor.leaseRoutes
import kotlinx.remote.ktor.remote
import kotlinx.remote.remoteClient
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val nodeUrl = "http://localhost:8001"
    val leaseManager = LeaseManager(LeaseConfig(), RemoteInstancesPool())
    val block: KRemoteClassesServerConfigBuilder.() -> Unit = {
        this.nodeUrl = nodeUrl
        this.leaseManager = leaseManager
    }
    remoteEmbeddedServer(nodeUrl, leaseManager).start(wait = false)
    delay(5000)
    context(MasterConfig(block).asContext()) {
        Master.register(Worker())
    }
    delay(10000000)
    Unit
}

class WorkerConfig(private val nodeUrl: String): RemoteConfig {
    override val client: RemoteClient
        get() = remoteClient(nodeUrl, { this.nodeUrl = ""})
}

@RemoteSerializable
class Worker {

    @Remote
    context(_: RemoteContext<WorkerConfig>)
    suspend fun deregister() {
        TODO()
    }

    @Remote
    context(_: RemoteContext<WorkerConfig>)
    suspend fun computeMandelbrotSingleThreaded(
        region: ComplexRegion,
        pixelWidth: Int,
        pixelHeight: Int,
        config: MandelbrotConfig = MandelbrotConfig()
    ): IntArray {
        val iterations = IntArray(pixelWidth * pixelHeight)

        val maxIter = config.maxIterations
        val escapeRadiusSq = config.escapeRadius * config.escapeRadius

        val xScale = region.width / pixelWidth
        val yScale = region.height / pixelHeight

        for (py in 0 until pixelHeight) {
            val y0 = region.yMax - py * yScale // Flip Y for correct orientation

            for (px in 0 until pixelWidth) {
                val x0 = region.xMin + px * xScale
                val index = py * pixelWidth + px

                // Main iteration loop with escape time algorithm
                var x = 0.0
                var y = 0.0
                var x2 = 0.0
                var y2 = 0.0
                var iter = 0

                // Optimized iteration: compute x² and y² once per iteration
                while (x2 + y2 <= escapeRadiusSq && iter < maxIter) {
                    y = 2.0 * x * y + y0
                    x = x2 - y2 + x0
                    x2 = x * x
                    y2 = y * y
                    iter++
                }

                iterations[index] = iter
            }
        }

        return iterations
    }

}


fun computeMandelbrotSingleThreaded(
    region: ComplexRegion,
    pixelWidth: Int,
    pixelHeight: Int,
    config: MandelbrotConfig = MandelbrotConfig()
): IntArray {
    val iterations = IntArray(pixelWidth * pixelHeight)

    val maxIter = config.maxIterations
    val escapeRadiusSq = config.escapeRadius * config.escapeRadius

    val xScale = region.width / pixelWidth
    val yScale = region.height / pixelHeight

    for (py in 0 until pixelHeight) {
        val y0 = region.yMax - py * yScale // Flip Y for correct orientation

        for (px in 0 until pixelWidth) {
            val x0 = region.xMin + px * xScale
            val index = py * pixelWidth + px

            // Main iteration loop with escape time algorithm
            var x = 0.0
            var y = 0.0
            var x2 = 0.0
            var y2 = 0.0
            var iter = 0

            // Optimized iteration: compute x² and y² once per iteration
            while (x2 + y2 <= escapeRadiusSq && iter < maxIter) {
                y = 2.0 * x * y + y0
                x = x2 - y2 + x0
                x2 = x * x
                y2 = y * y
                iter++
            }

            iterations[index] = iter
        }
    }

    return iterations
}

fun remoteClient(url: String, block: KRemoteClassesServerConfigBuilder.() -> Unit = {}): RemoteClient = HttpClient {
    defaultRequest {
        url(url)
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
                    server { block() }
                }
            }
        })
    }
    install(Logging) {
        level = LogLevel.BODY
    }
}.remoteClient(genCallableMap(), "/call")

fun remoteEmbeddedServer(
    nodeUrl: String,
    leaseManager: LeaseManager = LeaseManager(LeaseConfig(), RemoteInstancesPool())
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    return embeddedServer(Netty, port = nodeUrl.split(":").last().toInt(), watchPaths = listOf()) {
        install(CallLogging)
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.application.environment.log.error("Unhandled exception during request", cause)
                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
            }
        }
        val config: KRemoteConfigBuilder.() -> Unit = {
            callableMap = genCallableMap()
            classes {
                remoteClasses = genRemoteClassList()
                client {}
                server {
                    this.leaseManager = leaseManager
                    this.nodeUrl = nodeUrl
                }
            }
        }
        install(KRemote, config)
        routing {
            remote("/call")
            leaseRoutes()
        }
    }
}
