package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushToolMode
import com.example.model.BuiltInPresets
import com.example.model.ClockColorPreset
import com.example.model.ClockFontStyle
import com.example.model.ClockLayoutStyle
import com.example.model.DepthClockConfig
import com.example.model.SampleWallpaperPreset
import com.example.model.SegmentationMode
import com.example.ui.theme.AlabasterWhite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HyperCoral
import com.example.ui.theme.ObsidianCardGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateOutline
import com.example.viewmodel.DepthClockUiState
import com.example.viewmodel.EditorTab
import kotlin.math.roundToInt

@Composable
fun EditorControlSheet(
    uiState: DepthClockUiState,
    onSelectTab: (EditorTab) -> Unit,
    onToggleExpand: () -> Unit,
    onUpdateConfig: ((DepthClockConfig) -> DepthClockConfig) -> Unit,
    onResetClockPosition: () -> Unit,
    onPickGalleryPhoto: () -> Unit,
    onSelectPreset: (SampleWallpaperPreset) -> Unit,
    onSetBrushMode: (BrushToolMode) -> Unit,
    onSetBrushRadius: (Float) -> Unit,
    onResetBrushEdits: () -> Unit,
    onToggleMaskOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianCardGlass,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = SlateOutline.copy(alpha = 0.7f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 8.dp, bottom = 10.dp)
        ) {
            // Top Pill Handle + Tab Bar + Collapse Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorTab.entries.forEach { tab ->
                        val isSelected = uiState.activeTab == tab
                        val tabIcon = when (tab) {
                            EditorTab.STYLE -> Icons.Default.FormatPaint
                            EditorTab.POSITION -> Icons.Default.OpenWith
                            EditorTab.AI_MASK -> Icons.Default.AutoAwesome
                            EditorTab.PRESETS -> Icons.Default.PhotoLibrary
                        }
                        TabPillButton(
                            title = tab.title,
                            icon = tabIcon,
                            selected = isSelected,
                            onClick = { onSelectTab(tab) },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("toggle_panel_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isPanelExpanded) {
                            Icons.Default.ExpandMore
                        } else {
                            Icons.Default.ExpandLess
                        },
                        contentDescription = if (uiState.isPanelExpanded) {
                            "Collapse Studio Panel"
                        } else {
                            "Expand Studio Panel"
                        },
                        tint = AlabasterWhite
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isPanelExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (uiState.activeTab) {
                        EditorTab.STYLE -> StyleControlsTab(
                            config = uiState.config,
                            onUpdateConfig = onUpdateConfig
                        )

                        EditorTab.POSITION -> PositionControlsTab(
                            config = uiState.config,
                            onUpdateConfig = onUpdateConfig,
                            onResetClockPosition = onResetClockPosition
                        )

                        EditorTab.AI_MASK -> AiMaskControlsTab(
                            uiState = uiState,
                            onUpdateConfig = onUpdateConfig,
                            onSetBrushMode = onSetBrushMode,
                            onSetBrushRadius = onSetBrushRadius,
                            onResetBrushEdits = onResetBrushEdits,
                            onToggleMaskOverlay = onToggleMaskOverlay
                        )

                        EditorTab.PRESETS -> PhotosAndPresetsTab(
                            selectedPresetId = uiState.selectedPresetId,
                            onPickGalleryPhoto = onPickGalleryPhoto,
                            onSelectPreset = onSelectPreset
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) ElectricCyan else ObsidianSurfaceVariant.copy(alpha = 0.75f)
    val contentColor = if (selected) Color(0xFF002025) else AlabasterWhite

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StyleControlsTab(
    config: DepthClockConfig,
    onUpdateConfig: ((DepthClockConfig) -> DepthClockConfig) -> Unit
) {
    // 1. Lock Screen Layout Style Selector
    Text(
        text = "LOCK SCREEN ARCHITECTURE",
        style = MaterialTheme.typography.labelMedium,
        color = SlateMuted
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(ClockLayoutStyle.entries) { layout ->
            val selected = config.layoutStyle == layout
            Surface(
                color = if (selected) ElectricCyan.copy(alpha = 0.18f) else ObsidianSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(136.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ElectricCyan else SlateOutline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onUpdateConfig { it.copy(layoutStyle = layout) } }
                    .testTag("layout_${layout.name.lowercase()}")
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = layout.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) ElectricCyan else AlabasterWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = layout.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // 2. Clock Typography Selector
    Text(
        text = "CLOCK TYPOGRAPHY",
        style = MaterialTheme.typography.labelMedium,
        color = SlateMuted
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(ClockFontStyle.entries) { fontStyle ->
            val selected = config.fontStyle == fontStyle
            val previewFontFamily = FontFamily(Font(fontStyle.fontResId, FontWeight.Bold))
            Surface(
                color = if (selected) ElectricCyan.copy(alpha = 0.18f) else ObsidianSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(114.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ElectricCyan else SlateOutline,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onUpdateConfig { it.copy(fontStyle = fontStyle) } }
                    .testTag("font_${fontStyle.name.lowercase()}")
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "12:45",
                        fontFamily = previewFontFamily,
                        fontSize = 22.sp,
                        color = if (selected) ElectricCyan else AlabasterWhite
                    )
                    Text(
                        text = fontStyle.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // 3. Color & Gradient Swatches
    Text(
        text = "COLOR & METALLIC GRADIENT",
        style = MaterialTheme.typography.labelMedium,
        color = SlateMuted
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(ClockColorPreset.entries) { preset ->
            val selected = config.colorPreset == preset
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onUpdateConfig { it.copy(colorPreset = preset) } }
                    .testTag("color_${preset.name.lowercase()}")
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(preset.primaryColorArgb),
                                    Color(preset.secondaryColorArgb)
                                )
                            )
                        )
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) ElectricCyan else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "${preset.displayName} selected",
                            tint = Color(0xFF070A12),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) ElectricCyan else SlateMuted
                )
            }
        }
    }

    // 4. Seconds, 24-Hour, Date, & Shadow Chips
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = config.showSeconds,
            onClick = { onUpdateConfig { it.copy(showSeconds = !it.showSeconds) } },
            label = { Text("Show Seconds (:SS)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                selectedLabelColor = ElectricCyan
            ),
            modifier = Modifier.testTag("toggle_seconds_chip")
        )

        FilterChip(
            selected = config.use24HourFormat,
            onClick = { onUpdateConfig { it.copy(use24HourFormat = !it.use24HourFormat) } },
            label = { Text("24-Hour Time") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                selectedLabelColor = ElectricCyan
            ),
            modifier = Modifier.testTag("toggle_24h_chip")
        )

        FilterChip(
            selected = config.showDate,
            onClick = { onUpdateConfig { it.copy(showDate = !it.showDate) } },
            label = { Text("Show Date") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                selectedLabelColor = ElectricCyan
            ),
            modifier = Modifier.testTag("toggle_date_chip")
        )

        FilterChip(
            selected = config.enableClockDropShadow,
            onClick = { onUpdateConfig { it.copy(enableClockDropShadow = !it.enableClockDropShadow) } },
            label = { Text("3D Clock Glow") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                selectedLabelColor = ElectricCyan
            )
        )
    }
}

@Composable
private fun PositionControlsTab(
    config: DepthClockConfig,
    onUpdateConfig: ((DepthClockConfig) -> DepthClockConfig) -> Unit,
    onResetClockPosition: () -> Unit
) {
    // Depth Effect Master Toggle Card
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateOutline, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Depth Effect Layering",
                    tint = ElectricCyan
                )
                Column {
                    Text(
                        text = "3D Depth Occlusion (Clock Behind Subject)",
                        style = MaterialTheme.typography.labelLarge,
                        color = AlabasterWhite
                    )
                    Text(
                        text = if (config.enableDepthEffect) {
                            "Active • Subject overlaps clock numerals in 3D"
                        } else {
                            "Disabled • Clock sits flat in front of subject"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (config.enableDepthEffect) EmeraldSuccess else SlateMuted
                    )
                }
            }
            Switch(
                checked = config.enableDepthEffect,
                onCheckedChange = { enabled ->
                    onUpdateConfig { it.copy(enableDepthEffect = enabled) }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF002025),
                    checkedTrackColor = ElectricCyan
                ),
                modifier = Modifier.testTag("depth_effect_switch")
            )
        }
    }

    // Scale / Size Slider
    LabeledSliderRow(
        label = "Clock Scale",
        valueText = "${(config.clockScale * 100).roundToInt()}%",
        value = config.clockScale,
        valueRange = 0.50f..1.90f,
        onValueChange = { s -> onUpdateConfig { it.copy(clockScale = s) } },
        testTag = "clock_scale_slider"
    )

    // Vertical Y Position Slider
    LabeledSliderRow(
        label = "Vertical Depth (Y)",
        valueText = "${(config.clockNormY * 100).roundToInt()}%",
        value = config.clockNormY,
        valueRange = 0.12f..0.75f,
        onValueChange = { y -> onUpdateConfig { it.copy(clockNormY = y) } },
        testTag = "clock_y_slider"
    )

    // Horizontal X Position Slider
    LabeledSliderRow(
        label = "Horizontal Offset (X)",
        valueText = "${(config.clockNormX * 100).roundToInt()}%",
        value = config.clockNormX,
        valueRange = 0.20f..0.80f,
        onValueChange = { x -> onUpdateConfig { it.copy(clockNormX = x) } },
        testTag = "clock_x_slider"
    )

    // Clock Opacity & Background Dim Sliders
    LabeledSliderRow(
        label = "Clock Opacity",
        valueText = "${(config.clockOpacity * 100).roundToInt()}%",
        value = config.clockOpacity,
        valueRange = 0.30f..1.0f,
        onValueChange = { alpha -> onUpdateConfig { it.copy(clockOpacity = alpha) } },
        testTag = "clock_opacity_slider"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onResetClockPosition,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("reset_clock_position_button")
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Snap to optimal depth",
                tint = ElectricCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Snap to Optimal Subject Depth", color = AlabasterWhite)
        }
    }
}

@Composable
private fun AiMaskControlsTab(
    uiState: DepthClockUiState,
    onUpdateConfig: ((DepthClockConfig) -> DepthClockConfig) -> Unit,
    onSetBrushMode: (BrushToolMode) -> Unit,
    onSetBrushRadius: (Float) -> Unit,
    onResetBrushEdits: () -> Unit,
    onToggleMaskOverlay: () -> Unit
) {
    // AI Telemetry Header Card
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateOutline, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.activeEngineLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
                Text(
                    text = "Subject Mask Coverage: ${uiState.subjectCoveragePercent}% • ${uiState.processingTimeMs}ms offline",
                    style = MaterialTheme.typography.labelMedium,
                    color = SlateMuted
                )
            }

            FilterChip(
                selected = uiState.showMaskOverlay,
                onClick = onToggleMaskOverlay,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "X-Ray Mask",
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("X-Ray Mask") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                    selectedLabelColor = ElectricCyan
                ),
                modifier = Modifier.testTag("xray_mask_toggle")
            )
        }
    }

    // Segmentation Engine Mode Selector
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SegmentationMode.entries.forEach { mode ->
            val selected = uiState.config.segmentationMode == mode
            FilterChip(
                selected = selected,
                onClick = { onUpdateConfig { it.copy(segmentationMode = mode) } },
                label = { Text(mode.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                    selectedLabelColor = ElectricCyan
                ),
                modifier = Modifier.testTag("seg_mode_${mode.name.lowercase()}")
            )
        }
    }

    // Interactive Touch Mask Brush Bar (Restore Subject / Erase Background)
    Text(
        text = "INTERACTIVE TOUCH MASK REFINEMENT",
        style = MaterialTheme.typography.labelMedium,
        color = SlateMuted
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrushToolMode.entries.forEach { mode ->
            val selected = uiState.brushToolMode == mode
            val chipColor = when (mode) {
                BrushToolMode.NONE -> ElectricCyan
                BrushToolMode.RESTORE_SUBJECT -> EmeraldSuccess
                BrushToolMode.ERASE_SUBJECT -> HyperCoral
            }
            FilterChip(
                selected = selected,
                onClick = { onSetBrushMode(mode) },
                leadingIcon = {
                    Icon(
                        imageVector = if (mode == BrushToolMode.NONE) Icons.Default.OpenWith else Icons.Default.Brush,
                        contentDescription = mode.displayName,
                        modifier = Modifier.size(15.dp)
                    )
                },
                label = { Text(mode.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.24f),
                    selectedLabelColor = chipColor
                ),
                modifier = Modifier.testTag("brush_mode_${mode.name.lowercase()}")
            )
        }

        IconButton(
            onClick = onResetBrushEdits,
            modifier = Modifier
                .size(40.dp)
                .testTag("reset_brush_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset manual brush strokes",
                tint = SlateMuted
            )
        }
    }

    if (uiState.brushToolMode != BrushToolMode.NONE) {
        LabeledSliderRow(
            label = "Touch Brush Size",
            valueText = "${(uiState.brushRadiusNorm * 1000).roundToInt()} px",
            value = uiState.brushRadiusNorm,
            valueRange = 0.02f..0.12f,
            onValueChange = onSetBrushRadius,
            testTag = "brush_size_slider"
        )
    }

    // AI Mask Cutoff Threshold & Edge Feathering
    LabeledSliderRow(
        label = "Subject Sensitivity Threshold",
        valueText = "${(uiState.config.maskThreshold * 100).roundToInt()}%",
        value = uiState.config.maskThreshold,
        valueRange = 0.18f..0.72f,
        onValueChange = { t -> onUpdateConfig { it.copy(maskThreshold = t) } },
        testTag = "mask_threshold_slider"
    )

    LabeledSliderRow(
        label = "Edge Feathering Smoothness",
        valueText = "${uiState.config.maskFeatherRadius} px",
        value = uiState.config.maskFeatherRadius.toFloat(),
        valueRange = 0f..5f,
        onValueChange = { f -> onUpdateConfig { it.copy(maskFeatherRadius = f.roundToInt()) } },
        testTag = "mask_feather_slider"
    )
}

@Composable
private fun PhotosAndPresetsTab(
    selectedPresetId: String?,
    onPickGalleryPhoto: () -> Unit,
    onSelectPreset: (SampleWallpaperPreset) -> Unit
) {
    // Primary Call-to-Action: Pick Any Photo from Device Gallery
    Button(
        onClick = onPickGalleryPhoto,
        colors = ButtonDefaults.buttonColors(
            containerColor = ElectricCyan,
            contentColor = Color(0xFF002025)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("pick_gallery_photo_button")
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = "Pick photo from gallery"
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Choose Any Photo from Gallery",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }

    Text(
        text = "BUILT-IN CURATED DEPTH PRESETS (OFFLINE READY)",
        style = MaterialTheme.typography.labelMedium,
        color = SlateMuted
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(BuiltInPresets.presets) { preset ->
            val selected = preset.id == selectedPresetId
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(150.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) ElectricCyan else SlateOutline,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectPreset(preset) }
                    .testTag("preset_${preset.id}")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = preset.drawableResId),
                        contentDescription = preset.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.82f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = preset.category.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected preset",
                                tint = Color(0xFF002025),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = AlabasterWhite
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = ObsidianSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag(testTag)
        )
    }
}
