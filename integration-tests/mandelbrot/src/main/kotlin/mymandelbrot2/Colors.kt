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

        fun getColor(palette: ColorPalette, maxIterations: Int, iterations: Int): Int {
            if (iterations >= maxIterations) return 0xFF000000.toInt() // Black for points in the set

            val colors = palettes.getOrPut(palette) { generatePalette(palette) }
            val index = ((iterations * 20) % PALETTE_SIZE).coerceIn(0, PALETTE_SIZE - 1)
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

fun main() {
//    val p = Solution().deleteNode(N(10, N(5, N(4, N(3), N(-2)), N(2, null, N(1))), N(-3, null, N(11))), 4)
    val p = Solution().deleteNode(N(10, N(5, N(2), N(7)), N(20)), 5)
//    val root = N(10, N(5, N(2, null, N(3)), N(7)), N(20))
//    val p = Solution().removeMin(root)
    println(p)
//    println(root)
}

typealias N = TreeNode

data class TreeNode(var `val`: Int, var left: TreeNode? = null, var right: TreeNode? = null) {
    override fun equals(other: Any?): Boolean {
        return this.`val` == (other as? TreeNode)?.`val`
    }
}

class Solution {

    fun TreeNode.nullify() = TreeNode(`val`)

    fun removeMin1(root: TreeNode): Pair<TreeNode, TreeNode?> {
        return when {
            root.left == null -> root.nullify() to root.right
            root.left!!.left == null -> {
                val left = root.left!!
                root.left = left.right
                left.nullify() to root
            }
            else -> removeMin1(root.left!!).first to root
        }
    }

    fun removeRoot1(root: TreeNode): TreeNode? {
        return when {
            root.left == null -> root.right
            root.right == null -> root.left
            else -> {
                val (removed, newRoot) = removeMin1(root.right!!)
                removed.left = root.left
                removed.right = newRoot
                removed
            }
        }
    }

    fun deleteNode(root: TreeNode?, key: Int): TreeNode? {
        if (root == null) return null
        if (root.left?.`val` == key) {
            root.left = removeRoot1(root.left!!)
//            if (root.left!!.right == null) {
//                root.left = root.left!!.left
//            } else {
//                val node = removeMin(root.left!!.right!!)
//                if (node.`val` == root.left!!.right!!.`val`) {
//                    node.right = null
//                } else {
//                    node.right = root.left?.right
//                }
//                node.left = root.left?.left
//                root.left = node
//            }
        }
        if (root.right?.`val` == key) {
            root.right = removeRoot1(root.right!!)

//            if (root.right!!.right == null) {
//                root.right = root.right!!.left
//            } else {
//                val node = removeMin(root.right!!.right!!)
//                if (node.`val` == root.right!!.right!!.`val`) {
//                    node.right = null
//                } else {
//                    node.right = root.right?.right
//                }
//                node.left = root.right?.left
//                node.right = root.right?.right
//                root.right = node
//            }
        }
        if (root.`val` > key) {
            deleteNode(root.left, key)
        } else if (root.`val` < key) {
            deleteNode(root.right, key)
        } else {
            return removeRoot1(root)
        }
        return root
    }
}