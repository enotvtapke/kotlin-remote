package mymandelbrot2

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

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
