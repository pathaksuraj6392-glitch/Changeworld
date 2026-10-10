package com.example.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.ClockFontStyle
import com.example.model.ClockLayoutStyle
import com.example.model.DepthClockConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object DepthClockCanvasRenderer {

    private val typefaceCache = mutableMapOf<Int, Typeface>()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clockFillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val clockStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val occlusionShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun getTypeface(context: Context, fontStyle: ClockFontStyle): Typeface {
        return typefaceCache.getOrPut(fontStyle.fontResId) {
            try {
                ResourcesCompat.getFont(context, fontStyle.fontResId) ?: Typeface.DEFAULT_BOLD
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun getUiTypeface(context: Context): Typeface {
        return typefaceCache.getOrPut(R.font.plus_jakarta_sans) {
            try {
                ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.DEFAULT_BOLD
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
        }
    }

    /**
     * Computes the destination RectF that center-crops (ContentScale.Crop) a source bitmap
     * of (srcW, srcH) into a canvas of (dstW, dstH).
     */
    fun computeCenterCropRect(
        srcW: Int,
        srcH: Int,
        dstW: Float,
        dstH: Float
    ): RectF {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0f || dstH <= 0f) {
            return RectF(0f, 0f, dstW, dstH)
        }
        val scale = max(dstW / srcW.toFloat(), dstH / srcH.toFloat())
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val left = (dstW - scaledW) * 0.5f
        val top = (dstH - scaledH) * 0.5f
        return RectF(left, top, left + scaledW, top + scaledH)
    }

    /**
     * Converts a screen touch point (touchX, touchY) on a canvas of (dstW, dstH)
     * into normalized bitmap coordinates (0f..1f) inside the center-cropped image.
     */
    fun screenToNormalizedBitmapCoords(
        touchX: Float,
        touchY: Float,
        srcW: Int,
        srcH: Int,
        dstW: Float,
        dstH: Float
    ): Pair<Float, Float> {
        val cropRect = computeCenterCropRect(srcW, srcH, dstW, dstH)
        val normX = ((touchX - cropRect.left) / cropRect.width()).coerceIn(0f, 1f)
        val normY = ((touchY - cropRect.top) / cropRect.height()).coerceIn(0f, 1f)
        return normX to normY
    }

    /**
     * Renders the complete 3-Layer Depth Clock composition onto [canvas]:
     * Layer 1: Background Photo + Ambient Scrim
     * Layer 2: Customizable Digital Clock + Date + Seconds (BEHIND Subject)
     * Layer 3: AI-Segmented Subject Cutout (IN FRONT OF Clock)
     */
    fun renderDepthComposition(
        context: Context,
        canvas: Canvas,
        canvasWidth: Float,
        canvasHeight: Float,
        backgroundBitmap: Bitmap,
        foregroundCutout: Bitmap?,
        maskPreviewOverlay: Bitmap?,
        config: DepthClockConfig,
        timeMillis: Long = System.currentTimeMillis(),
        showMaskOverlay: Boolean = false,
        showAlignmentGuides: Boolean = false
    ) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return

        val dstRect = computeCenterCropRect(
            backgroundBitmap.width,
            backgroundBitmap.height,
            canvasWidth,
            canvasHeight
        )

        // =========================================================================
        // LAYER 1: Full Background Photo + Subtle Atmospheric Depth Scrim
        // =========================================================================
        bitmapPaint.alpha = 255
        canvas.drawBitmap(backgroundBitmap, null, dstRect, bitmapPaint)

        if (config.backgroundDimAlpha > 0.01f) {
            dimPaint.color = Color.argb(
                (config.backgroundDimAlpha * 255f).toInt().coerceIn(0, 220),
                4,
                7,
                15
            )
            canvas.drawRect(0f, 0f, canvasWidth, canvasHeight, dimPaint)
        }

        // Subtle top-to-middle atmospheric gradient so lockscreen clock is always legible
        scrimPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            canvasHeight * 0.48f,
            intArrayOf(
                Color.argb(96, 5, 8, 18),
                Color.argb(38, 5, 8, 18),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, canvasWidth, canvasHeight * 0.48f, scrimPaint)

        // =========================================================================
        // LAYER 2: Digital Clock + Date (Placed BEHIND the Subject when Depth is ON)
        // =========================================================================
        if (config.enableDepthEffect) {
            drawClockLayer(
                context = context,
                canvas = canvas,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                config = config,
                timeMillis = timeMillis
            )
        }

        // =========================================================================
        // LAYER 3: AI-Segmented Foreground Subject Cutout (Occludes the Clock!)
        // =========================================================================
        if (foregroundCutout != null && config.enableDepthEffect && config.subjectOcclusionStrength > 0.02f) {
            // Optional subtle 3D rim/drop shadow behind the subject edge onto the clock
            if (config.enableSubjectRimLight) {
                occlusionShadowPaint.alpha = (config.subjectOcclusionStrength * 95f).toInt().coerceIn(0, 140)
                val shadowOffset = max(2f, canvasWidth * 0.004f)
                val shadowRect = RectF(
                    dstRect.left,
                    dstRect.top - shadowOffset,
                    dstRect.right,
                    dstRect.bottom - shadowOffset
                )
                canvas.drawBitmap(foregroundCutout, null, shadowRect, occlusionShadowPaint)
            }

            bitmapPaint.alpha = (config.subjectOcclusionStrength * 255f).toInt().coerceIn(0, 255)
            canvas.drawBitmap(foregroundCutout, null, dstRect, bitmapPaint)
            bitmapPaint.alpha = 255
        }

        // If Depth Effect is toggled OFF by user for comparison, draw the clock in front
        if (!config.enableDepthEffect) {
            drawClockLayer(
                context = context,
                canvas = canvas,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                config = config,
                timeMillis = timeMillis
            )
        }

        // =========================================================================
        // LAYER 4: Optional X-Ray Subject Mask Overlay & Interactive Guides
        // =========================================================================
        if (showMaskOverlay && maskPreviewOverlay != null) {
            bitmapPaint.alpha = 210
            canvas.drawBitmap(maskPreviewOverlay, null, dstRect, bitmapPaint)
            bitmapPaint.alpha = 255
        }

        if (showAlignmentGuides) {
            drawAlignmentGuides(canvas, canvasWidth, canvasHeight, config)
        }
    }

    private fun drawClockLayer(
        context: Context,
        canvas: Canvas,
        canvasWidth: Float,
        canvasHeight: Float,
        config: DepthClockConfig,
        timeMillis: Long
    ) {
        val date = Date(timeMillis)
        val hourPattern = if (config.use24HourFormat) "HH" else "hh"
        val hoursStr = SimpleDateFormat(hourPattern, Locale.getDefault()).format(date)
        val minutesStr = SimpleDateFormat("mm", Locale.getDefault()).format(date)
        val secondsStr = SimpleDateFormat("ss", Locale.getDefault()).format(date)
        val secondsInt = secondsStr.toIntOrNull() ?: 0
        val dateStr = try {
            SimpleDateFormat(config.dateFormatPattern, Locale.getDefault())
                .format(date)
                .uppercase(Locale.getDefault())
        } catch (_: Exception) {
            SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                .format(date)
                .uppercase(Locale.getDefault())
        }

        val clockTypeface = getTypeface(context, config.fontStyle)
        val uiTypeface = getUiTypeface(context)

        val centerX = config.clockNormX.coerceIn(0.12f, 0.88f) * canvasWidth
        val centerY = config.clockNormY.coerceIn(0.10f, 0.82f) * canvasHeight

        val baseSize = min(canvasWidth, canvasHeight) * 0.285f
        val fontSizePx = (baseSize * config.clockScale.coerceIn(0.45f, 2.25f) * config.fontStyle.sizeMultiplier)

        val alphaInt = (config.clockOpacity.coerceIn(0.15f, 1f) * 255f).toInt()
        val primaryColor = applyAlpha(config.colorPreset.primaryColorArgb, alphaInt)
        val secondaryColor = applyAlpha(config.colorPreset.secondaryColorArgb, alphaInt)
        val accentColor = applyAlpha(config.colorPreset.accentColorArgb, alphaInt)

        // Setup main clock paint
        clockFillPaint.reset()
        clockFillPaint.isAntiAlias = true
        clockFillPaint.isSubpixelText = true
        clockFillPaint.typeface = clockTypeface
        clockFillPaint.textSize = fontSizePx
        clockFillPaint.letterSpacing = config.fontStyle.letterSpacingEm

        if (config.enableClockDropShadow) {
            clockFillPaint.setShadowLayer(
                fontSizePx * 0.12f,
                0f,
                fontSizePx * 0.04f,
                Color.argb((alphaInt * 0.55f).toInt(), 0, 0, 0)
            )
        } else {
            clockFillPaint.clearShadowLayer()
        }

        // Setup date paint
        datePaint.reset()
        datePaint.isAntiAlias = true
        datePaint.isSubpixelText = true
        datePaint.typeface = uiTypeface
        datePaint.color = primaryColor
        datePaint.setShadowLayer(
            12f,
            0f,
            4f,
            Color.argb((alphaInt * 0.65f).toInt(), 0, 0, 0)
        )

        when (config.layoutStyle) {
            ClockLayoutStyle.IOS_CLASSIC -> {
                drawIosClassicClock(
                    canvas = canvas,
                    centerX = centerX,
                    centerY = centerY,
                    fontSizePx = fontSizePx,
                    hoursStr = hoursStr,
                    minutesStr = minutesStr,
                    secondsStr = secondsStr,
                    dateStr = dateStr,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    alphaInt = alphaInt,
                    config = config
                )
            }

            ClockLayoutStyle.HYPER_MAGAZINE -> {
                drawHyperMagazineClock(
                    canvas = canvas,
                    centerX = centerX,
                    centerY = centerY,
                    fontSizePx = fontSizePx,
                    hoursStr = hoursStr,
                    minutesStr = minutesStr,
                    secondsStr = secondsStr,
                    dateStr = dateStr,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    alphaInt = alphaInt,
                    config = config
                )
            }

            ClockLayoutStyle.STACKED_VERTICAL -> {
                drawStackedVerticalClock(
                    canvas = canvas,
                    centerX = centerX,
                    centerY = centerY,
                    fontSizePx = fontSizePx,
                    hoursStr = hoursStr,
                    minutesStr = minutesStr,
                    secondsStr = secondsStr,
                    dateStr = dateStr,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    alphaInt = alphaInt,
                    config = config
                )
            }

            ClockLayoutStyle.HOLLOW_GLASS -> {
                drawHollowGlassClock(
                    canvas = canvas,
                    centerX = centerX,
                    centerY = centerY,
                    fontSizePx = fontSizePx,
                    hoursStr = hoursStr,
                    minutesStr = minutesStr,
                    secondsStr = secondsStr,
                    dateStr = dateStr,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    alphaInt = alphaInt,
                    config = config
                )
            }

            ClockLayoutStyle.CHRONO_MINIMAL -> {
                drawChronoMinimalClock(
                    canvas = canvas,
                    centerX = centerX,
                    centerY = centerY,
                    fontSizePx = fontSizePx,
                    hoursStr = hoursStr,
                    minutesStr = minutesStr,
                    secondsStr = secondsStr,
                    secondsInt = secondsInt,
                    dateStr = dateStr,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    alphaInt = alphaInt,
                    config = config
                )
            }
        }
    }

    private fun drawIosClassicClock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        fontSizePx: Float,
        hoursStr: String,
        minutesStr: String,
        secondsStr: String,
        dateStr: String,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        alphaInt: Int,
        config: DepthClockConfig
    ) {
        val mainTimeText = "$hoursStr:$minutesStr"
        clockFillPaint.textAlign = Paint.Align.CENTER

        val textBounds = Rect()
        clockFillPaint.getTextBounds(mainTimeText, 0, mainTimeText.length, textBounds)
        val textHeight = textBounds.height().toFloat()
        val textWidth = clockFillPaint.measureText(mainTimeText)

        val baselineY = centerY + textHeight * 0.36f
        val topY = baselineY - textHeight

        // Date above the clock
        if (config.showDate) {
            datePaint.textSize = (fontSizePx * 0.145f).coerceIn(22f, 56f)
            datePaint.textAlign = Paint.Align.CENTER
            datePaint.letterSpacing = 0.14f
            datePaint.color = primaryColor
            val dateY = topY - fontSizePx * 0.16f
            canvas.drawText(dateStr, centerX, dateY, datePaint)
        }

        // Gradient shader on the giant clock numerals
        clockFillPaint.shader = LinearGradient(
            centerX,
            topY,
            centerX,
            baselineY,
            primaryColor,
            secondaryColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawText(mainTimeText, centerX, baselineY, clockFillPaint)

        // Optional live seconds pill badge tucked cleanly on the right edge or below
        if (config.showSeconds) {
            val secFontSize = (fontSizePx * 0.19f).coerceIn(24f, 70f)
            datePaint.textSize = secFontSize
            datePaint.letterSpacing = 0.06f
            datePaint.textAlign = Paint.Align.CENTER
            val secLabel = ":$secondsStr"
            val secW = datePaint.measureText(secLabel) + secFontSize * 0.9f
            val secH = secFontSize * 1.45f

            val pillCenterX = (centerX + textWidth * 0.5f - secW * 0.35f)
            val pillTop = baselineY + fontSizePx * 0.08f
            val pillRect = RectF(
                pillCenterX - secW * 0.5f,
                pillTop,
                pillCenterX + secW * 0.5f,
                pillTop + secH
            )

            pillBgPaint.color = Color.argb((alphaInt * 0.32f).toInt(), 8, 14, 28)
            canvas.drawRoundRect(pillRect, secH * 0.5f, secH * 0.5f, pillBgPaint)

            pillStrokePaint.style = Paint.Style.STROKE
            pillStrokePaint.strokeWidth = max(2f, secFontSize * 0.06f)
            pillStrokePaint.color = applyAlpha(accentColor, (alphaInt * 0.7f).toInt())
            canvas.drawRoundRect(pillRect, secH * 0.5f, secH * 0.5f, pillStrokePaint)

            datePaint.color = accentColor
            canvas.drawText(
                secLabel,
                pillRect.centerX(),
                pillRect.centerY() + secFontSize * 0.35f,
                datePaint
            )
        }
    }

    private fun drawHyperMagazineClock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        fontSizePx: Float,
        hoursStr: String,
        minutesStr: String,
        secondsStr: String,
        dateStr: String,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        alphaInt: Int,
        config: DepthClockConfig
    ) {
        val mainTimeText = "$hoursStr:$minutesStr"
        clockFillPaint.textAlign = Paint.Align.CENTER

        val textBounds = Rect()
        clockFillPaint.getTextBounds(mainTimeText, 0, mainTimeText.length, textBounds)
        val textHeight = textBounds.height().toFloat()
        val textWidth = clockFillPaint.measureText(mainTimeText)

        val leftEdgeX = centerX - textWidth * 0.5f
        val baselineY = centerY + textHeight * 0.38f
        val topY = baselineY - textHeight

        if (config.showDate) {
            val headerSize = (fontSizePx * 0.125f).coerceIn(20f, 48f)
            // Draw vertical accent bar
            pillBgPaint.color = accentColor
            val barTop = topY - headerSize * 2.2f
            val barBottom = topY - headerSize * 0.5f
            canvas.drawRoundRect(
                RectF(leftEdgeX, barTop, leftEdgeX + max(6f, headerSize * 0.22f), barBottom),
                4f,
                4f,
                pillBgPaint
            )

            datePaint.textSize = headerSize
            datePaint.textAlign = Paint.Align.LEFT
            datePaint.letterSpacing = 0.16f
            datePaint.color = primaryColor
            val headerText = if (config.showSeconds) {
                "$dateStr  •  ${secondsStr}S"
            } else {
                "$dateStr  •  ${config.customLockSignature}"
            }
            canvas.drawText(
                headerText,
                leftEdgeX + headerSize * 0.65f,
                barBottom - headerSize * 0.25f,
                datePaint
            )
        }

        clockFillPaint.shader = LinearGradient(
            leftEdgeX,
            topY,
            leftEdgeX + textWidth,
            baselineY,
            primaryColor,
            secondaryColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawText(mainTimeText, centerX, baselineY, clockFillPaint)
    }

    private fun drawStackedVerticalClock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        fontSizePx: Float,
        hoursStr: String,
        minutesStr: String,
        secondsStr: String,
        dateStr: String,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        alphaInt: Int,
        config: DepthClockConfig
    ) {
        val stackedFontSize = fontSizePx * 0.92f
        clockFillPaint.textSize = stackedFontSize
        clockFillPaint.textAlign = Paint.Align.CENTER

        val bounds = Rect()
        clockFillPaint.getTextBounds(hoursStr, 0, hoursStr.length, bounds)
        val lineH = bounds.height().toFloat()

        val hourBaselineY = centerY - lineH * 0.08f
        val minBaselineY = centerY + lineH * 0.96f

        if (config.showDate) {
            datePaint.textSize = (stackedFontSize * 0.14f).coerceIn(20f, 50f)
            datePaint.textAlign = Paint.Align.CENTER
            datePaint.letterSpacing = 0.18f
            datePaint.color = primaryColor
            val header = if (config.showSeconds) "$dateStr · :$secondsStr" else dateStr
            canvas.drawText(header, centerX, hourBaselineY - lineH - stackedFontSize * 0.14f, datePaint)
        }

        // Top row: Hours in Primary Color
        clockFillPaint.shader = LinearGradient(
            centerX,
            hourBaselineY - lineH,
            centerX,
            hourBaselineY,
            primaryColor,
            secondaryColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawText(hoursStr, centerX, hourBaselineY, clockFillPaint)

        // Bottom row: Minutes in Secondary/Accent gradient
        clockFillPaint.shader = LinearGradient(
            centerX,
            minBaselineY - lineH,
            centerX,
            minBaselineY,
            secondaryColor,
            accentColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawText(minutesStr, centerX, minBaselineY, clockFillPaint)
    }

    private fun drawHollowGlassClock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        fontSizePx: Float,
        hoursStr: String,
        minutesStr: String,
        secondsStr: String,
        dateStr: String,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        alphaInt: Int,
        config: DepthClockConfig
    ) {
        val mainTimeText = "$hoursStr:$minutesStr"
        clockFillPaint.textAlign = Paint.Align.CENTER

        val textBounds = Rect()
        clockFillPaint.getTextBounds(mainTimeText, 0, mainTimeText.length, textBounds)
        val textHeight = textBounds.height().toFloat()

        val baselineY = centerY + textHeight * 0.36f
        val topY = baselineY - textHeight

        if (config.showDate) {
            datePaint.textSize = (fontSizePx * 0.14f).coerceIn(20f, 52f)
            datePaint.textAlign = Paint.Align.CENTER
            datePaint.letterSpacing = 0.16f
            datePaint.color = primaryColor
            val dateHeader = if (config.showSeconds) "$dateStr  |  :$secondsStr" else dateStr
            canvas.drawText(dateHeader, centerX, topY - fontSizePx * 0.16f, datePaint)
        }

        // 1. Translucent glass inner fill
        clockFillPaint.shader = null
        clockFillPaint.color = applyAlpha(primaryColor, (alphaInt * 0.24f).toInt())
        canvas.drawText(mainTimeText, centerX, baselineY, clockFillPaint)

        // 2. Crisp glowing vector outline stroke
        clockStrokePaint.set(clockFillPaint)
        clockStrokePaint.style = Paint.Style.STROKE
        clockStrokePaint.strokeWidth = max(3f, fontSizePx * 0.026f)
        clockStrokePaint.shader = LinearGradient(
            centerX,
            topY,
            centerX,
            baselineY,
            primaryColor,
            accentColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawText(mainTimeText, centerX, baselineY, clockStrokePaint)
    }

    private fun drawChronoMinimalClock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        fontSizePx: Float,
        hoursStr: String,
        minutesStr: String,
        secondsStr: String,
        secondsInt: Int,
        dateStr: String,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        alphaInt: Int,
        config: DepthClockConfig
    ) {
        val mainTimeText = "$hoursStr:$minutesStr"
        clockFillPaint.textAlign = Paint.Align.CENTER

        val textBounds = Rect()
        clockFillPaint.getTextBounds(mainTimeText, 0, mainTimeText.length, textBounds)
        val textHeight = textBounds.height().toFloat()
        val textWidth = clockFillPaint.measureText(mainTimeText)

        val baselineY = centerY + textHeight * 0.36f
        val topY = baselineY - textHeight

        if (config.showDate) {
            datePaint.textSize = (fontSizePx * 0.135f).coerceIn(20f, 50f)
            datePaint.textAlign = Paint.Align.CENTER
            datePaint.letterSpacing = 0.15f
            datePaint.color = primaryColor
            val topLabel = if (config.showSeconds) "$dateStr  •  ${secondsStr}s" else dateStr
            canvas.drawText(topLabel, centerX, topY - fontSizePx * 0.16f, datePaint)
        }

        clockFillPaint.shader = LinearGradient(
            centerX,
            topY,
            centerX,
            baselineY,
            primaryColor,
            secondaryColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawText(mainTimeText, centerX, baselineY, clockFillPaint)

        // Sweeping seconds progress bar underneath
        val barWidth = textWidth * 0.86f
        val barHeight = max(6f, fontSizePx * 0.032f)
        val barLeft = centerX - barWidth * 0.5f
        val barTop = baselineY + fontSizePx * 0.11f
        val trackRect = RectF(barLeft, barTop, barLeft + barWidth, barTop + barHeight)

        pillBgPaint.color = applyAlpha(primaryColor, (alphaInt * 0.22f).toInt())
        canvas.drawRoundRect(trackRect, barHeight, barHeight, pillBgPaint)

        val progressFraction = if (config.showSeconds) {
            ((secondsInt.coerceIn(0, 59) + 1) / 60f).coerceIn(0.05f, 1f)
        } else {
            0.68f
        }
        val fillRect = RectF(barLeft, barTop, barLeft + barWidth * progressFraction, barTop + barHeight)
        pillBgPaint.color = accentColor
        canvas.drawRoundRect(fillRect, barHeight, barHeight, pillBgPaint)
    }

    private fun drawAlignmentGuides(
        canvas: Canvas,
        canvasWidth: Float,
        canvasHeight: Float,
        config: DepthClockConfig
    ) {
        guidePaint.reset()
        guidePaint.isAntiAlias = true
        guidePaint.style = Paint.Style.STROKE
        guidePaint.strokeWidth = 2.5f
        guidePaint.color = Color.argb(150, 0, 229, 255)
        guidePaint.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)

        val cx = config.clockNormX * canvasWidth
        val cy = config.clockNormY * canvasHeight

        // Snap center vertical line if near 0.50
        if (kotlin.math.abs(config.clockNormX - 0.5f) < 0.03f) {
            canvas.drawLine(canvasWidth * 0.5f, 0f, canvasWidth * 0.5f, canvasHeight, guidePaint)
        }

        val boxW = canvasWidth * 0.38f * config.clockScale.coerceIn(0.6f, 1.8f)
        val boxH = canvasWidth * 0.19f * config.clockScale.coerceIn(0.6f, 1.8f)
        val rect = RectF(cx - boxW, cy - boxH, cx + boxW, cy + boxH)
        canvas.drawRoundRect(rect, 24f, 24f, guidePaint)
    }

    private fun applyAlpha(colorArgb: Int, newAlpha: Int): Int {
        val origAlpha = (colorArgb ushr 24) and 0xFF
        val combinedAlpha = ((origAlpha * newAlpha) / 255).coerceIn(0, 255)
        return (combinedAlpha shl 24) or (colorArgb and 0x00FFFFFF)
    }

    /**
     * Renders the complete 3-layer Depth Clock composition into a high-resolution [Bitmap]
     * for saving to Gallery or applying as a system Lock/Home Wallpaper.
     */
    fun renderToHighResBitmap(
        context: Context,
        backgroundBitmap: Bitmap,
        foregroundCutout: Bitmap?,
        config: DepthClockConfig,
        outputWidth: Int = 1440,
        outputHeight: Int = 3200,
        timeMillis: Long = System.currentTimeMillis()
    ): Bitmap {
        val outBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        renderDepthComposition(
            context = context,
            canvas = canvas,
            canvasWidth = outputWidth.toFloat(),
            canvasHeight = outputHeight.toFloat(),
            backgroundBitmap = backgroundBitmap,
            foregroundCutout = foregroundCutout,
            maskPreviewOverlay = null,
            config = config,
            timeMillis = timeMillis,
            showMaskOverlay = false,
            showAlignmentGuides = false
        )
        return outBitmap
    }
}
