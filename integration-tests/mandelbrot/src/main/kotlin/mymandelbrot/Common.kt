package mymandelbrot

data class ComplexRegion(
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double
) {
    val width: Double get() = xMax - xMin
    val height: Double get() = yMax - yMin
}

data class MandelbrotConfig(
    val maxIterations: Int = 1000,
    val escapeRadius: Double = 256.0,
)