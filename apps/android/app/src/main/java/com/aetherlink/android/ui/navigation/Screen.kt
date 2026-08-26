package com.aetherlink.android.ui.navigation

sealed class Screen(val route: String) {

    data object Home : Screen("home")

    data object Devices : Screen("devices")

    data object Settings : Screen("settings")

    data object Pairing : Screen("pairing")
}