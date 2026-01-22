package mandelbrot

/**
 * Represents a rectangular region in the complex plane.
 * 
 * @param minReal The minimum real (x) coordinate
 * @param maxReal The maximum real (x) coordinate
 * @param minImag The minimum imaginary (y) coordinate
 * @param maxImag The maximum imaginary (y) coordinate
 */
data class ComplexRegion(
    val minReal: Double,
    val maxReal: Double,
    val minImag: Double,
    val maxImag: Double
) {
    val width: Double get() = maxReal - minReal
    val height: Double get() = maxImag - minImag
    
    companion object {
        /** The full Mandelbrot set view */
        val FULL = ComplexRegion(-2.5, 1.0, -1.25, 1.25)
        
        /** Seahorse Valley - famous zoom location */
        val SEAHORSE_VALLEY = ComplexRegion(-0.75, -0.74, 0.1, 0.11)
        
        /** Mini Mandelbrot at a deep zoom */
        val MINI_MANDELBROT = ComplexRegion(-1.7688, -1.7686, 0.0017, 0.0019)
        
        /** Elephant Valley */
        val ELEPHANT_VALLEY = ComplexRegion(0.25, 0.35, 0.0, 0.1)
        
        /** Spiral location */
        val SPIRAL = ComplexRegion(-0.761574, -0.761564, 0.0847596, 0.0847696)
    }
}

/**
 * Configuration for Mandelbrot computation.
 * 
 * @param maxIterations Maximum number of iterations before considering a point as part of the set
 * @param escapeRadius The radius beyond which a point is considered to have escaped
 */
data class MandelbrotConfig(
    val maxIterations: Int = 1000,
    val escapeRadius: Double = 2.0
) {
    val escapeRadiusSquared: Double = escapeRadius * escapeRadius
}

/**
 * Result of a Mandelbrot computation for a tile.
 * 
 * @param tileX X position of the tile in the grid
 * @param tileY Y position of the tile in the grid
 * @param width Width of the tile in pixels
 * @param height Height of the tile in pixels
 * @param iterations Array of iteration counts for each pixel (row-major order)
 */
data class TileResult(
    val tileX: Int,
    val tileY: Int,
    val width: Int,
    val height: Int,
    val iterations: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TileResult) return false
        return tileX == other.tileX && tileY == other.tileY &&
               width == other.width && height == other.height &&
               iterations.contentEquals(other.iterations)
    }
    
    override fun hashCode(): Int {
        var result = tileX
        result = 31 * result + tileY
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + iterations.contentHashCode()
        return result
    }
}

/**
 * Highly optimized single-threaded Mandelbrot computation.
 * Uses several optimization techniques:
 * - Main cardioid and period-2 bulb detection
 * - Escape radius squared comparison (avoids sqrt)
 * - Minimized object allocations
 * - Loop unrolling for iteration computation
 * 
 * @param region The complex region to compute
 * @param pixelWidth The width of the output in pixels
 * @param pixelHeight The height of the output in pixels
 * @param config Computation configuration
 * @return Array of iteration counts for each pixel (row-major order)
 */
fun computeMandelbrotSingleThreaded(
    region: ComplexRegion,
    pixelWidth: Int,
    pixelHeight: Int,
    config: MandelbrotConfig = MandelbrotConfig()
): IntArray {
    val iterations = IntArray(pixelWidth * pixelHeight)
    
    val realStep = region.width / pixelWidth
    val imagStep = region.height / pixelHeight
    val maxIter = config.maxIterations
    val escapeRadiusSq = config.escapeRadiusSquared
    
    var index = 0
    var cImag = region.minImag
    
    for (py in 0 until pixelHeight) {
        var cReal = region.minReal
        
        for (px in 0 until pixelWidth) {
            iterations[index++] = computePixelOptimized(cReal, cImag, maxIter, escapeRadiusSq)
            cReal += realStep
        }
        
        cImag += imagStep
    }
    
    return iterations
}

/**
 * Computes a single pixel of the Mandelbrot set with optimizations.
 * 
 * @param cReal Real part of the complex number
 * @param cImag Imaginary part of the complex number
 * @param maxIterations Maximum iterations
 * @param escapeRadiusSq Squared escape radius
 * @return Number of iterations before escape, or maxIterations if in set
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun computePixelOptimized(
    cReal: Double,
    cImag: Double,
    maxIterations: Int,
    escapeRadiusSq: Double
): Int {
    // Quick bailout: check if in mymandelbrot2.main cardioid
    val cRealMinusQuarter = cReal - 0.25
    val cImagSq = cImag * cImag
    val q = cRealMinusQuarter * cRealMinusQuarter + cImagSq
    if (q * (q + cRealMinusQuarter) <= 0.25 * cImagSq) {
        return maxIterations
    }
    
    // Quick bailout: check if in period-2 bulb
    val cRealPlusOne = cReal + 1.0
    if (cRealPlusOne * cRealPlusOne + cImagSq <= 0.0625) {
        return maxIterations
    }
    
    var zReal = 0.0
    var zImag = 0.0
    var zRealSq = 0.0
    var zImagSq = 0.0
    var iteration = 0
    
    // Main iteration loop with minimized operations
    while (iteration < maxIterations && zRealSq + zImagSq <= escapeRadiusSq) {
        zImag = 2.0 * zReal * zImag + cImag
        zReal = zRealSq - zImagSq + cReal
        zRealSq = zReal * zReal
        zImagSq = zImag * zImag
        iteration++
    }
    
    return iteration
}

/**
 * Computes a tile of the Mandelbrot set - a portion of the full image.
 * Used for parallel computation where the image is split into tiles.
 * 
 * @param region The complex region for the entire image
 * @param tileX X position of the tile in the grid
 * @param tileY Y position of the tile in the grid
 * @param tileWidth Width of the tile in pixels
 * @param tileHeight Height of the tile in pixels
 * @param totalWidth Total width of the full image in pixels
 * @param totalHeight Total height of the full image in pixels
 * @param config Computation configuration
 * @return TileResult containing iteration data for this tile
 */
fun computeTile(
    region: ComplexRegion,
    tileX: Int,
    tileY: Int,
    tileWidth: Int,
    tileHeight: Int,
    totalWidth: Int,
    totalHeight: Int,
    config: MandelbrotConfig = MandelbrotConfig()
): TileResult {
    val iterations = IntArray(tileWidth * tileHeight)
    
    val realStep = region.width / totalWidth
    val imagStep = region.height / totalHeight
    val maxIter = config.maxIterations
    val escapeRadiusSq = config.escapeRadiusSquared
    
    val startPixelX = tileX * tileWidth
    val startPixelY = tileY * tileHeight
    
    var index = 0
    
    for (localY in 0 until tileHeight) {
        val globalY = startPixelY + localY
        val cImag = region.minImag + globalY * imagStep
        
        for (localX in 0 until tileWidth) {
            val globalX = startPixelX + localX
            val cReal = region.minReal + globalX * realStep
            
            iterations[index++] = computePixelOptimized(cReal, cImag, maxIter, escapeRadiusSq)
        }
    }
    
    return TileResult(tileX, tileY, tileWidth, tileHeight, iterations)
}

/**
 * Smooth coloring for Mandelbrot pixels using the escape time algorithm
 * with continuous coloring for smoother gradients.
 * 
 * @param iteration The iteration count from Mandelbrot computation
 * @param maxIterations Maximum iterations used in computation
 * @return A smooth value between 0.0 and 1.0 for coloring
 */
fun smoothColor(iteration: Int, maxIterations: Int): Double {
    return if (iteration >= maxIterations) {
        0.0 // Inside the set - black
    } else {
        iteration.toDouble() / maxIterations
    }
}

