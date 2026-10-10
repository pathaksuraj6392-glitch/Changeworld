package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DepthClockDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = AlabasterWhite,
    secondary = HyperCoral,
    onSecondary = AlabasterWhite,
    secondaryContainer = HyperCoralContainer,
    onSecondaryContainer = AlabasterWhite,
    tertiary = AuroraViolet,
    background = ObsidianBackground,
    onBackground = AlabasterWhite,
    surface = ObsidianSurface,
    onSurface = AlabasterWhite,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateOutline
)

@Composable
fun DepthClockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DepthClockDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    DepthClockTheme(content = content)
}
