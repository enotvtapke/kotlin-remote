package mymandelbrot2

import kotlinx.coroutines.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Adjusts a complex region to match the aspect ratio of the given pixel dimensions.
 * Preserves the center and vertical extent, adjusting horizontal extent as needed.
 */
fun ComplexRegion.adjustToAspectRatio(pixelWidth: Int, pixelHeight: Int): ComplexRegion {
    val imageAspect = pixelWidth.toDouble() / pixelHeight
    val regionAspect = width / height

    return if (imageAspect > regionAspect) {
        // Image is wider than region - expand horizontally
        val newWidth = height * imageAspect
        val centerX = (xMin + xMax) / 2
        ComplexRegion(centerX - newWidth / 2, centerX + newWidth / 2, yMin, yMax)
    } else {
        // Image is taller than region - expand vertically
        val newHeight = width / imageAspect
        val centerY = (yMin + yMax) / 2
        ComplexRegion(xMin, xMax, centerY - newHeight / 2, centerY + newHeight / 2)
    }
}

fun toImage(
    iterations: IntArray,
    pixelWidth: Int,
    pixelHeight: Int,
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

suspend fun renderMandelbrotToFile(
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

    val result = computeMandelbrotParallel(
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
    println("=== Mandelbrot Set Visualization ===")
    println()

    // Example 1: Render a high-resolution image of the full set
    println("1. Rendering full Mandelbrot set to file...")
    renderMandelbrotToFile(
        region = ComplexRegion.SEAHORSE_VALLEY,
        width = 8000,
        height = 6000,
        config = MandelbrotConfig(maxIterations = 2000, escapeRadius = 256.0),
        palette = ColorPalette.OCEAN,
        outputPath = "mandelbrot_full.png"
    )
}

