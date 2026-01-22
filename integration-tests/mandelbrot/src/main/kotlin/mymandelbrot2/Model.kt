package mymandelbrot2

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
)