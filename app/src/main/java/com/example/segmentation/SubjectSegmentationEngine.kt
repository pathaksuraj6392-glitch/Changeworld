package com.example.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.model.SegmentationMode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class RawSegmentationData(
    val sourceBitmap: Bitmap,
    val maskWidth: Int,
    val maskHeight: Int,
    val mlKitMask: FloatArray,
    val salienceMask: FloatArray,
    val userBrushDelta: FloatArray,
    val mlKitCoverage: Float,
    val suggestedClockY: Float
)

data class ProcessedSegmentationResult(
    val foregroundCutout: Bitmap,
    val maskPreviewOverlay: Bitmap,
    val activeCoveragePercent: Int,
    val engineLabel: String,
    val processingTimeMs: Long
)

class SubjectSegmentationEngine {

    private val segmenterOptions = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .build()

    private val segmenter: Segmenter by lazy {
        Segmentation.getClient(segmenterOptions)
    }

    suspend fun loadBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1440
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return@withContext null

            var sampleSize = 1
            while (
                boundsOptions.outWidth / sampleSize > maxDimension * 1.5 ||
                boundsOptions.outHeight / sampleSize > maxDimension * 1.5
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val oriented = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }

            scaleDownIfNeeded(oriented, maxDimension)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadBitmapFromDrawable(
        context: Context,
        drawableResId: Int,
        maxDimension: Int = 1440
    ): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeResource(context.resources, drawableResId, options)
        scaleDownIfNeeded(decoded, maxDimension)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = max(w, h)
        if (longest <= maxDimension) {
            return if (bitmap.config == Bitmap.Config.ARGB_8888) {
                bitmap
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
        val scale = maxDimension.toFloat() / longest.toFloat()
        val targetW = max(1, (w * scale).toInt())
        val targetH = max(1, (h * scale).toInt())
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    /**
     * Runs on-device Google ML Kit Selfie Segmentation AND Edge-Guided Subject Salience
     * at a working mask resolution so post-processing (threshold/feather/brush) is instantaneous.
     */
    suspend fun analyzePhoto(sourceBitmap: Bitmap): RawSegmentationData = withContext(Dispatchers.Default) {
        val workMaxDim = 360
        val ratio = min(1f, workMaxDim.toFloat() / max(sourceBitmap.width, sourceBitmap.height).toFloat())
        val maskW = max(64, (sourceBitmap.width * ratio).toInt())
        val maskH = max(64, (sourceBitmap.height * ratio).toInt())

        val workBitmap = Bitmap.createScaledBitmap(sourceBitmap, maskW, maskH, true)
        val mlKitMask = FloatArray(maskW * maskH)
        var mlKitStrongPixels = 0

        // 1. Run Google ML Kit Selfie Segmentation (On-Device Bundled Neural Network)
        try {
            val inputImage = InputImage.fromBitmap(workBitmap, 0)
            val resultMask = segmenter.process(inputImage).await()
            val buffer = resultMask.buffer
            buffer.rewind()
            buffer.order(ByteOrder.nativeOrder())
            val floatBuffer = buffer.asFloatBuffer()
            val mW = resultMask.width
            val mH = resultMask.height

            if (mW == maskW && mH == maskH) {
                for (i in 0 until maskW * maskH) {
                    val v = floatBuffer.get(i).coerceIn(0f, 1f)
                    mlKitMask[i] = v
                    if (v > 0.45f) mlKitStrongPixels++
                }
            } else {
                val raw = FloatArray(mW * mH)
                floatBuffer.get(raw)
                for (y in 0 until maskH) {
                    val sy = (y.toFloat() / maskH * (mH - 1)).toInt().coerceIn(0, mH - 1)
                    for (x in 0 until maskW) {
                        val sx = (x.toFloat() / maskW * (mW - 1)).toInt().coerceIn(0, mW - 1)
                        val v = raw[sy * mW + sx].coerceIn(0f, 1f)
                        mlKitMask[y * maskW + x] = v
                        if (v > 0.45f) mlKitStrongPixels++
                    }
                }
            }
        } catch (_: Throwable) {
            // Fallback gracefully if ML Kit native runtime is unavailable in JVM test environments
        }

        val mlKitCoverage = mlKitStrongPixels.toFloat() / (maskW * maskH).toFloat()

        // 2. Compute Edge-Guided Subject & Peak Salience Mask (handles helmets, mountains, objects, and refines portrait edges)
        val salienceMask = computeEdgeGuidedSalienceMask(workBitmap, maskW, maskH)

        // 3. Find the top-most subject crown Y position so the clock can sit right behind the subject's head/peak
        val combinedPreview = FloatArray(maskW * maskH)
        val useMlKitPrimary = mlKitCoverage >= 0.04f
        var topSubjectY = (maskH * 0.22f).toInt()
        var foundCrown = false

        for (y in (maskH * 0.06f).toInt() until (maskH * 0.85f).toInt()) {
            var rowSubjectCount = 0
            for (x in (maskW * 0.18f).toInt() until (maskW * 0.82f).toInt()) {
                val idx = y * maskW + x
                val v = if (useMlKitPrimary) {
                    max(mlKitMask[idx], salienceMask[idx] * 0.72f)
                } else {
                    salienceMask[idx]
                }
                combinedPreview[idx] = v
                if (v > 0.48f) rowSubjectCount++
            }
            if (!foundCrown && rowSubjectCount > maskW * 0.07f) {
                topSubjectY = y
                foundCrown = true
            }
        }

        val suggestedY = (topSubjectY.toFloat() / maskH.toFloat() + 0.05f).coerceIn(0.16f, 0.36f)

        RawSegmentationData(
            sourceBitmap = sourceBitmap,
            maskWidth = maskW,
            maskHeight = maskH,
            mlKitMask = mlKitMask,
            salienceMask = salienceMask,
            userBrushDelta = FloatArray(maskW * maskH),
            mlKitCoverage = mlKitCoverage,
            suggestedClockY = suggestedY
        )
    }

    /**
     * Multi-cue background modeling + edge-aware foreground salience extraction.
     * Samples the upper sky/backdrop perimeter to model background clusters and extracts
     * central/foreground subjects with crisp edge preservation.
     */
    private fun computeEdgeGuidedSalienceMask(
        bitmap: Bitmap,
        w: Int,
        h: Int
    ): FloatArray {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val rArr = FloatArray(w * h)
        val gArr = FloatArray(w * h)
        val bArr = FloatArray(w * h)
        val lumArr = FloatArray(w * h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = ((c shr 16) and 0xFF).toFloat()
            val g = ((c shr 8) and 0xFF).toFloat()
            val b = (c and 0xFF).toFloat()
            rArr[i] = r
            gArr[i] = g
            bArr[i] = b
            lumArr[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Sample background clusters along the top margin and upper-left/right borders
        data class ColorCluster(val r: Float, val g: Float, val b: Float, val lum: Float)
        val bgSamples = ArrayList<ColorCluster>(64)

        val topMarginH = max(4, (h * 0.10f).toInt())
        val stepX = max(2, w / 16)
        val stepY = max(2, topMarginH / 3)
        for (y in 1 until topMarginH step stepY) {
            for (x in 1 until w - 1 step stepX) {
                val idx = y * w + x
                bgSamples.add(ColorCluster(rArr[idx], gArr[idx], bArr[idx], lumArr[idx]))
            }
        }
        // Also sample upper left and upper right edges (top 45% of image)
        val sideMaxY = (h * 0.45f).toInt()
        val sideStepY = max(3, sideMaxY / 10)
        for (y in topMarginH until sideMaxY step sideStepY) {
            val leftIdx = y * w + 2
            val rightIdx = y * w + (w - 3)
            bgSamples.add(ColorCluster(rArr[leftIdx], gArr[leftIdx], bArr[leftIdx], lumArr[leftIdx]))
            bgSamples.add(ColorCluster(rArr[rightIdx], gArr[rightIdx], bArr[rightIdx], lumArr[rightIdx]))
        }

        // Compute Sobel gradient magnitude for edge awareness
        val edgeMag = FloatArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                val gx = -lumArr[idx - w - 1] + lumArr[idx - w + 1] -
                    2f * lumArr[idx - 1] + 2f * lumArr[idx + 1] -
                    lumArr[idx + w - 1] + lumArr[idx + w + 1]
                val gy = -lumArr[idx - w - 1] - 2f * lumArr[idx - w] - lumArr[idx - w + 1] +
                    lumArr[idx + w - 1] + 2f * lumArr[idx + w] + lumArr[idx + w + 1]
                edgeMag[idx] = (sqrt(gx * gx + gy * gy) / 255f).coerceIn(0f, 1f)
            }
        }

        val rawSalience = FloatArray(w * h)
        for (y in 0 until h) {
            val normY = y.toFloat() / h.toFloat()
            for (x in 0 until w) {
                val normX = x.toFloat() / w.toFloat()
                val idx = y * w + x
                val r = rArr[idx]
                val g = gArr[idx]
                val b = bArr[idx]
                val lum = lumArr[idx]

                // Find minimum perceptual color distance to background border samples
                var minBgDist = Float.MAX_VALUE
                for (s in bgSamples) {
                    val dr = r - s.r
                    val dg = g - s.g
                    val db = b - s.b
                    val dLum = abs(lum - s.lum)
                    val dist = sqrt(dr * dr * 0.30f + dg * dg * 0.59f + db * db * 0.11f) + dLum * 0.35f
                    if (dist < minBgDist) {
                        minBgDist = dist
                    }
                }

                // Normalize color contrast against background (typical threshold ~26..75)
                val colorContrast = ((minBgDist - 18f) / 52f).coerceIn(0f, 1f)

                // Spatial prior: subjects in portrait wallpapers occupy the central column and lower torso
                val dx = (normX - 0.5f) / 0.42f
                val horizontalPrior = exp(-dx * dx * 1.35f)
                val verticalPrior = when {
                    normY < 0.05f -> 0.10f
                    normY < 0.15f -> 0.45f + (normY - 0.05f) * 4.5f
                    else -> 0.92f + (normY - 0.15f) * 0.12f
                }.coerceIn(0f, 1f)

                val spatialWeight = (horizontalPrior * 0.72f + 0.28f) * verticalPrior
                val edgeBoost = edgeMag[idx] * 0.22f

                val score = ((colorContrast * 0.82f + edgeBoost) * spatialWeight).coerceIn(0f, 1f)
                rawSalience[idx] = score
            }
        }

        // Smooth and fill internal holes inside the subject silhouette
        return smoothMaskFloat(rawSalience, w, h, radius = 2)
    }

    /**
     * Generates the full-resolution ARGB_8888 Foreground Subject Cutout Bitmap
     * and the cyan/coral X-Ray Mask Preview overlay from the raw segmentation data.
     */
    suspend fun buildSegmentationResult(
        rawData: RawSegmentationData,
        mode: SegmentationMode,
        threshold: Float,
        featherRadius: Int
    ): ProcessedSegmentationResult = withContext(Dispatchers.Default) {
        val startMs = System.currentTimeMillis()
        val maskW = rawData.maskWidth
        val maskH = rawData.maskHeight
        val count = maskW * maskH

        val fusedSmallMask = FloatArray(count)
        val useMlKitInHybrid = rawData.mlKitCoverage >= 0.035f

        val activeEngineName = when (mode) {
            SegmentationMode.MLKIT_PORTRAIT ->
                if (rawData.mlKitCoverage >= 0.01f) "ML Kit Neural Portrait" else "ML Kit + Smart Fallback"
            SegmentationMode.OBJECT_CONTRAST -> "Edge & Contrast Salience"
            SegmentationMode.AUTO_HYBRID ->
                if (useMlKitInHybrid) "ML Kit Neural + Edge Hybrid" else "AI Salience + Edge Hybrid"
        }

        for (i in 0 until count) {
            val baseConfidence = when (mode) {
                SegmentationMode.MLKIT_PORTRAIT -> {
                    if (rawData.mlKitCoverage >= 0.01f) {
                        rawData.mlKitMask[i]
                    } else {
                        rawData.salienceMask[i]
                    }
                }
                SegmentationMode.OBJECT_CONTRAST -> rawData.salienceMask[i]
                SegmentationMode.AUTO_HYBRID -> {
                    if (useMlKitInHybrid) {
                        // Combine ML Kit portrait mask with edge salience for hats/accessories/shoulders
                        max(rawData.mlKitMask[i], rawData.salienceMask[i] * 0.68f)
                    } else {
                        max(rawData.salienceMask[i], rawData.mlKitMask[i])
                    }
                }
            }
            val withBrush = (baseConfidence + rawData.userBrushDelta[i]).coerceIn(0f, 1f)

            // Soft sigmoid-like ramp around the user's threshold for anti-aliased edges
            val softBand = 0.14f
            val alpha = ((withBrush - (threshold - softBand * 0.5f)) / softBand).coerceIn(0f, 1f)
            fusedSmallMask[i] = alpha
        }

        val featheredMask = if (featherRadius > 0) {
            smoothMaskFloat(fusedSmallMask, maskW, maskH, featherRadius.coerceIn(1, 6))
        } else {
            fusedSmallMask
        }

        var foregroundCount = 0
        for (i in 0 until count) {
            if (featheredMask[i] > 0.5f) foregroundCount++
        }
        val coveragePercent = ((foregroundCount.toFloat() / count.toFloat()) * 100f).toInt().coerceIn(0, 100)

        // Create low-res mask bitmap and bilinearly upscale to sourceBitmap resolution for silky-smooth edges
        val smallAlphaPixels = IntArray(count)
        val previewPixels = IntArray(count)

        for (i in 0 until count) {
            val a = (featheredMask[i] * 255f).toInt().coerceIn(0, 255)
            smallAlphaPixels[i] = (a shl 24) or 0x00FFFFFF

            // Cyan-coral highlight for X-Ray Mask Inspection overlay
            val overlayAlpha = (a * 0.58f).toInt()
            previewPixels[i] = (overlayAlpha shl 24) or (0x00 shl 16) or (0xE5 shl 8) or 0xFF
        }

        val smallAlphaBitmap = Bitmap.createBitmap(smallAlphaPixels, maskW, maskH, Bitmap.Config.ARGB_8888)
        val maskPreviewBitmap = Bitmap.createBitmap(previewPixels, maskW, maskH, Bitmap.Config.ARGB_8888)

        val srcW = rawData.sourceBitmap.width
        val srcH = rawData.sourceBitmap.height
        val upscaledAlphaBitmap = Bitmap.createScaledBitmap(smallAlphaBitmap, srcW, srcH, true)

        val srcPixels = IntArray(srcW * srcH)
        val alphaPixels = IntArray(srcW * srcH)
        rawData.sourceBitmap.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH)
        upscaledAlphaBitmap.getPixels(alphaPixels, 0, srcW, 0, 0, srcW, srcH)

        for (i in srcPixels.indices) {
            val maskAlpha = (alphaPixels[i] ushr 24) and 0xFF
            val rgb = srcPixels[i] and 0x00FFFFFF
            srcPixels[i] = (maskAlpha shl 24) or rgb
        }

        val foregroundCutout = Bitmap.createBitmap(srcPixels, srcW, srcH, Bitmap.Config.ARGB_8888)
        val elapsed = System.currentTimeMillis() - startMs

        ProcessedSegmentationResult(
            foregroundCutout = foregroundCutout,
            maskPreviewOverlay = maskPreviewBitmap,
            activeCoveragePercent = coveragePercent,
            engineLabel = activeEngineName,
            processingTimeMs = max(1L, elapsed)
        )
    }

    /**
     * Applies a soft circular brush stroke (Restore Subject = +1, Erase Mask = -1)
     * in normalized image coordinates (0f..1f).
     */
    fun applyBrushStroke(
        rawData: RawSegmentationData,
        normX: Float,
        normY: Float,
        normRadius: Float,
        restoreSubject: Boolean
    ): Boolean {
        val w = rawData.maskWidth
        val h = rawData.maskHeight
        val cx = (normX * w).toInt()
        val cy = (normY * h).toInt()
        val rPx = max(3, (normRadius * max(w, h)).toInt())
        val rSq = (rPx * rPx).toFloat()

        val minX = max(0, cx - rPx)
        val maxX = min(w - 1, cx + rPx)
        val minY = max(0, cy - rPx)
        val maxY = min(h - 1, cy + rPx)

        val direction = if (restoreSubject) 0.45f else -0.45f
        var modified = false

        for (y in minY..maxY) {
            val dy = (y - cy).toFloat()
            for (x in minX..maxX) {
                val dx = (x - cx).toFloat()
                val dSq = dx * dx + dy * dy
                if (dSq <= rSq) {
                    val falloff = 1f - (dSq / rSq)
                    val idx = y * w + x
                    val updated = (rawData.userBrushDelta[idx] + direction * falloff).coerceIn(-1f, 1f)
                    if (updated != rawData.userBrushDelta[idx]) {
                        rawData.userBrushDelta[idx] = updated
                        modified = true
                    }
                }
            }
        }
        return modified
    }

    fun clearUserBrushEdits(rawData: RawSegmentationData) {
        rawData.userBrushDelta.fill(0f)
    }

    private fun smoothMaskFloat(
        input: FloatArray,
        w: Int,
        h: Int,
        radius: Int
    ): FloatArray {
        val temp = FloatArray(w * h)
        val output = FloatArray(w * h)
        val window = radius * 2 + 1

        // Horizontal pass
        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                var sum = 0f
                for (k in -radius..radius) {
                    val sx = (x + k).coerceIn(0, w - 1)
                    sum += input[rowOffset + sx]
                }
                temp[rowOffset + x] = sum / window
            }
        }

        // Vertical pass
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                for (k in -radius..radius) {
                    val sy = (y + k).coerceIn(0, h - 1)
                    sum += temp[sy * w + x]
                }
                output[y * w + x] = sum / window
            }
        }
        return output
    }
}
