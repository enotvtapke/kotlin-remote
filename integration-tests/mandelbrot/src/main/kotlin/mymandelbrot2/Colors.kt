package mymandelbrot2

import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin


enum class ColorPalette {
    FIRE,
    OCEAN,
    ELECTRIC,
    GRAYSCALE,
    RAINBOW,
    COSMIC;

    companion object {
        private const val PALETTE_SIZE = 2048
        private val palettes = mutableMapOf<ColorPalette, IntArray>()

        fun getColor(palette: ColorPalette, smoothValue: Double, maxIterations: Int, iterations: Int): Int {
            if (iterations >= maxIterations) return 0xFF000000.toInt() // Black for points in the set

            val colors = palettes.getOrPut(palette) { generatePalette(palette) }
            val index = ((smoothValue * 20.0) % PALETTE_SIZE).toInt().coerceIn(0, PALETTE_SIZE - 1)
            return colors[index]
        }

        private fun generatePalette(palette: ColorPalette): IntArray {
            return IntArray(PALETTE_SIZE) { i ->
                val t = i.toDouble() / PALETTE_SIZE
                when (palette) {
                    FIRE -> fireColor(t)
                    OCEAN -> oceanColor(t)
                    ELECTRIC -> electricColor(t)
                    GRAYSCALE -> grayscaleColor(t)
                    RAINBOW -> rainbowColor(t)
                    COSMIC -> cosmicColor(t)
                }
            }
        }

        private fun fireColor(t: Double): Int {
            val r = (min(1.0, t * 3.0) * 255).toInt()
            val g = (max(0.0, min(1.0, t * 3.0 - 1.0)) * 255).toInt()
            val b = (max(0.0, t * 3.0 - 2.0) * 255).toInt()
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        private fun oceanColor(t: Double): Int {
            val phase = t * 2 * PI
            val r = ((sin(phase) * 0.3 + 0.2) * 255).toInt().coerceIn(0, 255)
            val g = ((sin(phase + PI / 3) * 0.4 + 0.4) * 255).toInt().coerceIn(0, 255)
            val b = ((cos(phase) * 0.3 + 0.7) * 255).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        private fun electricColor(t: Double): Int {
            val phase = t * 4 * PI
            val r = ((sin(phase) * 0.5 + 0.5) * 255).toInt()
            val g = ((sin(phase + 2 * PI / 3) * 0.5 + 0.3) * 255).toInt()
            val b = ((cos(phase) * 0.5 + 0.5) * 255).toInt()
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        private fun grayscaleColor(t: Double): Int {
            val v = ((1.0 - cos(t * 2 * PI)) * 0.5 * 255).toInt()
            return (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }

        private fun rainbowColor(t: Double): Int {
            val hue = (t * 360).toFloat()
            val color = Color.getHSBColor(hue / 360f, 0.9f, 0.9f)
            return color.rgb
        }

        private fun cosmicColor(t: Double): Int {
            val phase = t * 6 * PI
            val r = ((sin(phase) * 0.4 + 0.3) * 255).toInt().coerceIn(0, 255)
            val g = ((sin(phase * 0.7 + 1.0) * 0.3 + 0.1) * 255).toInt().coerceIn(0, 255)
            val b = ((cos(phase * 0.5) * 0.5 + 0.5) * 255).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}