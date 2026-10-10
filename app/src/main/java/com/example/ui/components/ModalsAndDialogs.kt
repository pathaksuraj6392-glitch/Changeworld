package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AlabasterWhite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HyperCoral
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateOutline
import com.example.wallpaper.WallpaperTarget

@Composable
fun OnboardingModal(
    onDismiss: () -> Unit,
    onPickPhotoNow: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .testTag("onboarding_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = ElectricCyan
                            )
                        }
                        Column {
                            Text(
                                text = "Depth Clock Studio",
                                style = MaterialTheme.typography.titleLarge,
                                color = AlabasterWhite
                            )
                            Text(
                                text = "HyperOS & iOS 18 3D Lock Screen Effect",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_onboarding_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close onboarding",
                            tint = SlateMuted
                        )
                    }
                }

                OnboardingStepRow(
                    stepNumber = "1",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = ElectricCyan,
                    title = "100% On-Device AI Subject Separation",
                    description = "Select any portrait, pet, helmet, or landscape photo. Google ML Kit + Edge Salience automatically extracts the foreground subject completely offline."
                )

                OnboardingStepRow(
                    stepNumber = "2",
                    icon = Icons.Default.OpenWith,
                    accentColor = HyperCoral,
                    title = "Drag & Pinch Clock Behind Subject",
                    description = "Drag with one finger to reposition or pinch with two fingers to resize the clock directly on the canvas. Watch the subject overlap the clock numerals in 3D!"
                )

                OnboardingStepRow(
                    stepNumber = "3",
                    icon = Icons.Default.Wallpaper,
                    accentColor = EmeraldSuccess,
                    title = "Save HD Image or Apply Live Wallpaper",
                    description = "Export a crisp full-resolution PNG to Gallery, apply directly to your Lock/Home screen, or activate the real-time ticking Depth Clock Live Wallpaper."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onPickPhotoNow()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianSurfaceVariant,
                            contentColor = AlabasterWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("onboarding_pick_photo_button")
                    ) {
                        Text(
                            text = "Pick Gallery Photo",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF002025)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("onboarding_start_button")
                    ) {
                        Text(
                            text = "Start Creating",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepRow(
    stepNumber: String,
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String
) {
    Surface(
        color = ObsidianBackground.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateOutline, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stepNumber,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AlabasterWhite
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted
                )
            }
        }
    }
}

@Composable
fun WallpaperExportModal(
    onDismiss: () -> Unit,
    onSaveHighResImage: () -> Unit,
    onApplyStaticWallpaper: (WallpaperTarget) -> Unit,
    onActivateLiveWallpaper: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .testTag("wallpaper_export_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export & Set Wallpaper",
                            style = MaterialTheme.typography.titleLarge,
                            color = AlabasterWhite
                        )
                        Text(
                            text = "Apply 3D Depth Clock to your device or save HD PNG",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_export_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateMuted
                        )
                    }
                }

                // 1. Live Ticking Depth Wallpaper Option
                ExportOptionCard(
                    icon = Icons.Default.AutoAwesome,
                    badge = "LIVE TICKING",
                    badgeColor = ElectricCyan,
                    title = "Activate Live Depth Clock Wallpaper",
                    subtitle = "Real-time ticking clock behind your subject on Home/Lock screen",
                    onClick = onActivateLiveWallpaper,
                    testTag = "apply_live_wallpaper_option"
                )

                // 2. Static Lock Screen Wallpaper
                ExportOptionCard(
                    icon = Icons.Default.Lock,
                    badge = "LOCK SCREEN",
                    badgeColor = HyperCoral,
                    title = "Set as Lock Screen Wallpaper",
                    subtitle = "Apply high-resolution depth composition directly to Lock Screen",
                    onClick = { onApplyStaticWallpaper(WallpaperTarget.LOCK_SCREEN) },
                    testTag = "apply_lock_wallpaper_option"
                )

                // 3. Static Home Screen Wallpaper
                ExportOptionCard(
                    icon = Icons.Default.Home,
                    badge = "HOME SCREEN",
                    badgeColor = EmeraldSuccess,
                    title = "Set as Home Screen Wallpaper",
                    subtitle = "Apply high-resolution depth composition directly to Home Screen",
                    onClick = { onApplyStaticWallpaper(WallpaperTarget.HOME_SCREEN) },
                    testTag = "apply_home_wallpaper_option"
                )

                // 4. Both Screens
                ExportOptionCard(
                    icon = Icons.Default.Smartphone,
                    badge = "BOTH",
                    badgeColor = ElectricCyan,
                    title = "Set on Both Lock & Home Screens",
                    subtitle = "Apply to both Lock Screen and Home Screen simultaneously",
                    onClick = { onApplyStaticWallpaper(WallpaperTarget.BOTH_SCREENS) },
                    testTag = "apply_both_wallpaper_option"
                )

                // 5. Save HD PNG to Gallery
                Button(
                    onClick = {
                        onDismiss()
                        onSaveHighResImage()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF002025)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("modal_save_hd_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save HD Image"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Save High-Resolution PNG to Gallery",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    badge: String,
    badgeColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = ObsidianBackground.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SlateOutline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = AlabasterWhite
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted
                )
            }
            Surface(
                color = badgeColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
