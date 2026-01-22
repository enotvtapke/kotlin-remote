package mymandelbrot2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
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
import java.awt.image.BufferedImage

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
        state = rememberWindowState(width = 1200.dp, height = 900.dp)
    ) {
        MandelbrotApp()
    }
}

@Composable
fun MandelbrotApp() {
    val scope = rememberCoroutineScope()

    // Image dimensions
    val imageWidth = 1600
    val imageHeight = 1200

    // State
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var tilesCompleted by remember { mutableIntStateOf(0) }
    var totalTiles by remember { mutableIntStateOf(0) }
    var isComputing by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf(ComplexRegion.FULL_SET) }
    var selectedPalette by remember { mutableStateOf(ColorPalette.OCEAN) }
    var maxIterations by remember { mutableIntStateOf(1000) }

    // Mutable image buffer
    var imageBuffer by remember { mutableStateOf<BufferedImage?>(null) }

    fun startComputation() {
        if (isComputing) return

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

            val adjustedRegion = selectedRegion.adjustToAspectRatio(imageWidth, imageHeight)
            val config = MandelbrotConfig(maxIterations = maxIterations, escapeRadius = 256.0)

            // Create empty image buffer
            val buffer = createEmptyImage(imageWidth, imageHeight)
            
            withContext(Dispatchers.Swing) {
                imageBuffer = buffer
                imageBitmap = buffer.toComposeImageBitmap()
            }

            // Stream tiles and update UI as each completes
            computeMandelbrotStreaming(
                region = adjustedRegion,
                pixelWidth = imageWidth,
                pixelHeight = imageHeight,
                config = config,
                tilesX = tilesX,
                tilesY = tilesY
            ).flowOn(Dispatchers.Default).collect { tileResult ->
                // Apply tile to buffer (can be done off main thread)
                applyTileToImage(buffer, tileResult, selectedPalette, maxIterations)
                
                // Update UI on Swing EDT
                withContext(Dispatchers.Swing) {
                    tilesCompleted++
                    // Convert to ImageBitmap for display
                    imageBitmap = buffer.toComposeImageBitmap()
                }
            }

            withContext(Dispatchers.Swing) {
                isComputing = false
            }
        }
    }

    // Start computation on first load
    LaunchedEffect(Unit) {
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
                // Image display
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, CosmicLight, RoundedCornerShape(12.dp))
                        .background(CosmicMid),
                    contentAlignment = Alignment.Center
                ) {
                    imageBitmap?.let { bitmap ->
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            // Calculate scaling to fit while maintaining aspect ratio
                            val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                            val canvasAspect = canvasWidth / canvasHeight

                            val (drawWidth, drawHeight) = if (imageAspect > canvasAspect) {
                                canvasWidth to canvasWidth / imageAspect
                            } else {
                                canvasHeight * imageAspect to canvasHeight
                            }

                            val offsetX = (canvasWidth - drawWidth) / 2
                            val offsetY = (canvasHeight - drawHeight) / 2

                            drawImage(
                                image = bitmap,
                                dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt()),
                                dstOffset = androidx.compose.ui.geometry.Offset(offsetX, offsetY).let {
                                    androidx.compose.ui.unit.IntOffset(it.x.toInt(), it.y.toInt())
                                }
                            )
                        }
                    }

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

            // Control panel
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(CosmicMid)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
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

                // Region selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "REGION",
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
                            isSelected = selectedRegion == region,
                            onClick = { selectedRegion = region }
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        .height(56.dp),
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
                        fontSize = 16.sp,
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

