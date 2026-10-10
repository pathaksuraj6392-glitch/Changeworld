package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.BrushToolMode
import com.example.renderer.DepthClockCanvasRenderer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HyperCoral
import com.example.ui.theme.ObsidianCardGlass
import com.example.viewmodel.DepthClockUiState
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun DepthClockInteractiveCanvas(
    uiState: DepthClockUiState,
    onTransformClock: (deltaNormX: Float, deltaNormY: Float, zoomChange: Float) -> Unit,
    onBrushPoint: (normX: Float, normY: Float) -> Unit,
    onExitImmersivePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lastInteractionMillis by remember { mutableLongStateOf(0L) }
    var activeBrushPosition by remember { mutableStateOf<Offset?>(null) }

    // Live ticking clock loop (updates every 1 second)
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            val delayToNextSecond = 1000L - (currentTimeMillis % 1000L)
            delay(max(100L, delayToNextSecond))
        }
    }

    val showGuides = remember(lastInteractionMillis, currentTimeMillis) {
        (System.currentTimeMillis() - lastInteractionMillis) < 1100L
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .testTag("depth_clock_canvas")
            .pointerInput(uiState.brushToolMode, uiState.isImmersiveLockPreview, canvasSize) {
                if (canvasSize.width <= 0 || canvasSize.height <= 0) return@pointerInput

                if (uiState.brushToolMode != BrushToolMode.NONE && !uiState.isImmersiveLockPreview) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeBrushPosition = offset
                            val bg = uiState.backgroundBitmap ?: return@detectDragGestures
                            val (nx, ny) = DepthClockCanvasRenderer.screenToNormalizedBitmapCoords(
                                touchX = offset.x,
                                touchY = offset.y,
                                srcW = bg.width,
                                srcH = bg.height,
                                dstW = canvasSize.width.toFloat(),
                                dstH = canvasSize.height.toFloat()
                            )
                            onBrushPoint(nx, ny)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            activeBrushPosition = change.position
                            val bg = uiState.backgroundBitmap ?: return@detectDragGestures
                            val (nx, ny) = DepthClockCanvasRenderer.screenToNormalizedBitmapCoords(
                                touchX = change.position.x,
                                touchY = change.position.y,
                                srcW = bg.width,
                                srcH = bg.height,
                                dstW = canvasSize.width.toFloat(),
                                dstH = canvasSize.height.toFloat()
                            )
                            onBrushPoint(nx, ny)
                        },
                        onDragEnd = { activeBrushPosition = null },
                        onDragCancel = { activeBrushPosition = null }
                    )
                } else {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val dxNorm = pan.x / canvasSize.width.toFloat()
                        val dyNorm = pan.y / canvasSize.height.toFloat()
                        lastInteractionMillis = System.currentTimeMillis()
                        onTransformClock(dxNorm, dyNorm, zoom)
                    }
                }
            }
            .pointerInput(uiState.isImmersiveLockPreview) {
                if (uiState.isImmersiveLockPreview) {
                    detectTapGestures(
                        onDoubleTap = { onExitImmersivePreview() }
                    )
                }
            }
    ) {
        val bgBitmap = uiState.backgroundBitmap
        if (bgBitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Force read of currentTimeMillis so Canvas redraws every second
                val tick = currentTimeMillis
                DepthClockCanvasRenderer.renderDepthComposition(
                    context = context,
                    canvas = drawContext.canvas.nativeCanvas,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    backgroundBitmap = bgBitmap,
                    foregroundCutout = uiState.foregroundCutout,
                    maskPreviewOverlay = uiState.maskPreviewOverlay,
                    config = uiState.config,
                    timeMillis = tick,
                    showMaskOverlay = uiState.showMaskOverlay,
                    showAlignmentGuides = showGuides && !uiState.isImmersiveLockPreview
                )

                // Draw live circular brush cursor when painting the subject mask
                val brushPos = activeBrushPosition
                if (brushPos != null && uiState.brushToolMode != BrushToolMode.NONE) {
                    val radiusPx = uiState.brushRadiusNorm * max(size.width, size.height)
                    val brushColor = if (uiState.brushToolMode == BrushToolMode.RESTORE_SUBJECT) {
                        ElectricCyan
                    } else {
                        HyperCoral
                    }
                    drawCircle(
                        color = brushColor.copy(alpha = 0.28f),
                        radius = radiusPx,
                        center = brushPos
                    )
                    drawCircle(
                        color = brushColor,
                        radius = radiusPx,
                        center = brushPos,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        // Loading / AI Segmentation Shimmer Indicator
        AnimatedVisibility(
            visible = uiState.isAnalyzingPhoto || uiState.isExporting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = ObsidianCardGlass,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 12.dp,
                modifier = Modifier
                    .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = ElectricCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (uiState.isExporting) {
                                "Rendering High-Res Depth Layer..."
                            } else {
                                "On-Device AI Separating Subject..."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Google ML Kit + Edge Salience Engine",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // Immersive Lock Screen Preview Chrome (Padlock, Flashlight/Camera Shortcuts, & Exit Pill)
        AnimatedVisibility(
            visible = uiState.isImmersiveLockPreview,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top lock icon + Exit pill
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.48f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                            .clickable(onClick = onExitImmersivePreview)
                            .testTag("exit_immersive_preview_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock screen preview active",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Lock Screen Preview • Tap to Edit",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Exit preview",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Authentic Lock Screen Bottom Corner Affordances (Flashlight & Camera) + Home Indicator
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 32.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashlightOn,
                                contentDescription = "Lock screen flashlight preview",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = "Swipe or drag clock directly",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Lock screen camera preview",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Lock screen gesture bar
                    Box(
                        modifier = Modifier
                            .width(136.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}
