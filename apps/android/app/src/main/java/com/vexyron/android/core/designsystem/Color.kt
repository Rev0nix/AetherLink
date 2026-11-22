package com.vexyron.android.core.designsystem

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class VexyronThemeMode(val id: String, val displayName: String, val subtitle: String) {
    MIDNIGHT("midnight", "Midnight Dark", "Futuristic deep navy & neon accents"),
    LIGHT("light", "Clean Studio Light", "Minimalist crisp high-contrast light"),
    AMOLED("amoled", "AMOLED Pitch Black", "Pure 0% black for OLED power savings"),
    CYBERPUNK("cyberpunk", "Cyberpunk Neon", "High-contrast ultraviolet & cyan glow");

    companion object {
        fun fromId(id: String): VexyronThemeMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: MIDNIGHT
        }
    }
}

data class VexyronColorPalette(
    val bg: Color,
    val bgSecondary: Color,
    val cardBg: Color,
    val surfaceElevated: Color,
    val border: Color,
    val purple: Color,
    val cyan: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isLight: Boolean
)

object ThemeManager {
    private const val PREFS_NAME = "vexyron_prefs"
    private const val KEY_THEME = "app_theme"

    val MidnightPalette = VexyronColorPalette(
        bg = Color(0xFF090B14),
        bgSecondary = Color(0xFF111522),
        cardBg = Color(0xFF171D2B),
        surfaceElevated = Color(0xFF20283A),
        border = Color(0xFF263045),
        purple = Color(0xFF7C5CFC),
        cyan = Color(0xFF32C5FF),
        green = Color(0xFF35D49A),
        orange = Color(0xFFFFB84D),
        red = Color(0xFFFF5D73),
        textPrimary = Color(0xFFF5F7FF),
        textSecondary = Color(0xFF929BB0),
        textMuted = Color(0xFF636D82),
        isLight = false
    )

    val LightPalette = VexyronColorPalette(
        bg = Color(0xFFF4F6FB),
        bgSecondary = Color(0xFFE9EDF5),
        cardBg = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFEEF2F6),
        border = Color(0xFFD6DEE8),
        purple = Color(0xFF6344E7),
        cyan = Color(0xFF0091D4),
        green = Color(0xFF059669),
        orange = Color(0xFFD97706),
        red = Color(0xFFDC2626),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF94A3B8),
        isLight = true
    )

    val AmoledPalette = VexyronColorPalette(
        bg = Color(0xFF000000),
        bgSecondary = Color(0xFF0C0C0C),
        cardBg = Color(0xFF121212),
        surfaceElevated = Color(0xFF1E1E1E),
        border = Color(0xFF2A2A2A),
        purple = Color(0xFF8A6FFF),
        cyan = Color(0xFF38D5FF),
        green = Color(0xFF38E5A3),
        orange = Color(0xFFFFC05A),
        red = Color(0xFFFF667C),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFA0A0A0),
        textMuted = Color(0xFF666666),
        isLight = false
    )

    val CyberpunkPalette = VexyronColorPalette(
        bg = Color(0xFF0D0618),
        bgSecondary = Color(0xFF180D2B),
        cardBg = Color(0xFF21123D),
        surfaceElevated = Color(0xFF2F1A57),
        border = Color(0xFF4A2580),
        purple = Color(0xFFBF00FF),
        cyan = Color(0xFF00F0FF),
        green = Color(0xFF00FF9D),
        orange = Color(0xFFFF8400),
        red = Color(0xFFFF0055),
        textPrimary = Color(0xFFFDE8FF),
        textSecondary = Color(0xFFC8A5DE),
        textMuted = Color(0xFF8E6B9E),
        isLight = false
    )

    var currentMode by mutableStateOf(VexyronThemeMode.MIDNIGHT)
        private set

    val currentColors: VexyronColorPalette
        get() = when (currentMode) {
            VexyronThemeMode.MIDNIGHT -> MidnightPalette
            VexyronThemeMode.LIGHT -> LightPalette
            VexyronThemeMode.AMOLED -> AmoledPalette
            VexyronThemeMode.CYBERPUNK -> CyberpunkPalette
        }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, VexyronThemeMode.MIDNIGHT.id) ?: VexyronThemeMode.MIDNIGHT.id
        currentMode = VexyronThemeMode.fromId(saved)
    }

    fun setTheme(mode: VexyronThemeMode, context: Context? = null) {
        currentMode = mode
        context?.let {
            it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, mode.id)
                .apply()
        }
    }
}

// Transparent dynamic bindings so all existing composables seamlessly update
val VexyronBg: Color get() = ThemeManager.currentColors.bg
val VexyronBgSecondary: Color get() = ThemeManager.currentColors.bgSecondary
val VexyronCardBg: Color get() = ThemeManager.currentColors.cardBg
val VexyronSurfaceElevated: Color get() = ThemeManager.currentColors.surfaceElevated
val VexyronBorder: Color get() = ThemeManager.currentColors.border

// Accents
val VexyronPurple: Color get() = ThemeManager.currentColors.purple
val VexyronCyan: Color get() = ThemeManager.currentColors.cyan
val VexyronGreen: Color get() = ThemeManager.currentColors.green
val VexyronOrange: Color get() = ThemeManager.currentColors.orange
val VexyronRed: Color get() = ThemeManager.currentColors.red

// Text
val VexyronTextPrimary: Color get() = ThemeManager.currentColors.textPrimary
val VexyronTextSecondary: Color get() = ThemeManager.currentColors.textSecondary
val VexyronTextMuted: Color get() = ThemeManager.currentColors.textMuted

// Gradients
val VexyronGradientPrimary: Brush
    get() = Brush.horizontalGradient(listOf(VexyronPurple, VexyronCyan))

val VexyronGradientRadial: Brush
    get() = Brush.radialGradient(listOf(VexyronPurple.copy(alpha = 0.35f), Color.Transparent))

val VexyronGradientCard: Brush
    get() = Brush.linearGradient(listOf(VexyronCardBg, VexyronSurfaceElevated.copy(alpha = 0.6f)))
