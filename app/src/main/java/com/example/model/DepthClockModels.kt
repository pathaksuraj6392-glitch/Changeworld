package com.example.model

import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import com.example.R

enum class ClockFontStyle(
    val displayName: String,
    val badge: String,
    @FontRes val fontResId: Int,
    val sizeMultiplier: Float = 1.0f,
    val letterSpacingEm: Float = -0.02f
) {
    BEBAS_NEUE(
        displayName = "Bebas Tall",
        badge = "iOS Classic",
        fontResId = R.font.bebas_neue,
        sizeMultiplier = 1.18f,
        letterSpacingEm = 0.01f
    ),
    OSWALD(
        displayName = "Oswald Bold",
        badge = "HyperOS",
        fontResId = R.font.oswald,
        sizeMultiplier = 1.06f,
        letterSpacingEm = -0.02f
    ),
    SYNE(
        displayName = "Syne Art",
        badge = "Magazine",
        fontResId = R.font.syne,
        sizeMultiplier = 0.88f,
        letterSpacingEm = -0.04f
    ),
    SPACE_GROTESK(
        displayName = "Space Tech",
        badge = "Cyber",
        fontResId = R.font.space_grotesk,
        sizeMultiplier = 0.96f,
        letterSpacingEm = -0.03f
    ),
    PLAYFAIR_DISPLAY(
        displayName = "Playfair",
        badge = "Editorial",
        fontResId = R.font.playfair_display,
        sizeMultiplier = 0.95f,
        letterSpacingEm = -0.02f
    ),
    RIGHTEOUS(
        displayName = "Righteous",
        badge = "Futuristic",
        fontResId = R.font.righteous,
        sizeMultiplier = 0.94f,
        letterSpacingEm = 0.0f
    ),
    PLUS_JAKARTA(
        displayName = "Jakarta Sans",
        badge = "Minimal",
        fontResId = R.font.plus_jakarta_sans,
        sizeMultiplier = 0.94f,
        letterSpacingEm = -0.03f
    )
}

enum class ClockLayoutStyle(
    val displayName: String,
    val subtitle: String
) {
    IOS_CLASSIC(
        displayName = "iOS Depth",
        subtitle = "Centered date + towering depth numerals"
    ),
    HYPER_MAGAZINE(
        displayName = "HyperOS",
        subtitle = "Editorial header + signature lock layout"
    ),
    STACKED_VERTICAL(
        displayName = "Stacked 3D",
        subtitle = "Double-decker hours over minutes"
    ),
    HOLLOW_GLASS(
        displayName = "Glass Outline",
        subtitle = "Translucent fill with crisp vector stroke"
    ),
    CHRONO_MINIMAL(
        displayName = "Chrono Bar",
        subtitle = "Precision clock with live seconds bar"
    )
}

enum class ClockColorPreset(
    val displayName: String,
    val primaryColorArgb: Int,
    val secondaryColorArgb: Int,
    val accentColorArgb: Int
) {
    PURE_FROST(
        displayName = "Pure Frost",
        primaryColorArgb = 0xFFFFFFFF.toInt(),
        secondaryColorArgb = 0xFFD8E6F8.toInt(),
        accentColorArgb = 0xFF00E5FF.toInt()
    ),
    ELECTRIC_CYAN(
        displayName = "Cyber Cyan",
        primaryColorArgb = 0xFF80F4FF.toInt(),
        secondaryColorArgb = 0xFF00B8D4.toInt(),
        accentColorArgb = 0xFFFFFFFF.toInt()
    ),
    HYPER_CORAL(
        displayName = "Sunset Coral",
        primaryColorArgb = 0xFFFFB199.toInt(),
        secondaryColorArgb = 0xFFFF5E3A.toInt(),
        accentColorArgb = 0xFFFFE082.toInt()
    ),
    CHAMPAGNE_GOLD(
        displayName = "Champagne",
        primaryColorArgb = 0xFFFFF3D1.toInt(),
        secondaryColorArgb = 0xFFE6B953.toInt(),
        accentColorArgb = 0xFFFFFFFF.toInt()
    ),
    AURORA_MINT(
        displayName = "Aurora Mint",
        primaryColorArgb = 0xFFA7F3D0.toInt(),
        secondaryColorArgb = 0xFF10B981.toInt(),
        accentColorArgb = 0xFF6EE7B7.toInt()
    ),
    VIOLET_NEBULA(
        displayName = "Nebula Violet",
        primaryColorArgb = 0xFFEDE9FE.toInt(),
        secondaryColorArgb = 0xFFA78BFA.toInt(),
        accentColorArgb = 0xFFC4B5FD.toInt()
    ),
    ROSE_QUARTZ(
        displayName = "Rose Quartz",
        primaryColorArgb = 0xFFFFE4E6.toInt(),
        secondaryColorArgb = 0xFFFB7185.toInt(),
        accentColorArgb = 0xFFFDA4AF.toInt()
    ),
    OBSIDIAN_CHROME(
        displayName = "Silver Chrome",
        primaryColorArgb = 0xFFF1F5F9.toInt(),
        secondaryColorArgb = 0xFF94A3B8.toInt(),
        accentColorArgb = 0xFF38BDF8.toInt()
    )
}

enum class SegmentationMode(
    val displayName: String,
    val description: String
) {
    AUTO_HYBRID(
        displayName = "AI Smart Hybrid",
        description = "ML Kit Neural + Edge Salience (People & Objects)"
    ),
    MLKIT_PORTRAIT(
        displayName = "ML Kit Portrait",
        description = "Google ML Kit Selfie Segmentation neural network"
    ),
    OBJECT_CONTRAST(
        displayName = "Subject & Peak",
        description = "Edge & contrast extraction for objects/landscapes"
    )
}

enum class BrushToolMode(val displayName: String) {
    NONE("Move Clock"),
    RESTORE_SUBJECT("Restore Subject"),
    ERASE_SUBJECT("Erase Mask")
}

data class SampleWallpaperPreset(
    val id: String,
    val title: String,
    val category: String,
    @DrawableRes val drawableResId: Int,
    val recommendedConfig: DepthClockConfig
)

data class DepthClockConfig(
    val clockNormX: Float = 0.50f,
    val clockNormY: Float = 0.24f,
    val clockScale: Float = 1.08f,
    val fontStyle: ClockFontStyle = ClockFontStyle.BEBAS_NEUE,
    val layoutStyle: ClockLayoutStyle = ClockLayoutStyle.IOS_CLASSIC,
    val colorPreset: ClockColorPreset = ClockColorPreset.PURE_FROST,
    val clockOpacity: Float = 0.96f,
    val showSeconds: Boolean = true,
    val use24HourFormat: Boolean = false,
    val showDate: Boolean = true,
    val dateFormatPattern: String = "EEE, MMM d",
    val enableDepthEffect: Boolean = true,
    val subjectOcclusionStrength: Float = 1.0f,
    val enableSubjectRimLight: Boolean = true,
    val enableClockDropShadow: Boolean = true,
    val backgroundDimAlpha: Float = 0.12f,
    val segmentationMode: SegmentationMode = SegmentationMode.AUTO_HYBRID,
    val maskThreshold: Float = 0.42f,
    val maskFeatherRadius: Int = 2,
    val customLockSignature: String = "DEPTH STUDIO"
)

object BuiltInPresets {
    val presets: List<SampleWallpaperPreset> = listOf(
        SampleWallpaperPreset(
            id = "cyber_portrait",
            title = "Neon Editorial",
            category = "Portrait AI",
            drawableResId = R.drawable.img_sample_cyber_portrait,
            recommendedConfig = DepthClockConfig(
                clockNormX = 0.50f,
                clockNormY = 0.23f,
                clockScale = 1.15f,
                fontStyle = ClockFontStyle.BEBAS_NEUE,
                layoutStyle = ClockLayoutStyle.IOS_CLASSIC,
                colorPreset = ClockColorPreset.ELECTRIC_CYAN,
                showSeconds = true,
                use24HourFormat = false,
                segmentationMode = SegmentationMode.AUTO_HYBRID,
                maskThreshold = 0.40f
            )
        ),
        SampleWallpaperPreset(
            id = "urban_astronaut",
            title = "Orbital Visor",
            category = "Helmet Depth",
            drawableResId = R.drawable.img_sample_urban_astronaut,
            recommendedConfig = DepthClockConfig(
                clockNormX = 0.50f,
                clockNormY = 0.25f,
                clockScale = 1.12f,
                fontStyle = ClockFontStyle.OSWALD,
                layoutStyle = ClockLayoutStyle.HYPER_MAGAZINE,
                colorPreset = ClockColorPreset.CHAMPAGNE_GOLD,
                showSeconds = true,
                use24HourFormat = true,
                segmentationMode = SegmentationMode.AUTO_HYBRID,
                maskThreshold = 0.42f
            )
        ),
        SampleWallpaperPreset(
            id = "alpine_peak",
            title = "Aurora Ridge",
            category = "Explorer",
            drawableResId = R.drawable.img_sample_alpine_peak,
            recommendedConfig = DepthClockConfig(
                clockNormX = 0.50f,
                clockNormY = 0.22f,
                clockScale = 1.10f,
                fontStyle = ClockFontStyle.SYNE,
                layoutStyle = ClockLayoutStyle.HOLLOW_GLASS,
                colorPreset = ClockColorPreset.PURE_FROST,
                showSeconds = true,
                use24HourFormat = false,
                segmentationMode = SegmentationMode.AUTO_HYBRID,
                maskThreshold = 0.40f
            )
        )
    )
}
