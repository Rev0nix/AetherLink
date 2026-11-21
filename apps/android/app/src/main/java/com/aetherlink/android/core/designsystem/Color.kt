package com.aetherlink.android.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Futuristic Dark Palette (Linear / Nothing OS / Arc inspired)
val AetherBg = Color(0xFF090B14)
val AetherBgSecondary = Color(0xFF111522)
val AetherCardBg = Color(0xFF171D2B)
val AetherSurfaceElevated = Color(0xFF20283A)
val AetherBorder = Color(0xFF263045)

// Accents
val AetherPurple = Color(0xFF7C5CFC)
val AetherCyan = Color(0xFF32C5FF)
val AetherGreen = Color(0xFF35D49A)
val AetherOrange = Color(0xFFFFB84D)
val AetherRed = Color(0xFFFF5D73)

// Text
val AetherTextPrimary = Color(0xFFF5F7FF)
val AetherTextSecondary = Color(0xFF929BB0)
val AetherTextMuted = Color(0xFF636D82)

// Gradients
val AetherGradientPrimary = Brush.horizontalGradient(
    listOf(AetherPurple, AetherCyan)
)

val AetherGradientRadial = Brush.radialGradient(
    listOf(AetherPurple.copy(alpha = 0.35f), Color.Transparent)
)

val AetherGradientCard = Brush.linearGradient(
    listOf(AetherCardBg, AetherSurfaceElevated.copy(alpha = 0.6f))
)
