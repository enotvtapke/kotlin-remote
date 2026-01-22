package mymandelbrot

class Worker {
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

}
