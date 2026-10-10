package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BrushToolMode
import com.example.ui.components.DepthClockInteractiveCanvas
import com.example.ui.components.EditorControlSheet
import com.example.ui.components.OnboardingModal
import com.example.ui.components.WallpaperExportModal
import com.example.ui.theme.AlabasterWhite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCardGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateOutline
import com.example.viewmodel.DepthClockViewModel
import com.example.wallpaper.WallpaperExportManager

@Composable
fun DepthClockStudioScreen(
    viewModel: DepthClockViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Zero-permission Android Photo Picker for choosing any photo from Gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onGalleryPhotoSelected(uri)
        }
    }

    val launchGalleryPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Back button exits full-screen immersive Lock Screen Preview first
    BackHandler(enabled = uiState.isImmersiveLockPreview) {
        viewModel.toggleImmersiveLockPreview(false)
    }

    // Synchronize Android System Bars for Full-Screen Immersive Mode
    LaunchedEffect(uiState.isImmersiveLockPreview) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (uiState.isImmersiveLockPreview) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Show Snackbar notifications
    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Screen 3-Layer Interactive Depth Clock Canvas
        DepthClockInteractiveCanvas(
            uiState = uiState,
            onTransformClock = viewModel::onTransformClock,
            onBrushPoint = viewModel::onBrushStrokeAtNormalizedPoint,
            onExitImmersivePreview = { viewModel.toggleImmersiveLockPreview(false) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Floating Glass Studio Bar (Hidden in Immersive Lock Preview)
        AnimatedVisibility(
            visible = !uiState.isImmersiveLockPreview,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(
                color = ObsidianCardGlass,
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateOutline.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Brand Title + Quick 3D Depth Toggle Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.18f))
                                .clickable {
                                    viewModel.updateConfig {
                                        it.copy(enableDepthEffect = !it.enableDepthEffect)
                                    }
                                }
                                .testTag("quick_depth_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Toggle 3D Depth",
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Depth Clock",
                                style = MaterialTheme.typography.titleMedium,
                                color = AlabasterWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.config.enableDepthEffect) {
                                    "3D Depth Active"
                                } else {
                                    "Flat Clock Mode"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.config.enableDepthEffect) EmeraldSuccess else SlateMuted
                            )
                        }
                    }

                    // Action Buttons: Gallery, Immersive Lock Preview, Help, Save, Set Wallpaper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = launchGalleryPicker,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("top_pick_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Pick Photo from Gallery",
                                tint = ElectricCyan
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleImmersiveLockPreview(true) },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("top_lock_preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full Screen Lock Preview",
                                tint = AlabasterWhite
                            )
                        }

                        IconButton(
                            onClick = { viewModel.saveHighResImageToGallery() },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("top_save_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Save HD Image",
                                tint = AlabasterWhite
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setOnboardingVisible(true) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("top_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "How to use Depth Clock",
                                tint = SlateMuted
                            )
                        }

                        Button(
                            onClick = { viewModel.setWallpaperModalVisible(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF002025)
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 14.dp,
                                vertical = 8.dp
                            ),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("top_set_wallpaper_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Apply",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Bottom Glassmorphic Studio Controls + Interactive Hint Pill
        AnimatedVisibility(
            visible = !uiState.isImmersiveLockPreview,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subtle Interactive Gesture Hint Pill
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = when (uiState.brushToolMode) {
                            BrushToolMode.NONE -> "Drag or pinch clock on canvas • Tap Fullscreen to lock preview"
                            BrushToolMode.RESTORE_SUBJECT -> "Brush Mode: Paint over subject areas to bring in front of clock"
                            BrushToolMode.ERASE_SUBJECT -> "Eraser Mode: Paint over background areas to reveal clock"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                EditorControlSheet(
                    uiState = uiState,
                    onSelectTab = viewModel::selectTab,
                    onToggleExpand = viewModel::togglePanelExpanded,
                    onUpdateConfig = viewModel::updateConfig,
                    onResetClockPosition = viewModel::resetClockPosition,
                    onPickGalleryPhoto = launchGalleryPicker,
                    onSelectPreset = viewModel::loadPreset,
                    onSetBrushMode = viewModel::setBrushToolMode,
                    onSetBrushRadius = viewModel::setBrushRadius,
                    onResetBrushEdits = viewModel::resetBrushEdits,
                    onToggleMaskOverlay = { viewModel.toggleMaskOverlay() }
                )
            }
        }

        // 4. Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 72.dp, start = 16.dp, end = 16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = ObsidianSurface,
                contentColor = AlabasterWhite,
                actionColor = ElectricCyan,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 5. Onboarding Modal
        if (uiState.showOnboardingDialog) {
            OnboardingModal(
                onDismiss = { viewModel.setOnboardingVisible(false) },
                onPickPhotoNow = launchGalleryPicker
            )
        }

        // 6. Wallpaper & Export Modal
        if (uiState.showWallpaperModal) {
            WallpaperExportModal(
                onDismiss = { viewModel.setWallpaperModalVisible(false) },
                onSaveHighResImage = { viewModel.saveHighResImageToGallery() },
                onApplyStaticWallpaper = { target -> viewModel.applyStaticWallpaper(target) },
                onActivateLiveWallpaper = {
                    viewModel.prepareLiveWallpaperAndLaunch {
                        runCatching {
                            context.startActivity(
                                WallpaperExportManager.createLiveWallpaperIntent(context)
                            )
                        }.onFailure {
                            viewModel.applyStaticWallpaper(
                                com.example.wallpaper.WallpaperTarget.BOTH_SCREENS
                            )
                        }
                    }
                }
            )
        }
    }
}
