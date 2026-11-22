package com.aetherlink.android.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherlink.android.R
import androidx.navigation.NavController
import com.aetherlink.android.core.components.AetherCard
import com.aetherlink.android.core.components.AetherHeader
import com.aetherlink.android.core.components.AetherPrimaryButton
import com.aetherlink.android.core.components.AetherSecondaryButton
import com.aetherlink.android.core.constants.AppConstants
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCardBg
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGreen
import com.aetherlink.android.core.designsystem.AetherOrange
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherRed
import com.aetherlink.android.core.designsystem.AetherSurfaceElevated
import com.aetherlink.android.core.designsystem.AetherTextMuted
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary
import com.aetherlink.android.core.designsystem.AetherThemeMode
import com.aetherlink.android.core.designsystem.ThemeManager
import com.aetherlink.android.core.i18n.I18nManager
import com.aetherlink.android.core.update.InAppUpdateInstaller
import com.aetherlink.android.core.update.UpdateManager
import com.aetherlink.android.ui.navigation.Screen

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("aetherlink_prefs", Context.MODE_PRIVATE)
    }

    var autoReconnect by remember {
        mutableStateOf(prefs.getBoolean("auto_reconnect", true))
    }
    var backgroundDiscovery by remember {
        mutableStateOf(prefs.getBoolean("bg_discovery", true))
    }
    var startupBehavior by remember {
        mutableStateOf(prefs.getBoolean("startup_boot", true))
    }

    // Active Dialog States
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPortDialog by remember { mutableStateOf(false) }
    var portInput by remember { mutableStateOf("45876") }

    var showUfwDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            AetherHeader(
                title = I18nManager.t("settings.title"),
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route)
                    }
                }
            )
        }

        // 1. GENERAL CATEGORY
        item {
            SettingsCategoryHeader(I18nManager.t("settings.general"))
            Spacer(modifier = Modifier.height(10.dp))
            AetherCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        iconTint = AetherPurple,
                        title = I18nManager.t("settings.appearance"),
                        subtitle = "${ThemeManager.currentMode.displayName} • ${ThemeManager.currentMode.subtitle}",
                        onClick = { showAppearanceDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Language,
                        iconTint = AetherCyan,
                        title = I18nManager.t("settings.language"),
                        subtitle = "${I18nManager.currentLanguage.name} (${I18nManager.currentLanguage.nativeName})",
                        onClick = { showLanguageDialog = true }
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.PowerSettingsNew,
                        iconTint = AetherGreen,
                        title = I18nManager.t("settings.startup"),
                        subtitle = "Enable discovery services on device boot",
                        checked = startupBehavior,
                        onCheckedChange = {
                            startupBehavior = it
                            prefs.edit().putBoolean("startup_boot", it).apply()
                            Toast.makeText(context, if (it) "Auto-startup enabled" else "Auto-startup disabled", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // 2. DEVICES CATEGORY
        item {
            SettingsCategoryHeader(I18nManager.t("settings.devices"))
            Spacer(modifier = Modifier.height(10.dp))
            AetherCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.Devices,
                        iconTint = AetherCyan,
                        title = "Automatic Reconnection",
                        subtitle = "Reconnect automatically when laptop is found on Wi-Fi",
                        checked = autoReconnect,
                        onCheckedChange = {
                            autoReconnect = it
                            prefs.edit().putBoolean("auto_reconnect", it).apply()
                            Toast.makeText(context, if (it) "Auto-reconnection enabled" else "Auto-reconnection disabled", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.Security,
                        iconTint = AetherOrange,
                        title = "Device Permissions",
                        subtitle = "Camera, Storage & Notification Access",
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open system settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.Devices,
                        iconTint = AetherPurple,
                        title = "Paired Devices Registry",
                        subtitle = "View and manage linked desktop machines",
                        onClick = { navController.navigate(Screen.Devices.route) }
                    )
                }
            }
        }

        // 3. NETWORK & DISCOVERY CATEGORY
        item {
            SettingsCategoryHeader(I18nManager.t("settings.network"))
            Spacer(modifier = Modifier.height(10.dp))
            AetherCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.Wifi,
                        iconTint = AetherGreen,
                        title = "mDNS Local Discovery",
                        subtitle = "Broadcast and listen on _aetherlink._tcp",
                        checked = backgroundDiscovery,
                        onCheckedChange = {
                            backgroundDiscovery = it
                            prefs.edit().putBoolean("bg_discovery", it).apply()
                            Toast.makeText(context, if (it) "mDNS Discovery enabled" else "mDNS Discovery paused", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.Lan,
                        iconTint = AetherCyan,
                        title = "Port Configuration",
                        subtitle = "Default TCP Port $portInput (Tap to configure)",
                        onClick = { showPortDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Terminal,
                        iconTint = AetherOrange,
                        title = "Linux Firewall Helper (All Distros)",
                        subtitle = "UFW, Firewalld, iptables, openSUSE & nftables",
                        onClick = { showUfwDialog = true }
                    )
                }
            }
        }

        // 4. ABOUT & MAINTENANCE
        item {
            SettingsCategoryHeader(I18nManager.t("settings.about"))
            Spacer(modifier = Modifier.height(10.dp))

            // Real-time In-App Downloading Card
            if (InAppUpdateInstaller.isDownloading) {
                AetherCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading Update...",
                                color = AetherCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${(InAppUpdateInstaller.downloadProgress * 100).toInt()}%",
                                color = AetherGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { InAppUpdateInstaller.downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AetherCyan,
                            trackColor = AetherSurfaceElevated
                        )
                        Text(
                            text = InAppUpdateInstaller.statusText,
                            color = AetherTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (UpdateManager.isUpdateAvailable && UpdateManager.updateInfo != null) {
                // Quick Direct Install In-App Card when update detected
                AetherCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update Available: v${UpdateManager.updateInfo!!.version}",
                                    color = AetherGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Tap to download and install inside the app",
                                    color = AetherTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            AetherPrimaryButton(
                                text = "Install In-App",
                                onClick = {
                                    InAppUpdateInstaller.startDirectDownloadAndInstall(
                                        context,
                                        UpdateManager.updateInfo!!.apkDownloadUrl
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            AetherCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Check for Updates Row
                    SettingsRow(
                        icon = Icons.Default.Download,
                        iconTint = AetherGreen,
                        title = I18nManager.t("settings.check_updates"),
                        subtitle = if (InAppUpdateInstaller.isDownloading) InAppUpdateInstaller.statusText
                                   else if (UpdateManager.isChecking) "Checking for updates..."
                                   else if (UpdateManager.isUpdateAvailable) "v${UpdateManager.updateInfo?.version} available • Tap to install in-app"
                                   else "Tap to check for updates • Current v${AppConstants.VERSION}",
                        onClick = {
                            if (InAppUpdateInstaller.isDownloading) {
                                UpdateManager.showDialog = true
                            } else if (UpdateManager.isUpdateAvailable && UpdateManager.updateInfo != null) {
                                InAppUpdateInstaller.startDirectDownloadAndInstall(
                                    context,
                                    UpdateManager.updateInfo!!.apkDownloadUrl
                                )
                                UpdateManager.showDialog = true
                            } else {
                                UpdateManager.checkForUpdates(context, manual = true)
                            }
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconTint = AetherCyan,
                        title = "VEXYRON Android",
                        subtitle = "v${AppConstants.VERSION} (Build ${AppConstants.BUILD_NUMBER}) • Production Release",
                        onClick = { showAboutDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconTint = AetherPurple,
                        title = "Developer",
                        subtitle = "Revanth H M • System Architect",
                        onClick = {
                            Toast.makeText(context, "VEXYRON • Developed by Revanth H M", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconTint = AetherTextMuted,
                        title = "Software License",
                        subtitle = "PolyForm Noncommercial License 1.0.0",
                        onClick = { showLicenseDialog = true }
                    )
                    SettingsRow(
                        icon = Icons.Default.DeleteSweep,
                        iconTint = AetherRed,
                        title = I18nManager.t("settings.clear_cache"),
                        subtitle = "Reset saved desktop IPs, pairings & settings",
                        onClick = { showClearCacheDialog = true }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Appearance Dialog
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            containerColor = AetherCardBg,
            title = {
                Text(
                    text = "Choose Theme & Appearance",
                    color = AetherTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AetherThemeMode.entries.forEach { mode ->
                        val isSelected = ThemeManager.currentMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AetherSurfaceElevated else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) AetherCyan else AetherBorder.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    ThemeManager.setTheme(mode, context)
                                    Toast.makeText(context, "Applied ${mode.displayName}", Toast.LENGTH_SHORT).show()
                                    showAppearanceDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.displayName,
                                    color = if (isSelected) AetherCyan else AetherTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = mode.subtitle,
                                    color = AetherTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AetherCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AetherPrimaryButton(text = "Close", onClick = { showAppearanceDialog = false })
            }
        )
    }

    // 2. Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = AetherCardBg,
            title = {
                Text(
                    text = "Select Language",
                    color = AetherTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    I18nManager.supportedLanguages.forEach { lang ->
                        val isSelected = I18nManager.currentLanguageCode == lang.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AetherSurfaceElevated else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) AetherCyan else AetherBorder.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    I18nManager.setLanguage(lang.code, context)
                                    Toast.makeText(context, "Language: ${lang.name}", Toast.LENGTH_SHORT).show()
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = lang.name,
                                    color = if (isSelected) AetherCyan else AetherTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = lang.nativeName,
                                    color = AetherTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AetherCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AetherPrimaryButton(text = "Done", onClick = { showLanguageDialog = false })
            }
        )
    }

    // 3. Port Configuration Dialog
    if (showPortDialog) {
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            containerColor = AetherCardBg,
            title = { Text("TCP Port Configuration", color = AetherTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("The desktop socket server listens on port ${AppConstants.DISCOVERY_PORT} by default.", color = AetherTextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AetherCyan,
                            unfocusedBorderColor = AetherBorder,
                            focusedTextColor = AetherTextPrimary,
                            unfocusedTextColor = AetherTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                AetherPrimaryButton(text = "Save", onClick = {
                    Toast.makeText(context, "Port set to $portInput", Toast.LENGTH_SHORT).show()
                    showPortDialog = false
                })
            },
            dismissButton = {
                AetherSecondaryButton(text = "Reset", onClick = { portInput = AppConstants.DISCOVERY_PORT.toString() })
            }
        )
    }

    // 4. Linux Firewall Helper Dialog (All Distros)
    if (showUfwDialog) {
        val firewallRules = remember {
            listOf(
                Triple("UFW", "Ubuntu / Debian / Mint", "sudo ufw allow ${AppConstants.DISCOVERY_PORT}/tcp && sudo ufw allow 5353/udp"),
                Triple("Firewalld", "Fedora / RHEL / CentOS", "sudo firewall-cmd --add-port=${AppConstants.DISCOVERY_PORT}/tcp --add-port=5353/udp --permanent && sudo firewall-cmd --reload"),
                Triple("iptables", "Arch Linux / Manjaro / Generic", "sudo iptables -I INPUT -p tcp --dport ${AppConstants.DISCOVERY_PORT} -j ACCEPT && sudo iptables -I INPUT -p udp --dport 5353 -j ACCEPT"),
                Triple("openSUSE", "openSUSE Leap & Tumbleweed", "sudo firewall-cmd --zone=public --add-port=${AppConstants.DISCOVERY_PORT}/tcp --add-port=5353/udp --permanent && sudo firewall-cmd --reload"),
                Triple("nftables", "Debian 12+ / Alpine / NixOS", "sudo nft add rule inet filter input tcp dport ${AppConstants.DISCOVERY_PORT} accept && sudo nft add rule inet filter input udp dport 5353 accept")
            )
        }
        var selectedDistroIndex by remember { mutableIntStateOf(0) }
        val activeRule = firewallRules[selectedDistroIndex]

        AlertDialog(
            onDismissRequest = { showUfwDialog = false },
            containerColor = AetherCardBg,
            title = { Text("Linux Firewall Helper", color = AetherTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select your Linux distribution to copy the required firewall port rule:", color = AetherTextSecondary, fontSize = 12.sp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        firewallRules.forEachIndexed { index, rule ->
                            val isSelected = index == selectedDistroIndex
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AetherCyan else AetherSurfaceElevated)
                                    .clickable { selectedDistroIndex = index }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = rule.first,
                                    color = if (isSelected) Color.Black else AetherTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Text("Target: ${activeRule.second}", color = AetherTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AetherSurfaceElevated)
                            .border(1.dp, AetherBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(activeRule.third, color = AetherCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                AetherPrimaryButton(
                    text = "Copy Command",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Firewall Command", activeRule.third))
                        Toast.makeText(context, "${activeRule.first} command copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showUfwDialog = false
                    }
                )
            },
            dismissButton = {
                AetherSecondaryButton(text = "Close", onClick = { showUfwDialog = false })
            }
        )
    }

    // 6. About VEXYRON Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = AetherCardBg,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_aetherlink_logo),
                        contentDescription = "VEXYRON Logo",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, AetherCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    )
                    Text("VEXYRON Ecosystem", color = AetherTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One Connection. Every Device.", color = AetherCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Version ${AppConstants.VERSION} (Build ${AppConstants.BUILD_NUMBER}) • Production", color = AetherGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("VEXYRON is an open cross-platform device ecosystem bridging Android with Ubuntu Linux, Windows, and macOS.", color = AetherTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Built with Kotlin, Jetpack Compose, Tauri 2.0, Tokio Async & Rust.", color = AetherTextMuted, fontSize = 11.sp)
                }
            },
            confirmButton = {
                AetherPrimaryButton(
                    text = "Check Updates",
                    onClick = {
                        showAboutDialog = false
                        UpdateManager.checkForUpdates(context, manual = true)
                    }
                )
            },
            dismissButton = {
                AetherSecondaryButton(text = "Close", onClick = { showAboutDialog = false })
            }
        )
    }

    // 7. PolyForm Noncommercial License Dialog
    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            containerColor = AetherCardBg,
            title = { Text("PolyForm Noncommercial 1.0.0", color = AetherTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text(
                        "PolyForm Noncommercial License 1.0.0\nhttps://polyformproject.org/licenses/noncommercial/1.0.0\n\nCopyright (c) 2026 Revanth H M\n\nThis license allows each user to run, copy, distribute, make available, and make derivative works of this software for noncommercial purposes.\n\nPersonal uses including research, experiment, testing, private study, hobby projects, and education are permitted purposes.",
                        color = AetherTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                AetherPrimaryButton(text = "Close", onClick = { showLicenseDialog = false })
            }
        )
    }

    // 8. Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = AetherCardBg,
            title = { Text("Clear Connection Cache?", color = AetherTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text("This will reset all remembered desktop IP addresses, device identifiers, and cached pairing keys.", color = AetherTextSecondary, fontSize = 12.sp)
            },
            confirmButton = {
                AetherPrimaryButton(
                    text = "Clear All",
                    onClick = {
                        prefs.edit().clear().apply()
                        Toast.makeText(context, "Connection cache cleared successfully!", Toast.LENGTH_SHORT).show()
                        showClearCacheDialog = false
                    }
                )
            },
            dismissButton = {
                AetherSecondaryButton(text = "Cancel", onClick = { showClearCacheDialog = false })
            }
        )
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        color = AetherTextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AetherTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = AetherTextMuted,
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AetherTextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = AetherTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = AetherTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AetherTextPrimary,
                checkedTrackColor = AetherCyan,
                uncheckedThumbColor = AetherTextMuted,
                uncheckedTrackColor = AetherSurfaceElevated
            )
        )
    }
}
