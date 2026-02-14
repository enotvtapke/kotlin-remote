package mymandelbrot2

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

/**
 * Applies a tile result to an existing BufferedImage.
 * Used for incremental rendering where tiles are drawn as they complete.
 */
fun applyTileToImage(
    image: BufferedImage,
    tileResult: TileResult,
    palette: ColorPalette = ColorPalette.FIRE,
    maxIterations: Int = 1000
) {
    val tile = tileResult.tile
    for (ly in 0 until tile.height) {
        for (lx in 0 until tile.width) {
            val localIndex = ly * tile.width + lx
            val color = ColorPalette.getColor(
                palette,
                maxIterations,
                tileResult.iterations[localIndex]
            )
            image.setRGB(tile.startX + lx, tile.startY + ly, color)
        }
    }
}

/**
 * Creates an empty BufferedImage with a dark background for visualization.
 */
fun createEmptyImage(width: Int, height: Int): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.color = java.awt.Color(0x1a, 0x1a, 0x2e) // Dark blue-gray background
    graphics.fillRect(0, 0, width, height)
    graphics.dispose()
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

fun main() {
    remoteEmbeddedServer("http://localhost:8000").start(wait = false)
    // Launch the Compose Desktop application for real-time visualization
    launchMandelbrotApp()
}
