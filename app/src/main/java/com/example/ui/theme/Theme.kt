package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BroadcastNoirColorScheme =
  darkColorScheme(
    primary = CyanBroadcast,
    onPrimary = ObsidianBg,
    primaryContainer = ActiveCard,
    onPrimaryContainer = CyanBroadcast,
    secondary = AmberFavorite,
    onSecondary = ObsidianBg,
    secondaryContainer = ElevatedCard,
    onSecondaryContainer = InkPrimary,
    tertiary = CrimsonLive,
    onTertiary = InkPrimary,
    background = ObsidianBg,
    onBackground = InkPrimary,
    surface = ControlSurface,
    onSurface = InkPrimary,
    surfaceVariant = ElevatedCard,
    onSurfaceVariant = InkSecondary,
    outline = BorderSlate,
    outlineVariant = BorderSlate,
    error = CrimsonLive,
    onError = InkPrimary,
  )

@Composable
fun OndeTvTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = BroadcastNoirColorScheme,
    typography = Typography,
    content = content,
  )
}
