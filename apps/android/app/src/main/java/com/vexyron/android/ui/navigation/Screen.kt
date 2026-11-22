package com.vexyron.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String = "",
    val icon: ImageVector? = null
) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")

    // Main 5 Bottom Bar Tabs
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Devices : Screen("devices", "Devices", Icons.Default.Devices)
    data object Files : Screen("files", "Files", Icons.Default.Folder)
    data object Assistant : Screen("assistant", "AI", Icons.Default.Psychology)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Feature Sub-screens
    data object Pairing : Screen("pairing", "Pair Device")
    data object Mirroring : Screen("mirroring", "Screen Mirror")
    data object Clipboard : Screen("clipboard", "Clipboard Sync")
    data object Notifications : Screen("notifications", "Notifications")
    data object Terminal : Screen("terminal", "Remote Terminal")

    companion object {
        val bottomBarTabs = listOf(Home, Devices, Files, Assistant, Settings)
    }
}
