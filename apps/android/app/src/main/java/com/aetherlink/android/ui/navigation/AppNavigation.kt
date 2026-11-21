package com.aetherlink.android.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.feature.assistant.AiAssistantScreen
import com.aetherlink.android.feature.clipboard.ClipboardScreen
import com.aetherlink.android.feature.devices.DevicesScreen
import com.aetherlink.android.feature.files.FileTransferScreen
import com.aetherlink.android.feature.home.HomeScreen
import com.aetherlink.android.feature.mirroring.ScreenMirroringScreen
import com.aetherlink.android.feature.notifications.NotificationSyncScreen
import com.aetherlink.android.feature.onboarding.OnboardingScreen
import com.aetherlink.android.feature.pairing.PairingScreen
import com.aetherlink.android.feature.settings.SettingsScreen
import com.aetherlink.android.feature.splash.SplashScreen
import com.aetherlink.android.viewmodel.HomeViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val activity = LocalContext.current as? ComponentActivity
    val sharedHomeViewModel: HomeViewModel = if (activity != null) {
        viewModel(activity)
    } else {
        viewModel()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AetherBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AetherBottomBar(navController = navController)
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
            // 1. Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Onboarding Screen
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // 3. Home Dashboard (Main Tab 1)
            composable(Screen.Home.route) {
                HomeScreen(navController = navController, vm = sharedHomeViewModel)
            }

            // 4. Devices Screen (Main Tab 2)
            composable(Screen.Devices.route) {
                DevicesScreen(navController = navController, vm = sharedHomeViewModel)
            }

            // 5. Files Transfer Screen (Main Tab 3)
            composable(Screen.Files.route) {
                FileTransferScreen(navController = navController)
            }

            // 6. AI Assistant Screen (Main Tab 4)
            composable(Screen.Assistant.route) {
                AiAssistantScreen(navController = navController, vm = sharedHomeViewModel)
            }

            // 7. Settings Screen (Main Tab 5)
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }

            // 8. Device Pairing Screen
            composable(Screen.Pairing.route) {
                PairingScreen(navController = navController, vm = sharedHomeViewModel)
            }

            // 9. Screen Mirroring Screen
            composable(Screen.Mirroring.route) {
                ScreenMirroringScreen(navController = navController, vm = sharedHomeViewModel)
            }

            // 10. Clipboard Sync Screen
            composable(Screen.Clipboard.route) {
                ClipboardScreen(navController = navController)
            }

            // 11. Notification Sync Screen
            composable(Screen.Notifications.route) {
                NotificationSyncScreen(navController = navController)
            }
        }
    }
}
