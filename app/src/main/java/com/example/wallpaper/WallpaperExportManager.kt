package com.example.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.ClockColorPreset
import com.example.model.ClockFontStyle
import com.example.model.ClockLayoutStyle
import com.example.model.DepthClockConfig
import com.example.model.SegmentationMode
import com.example.renderer.DepthClockCanvasRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class WallpaperTarget(val displayName: String, val subtitle: String) {
    LOCK_SCREEN("Lock Screen", "Apply depth composition to your lock screen"),
    HOME_SCREEN("Home Screen", "Apply depth composition to your home screen"),
    BOTH_SCREENS("Both Screens", "Apply to both Lock Screen & Home Screen")
}

object WallpaperExportManager {

    private const val PREFS_NAME = "depth_clock_live_prefs"
    private const val FILE_LIVE_BG = "live_depth_bg.jpg"
    private const val FILE_LIVE_FG = "live_depth_fg.png"

    suspend fun saveHighResToGallery(
        context: Context,
        backgroundBitmap: Bitmap,
        foregroundCutout: Bitmap?,
        config: DepthClockConfig
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val metrics = context.resources.displayMetrics
            val outW = max(1440, metrics.widthPixels)
            val aspect = if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
                metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
            } else {
                20f / 9f
            }
            val outH = (outW * aspect.coerceIn(1.6f, 2.3f)).toInt()

            val composedBitmap = DepthClockCanvasRenderer.renderToHighResBitmap(
                context = context,
                backgroundBitmap = backgroundBitmap,
                foregroundCutout = foregroundCutout,
                config = config,
                outputWidth = outW,
                outputHeight = outH
            )

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "DepthClock_$timestamp.png"
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/DepthClock"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, contentValues)
                ?: throw IllegalStateException("Unable to create MediaStore entry")

            resolver.openOutputStream(itemUri)?.use { outputStream ->
                composedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            } ?: throw IllegalStateException("Unable to open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            itemUri
        }
    }

    suspend fun applySystemWallpaper(
        context: Context,
        backgroundBitmap: Bitmap,
        foregroundCutout: Bitmap?,
        config: DepthClockConfig,
        target: WallpaperTarget
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val metrics = context.resources.displayMetrics
            val outW = max(1080, metrics.widthPixels)
            val outH = max(2400, metrics.heightPixels)

            val composedBitmap = DepthClockCanvasRenderer.renderToHighResBitmap(
                context = context,
                backgroundBitmap = backgroundBitmap,
                foregroundCutout = foregroundCutout,
                config = config,
                outputWidth = outW,
                outputHeight = outH
            )

            val wallpaperManager = WallpaperManager.getInstance(context)
            val flags = when (target) {
                WallpaperTarget.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.BOTH_SCREENS -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
            }

            wallpaperManager.setBitmap(composedBitmap, null, true, flags)
            // Also sync to live wallpaper storage so both static and live stay updated
            persistLiveWallpaperData(context, backgroundBitmap, foregroundCutout, config)
            "Applied Depth Clock to ${target.displayName}!"
        }
    }

    suspend fun persistLiveWallpaperData(
        context: Context,
        backgroundBitmap: Bitmap,
        foregroundCutout: Bitmap?,
        config: DepthClockConfig
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val bgFile = File(context.filesDir, FILE_LIVE_BG)
            FileOutputStream(bgFile).use { out ->
                backgroundBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            val fgFile = File(context.filesDir, FILE_LIVE_FG)
            if (foregroundCutout != null) {
                FileOutputStream(fgFile).use { out ->
                    foregroundCutout.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } else if (fgFile.exists()) {
                fgFile.delete()
            }

            saveConfigToPrefs(context, config)
        }
    }

    fun saveConfigToPrefs(context: Context, config: DepthClockConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("clockNormX", config.clockNormX)
            .putFloat("clockNormY", config.clockNormY)
            .putFloat("clockScale", config.clockScale)
            .putString("fontStyle", config.fontStyle.name)
            .putString("layoutStyle", config.layoutStyle.name)
            .putString("colorPreset", config.colorPreset.name)
            .putFloat("clockOpacity", config.clockOpacity)
            .putBoolean("showSeconds", config.showSeconds)
            .putBoolean("use24HourFormat", config.use24HourFormat)
            .putBoolean("showDate", config.showDate)
            .putString("dateFormatPattern", config.dateFormatPattern)
            .putBoolean("enableDepthEffect", config.enableDepthEffect)
            .putFloat("subjectOcclusionStrength", config.subjectOcclusionStrength)
            .putBoolean("enableSubjectRimLight", config.enableSubjectRimLight)
            .putBoolean("enableClockDropShadow", config.enableClockDropShadow)
            .putFloat("backgroundDimAlpha", config.backgroundDimAlpha)
            .putString("segmentationMode", config.segmentationMode.name)
            .putFloat("maskThreshold", config.maskThreshold)
            .putInt("maskFeatherRadius", config.maskFeatherRadius)
            .putString("customLockSignature", config.customLockSignature)
            .putLong("lastUpdatedEpoch", System.currentTimeMillis())
            .apply()
    }

    fun loadConfigFromPrefs(context: Context): DepthClockConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains("clockNormX")) return DepthClockConfig()

        val fontName = prefs.getString("fontStyle", ClockFontStyle.BEBAS_NEUE.name)
        val layoutName = prefs.getString("layoutStyle", ClockLayoutStyle.IOS_CLASSIC.name)
        val colorName = prefs.getString("colorPreset", ClockColorPreset.PURE_FROST.name)
        val segName = prefs.getString("segmentationMode", SegmentationMode.AUTO_HYBRID.name)

        return DepthClockConfig(
            clockNormX = prefs.getFloat("clockNormX", 0.50f),
            clockNormY = prefs.getFloat("clockNormY", 0.24f),
            clockScale = prefs.getFloat("clockScale", 1.08f),
            fontStyle = runCatching { ClockFontStyle.valueOf(fontName!!) }.getOrDefault(ClockFontStyle.BEBAS_NEUE),
            layoutStyle = runCatching { ClockLayoutStyle.valueOf(layoutName!!) }.getOrDefault(ClockLayoutStyle.IOS_CLASSIC),
            colorPreset = runCatching { ClockColorPreset.valueOf(colorName!!) }.getOrDefault(ClockColorPreset.PURE_FROST),
            clockOpacity = prefs.getFloat("clockOpacity", 0.96f),
            showSeconds = prefs.getBoolean("showSeconds", true),
            use24HourFormat = prefs.getBoolean("use24HourFormat", false),
            showDate = prefs.getBoolean("showDate", true),
            dateFormatPattern = prefs.getString("dateFormatPattern", "EEE, MMM d") ?: "EEE, MMM d",
            enableDepthEffect = prefs.getBoolean("enableDepthEffect", true),
            subjectOcclusionStrength = prefs.getFloat("subjectOcclusionStrength", 1.0f),
            enableSubjectRimLight = prefs.getBoolean("enableSubjectRimLight", true),
            enableClockDropShadow = prefs.getBoolean("enableClockDropShadow", true),
            backgroundDimAlpha = prefs.getFloat("backgroundDimAlpha", 0.12f),
            segmentationMode = runCatching { SegmentationMode.valueOf(segName!!) }.getOrDefault(SegmentationMode.AUTO_HYBRID),
            maskThreshold = prefs.getFloat("maskThreshold", 0.42f),
            maskFeatherRadius = prefs.getInt("maskFeatherRadius", 2),
            customLockSignature = prefs.getString("customLockSignature", "DEPTH STUDIO") ?: "DEPTH STUDIO"
        )
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    fun loadPersistedLiveBitmaps(context: Context): Pair<Bitmap?, Bitmap?> {
        val bgFile = File(context.filesDir, FILE_LIVE_BG)
        val fgFile = File(context.filesDir, FILE_LIVE_FG)
        val bg = if (bgFile.exists()) BitmapFactory.decodeFile(bgFile.absolutePath) else null
        val fg = if (fgFile.exists()) BitmapFactory.decodeFile(fgFile.absolutePath) else null
        return bg to fg
    }

    fun createLiveWallpaperIntent(context: Context): Intent {
        return Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, DepthClockWallpaperService::class.java)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
