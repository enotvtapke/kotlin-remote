package mandelbrot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.collect
import java.awt.image.BufferedImage

/**
 * Creates an ImageBitmap from ARGB pixel array using AWT interop.
 */
fun createBitmapFromPixels(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    bufferedImage.setRGB(0, 0, width, height, pixels, 0, width)
    return bufferedImage.toComposeImageBitmap()
}

/**
 * Creates an ImageBitmap from iteration data using a color palette.
 */
fun createMandelbrotBitmap(
    iterations: IntArray,
    width: Int,
    height: Int,
    palette: ColorPalette,
    maxIterations: Int
): ImageBitmap {
    val pixels = IntArray(width * height)
    
    for (i in iterations.indices) {
        pixels[i] = palette.iterationToColor(iterations[i], maxIterations)
    }
    
    return createBitmapFromPixels(pixels, width, height)
}

/**
 * Creates an ImageBitmap from a single tile's data.
 */
fun createTileBitmap(
    tile: TileResult,
    palette: ColorPalette,
    maxIterations: Int
): ImageBitmap {
    val pixels = IntArray(tile.width * tile.height)
    
    for (i in tile.iterations.indices) {
        pixels[i] = palette.iterationToColor(tile.iterations[i], maxIterations)
    }
    
    return createBitmapFromPixels(pixels, tile.width, tile.height)
}

/**
 * Composable function that renders the Mandelbrot set with progressive tile rendering.
 * Tiles are rendered independently as they are computed, allowing the user to see
 * the image build up progressively.
 * 
 * @param region The complex region to visualize
 * @param imageWidth Width of the image in pixels
 * @param imageHeight Height of the image in pixels
 * @param mandelbrotConfig Configuration for Mandelbrot computation
 * @param parallelConfig Configuration for parallel execution
 * @param palette Color palette for rendering
 * @param modifier Compose modifier
 */
@Composable
fun MandelbrotImage(
    region: ComplexRegion,
    imageWidth: Int,
    imageHeight: Int,
    mandelbrotConfig: MandelbrotConfig = MandelbrotConfig(),
    parallelConfig: ParallelConfig = ParallelConfig(),
    palette: ColorPalette = ColorPalette.DEFAULT,
    modifier: Modifier = Modifier
) {
    // State to hold computed tile bitmaps - keyed by (tileX, tileY)
    val tiles = remember { mutableStateMapOf<Pair<Int, Int>, ImageBitmap>() }
    val tileSize = parallelConfig.tileSize
    
    // Launch computation when parameters change
    LaunchedEffect(region, imageWidth, imageHeight, mandelbrotConfig, parallelConfig, palette) {
        tiles.clear()
        
        computeMandelbrotParallel(
            region = region,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            mandelbrotConfig = mandelbrotConfig,
            parallelConfig = parallelConfig
        ).collect { tile ->
            // Convert tile to bitmap and add to state - triggers recomposition
            val bitmap = createTileBitmap(tile, palette, mandelbrotConfig.maxIterations)
            tiles[tile.tileX to tile.tileY] = bitmap
        }
    }
    
    // Render canvas with all completed tiles
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Calculate scaling to fit the image in the canvas while maintaining aspect ratio
        val scaleX = size.width / imageWidth
        val scaleY = size.height / imageHeight
        val scale = minOf(scaleX, scaleY)
        
        // Center the image in the canvas
        val offsetX = (size.width - imageWidth * scale) / 2
        val offsetY = (size.height - imageHeight * scale) / 2
        
        // Draw each completed tile at its correct position
        tiles.forEach { (pos, bitmap) ->
            val (tileX, tileY) = pos
            val x = offsetX + tileX * tileSize * scale
            val y = offsetY + tileY * tileSize * scale
            
            drawImage(
                image = bitmap,
                dstOffset = IntOffset(x.toInt(), y.toInt()),
                dstSize = IntSize(
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1)
                )
            )
        }
    }
}

/**
 * Simplified Mandelbrot viewer that computes and displays the entire image at once.
 * Uses parallel computation internally but waits for completion before displaying.
 * 
 * @param region The complex region to visualize
 * @param imageWidth Width of the image in pixels
 * @param imageHeight Height of the image in pixels
 * @param mandelbrotConfig Configuration for Mandelbrot computation
 * @param parallelConfig Configuration for parallel execution
 * @param palette Color palette for rendering
 * @param modifier Compose modifier
 */
@Composable
fun MandelbrotImageComplete(
    region: ComplexRegion,
    imageWidth: Int,
    imageHeight: Int,
    mandelbrotConfig: MandelbrotConfig = MandelbrotConfig(),
    parallelConfig: ParallelConfig = ParallelConfig(),
    palette: ColorPalette = ColorPalette.DEFAULT,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Compute the Mandelbrot set when parameters change
    LaunchedEffect(region, imageWidth, imageHeight, mandelbrotConfig, palette) {
        val iterations = computeMandelbrotParallelComplete(
            region = region,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            mandelbrotConfig = mandelbrotConfig,
            parallelConfig = parallelConfig
        )
        bitmap = createMandelbrotBitmap(
            iterations = iterations,
            width = imageWidth,
            height = imageHeight,
            palette = palette,
            maxIterations = mandelbrotConfig.maxIterations
        )
    }
    
    // Render the complete image
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        bitmap?.let { img ->
            val scaleX = size.width / img.width
            val scaleY = size.height / img.height
            val scale = minOf(scaleX, scaleY)
            
            val offsetX = (size.width - img.width * scale) / 2
            val offsetY = (size.height - img.height * scale) / 2
            
            drawImage(
                image = img,
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(
                    (img.width * scale).toInt().coerceAtLeast(1),
                    (img.height * scale).toInt().coerceAtLeast(1)
                )
            )
        }
    }
}
