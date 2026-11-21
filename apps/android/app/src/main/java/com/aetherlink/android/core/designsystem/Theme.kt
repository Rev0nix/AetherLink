package com.aetherlink.android.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AetherPurple,
    onPrimary = AetherTextPrimary,
    primaryContainer = AetherSurfaceElevated,
    onPrimaryContainer = AetherCyan,
    secondary = AetherCyan,
    onSecondary = AetherBg,
    tertiary = AetherGreen,
    background = AetherBg,
    onBackground = AetherTextPrimary,
    surface = AetherCardBg,
    onSurface = AetherTextPrimary,
    surfaceVariant = AetherSurfaceElevated,
    onSurfaceVariant = AetherTextSecondary,
    outline = AetherBorder,
    error = AetherRed,
    onError = AetherTextPrimary
)

@Composable
fun AetherLinkTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AetherBg.toArgb()
            window.navigationBarColor = AetherBg.toArgb()
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = false
            windowInsetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AetherTypography,
        content = content
    )
}
