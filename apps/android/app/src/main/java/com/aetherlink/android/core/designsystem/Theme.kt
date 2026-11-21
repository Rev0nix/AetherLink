package com.aetherlink.android.core.designsystem

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AetherLinkTheme(
    content: @Composable () -> Unit
) {
    val palette = ThemeManager.currentColors

    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.purple,
            onPrimary = Color.White,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.purple,
            secondary = palette.cyan,
            onSecondary = Color.White,
            tertiary = palette.green,
            background = palette.bg,
            onBackground = palette.textPrimary,
            surface = palette.cardBg,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.red,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = palette.purple,
            onPrimary = palette.textPrimary,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.cyan,
            secondary = palette.cyan,
            onSecondary = palette.bg,
            tertiary = palette.green,
            background = palette.bg,
            onBackground = palette.textPrimary,
            surface = palette.cardBg,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.red,
            onError = palette.textPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = palette.bg.toArgb()
            window.navigationBarColor = palette.bg.toArgb()
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = palette.isLight
            windowInsetsController.isAppearanceLightNavigationBars = palette.isLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AetherTypography,
        content = content
    )
}
