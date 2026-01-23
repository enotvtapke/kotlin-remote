package mymandelbrot2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import javax.swing.JPanel

// Image resolution presets
data class ImageResolution(val width: Int, val height: Int, val label: String)

// Color Palette - Cosmic Dark Theme
private val CosmicDark = Color(0xFF0d0d1a)
private val CosmicMid = Color(0xFF1a1a2e)
private val CosmicLight = Color(0xFF25253d)
private val NeonCyan = Color(0xFF00fff5)
private val NeonPink = Color(0xFFff00ff)
private val NeonOrange = Color(0xFFff6b35)
private val TextPrimary = Color(0xFFF0F0F0)
private val TextSecondary = Color(0xFFa0a0b0)

fun launchMandelbrotApp() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mandelbrot Explorer",
        state = rememberWindowState(width = 1400.dp, height = 950.dp)
    ) {
        MandelbrotApp()
    }
}

private val resolutionPresets = listOf(
    ImageResolution(800, 600, "800x600"),
    ImageResolution(1280, 960, "1280x960"),
    ImageResolution(1600, 1200, "1600x1200"),
    ImageResolution(1920, 1440, "1920x1440"),
    ImageResolution(2560, 1920, "2560x1920"),
    ImageResolution(3840, 2880, "4K"),
)

/**
 * Custom JPanel that renders a BufferedImage directly without conversion.
 * This is much more efficient than converting to Compose ImageBitmap.
 */
class MandelbrotPanel : JPanel() {
    var image: BufferedImage? = null
        set(value) {
            field = value
            repaint()
        }
    
    // Callback for zoom interactions
    var onZoom: ((relX: Double, relY: Double, zoomFactor: Double) -> Unit)? = null
    
    // Stored draw area for coordinate calculations
    private var drawX = 0
    private var drawY = 0
    private var drawWidth = 0
    private var drawHeight = 0
    
    init {
        background = java.awt.Color(0x1a, 0x1a, 0x2e)
        
        // Handle mouse clicks for zooming
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1) {
                    handleZoom(e.x, e.y, 0.3) // Left click = zoom in 3x
                } else if (e.button == MouseEvent.BUTTON3) {
                    handleZoom(e.x, e.y, 2.0) // Right click = zoom out 2x
                }
            }
        })
        
        // Handle mouse wheel for zooming
        addMouseWheelListener { e: MouseWheelEvent ->
            val zoomFactor = if (e.wheelRotation > 0) 1.5 else 0.5
            handleZoom(e.x, e.y, zoomFactor)
        }
    }
    
    private fun handleZoom(mouseX: Int, mouseY: Int, zoomFactor: Double) {
        if (drawWidth == 0 || drawHeight == 0) return
        
        // Convert to relative position (0-1)
        val relX = ((mouseX - drawX).toDouble() / drawWidth).coerceIn(0.0, 1.0)
        val relY = ((mouseY - drawY).toDouble() / drawHeight).coerceIn(0.0, 1.0)
        
        onZoom?.invoke(relX, relY, zoomFactor)
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        
        // Enable high quality rendering
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        
        val img = image ?: return
        
        // Calculate scaling to fit while maintaining aspect ratio
        val panelWidth = width
        val panelHeight = height
        val imageAspect = img.width.toDouble() / img.height
        val panelAspect = panelWidth.toDouble() / panelHeight
        
        if (imageAspect > panelAspect) {
            drawWidth = panelWidth
            drawHeight = (panelWidth / imageAspect).toInt()
        } else {
            drawHeight = panelHeight
            drawWidth = (panelHeight * imageAspect).toInt()
        }
        
        drawX = (panelWidth - drawWidth) / 2
        drawY = (panelHeight - drawHeight) / 2
        
        g2.drawImage(img, drawX, drawY, drawWidth, drawHeight, null)
    }
}

@Composable
fun MandelbrotApp() {
    val scope = rememberCoroutineScope()

    // Image dimensions
    var selectedResolution by remember { mutableStateOf(resolutionPresets[2]) } // Default 1600x1200
    
    // Current viewing region (can be zoomed/panned independently of presets)
    var currentRegion by remember { mutableStateOf(ComplexRegion.FULL_SET) }
    var zoomHistory by remember { mutableStateOf(listOf(ComplexRegion.FULL_SET)) }

    // State
    var tilesCompleted by remember { mutableIntStateOf(0) }
    var totalTiles by remember { mutableIntStateOf(0) }
    var isComputing by remember { mutableStateOf(false) }
    var selectedPalette by remember { mutableStateOf(ColorPalette.OCEAN) }
    var maxIterations by remember { mutableIntStateOf(1000) }

    // Reference to the Swing panel for direct image updates
    val mandelbrotPanel = remember { MandelbrotPanel() }
    
    // Trigger recomposition when computation state changes
    var computationTrigger by remember { mutableIntStateOf(0) }

    fun zoomToPoint(relX: Double, relY: Double, zoomFactor: Double) {
        if (isComputing) return
        
        // Map to complex plane coordinates
        val adjustedRegion = currentRegion.adjustToAspectRatio(selectedResolution.width, selectedResolution.height)
        val complexX = adjustedRegion.xMin + relX * adjustedRegion.width
        val complexY = adjustedRegion.yMax - relY * adjustedRegion.height // Y is inverted
        
        // Calculate new region centered on click point
        val newWidth = adjustedRegion.width * zoomFactor
        val newHeight = adjustedRegion.height * zoomFactor
        
        val newRegion = ComplexRegion(
            xMin = complexX - newWidth / 2,
            xMax = complexX + newWidth / 2,
            yMin = complexY - newHeight / 2,
            yMax = complexY + newHeight / 2
        )
        
        // Save history and update region
        zoomHistory = zoomHistory + currentRegion
        currentRegion = newRegion
        computationTrigger++
    }

    fun startComputation() {
        if (isComputing) return
        
        val imageWidth = selectedResolution.width
        val imageHeight = selectedResolution.height

        scope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Swing) {
                isComputing = true
                tilesCompleted = 0
            }

            val tilesX = Runtime.getRuntime().availableProcessors()
            val tilesY = Runtime.getRuntime().availableProcessors()
            
            withContext(Dispatchers.Swing) {
                totalTiles = tilesX * tilesY
            }

            val adjustedRegion = currentRegion.adjustToAspectRatio(imageWidth, imageHeight)
            val config = MandelbrotConfig(maxIterations = maxIterations, escapeRadius = 256.0)

            // Create empty image buffer
            val buffer = createEmptyImage(imageWidth, imageHeight)
            
            withContext(Dispatchers.Swing) {
                mandelbrotPanel.image = buffer
            }

            var localTilesCompleted = 0

            // Stream tiles and update UI as each completes
            computeMandelbrotStreaming(
                region = adjustedRegion,
                pixelWidth = imageWidth,
                pixelHeight = imageHeight,
                config = config,
                tilesX = tilesX,
                tilesY = tilesY
            ).flowOn(Dispatchers.Default).collect { tileResult ->
                // Apply tile directly to buffer
                applyTileToImage(buffer, tileResult, selectedPalette, maxIterations)
                localTilesCompleted++
                
                // Just repaint the panel - no expensive conversion!
                withContext(Dispatchers.Swing) {
                    tilesCompleted = localTilesCompleted
                    mandelbrotPanel.repaint()
                }
            }

            withContext(Dispatchers.Swing) {
                tilesCompleted = localTilesCompleted
                mandelbrotPanel.repaint()
                isComputing = false
            }
        }
    }
    
    fun zoomOut() {
        if (isComputing) return
        
        // Zoom out by 2x from center
        val adjustedRegion = currentRegion.adjustToAspectRatio(selectedResolution.width, selectedResolution.height)
        val centerX = (adjustedRegion.xMin + adjustedRegion.xMax) / 2
        val centerY = (adjustedRegion.yMin + adjustedRegion.yMax) / 2
        val newWidth = adjustedRegion.width * 2
        val newHeight = adjustedRegion.height * 2
        
        val newRegion = ComplexRegion(
            xMin = centerX - newWidth / 2,
            xMax = centerX + newWidth / 2,
            yMin = centerY - newHeight / 2,
            yMax = centerY + newHeight / 2
        )
        
        zoomHistory = zoomHistory + currentRegion
        currentRegion = newRegion
        computationTrigger++
    }
    
    fun goBack() {
        if (isComputing || zoomHistory.isEmpty()) return
        
        currentRegion = zoomHistory.last()
        zoomHistory = zoomHistory.dropLast(1)
        computationTrigger++
    }
    
    fun resetToPreset(preset: ComplexRegion) {
        if (isComputing) return
        currentRegion = preset
        zoomHistory = listOf(preset)
        computationTrigger++
    }
    
    // Set up zoom callback
    LaunchedEffect(Unit) {
        mandelbrotPanel.onZoom = { relX, relY, zoomFactor ->
            zoomToPoint(relX, relY, zoomFactor)
        }
    }

    // Start/restart computation when trigger changes
    LaunchedEffect(computationTrigger) {
        startComputation()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main canvas area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Image display using SwingPanel for direct BufferedImage rendering
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, CosmicLight, RoundedCornerShape(12.dp))
                ) {
                    SwingPanel(
                        factory = { mandelbrotPanel },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Progress overlay
                    if (isComputing && totalTiles > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicDark.copy(alpha = 0.85f))
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { tilesCompleted.toFloat() / totalTiles },
                                    modifier = Modifier.size(24.dp),
                                    color = NeonCyan,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Rendering: $tilesCompleted / $totalTiles tiles",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Control panel with scroll
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(CosmicMid)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "MANDELBROT",
                    color = NeonCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp
                )

                Divider(color = CosmicLight, thickness = 1.dp)
                
                // Zoom controls
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ZOOM",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Back button
                        OutlinedButton(
                            onClick = { goBack() },
                            enabled = !isComputing && zoomHistory.size > 1,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonPink,
                                disabledContentColor = TextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (!isComputing && zoomHistory.size > 1) NeonPink else CosmicLight
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("BACK", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                        
                        // Zoom out button
                        OutlinedButton(
                            onClick = { zoomOut() },
                            enabled = !isComputing,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonOrange,
                                disabledContentColor = TextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (!isComputing) NeonOrange else CosmicLight
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("OUT", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                    
                    Text(
                        text = "Click image to zoom in, scroll to zoom",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Divider(color = CosmicLight, thickness = 1.dp)
                
                // Resolution selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "RESOLUTION",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        resolutionPresets.take(3).forEach { resolution ->
                            ResolutionButton(
                                resolution = resolution,
                                isSelected = selectedResolution == resolution,
                                onClick = { selectedResolution = resolution },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        resolutionPresets.drop(3).forEach { resolution ->
                            ResolutionButton(
                                resolution = resolution,
                                isSelected = selectedResolution == resolution,
                                onClick = { selectedResolution = resolution },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Divider(color = CosmicLight, thickness = 1.dp)

                // Region presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PRESETS",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    val regions = listOf(
                        "Full Set" to ComplexRegion.FULL_SET,
                        "Seahorse Valley" to ComplexRegion.SEAHORSE_VALLEY,
                        "Elephant Valley" to ComplexRegion.ELEPHANT_VALLEY,
                        "Triple Spiral" to ComplexRegion.TRIPLE_SPIRAL,
                        "Mini Mandelbrot" to ComplexRegion.MINI_MANDELBROT,
                        "Lightning" to ComplexRegion.LIGHTNING
                    )

                    regions.forEach { (name, region) ->
                        RegionButton(
                            name = name,
                            isSelected = currentRegion == region,
                            onClick = { resetToPreset(region) }
                        )
                    }
                }

                Divider(color = CosmicLight, thickness = 1.dp)

                // Palette selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PALETTE",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorPalette.entries.forEach { palette ->
                            PaletteButton(
                                palette = palette,
                                isSelected = selectedPalette == palette,
                                onClick = { selectedPalette = palette },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Divider(color = CosmicLight, thickness = 1.dp)

                // Iterations slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MAX ITERATIONS: $maxIterations",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Slider(
                        value = maxIterations.toFloat(),
                        onValueChange = { maxIterations = it.toInt() },
                        valueRange = 100f..5000f,
                        steps = 48,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = CosmicLight
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Render button
                Button(
                    onClick = { startComputation() },
                    enabled = !isComputing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isComputing) CosmicLight else NeonCyan,
                        contentColor = CosmicDark,
                        disabledContainerColor = CosmicLight,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isComputing) "RENDERING..." else "RENDER",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionButton(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) NeonCyan else CosmicLight,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) NeonCyan else TextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PaletteButton(
    palette: ColorPalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (palette) {
        ColorPalette.FIRE -> Color(0xFFFF6B35)
        ColorPalette.OCEAN -> Color(0xFF4169E1)
        ColorPalette.ELECTRIC -> Color(0xFF9B59B6)
        ColorPalette.GRAYSCALE -> Color(0xFF808080)
        ColorPalette.RAINBOW -> Color(0xFFFF1493)
        ColorPalette.COSMIC -> Color(0xFF00CED1)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) TextPrimary else CosmicLight,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ResolutionButton(
    resolution: ImageResolution,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) NeonCyan else CosmicLight,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = resolution.label,
            color = if (isSelected) NeonCyan else TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

