package mandelbrot

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * Configuration for parallel Mandelbrot computation.
 * 
 * @param tileSize Size of each tile in pixels (tiles are square)
 * @param parallelism Number of coroutines to use for computation
 */
data class ParallelConfig(
    val tileSize: Int = 64,
    val parallelism: Int = Runtime.getRuntime().availableProcessors()
)

/**
 * Splits a region into tiles and computes them in parallel using coroutines.
 * Returns a Flow that emits tile results as they are computed.
 * This allows the UI to render tiles progressively as they become available.
 * 
 * @param region The complex region to compute
 * @param imageWidth Total image width in pixels
 * @param imageHeight Total image height in pixels
 * @param mandelbrotConfig Configuration for Mandelbrot computation
 * @param parallelConfig Configuration for parallel execution
 * @return Flow of TileResult that emits each tile as it's computed
 */
fun computeMandelbrotParallel(
    region: ComplexRegion,
    imageWidth: Int,
    imageHeight: Int,
    mandelbrotConfig: MandelbrotConfig = MandelbrotConfig(),
    parallelConfig: ParallelConfig = ParallelConfig()
): Flow<TileResult> = flow {
    val tileSize = parallelConfig.tileSize
    val tilesX = (imageWidth + tileSize - 1) / tileSize
    val tilesY = (imageHeight + tileSize - 1) / tileSize
    
    // Create a channel to receive computed tiles
    val resultChannel = Channel<TileResult>(Channel.BUFFERED)
    
    // Use a custom dispatcher with the specified parallelism
    val dispatcher = Dispatchers.Default.limitedParallelism(parallelConfig.parallelism)
    
    coroutineScope {
        // Launch producer coroutines for each tile
        val jobs = mutableListOf<Job>()
        
        for (tileY in 0 until tilesY) {
            for (tileX in 0 until tilesX) {
                // Calculate actual tile dimensions (may be smaller at edges)
                val actualTileWidth = minOf(tileSize, imageWidth - tileX * tileSize)
                val actualTileHeight = minOf(tileSize, imageHeight - tileY * tileSize)
                
                val job = launch(dispatcher) {
                    val result = computeTile(
                        region = region,
                        tileX = tileX,
                        tileY = tileY,
                        tileWidth = actualTileWidth,
                        tileHeight = actualTileHeight,
                        totalWidth = imageWidth,
                        totalHeight = imageHeight,
                        config = mandelbrotConfig
                    )
                    resultChannel.send(result)
                }
                jobs.add(job)
            }
        }
        
        // Close the channel when all jobs are done
        launch {
            jobs.forEach { it.join() }
            resultChannel.close()
        }
        
        // Emit results as they arrive
        for (result in resultChannel) {
            emit(result)
        }
    }
}.flowOn(Dispatchers.Default)

/**
 * Computes the entire Mandelbrot set image in parallel and returns
 * the complete iteration array when finished.
 * 
 * @param region The complex region to compute
 * @param imageWidth Total image width in pixels
 * @param imageHeight Total image height in pixels
 * @param mandelbrotConfig Configuration for Mandelbrot computation
 * @param parallelConfig Configuration for parallel execution
 * @return Complete IntArray of iteration values (row-major order)
 */
suspend fun computeMandelbrotParallelComplete(
    region: ComplexRegion,
    imageWidth: Int,
    imageHeight: Int,
    mandelbrotConfig: MandelbrotConfig = MandelbrotConfig(),
    parallelConfig: ParallelConfig = ParallelConfig()
): IntArray {
    val tileSize = parallelConfig.tileSize
    val result = IntArray(imageWidth * imageHeight)
    
    computeMandelbrotParallel(
        region = region,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        mandelbrotConfig = mandelbrotConfig,
        parallelConfig = parallelConfig
    ).collect { tile ->
        // Copy tile data into the mymandelbrot2.main result array
        copyTileToArray(tile, result, imageWidth, tileSize)
    }
    
    return result
}

/**
 * Copies tile iteration data into the mymandelbrot2.main result array.
 */
private fun copyTileToArray(
    tile: TileResult,
    dest: IntArray,
    imageWidth: Int,
    tileSize: Int
) {
    val startX = tile.tileX * tileSize
    val startY = tile.tileY * tileSize
    
    var tileIndex = 0
    for (localY in 0 until tile.height) {
        val destIndex = (startY + localY) * imageWidth + startX
        for (localX in 0 until tile.width) {
            dest[destIndex + localX] = tile.iterations[tileIndex++]
        }
    }
}

/**
 * Computes Mandelbrot in parallel with a callback for each completed tile.
 * Useful for progressive rendering where you want to update the UI as each tile completes.
 * 
 * @param region The complex region to compute
 * @param imageWidth Total image width in pixels
 * @param imageHeight Total image height in pixels
 * @param mandelbrotConfig Configuration for Mandelbrot computation
 * @param parallelConfig Configuration for parallel execution
 * @param onTileComplete Callback invoked when each tile is computed
 */
suspend fun computeMandelbrotParallelWithCallback(
    region: ComplexRegion,
    imageWidth: Int,
    imageHeight: Int,
    mandelbrotConfig: MandelbrotConfig = MandelbrotConfig(),
    parallelConfig: ParallelConfig = ParallelConfig(),
    onTileComplete: suspend (TileResult) -> Unit
) {
    computeMandelbrotParallel(
        region = region,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        mandelbrotConfig = mandelbrotConfig,
        parallelConfig = parallelConfig
    ).collect { tile ->
        onTileComplete(tile)
    }
}

/**
 * Returns the total number of tiles that will be computed for the given dimensions.
 */
fun calculateTileCount(imageWidth: Int, imageHeight: Int, tileSize: Int): Int {
    val tilesX = (imageWidth + tileSize - 1) / tileSize
    val tilesY = (imageHeight + tileSize - 1) / tileSize
    return tilesX * tilesY
}

