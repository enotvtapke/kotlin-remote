package mymandelbrot2

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.remote.*
import kotlinx.remote.classes.Stub
import kotlinx.remote.ktor.KRemoteConfigBuilder.KRemoteClassesConfigBuilder.KRemoteClassesServerConfigBuilder
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO
import kotlin.also

suspend fun renderMandelbrotToFileDistributed(
    region: ComplexRegion = ComplexRegion.FULL_SET,
    width: Int = 1920,
    height: Int = 1080,
    config: MandelbrotConfig = MandelbrotConfig(maxIterations = 2000),
    palette: ColorPalette = ColorPalette.FIRE,
    outputPath: String = "mandelbrot.png"
) {
    // Adjust region to match image aspect ratio
    val adjustedRegion = region.adjustToAspectRatio(width, height)

    println("Computing Mandelbrot set...")
    println("Region: $adjustedRegion")
    println("Size: ${width}x${height}")
    println("Max iterations: ${config.maxIterations}")

    val startTime = System.currentTimeMillis()

    val result = Master.computeMandelbrotParallel(
        region = adjustedRegion,
        pixelWidth = width,
        pixelHeight = height,
        config = config
    )

    val computeTime = System.currentTimeMillis() - startTime
    println("Computation completed in ${computeTime}ms")

    val image = toImage(result, width, height, palette, config.maxIterations)

    ImageIO.write(image, "PNG", File(outputPath))
    println("Image saved to $outputPath")
}


fun main() = runBlocking {
    remoteEmbeddedServer("http://localhost:8000").start(wait = false)
    delay(5000)
    renderMandelbrotToFileDistributed(
        region = ComplexRegion.FULL_SET,
        1920,
        1080
    )
    Unit
}

class MasterConfig(private val block: KRemoteClassesServerConfigBuilder.() -> Unit = {}): RemoteConfig {
    override val client: RemoteClient
        get() = remoteClient("http://localhost:8000", block)

}

object Master {
    private val freeWorkers: ConcurrentLinkedQueue<WorkerEntry> = ConcurrentLinkedQueue()
    private val id = atomic(0L)

    private data class WorkerEntry(val id: Long, val worker: Worker, val ctx: RemoteContext<WorkerConfig>)

    @Remote
    context(_: RemoteContext<MasterConfig>)
    suspend fun register(worker: Worker): Long {
        val newId = id.getAndIncrement()
        freeWorkers.add(WorkerEntry(newId, worker, WorkerConfig((worker as Stub).url).asContext()))
        return newId
    }

    @Remote
    context(_: RemoteContext<MasterConfig>)
    suspend fun deregister(id: Long): Boolean {
        return freeWorkers.removeIf { it.id == id }
    }

    suspend fun computeMandelbrotParallel(
        region: ComplexRegion,
        pixelWidth: Int,
        pixelHeight: Int,
        config: MandelbrotConfig = MandelbrotConfig(),
        tilesX: Int = Runtime.getRuntime().availableProcessors(),
        tilesY: Int = Runtime.getRuntime().availableProcessors(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): IntArray = coroutineScope {
        val iterations = IntArray(pixelWidth * pixelHeight)

        // Create tiles
        val tiles = createTiles(region, pixelWidth, pixelHeight, tilesX, tilesY)

        // Process tiles in parallel
        val deferredResults = tiles.map { tile ->
            async(dispatcher) {
                var workerEntry = freeWorkers.poll()
                while (workerEntry == null) {
                    delay(200)
                    workerEntry = freeWorkers.poll()
                }
                val (_, worker, ctx) = workerEntry
                try {
                    context(ctx) {
                        TileResult(
                            tile,
                            worker.computeMandelbrotSingleThreaded(
                                region = tile.region,
                                pixelWidth = tile.width,
                                pixelHeight = tile.height,
                                config = config
                            )
                        )
                    }.also {
                        freeWorkers.offer(workerEntry)
                    }
                } catch(e: Exception) {
                    println(e.stackTraceToString())
                    context(ctx) {
                        worker.deregister()
                    }
                    null
                }
            }
        }

        // Collect results and merge into final arrays
        deferredResults.forEach { deferred ->
            val tileResult = deferred.await()
            if (tileResult != null) mergeTileResult(tileResult, iterations, pixelWidth)
        }

        iterations
    }

    fun computeMandelbrotStreaming(
        region: ComplexRegion,
        pixelWidth: Int,
        pixelHeight: Int,
        config: MandelbrotConfig = MandelbrotConfig(),
        tilesX: Int = Runtime.getRuntime().availableProcessors(),
        tilesY: Int = Runtime.getRuntime().availableProcessors(),
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Flow<TileResult> = channelFlow {
        val tiles = createTiles(region, pixelWidth, pixelHeight, tilesX, tilesY)

        tiles.forEach { tile ->
            var workerEntry = freeWorkers.poll()
            while (workerEntry == null) {
                delay(200)
                workerEntry = freeWorkers.poll()
            }
            val (_, worker, ctx) = workerEntry
//            launch(dispatcher) {
                context(ctx) {
                    val result = TileResult(
                        tile,
                        worker.computeMandelbrotSingleThreaded(
                            region = tile.region,
                            pixelWidth = tile.width,
                            pixelHeight = tile.height,
                            config = config
                        )
                    )
                    send(result)
                }
//            }
            freeWorkers.offer(workerEntry)
        }
    }
}

/**
 * Computes the Mandelbrot set by splitting the region into tiles and
 * processing them in parallel using coroutines.
 *
 * @param region The complex plane region to compute
 * @param pixelWidth Width of the output in pixels
 * @param pixelHeight Height of the output in pixels
 * @param config Configuration for the computation
 * @param tilesX Number of horizontal tiles
 * @param tilesY Number of vertical tiles
 * @param dispatcher Coroutine dispatcher for parallel execution
 * @return mymandelbrot2.MandelbrotResult containing the combined results
 */
suspend fun computeMandelbrotParallel(
    region: ComplexRegion,
    pixelWidth: Int,
    pixelHeight: Int,
    config: MandelbrotConfig = MandelbrotConfig(),
    tilesX: Int = Runtime.getRuntime().availableProcessors(),
    tilesY: Int = Runtime.getRuntime().availableProcessors(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): IntArray = coroutineScope {
    val iterations = IntArray(pixelWidth * pixelHeight)

    // Create tiles
    val tiles = createTiles(region, pixelWidth, pixelHeight, tilesX, tilesY)

    // Process tiles in parallel
    val deferredResults = tiles.map { tile ->
        async(dispatcher) {
            TileResult(
                tile,
                computeMandelbrotSingleThreaded(
                    region = tile.region,
                    pixelWidth = tile.width,
                    pixelHeight = tile.height,
                    config = config
                )
            )
        }
    }

    // Collect results and merge into final arrays
    deferredResults.forEach { deferred ->
        val tileResult = deferred.await()
        mergeTileResult(tileResult, iterations, pixelWidth)
    }

    iterations
}

/**
 * Computes the Mandelbrot set by splitting the region into tiles and
 * emitting each tile result as a Flow as soon as computation completes.
 * This allows real-time visualization of tiles as they are computed.
 */
fun computeMandelbrotStreaming(
    region: ComplexRegion,
    pixelWidth: Int,
    pixelHeight: Int,
    config: MandelbrotConfig = MandelbrotConfig(),
    tilesX: Int = Runtime.getRuntime().availableProcessors(),
    tilesY: Int = Runtime.getRuntime().availableProcessors(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): Flow<TileResult> = channelFlow {
    val tiles = createTiles(region, pixelWidth, pixelHeight, tilesX, tilesY)
    
    tiles.forEach { tile ->
        launch(dispatcher) {
            val result = TileResult(
                tile,
                computeMandelbrotSingleThreaded(
                    region = tile.region,
                    pixelWidth = tile.width,
                    pixelHeight = tile.height,
                    config = config
                )
            )
            send(result)
        }
    }
}

/**
 * Returns the total number of tiles for the given configuration.
 */
fun getTileCount(
    tilesX: Int = Runtime.getRuntime().availableProcessors(),
    tilesY: Int = Runtime.getRuntime().availableProcessors()
): Int = tilesX * tilesY

data class Tile(
    val startX: Int,
    val startY: Int,
    val width: Int,
    val height: Int,
    val region: ComplexRegion
)

data class TileResult(
    val tile: Tile,
    val iterations: IntArray
)

fun createTiles(
    region: ComplexRegion,
    pixelWidth: Int,
    pixelHeight: Int,
    tilesX: Int,
    tilesY: Int
): List<Tile> {
    val tiles = mutableListOf<Tile>()
    val tileWidth = pixelWidth / tilesX
    val tileHeight = pixelHeight / tilesY

    val xScale = region.width / pixelWidth
    val yScale = region.height / pixelHeight

    for (ty in 0 until tilesY) {
        for (tx in 0 until tilesX) {
            val startX = tx * tileWidth
            val startY = ty * tileHeight

            // Handle edge tiles that might be larger due to rounding
            val width = if (tx == tilesX - 1) pixelWidth - startX else tileWidth
            val height = if (ty == tilesY - 1) pixelHeight - startY else tileHeight

            // Calculate complex plane region for this tile
            val tileRegion = ComplexRegion(
                xMin = region.xMin + startX * xScale,
                xMax = region.xMin + (startX + width) * xScale,
                yMin = region.yMax - (startY + height) * yScale,
                yMax = region.yMax - startY * yScale
            )

            tiles.add(Tile(startX, startY, width, height, tileRegion))
        }
    }

    return tiles
}

fun mergeTileResult(
    tileResult: TileResult,
    iterations: IntArray,
    totalWidth: Int
) {
    val tile = tileResult.tile

    for (ly in 0 until tile.height) {
        for (lx in 0 until tile.width) {
            val localIndex = ly * tile.width + lx
            val globalIndex = (tile.startY + ly) * totalWidth + (tile.startX + lx)

            iterations[globalIndex] = tileResult.iterations[localIndex]
        }
    }
}
