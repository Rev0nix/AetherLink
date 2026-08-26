package com.aetherlink.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aetherlink.android.ui.screens.HomeScreen
import com.aetherlink.android.ui.screens.DevicesScreen
import com.aetherlink.android.ui.screens.SettingsScreen
import com.aetherlink.android.ui.screens.PairingScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {

        composable(Screen.Home.route) {
            HomeScreen()
        }

        composable(Screen.Devices.route) {
            DevicesScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(Screen.Pairing.route) {
            PairingScreen()
        }
    }
}