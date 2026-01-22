package mandelbrot

import kotlin.math.ln
import kotlin.math.sin

/**
 * Color palette for Mandelbrot visualization.
 * Converts iteration counts to ARGB colors.
 */
sealed class ColorPalette {
    
    /**
     * Converts an iteration count to an ARGB color value.
     * 
     * @param iteration The iteration count
     * @param maxIterations The maximum iterations used in computation
     * @return ARGB color as Int (0xAARRGGBB format)
     */
    abstract fun iterationToColor(iteration: Int, maxIterations: Int): Int
    
    /**
     * Classic blue-black palette with smooth gradients
     */
    object Classic : ColorPalette() {
        override fun iterationToColor(iteration: Int, maxIterations: Int): Int {
            if (iteration >= maxIterations) {
                return 0xFF000000.toInt() // Black for points in the set
            }
            
            val t = iteration.toDouble() / maxIterations
            val r = (9 * (1 - t) * t * t * t * 255).toInt().coerceIn(0, 255)
            val g = (15 * (1 - t) * (1 - t) * t * t * 255).toInt().coerceIn(0, 255)
            val b = (8.5 * (1 - t) * (1 - t) * (1 - t) * t * 255).toInt().coerceIn(0, 255)
            
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    
    /**
     * Fire-themed palette with reds, oranges, and yellows
     */
    object Fire : ColorPalette() {
        override fun iterationToColor(iteration: Int, maxIterations: Int): Int {
            if (iteration >= maxIterations) {
                return 0xFF000000.toInt()
            }
            
            val t = iteration.toDouble() / maxIterations
            val r = (255 * kotlin.math.sqrt(t)).toInt().coerceIn(0, 255)
            val g = (255 * t * t).toInt().coerceIn(0, 255)
            val b = (255 * t * t * t).toInt().coerceIn(0, 255)
            
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    
    /**
     * Ocean-themed palette with blues and cyans
     */
    object Ocean : ColorPalette() {
        override fun iterationToColor(iteration: Int, maxIterations: Int): Int {
            if (iteration >= maxIterations) {
                return 0xFF000000.toInt()
            }
            
            val t = iteration.toDouble() / maxIterations
            val r = (255 * t * t * t).toInt().coerceIn(0, 255)
            val g = (255 * t * t).toInt().coerceIn(0, 255)
            val b = (255 * kotlin.math.sqrt(t)).toInt().coerceIn(0, 255)
            
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    
    /**
     * Psychedelic palette with cycling hues
     */
    object Psychedelic : ColorPalette() {
        override fun iterationToColor(iteration: Int, maxIterations: Int): Int {
            if (iteration >= maxIterations) {
                return 0xFF000000.toInt()
            }
            
            val t = iteration.toDouble() / maxIterations
            val hue = t * 6.0
            
            val r = (127.5 * (1 + sin(hue * Math.PI))).toInt().coerceIn(0, 255)
            val g = (127.5 * (1 + sin((hue + 2) * Math.PI))).toInt().coerceIn(0, 255)
            val b = (127.5 * (1 + sin((hue + 4) * Math.PI))).toInt().coerceIn(0, 255)
            
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    
    /**
     * Grayscale palette
     */
    object Grayscale : ColorPalette() {
        override fun iterationToColor(iteration: Int, maxIterations: Int): Int {
            if (iteration >= maxIterations) {
                return 0xFF000000.toInt()
            }
            
            val gray = (255.0 * iteration / maxIterations).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        }
    }
    
    /**
     * Ultra fractal style smooth coloring with logarithmic scaling
     */
    object Smooth : ColorPalette() {
        private val colors = IntArray(2048) { i ->
            val t = i.toDouble() / 2048
            val hue = 0.7 + 10 * t
            hsvToRgb(hue % 1.0, 0.6, if (t < 0.5) t * 2 else 1.0)
        }
        
        override fun iterationToColor(iteration: Int, maxIterations: Int): Int {
            if (iteration >= maxIterations) {
                return 0xFF000000.toInt()
            }
            
            // Use log scaling for smoother color distribution
            val logScale = ln(1.0 + iteration.toDouble()) / ln(1.0 + maxIterations.toDouble())
            val index = (logScale * (colors.size - 1)).toInt().coerceIn(0, colors.size - 1)
            return colors[index]
        }
        
        private fun hsvToRgb(h: Double, s: Double, v: Double): Int {
            val i = (h * 6).toInt()
            val f = h * 6 - i
            val p = v * (1 - s)
            val q = v * (1 - f * s)
            val t = v * (1 - (1 - f) * s)
            
            val (r, g, b) = when (i % 6) {
                0 -> Triple(v, t, p)
                1 -> Triple(q, v, p)
                2 -> Triple(p, v, t)
                3 -> Triple(p, q, v)
                4 -> Triple(t, p, v)
                else -> Triple(v, p, q)
            }
            
            return (0xFF shl 24) or 
                   ((r * 255).toInt() shl 16) or 
                   ((g * 255).toInt() shl 8) or 
                   (b * 255).toInt()
        }
    }
    
    companion object {
        val DEFAULT = Smooth
        
        val ALL = listOf(Classic, Fire, Ocean, Psychedelic, Grayscale, Smooth)
    }
}

