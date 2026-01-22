package mymandelbrot2

import kotlinx.coroutines.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// ============================================================================
// Data Classes for Region and Result Representation
// ============================================================================

/**
 * Represents a rectangular region in the complex plane.
 * @param xMin Left boundary (real axis)
 * @param xMax Right boundary (real axis)
 * @param yMin Bottom boundary (imaginary axis)
 * @param yMax Top boundary (imaginary axis)
 */
data class ComplexRegion(
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double
) {
    val width: Double get() = xMax - xMin
    val height: Double get() = yMax - yMin

    companion object {
        /** The classic full Mandelbrot set view */
        val FULL_SET = ComplexRegion(-2.5, 1.0, -1.25, 1.25)

        /** Seahorse Valley - beautiful spiral patterns */
        val SEAHORSE_VALLEY = ComplexRegion(-0.75, -0.74, 0.09, 0.10)

        /** Elephant Valley - trunk-like structures */
        val ELEPHANT_VALLEY = ComplexRegion(0.26, 0.27, 0.0, 0.01)

        /** Triple Spiral - intricate spiral formations */
        val TRIPLE_SPIRAL = ComplexRegion(-0.090, -0.086, 0.654, 0.658)

        /** Mini Mandelbrot deep zoom */
        val MINI_MANDELBROT = ComplexRegion(-1.7690, -1.7688, 0.00285, 0.00295)

        /** Lightning - electric-looking patterns */
        val LIGHTNING = ComplexRegion(-1.315, -1.305, 0.073, 0.083)
    }
}

/**
 * Configuration for Mandelbrot computation.
 */
data class MandelbrotConfig(
    val maxIterations: Int = 1000,
    val escapeRadius: Double = 256.0,
    val colorPalette: ColorPalette = ColorPalette.FIRE
)

/**
 * Result of computing a rectangular region of the Mandelbrot set.
 */
data class MandelbrotResult(
    val region: ComplexRegion,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val iterations: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MandelbrotResult) return false
        return region == other.region &&
                pixelWidth == other.pixelWidth &&
                pixelHeight == other.pixelHeight &&
                iterations.contentEquals(other.iterations)
    }

    override fun hashCode(): Int {
        var result = region.hashCode()
        result = 31 * result + pixelWidth
        result = 31 * result + pixelHeight
        result = 31 * result + iterations.contentHashCode()
        return result
    }
}

// ============================================================================
// Highly Optimized Single-Threaded Mandelbrot Computation
// ============================================================================

/**
 * Highly optimized single-threaded function that computes the Mandelbrot set
 * for a rectangular region.
 *
 * Optimizations applied:
 * 1. Cardioid and period-2 bulb checking for early bailout
 * 2. Escape radius squared pre-computation
 * 3. Loop unrolling hints via inline computation
 * 4. Minimal object allocation during iteration
 *
 * @param region The complex plane region to compute
 * @param pixelWidth Width of the output in pixels
 * @param pixelHeight Height of the output in pixels
 * @param config Configuration for the computation
 * @return mymandelbrot2.MandelbrotResult containing iteration counts
 */
fun computeMandelbrotSingleThreaded(
    region: ComplexRegion,
    pixelWidth: Int,
    pixelHeight: Int,
    config: MandelbrotConfig = MandelbrotConfig()
): MandelbrotResult {
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

            // Cardioid check: z is in the mymandelbrot2.main cardioid if |1 - sqrt(1 - 4c)| <= 1
            val q = (x0 - 0.25) * (x0 - 0.25) + y0 * y0
            val inCardioid = q * (q + (x0 - 0.25)) <= 0.25 * y0 * y0

            // Period-2 bulb check: circle centered at (-1, 0) with radius 0.25
            val inBulb = (x0 + 1.0) * (x0 + 1.0) + y0 * y0 <= 0.0625

            if (inCardioid || inBulb) {
                iterations[index] = maxIter
                continue
            }

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

    return MandelbrotResult(region, pixelWidth, pixelHeight, iterations)
}

// ============================================================================
// Parallel Computation Using Coroutines
// ============================================================================

/**
 * Represents a tile (sub-region) for parallel computation.
 */
private data class Tile(
    val startX: Int,
    val startY: Int,
    val width: Int,
    val height: Int,
    val region: ComplexRegion
)

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
): MandelbrotResult = coroutineScope {
    val iterations = IntArray(pixelWidth * pixelHeight)

    // Create tiles
    val tiles = createTiles(region, pixelWidth, pixelHeight, tilesX, tilesY)

    // Process tiles in parallel
    val deferredResults = tiles.map { tile ->
        async(dispatcher) {
            computeTile(tile, config)
        }
    }

    // Collect results and merge into final arrays
    deferredResults.forEach { deferred ->
        val tileResult = deferred.await()
        mergeTileResult(tileResult, iterations, pixelWidth)
    }

    MandelbrotResult(region, pixelWidth, pixelHeight, iterations)
}

/**
 * Creates tiles for parallel processing.
 */
private fun createTiles(
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

/**
 * Result of computing a single tile.
 */
private data class TileResult(
    val tile: Tile,
    val iterations: IntArray
)

/**
 * Computes a single tile using the optimized single-threaded algorithm.
 */
private fun computeTile(tile: Tile, config: MandelbrotConfig): TileResult {
    val result = computeMandelbrotSingleThreaded(
        region = tile.region,
        pixelWidth = tile.width,
        pixelHeight = tile.height,
        config = config
    )
    return TileResult(tile, result.iterations)
}

/**
 * Merges a tile result into the mymandelbrot2.main arrays.
 */
private fun mergeTileResult(
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

// ============================================================================
// Visualization Using Kotlin/JVM (AWT/Swing)
// ============================================================================

/**
 * Converts a mymandelbrot2.MandelbrotResult to a BufferedImage with the specified color palette.
 */
fun MandelbrotResult.toImage(
    palette: ColorPalette = ColorPalette.FIRE,
    maxIterations: Int = 1000
): BufferedImage {
    val image = BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB)

    for (y in 0 until pixelHeight) {
        for (x in 0 until pixelWidth) {
            val index = y * pixelWidth + x
            val color = ColorPalette.getColor(
                palette,
                maxIterations,
                iterations[index]
            )
            image.setRGB(x, y, color)
        }
    }

    return image
}

/**
 * Renders a Mandelbrot image to a file.
 *
 * @param region The complex plane region to render
 * @param width Output image width in pixels
 * @param height Output image height in pixels
 * @param config Computation configuration
 * @param palette Color palette to use
 * @param outputPath Path to save the image (PNG format)
 */
suspend fun renderMandelbrotToFile(
    region: ComplexRegion = ComplexRegion.FULL_SET,
    width: Int = 1920,
    height: Int = 1080,
    config: MandelbrotConfig = MandelbrotConfig(maxIterations = 2000),
    palette: ColorPalette = ColorPalette.FIRE,
    outputPath: String = "mandelbrot.png"
) {
    println("Computing Mandelbrot set...")
    println("Region: $region")
    println("Size: ${width}x${height}")
    println("Max iterations: ${config.maxIterations}")

    val startTime = System.currentTimeMillis()

    val result = computeMandelbrotParallel(
        region = region,
        pixelWidth = width,
        pixelHeight = height,
        config = config
    )

    val computeTime = System.currentTimeMillis() - startTime
    println("Computation completed in ${computeTime}ms")

    val image = result.toImage(palette, config.maxIterations)

    ImageIO.write(image, "PNG", File(outputPath))
    println("Image saved to $outputPath")
}

// ============================================================================
// Main Entry Point
// ============================================================================

fun main() = runBlocking {
    println("=== Mandelbrot Set Visualization ===")
    println()

    // Example 1: Render a high-resolution image of the full set
    println("1. Rendering full Mandelbrot set to file...")
    renderMandelbrotToFile(
        region = ComplexRegion.FULL_SET,
        width = 1920,
        height = 1080,
        config = MandelbrotConfig(maxIterations = 1000),
        palette = ColorPalette.FIRE,
        outputPath = "mandelbrot_full.png"
    )
}

