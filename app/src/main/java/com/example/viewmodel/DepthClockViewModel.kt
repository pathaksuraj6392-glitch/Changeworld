package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BrushToolMode
import com.example.model.BuiltInPresets
import com.example.model.ClockColorPreset
import com.example.model.ClockFontStyle
import com.example.model.ClockLayoutStyle
import com.example.model.DepthClockConfig
import com.example.model.SampleWallpaperPreset
import com.example.model.SegmentationMode
import com.example.segmentation.RawSegmentationData
import com.example.segmentation.SubjectSegmentationEngine
import com.example.wallpaper.WallpaperExportManager
import com.example.wallpaper.WallpaperTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class EditorTab(val title: String) {
    STYLE("Clock Style"),
    POSITION("3D Position"),
    AI_MASK("AI Cutout"),
    PRESETS("Photos")
}

data class DepthClockUiState(
    val backgroundBitmap: Bitmap? = null,
    val foregroundCutout: Bitmap? = null,
    val maskPreviewOverlay: Bitmap? = null,
    val config: DepthClockConfig = BuiltInPresets.presets.first().recommendedConfig,
    val selectedPresetId: String? = BuiltInPresets.presets.first().id,
    val activeTab: EditorTab = EditorTab.STYLE,
    val isPanelExpanded: Boolean = true,
    val isImmersiveLockPreview: Boolean = false,
    val brushToolMode: BrushToolMode = BrushToolMode.NONE,
    val brushRadiusNorm: Float = 0.055f,
    val showMaskOverlay: Boolean = false,
    val isAnalyzingPhoto: Boolean = true,
    val isExporting: Boolean = false,
    val subjectCoveragePercent: Int = 0,
    val activeEngineLabel: String = "ML Kit Neural + Edge Hybrid",
    val processingTimeMs: Long = 0L,
    val showOnboardingDialog: Boolean = false,
    val showWallpaperModal: Boolean = false,
    val snackbarMessage: String? = null,
    val lastSavedImageUri: Uri? = null
)

class DepthClockViewModel(application: Application) : AndroidViewModel(application) {

    private val segmentationEngine = SubjectSegmentationEngine()
    private var currentRawSegmentation: RawSegmentationData? = null
    private var maskRebuildJob: Job? = null

    private val _uiState = MutableStateFlow(DepthClockUiState())
    val uiState: StateFlow<DepthClockUiState> = _uiState.asStateFlow()

    init {
        val context = getApplication<Application>()
        val onboardingDone = WallpaperExportManager.isOnboardingCompleted(context)
        _uiState.update { it.copy(showOnboardingDialog = !onboardingDone) }
        loadPreset(BuiltInPresets.presets.first())
    }

    fun loadPreset(preset: SampleWallpaperPreset) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzingPhoto = true,
                    selectedPresetId = preset.id,
                    config = preset.recommendedConfig,
                    brushToolMode = BrushToolMode.NONE,
                    showMaskOverlay = false
                )
            }
            val bitmap = segmentationEngine.loadBitmapFromDrawable(
                context = context,
                drawableResId = preset.drawableResId,
                maxDimension = 1280
            )
            processNewBitmap(bitmap, preset.recommendedConfig, autoPositionClock = false)
        }
    }

    fun onGalleryPhotoSelected(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzingPhoto = true,
                    selectedPresetId = null,
                    brushToolMode = BrushToolMode.NONE,
                    showMaskOverlay = false
                )
            }
            val loaded = segmentationEngine.loadBitmapFromUri(context, uri, maxDimension = 1440)
            if (loaded != null) {
                processNewBitmap(
                    bitmap = loaded,
                    config = _uiState.value.config.copy(enableDepthEffect = true),
                    autoPositionClock = true
                )
                _uiState.update {
                    it.copy(snackbarMessage = "AI subject cutout generated! Drag or pinch the clock.")
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAnalyzingPhoto = false,
                        snackbarMessage = "Could not load selected image. Please try another photo."
                    )
                }
            }
        }
    }

    private suspend fun processNewBitmap(
        bitmap: Bitmap,
        config: DepthClockConfig,
        autoPositionClock: Boolean
    ) {
        val rawData = segmentationEngine.analyzePhoto(bitmap)
        currentRawSegmentation = rawData

        val updatedConfig = if (autoPositionClock) {
            config.copy(
                clockNormX = 0.50f,
                clockNormY = rawData.suggestedClockY
            )
        } else {
            config
        }

        val result = segmentationEngine.buildSegmentationResult(
            rawData = rawData,
            mode = updatedConfig.segmentationMode,
            threshold = updatedConfig.maskThreshold,
            featherRadius = updatedConfig.maskFeatherRadius
        )

        _uiState.update {
            it.copy(
                backgroundBitmap = bitmap,
                foregroundCutout = result.foregroundCutout,
                maskPreviewOverlay = result.maskPreviewOverlay,
                config = updatedConfig,
                isAnalyzingPhoto = false,
                subjectCoveragePercent = result.activeCoveragePercent,
                activeEngineLabel = result.engineLabel,
                processingTimeMs = result.processingTimeMs
            )
        }

        WallpaperExportManager.persistLiveWallpaperData(
            context = getApplication(),
            backgroundBitmap = bitmap,
            foregroundCutout = result.foregroundCutout,
            config = updatedConfig
        )
    }

    fun onTransformClock(
        deltaNormX: Float,
        deltaNormY: Float,
        zoomChange: Float
    ) {
        _uiState.update { state ->
            val current = state.config
            var nextX = (current.clockNormX + deltaNormX).coerceIn(0.15f, 0.85f)
            // Gentle magnetic snap to horizontal center
            if (abs(nextX - 0.50f) < 0.012f && abs(deltaNormX) < 0.008f) {
                nextX = 0.50f
            }
            val nextY = (current.clockNormY + deltaNormY).coerceIn(0.10f, 0.78f)
            val nextScale = (current.clockScale * zoomChange).coerceIn(0.50f, 1.95f)

            state.copy(
                config = current.copy(
                    clockNormX = nextX,
                    clockNormY = nextY,
                    clockScale = nextScale
                )
            )
        }
    }

    fun updateConfig(transform: (DepthClockConfig) -> DepthClockConfig) {
        val prevConfig = _uiState.value.config
        val newConfig = transform(prevConfig)
        _uiState.update { it.copy(config = newConfig) }

        val maskChanged = prevConfig.segmentationMode != newConfig.segmentationMode ||
            abs(prevConfig.maskThreshold - newConfig.maskThreshold) > 0.005f ||
            prevConfig.maskFeatherRadius != newConfig.maskFeatherRadius

        if (maskChanged) {
            rebuildCutoutFromCurrentRaw(newConfig)
        } else {
            WallpaperExportManager.saveConfigToPrefs(getApplication(), newConfig)
        }
    }

    private fun rebuildCutoutFromCurrentRaw(config: DepthClockConfig) {
        val raw = currentRawSegmentation ?: return
        maskRebuildJob?.cancel()
        maskRebuildJob = viewModelScope.launch {
            val result = segmentationEngine.buildSegmentationResult(
                rawData = raw,
                mode = config.segmentationMode,
                threshold = config.maskThreshold,
                featherRadius = config.maskFeatherRadius
            )
            _uiState.update {
                it.copy(
                    foregroundCutout = result.foregroundCutout,
                    maskPreviewOverlay = result.maskPreviewOverlay,
                    subjectCoveragePercent = result.activeCoveragePercent,
                    activeEngineLabel = result.engineLabel,
                    processingTimeMs = result.processingTimeMs
                )
            }
            val bg = _uiState.value.backgroundBitmap
            if (bg != null) {
                WallpaperExportManager.persistLiveWallpaperData(
                    context = getApplication(),
                    backgroundBitmap = bg,
                    foregroundCutout = result.foregroundCutout,
                    config = config
                )
            }
        }
    }

    fun onBrushStrokeAtNormalizedPoint(normX: Float, normY: Float) {
        val raw = currentRawSegmentation ?: return
        val state = _uiState.value
        val mode = state.brushToolMode
        if (mode == BrushToolMode.NONE) return

        val restore = (mode == BrushToolMode.RESTORE_SUBJECT)
        val changed = segmentationEngine.applyBrushStroke(
            rawData = raw,
            normX = normX,
            normY = normY,
            normRadius = state.brushRadiusNorm,
            restoreSubject = restore
        )
        if (changed) {
            rebuildCutoutFromCurrentRaw(state.config)
        }
    }

    fun resetBrushEdits() {
        val raw = currentRawSegmentation ?: return
        segmentationEngine.clearUserBrushEdits(raw)
        rebuildCutoutFromCurrentRaw(_uiState.value.config)
        _uiState.update { it.copy(snackbarMessage = "Reset manual mask brush edits") }
    }

    fun setBrushToolMode(mode: BrushToolMode) {
        _uiState.update {
            it.copy(
                brushToolMode = mode,
                showMaskOverlay = (mode != BrushToolMode.NONE) || it.showMaskOverlay
            )
        }
    }

    fun setBrushRadius(radiusNorm: Float) {
        _uiState.update { it.copy(brushRadiusNorm = radiusNorm.coerceIn(0.02f, 0.14f)) }
    }

    fun toggleMaskOverlay(show: Boolean? = null) {
        _uiState.update {
            val next = show ?: !it.showMaskOverlay
            it.copy(showMaskOverlay = next)
        }
    }

    fun selectTab(tab: EditorTab) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                isPanelExpanded = true,
                brushToolMode = if (tab != EditorTab.AI_MASK) BrushToolMode.NONE else it.brushToolMode,
                showMaskOverlay = if (tab != EditorTab.AI_MASK) false else it.showMaskOverlay
            )
        }
    }

    fun togglePanelExpanded() {
        _uiState.update { it.copy(isPanelExpanded = !it.isPanelExpanded) }
    }

    fun toggleImmersiveLockPreview(immersive: Boolean? = null) {
        _uiState.update {
            val next = immersive ?: !it.isImmersiveLockPreview
            it.copy(
                isImmersiveLockPreview = next,
                brushToolMode = BrushToolMode.NONE,
                showMaskOverlay = false
            )
        }
    }

    fun resetClockPosition() {
        val suggestedY = currentRawSegmentation?.suggestedClockY ?: 0.24f
        updateConfig {
            it.copy(
                clockNormX = 0.50f,
                clockNormY = suggestedY,
                clockScale = 1.10f
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Clock snapped to optimal depth position") }
    }

    fun setOnboardingVisible(visible: Boolean) {
        if (!visible) {
            WallpaperExportManager.setOnboardingCompleted(getApplication(), true)
        }
        _uiState.update { it.copy(showOnboardingDialog = visible) }
    }

    fun setWallpaperModalVisible(visible: Boolean) {
        _uiState.update { it.copy(showWallpaperModal = visible) }
    }

    fun saveHighResImageToGallery() {
        val state = _uiState.value
        val bg = state.backgroundBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = WallpaperExportManager.saveHighResToGallery(
                context = getApplication(),
                backgroundBitmap = bg,
                foregroundCutout = state.foregroundCutout,
                config = state.config
            )
            result.fold(
                onSuccess = { uri ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            lastSavedImageUri = uri,
                            snackbarMessage = "Saved HD Depth Clock image to Gallery (Pictures/DepthClock)!"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            snackbarMessage = "Save failed: ${err.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    fun applyStaticWallpaper(target: WallpaperTarget) {
        val state = _uiState.value
        val bg = state.backgroundBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, showWallpaperModal = false) }
            val result = WallpaperExportManager.applySystemWallpaper(
                context = getApplication(),
                backgroundBitmap = bg,
                foregroundCutout = state.foregroundCutout,
                config = state.config,
                target = target
            )
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            snackbarMessage = msg
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            snackbarMessage = "Could not set wallpaper: ${err.localizedMessage ?: "Check permissions"}"
                        )
                    }
                }
            )
        }
    }

    fun prepareLiveWallpaperAndLaunch(onReadyToLaunchIntent: () -> Unit) {
        val state = _uiState.value
        val bg = state.backgroundBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, showWallpaperModal = false) }
            WallpaperExportManager.persistLiveWallpaperData(
                context = getApplication(),
                backgroundBitmap = bg,
                foregroundCutout = state.foregroundCutout,
                config = state.config
            )
            _uiState.update { it.copy(isExporting = false) }
            onReadyToLaunchIntent()
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
