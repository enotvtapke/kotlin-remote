package mandelbrot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Main entry point for the Mandelbrot set visualization.
 * 
 * You can customize the view by changing the parameters:
 * - region: The complex region to visualize (use predefined regions or create custom ones)
 * - imageWidth/imageHeight: Resolution of the computed image
 * - mandelbrotConfig: Computation parameters (maxIterations affects detail level)
 * - parallelConfig: Parallelism settings (tileSize, number of threads)
 * - palette: Color scheme for rendering
 */
fun main() = application {
    // ============================================================
    // CONFIGURATION - Modify these values to zoom into different regions
    // ============================================================
    
    // Choose a region to visualize:
    // - mymandelbrot2.ComplexRegion.FULL - The complete Mandelbrot set
    // - mymandelbrot2.ComplexRegion.SEAHORSE_VALLEY - Famous seahorse valley detail
    // - mymandelbrot2.ComplexRegion.MINI_MANDELBROT - A miniature copy of the set
    // - mymandelbrot2.ComplexRegion.ELEPHANT_VALLEY - Elephant-like shapes
    // - mymandelbrot2.ComplexRegion.SPIRAL - Deep spiral structure
    // - Or create a custom region: mymandelbrot2.ComplexRegion(minReal, maxReal, minImag, maxImag)
    
    val region = ComplexRegion.FULL
    
    // Image resolution (higher = more detail but slower computation)
    val imageWidth = 10000
    val imageHeight = 8000
    
    // Mandelbrot computation settings
    // Higher maxIterations = more detail in boundary areas but slower
    val mandelbrotConfig = MandelbrotConfig(
        maxIterations = 1000,
        escapeRadius = 2.0
    )
    
    // Parallel computation settings
    val parallelConfig = ParallelConfig(
        tileSize = 64,  // Size of tiles for parallel computation
        parallelism = Runtime.getRuntime().availableProcessors()
    )
    
    // Color palette (try: Classic, Fire, Ocean, Psychedelic, Grayscale, Smooth)
    val palette = ColorPalette.Smooth
    
    // ============================================================
    // WINDOW SETUP
    // ============================================================
    
    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 960.dp)
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Mandelbrot Set Viewer - ${regionToString(region)}"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Use MandelbrotImage for progressive tile-by-tile rendering
            // Use MandelbrotImageComplete for rendering only when fully computed
            MandelbrotImage(
                region = region,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                mandelbrotConfig = mandelbrotConfig,
                parallelConfig = parallelConfig,
                palette = palette,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Converts a mymandelbrot2.ComplexRegion to a human-readable string for the window title.
 */
private fun regionToString(region: ComplexRegion): String {
    return when (region) {
        ComplexRegion.FULL -> "Full View"
        ComplexRegion.SEAHORSE_VALLEY -> "Seahorse Valley"
        ComplexRegion.MINI_MANDELBROT -> "Mini Mandelbrot"
        ComplexRegion.ELEPHANT_VALLEY -> "Elephant Valley"
        ComplexRegion.SPIRAL -> "Spiral"
        else -> "Custom Region (${region.minReal}, ${region.minImag}) to (${region.maxReal}, ${region.maxImag})"
    }
}

// ============================================================
// EXAMPLE USAGE: Different zoom levels and regions
// ============================================================

/**
 * Example: View the full Mandelbrot set
 */
fun viewFullSet() = renderMandelbrot(
    region = ComplexRegion.FULL,
    config = MandelbrotConfig(maxIterations = 500)
)

/**
 * Example: Zoom into the seahorse valley
 */
fun viewSeahorseValley() = renderMandelbrot(
    region = ComplexRegion.SEAHORSE_VALLEY,
    config = MandelbrotConfig(maxIterations = 2000)  // Higher iterations for detail
)

/**
 * Example: Deep zoom into a mini Mandelbrot
 */
fun viewMiniMandelbrot() = renderMandelbrot(
    region = ComplexRegion.MINI_MANDELBROT,
    config = MandelbrotConfig(maxIterations = 5000)  // Very high iterations for deep zoom
)

/**
 * Example: Custom zoom - you can define any rectangular region
 */
fun viewCustomRegion() = renderMandelbrot(
    region = ComplexRegion(
        minReal = -0.7463,
        maxReal = -0.7413,
        minImag = 0.1102,
        maxImag = 0.1152
    ),
    config = MandelbrotConfig(maxIterations = 1500)
)

/**
 * Helper function to render a specific region with given config
 */
private fun renderMandelbrot(
    region: ComplexRegion,
    config: MandelbrotConfig,
    width: Int = 1200,
    height: Int = 900,
    palette: ColorPalette = ColorPalette.Smooth
) = application {
    val windowState = rememberWindowState(size = DpSize(1280.dp, 960.dp))
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Mandelbrot Set - ${regionToString(region)}"
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            MandelbrotImage(
                region = region,
                imageWidth = width,
                imageHeight = height,
                mandelbrotConfig = config,
                palette = palette,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

