package com.vexyron.android.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vexyron.android.core.designsystem.VexyronBg
import com.vexyron.android.feature.assistant.AiAssistantScreen
import com.vexyron.android.feature.clipboard.ClipboardScreen
import com.vexyron.android.feature.devices.DevicesScreen
import com.vexyron.android.feature.files.FileTransferScreen
import com.vexyron.android.feature.home.HomeScreen
import com.vexyron.android.feature.mirroring.ScreenMirroringScreen
import com.vexyron.android.feature.notifications.NotificationSyncScreen
import com.vexyron.android.feature.onboarding.OnboardingScreen
import com.vexyron.android.feature.pairing.PairingScreen
import com.vexyron.android.feature.settings.SettingsScreen
import com.vexyron.android.feature.splash.SplashScreen
import com.vexyron.android.feature.terminal.RemoteTerminalScreen
import com.vexyron.android.viewmodel.HomeViewModel

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
        containerColor = VexyronBg,
        bottomBar = {
            VexyronBottomBar(navController = navController)
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

            // 12. Remote Terminal Screen
            composable(Screen.Terminal.route) {
                RemoteTerminalScreen(navController = navController, vm = sharedHomeViewModel)
            }
        }
    }
}
