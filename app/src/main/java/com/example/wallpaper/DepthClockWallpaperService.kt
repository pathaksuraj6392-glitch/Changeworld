package com.example.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.R
import com.example.model.DepthClockConfig
import com.example.renderer.DepthClockCanvasRenderer

/**
 * Real-time Android Live Wallpaper Service that renders the ticking Depth Clock
 * BEHIND the segmented foreground subject directly on the user's home/lock screen.
 */
class DepthClockWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return DepthClockEngine()
    }

    inner class DepthClockEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private var backgroundBitmap: Bitmap? = null
        private var foregroundCutout: Bitmap? = null
        private var config: DepthClockConfig = DepthClockConfig()
        private var lastLoadedEpoch = 0L

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisible) {
                    val delayMs = if (config.showSeconds) 1000L else 10_000L
                    handler.postDelayed(this, delayMs)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            reloadAssetsIfNeeded(force = true)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                reloadAssetsIfNeeded(force = false)
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            isVisible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
        }

        private fun reloadAssetsIfNeeded(force: Boolean) {
            val prefs = applicationContext.getSharedPreferences("depth_clock_live_prefs", MODE_PRIVATE)
            val updatedEpoch = prefs.getLong("lastUpdatedEpoch", 0L)
            if (!force && backgroundBitmap != null && updatedEpoch == lastLoadedEpoch) {
                return
            }
            lastLoadedEpoch = updatedEpoch
            config = WallpaperExportManager.loadConfigFromPrefs(applicationContext)
            val (persistedBg, persistedFg) = WallpaperExportManager.loadPersistedLiveBitmaps(applicationContext)
            if (persistedBg != null) {
                backgroundBitmap = persistedBg
                foregroundCutout = persistedFg
            } else if (backgroundBitmap == null) {
                backgroundBitmap = BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.img_sample_cyber_portrait
                )
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            val bg = backgroundBitmap ?: return
            var canvas = try {
                if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            } catch (_: Exception) {
                try {
                    holder.lockCanvas()
                } catch (_: Exception) {
                    null
                }
            } ?: return

            try {
                DepthClockCanvasRenderer.renderDepthComposition(
                    context = applicationContext,
                    canvas = canvas,
                    canvasWidth = canvas.width.toFloat(),
                    canvasHeight = canvas.height.toFloat(),
                    backgroundBitmap = bg,
                    foregroundCutout = foregroundCutout,
                    maskPreviewOverlay = null,
                    config = config,
                    timeMillis = System.currentTimeMillis(),
                    showMaskOverlay = false,
                    showAlignmentGuides = false
                )
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {
                }
            }
        }
    }
}
