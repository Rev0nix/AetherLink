package com.vexyron.android.feature.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vexyron.android.service.network.ConnectionManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vexyron.android.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.vexyron.android.core.components.VexyronCard
import com.vexyron.android.core.components.VexyronPrimaryButton
import com.vexyron.android.core.components.VexyronSecondaryButton
import com.vexyron.android.core.components.VexyronStatusBadge
import com.vexyron.android.core.designsystem.VexyronBg
import com.vexyron.android.core.designsystem.VexyronBorder
import com.vexyron.android.core.designsystem.VexyronCardBg
import com.vexyron.android.core.designsystem.VexyronCyan
import com.vexyron.android.core.designsystem.VexyronGreen
import com.vexyron.android.core.designsystem.VexyronOrange
import com.vexyron.android.core.designsystem.VexyronPurple
import com.vexyron.android.core.designsystem.VexyronRed
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary
import com.vexyron.android.domain.model.DeviceState
import com.vexyron.android.ui.navigation.Screen
import com.vexyron.android.viewmodel.HomeViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val batteryLevel = remember {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) (level * 100 / scale) else 85
    }

    val isConnected = vm.state == DeviceState.CONNECTED
    val desktopDevice = vm.devices.firstOrNull()
    val hostOs by ConnectionManager.hostOs.collectAsState()
    val hostName by ConnectionManager.hostName.collectAsState()

    var showIpDialog by remember { mutableStateOf(false) }
    val defaultIp = remember {
        val isEmu = Build.FINGERPRINT.startsWith("generic")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.lowercase().contains("emulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
        if (isEmu) "10.0.2.2"
        else {
            val prefs = context.getSharedPreferences("vexyron_prefs", Context.MODE_PRIVATE)
            prefs.getString("last_connected_ip", "192.168.0.118") ?: "192.168.0.118"
        }
    }
    var manualIpInput by remember { mutableStateOf(defaultIp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VexyronBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. TOP HEADER SECTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        color = VexyronTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vexyron_logo),
                            contentDescription = "VEXYRON Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, VexyronCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VEXYRON",
                                color = VexyronTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(VexyronCyan)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Notification Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(VexyronCardBg)
                            .border(1.dp, VexyronBorder, CircleShape)
                            .clickable { navController.navigate(Screen.Notifications.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = VexyronTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings / Profile Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(VexyronCardBg)
                            .border(1.dp, VexyronBorder, CircleShape)
                            .clickable {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = VexyronTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. PAIRING PIN CARD (When in pairing verification state)
        if (vm.state == DeviceState.PAIRING) {
            item {
                VexyronCard(
                    borderColor = VexyronCyan.copy(alpha = 0.5f),
                    backgroundColor = VexyronSurfaceElevated
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VexyronStatusBadge(
                            text = "PAIRING VERIFICATION",
                            indicatorColor = VexyronCyan
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = vm.pairingPin ?: "----",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VexyronCyan,
                            letterSpacing = 8.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Confirm this 4-digit PIN matches your laptop screen",
                            color = VexyronTextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VexyronSecondaryButton(
                                text = "Cancel",
                                onClick = { vm.cancelPairing() },
                                modifier = Modifier.weight(1f)
                            )
                            VexyronPrimaryButton(
                                text = "Confirm & Trust",
                                onClick = { vm.confirmPairing() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. MAIN CONNECTION CARD (Properly Arranged Badge & Buttons)
        item {
            VexyronCard(
                borderColor = if (isConnected) VexyronGreen.copy(alpha = 0.4f) else VexyronBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row: Computer icon + text on the left (weight 1f) + Status Badge on the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isConnected) VexyronGreen.copy(alpha = 0.15f)
                                        else VexyronSurfaceElevated
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Computer,
                                    contentDescription = "Desktop",
                                    tint = if (isConnected) VexyronGreen else VexyronTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                val displayDeviceName = if (isConnected) {
                                    if (hostName.isNotBlank() && hostName != "Laptop") {
                                        "$hostName ($hostOs)"
                                    } else if (hostOs != "Unknown") {
                                        "VEXYRON $hostOs Laptop"
                                    } else {
                                        desktopDevice?.name ?: "VEXYRON Desktop"
                                    }
                                } else if (vm.devices.isNotEmpty()) vm.devices.first().name
                                else "No Desktop Connected"

                                Text(
                                    text = displayDeviceName,
                                    color = VexyronTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isConnected) {
                                        if (hostOs != "Unknown") "Connected to $hostOs Laptop • Wi-Fi LAN"
                                        else "Connected • Wi-Fi LAN"
                                    }
                                    else if (vm.state == DeviceState.SEARCHING) "Scanning network on port 45876..."
                                    else "Tap Connect or enter IP to link",
                                    color = VexyronTextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Status Badge & OS Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isConnected && hostOs != "Unknown") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF0078D4).copy(alpha = 0.2f) else VexyronGreen.copy(alpha = 0.2f))
                                        .border(1.dp, if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF0078D4) else VexyronGreen, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (hostOs.equals("Windows", ignoreCase = true)) "🪟 Windows" else "🐧 Linux",
                                        color = if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF60CDFF) else VexyronGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            VexyronStatusBadge(
                                text = when (vm.state) {
                                    DeviceState.CONNECTED -> "Online"
                                    DeviceState.SEARCHING -> "Scanning"
                                    DeviceState.FOUND -> "Available"
                                    DeviceState.PAIRING -> "Pairing"
                                    DeviceState.DISCONNECTED -> "Offline"
                                },
                                indicatorColor = when (vm.state) {
                                    DeviceState.CONNECTED -> VexyronGreen
                                    DeviceState.SEARCHING, DeviceState.PAIRING -> VexyronCyan
                                    DeviceState.FOUND -> VexyronOrange
                                    DeviceState.DISCONNECTED -> VexyronRed
                                }
                            )
                        }
                    }

                    // Error Message notice if connection attempt failed
                    if (vm.errorMessage != null && !isConnected) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VexyronRed.copy(alpha = 0.12f))
                                .border(1.dp, VexyronRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = VexyronRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = vm.errorMessage ?: "",
                                    color = VexyronTextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons Row (Clean 50/50 arrangement)
                    if (isConnected) {
                        VexyronSecondaryButton(
                            text = "Disconnect",
                            onClick = { vm.disconnect() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VexyronPrimaryButton(
                                text = if (vm.state == DeviceState.SEARCHING) "Scanning..."
                                       else if (vm.devices.isNotEmpty()) "Connect"
                                       else "Scan & Connect",
                                onClick = { vm.connectToDevice() },
                                isLoading = vm.state == DeviceState.SEARCHING,
                                enabled = vm.state != DeviceState.SEARCHING,
                                modifier = Modifier.weight(1f),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = VexyronTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            VexyronSecondaryButton(
                                text = "Direct IP",
                                onClick = { showIpDialog = true },
                                modifier = Modifier.weight(1f),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Lan,
                                        contentDescription = null,
                                        tint = VexyronCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. QUICK ACTIONS SECTION
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    color = VexyronTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Send Files",
                        icon = Icons.Default.Upload,
                        accentColor = VexyronPurple,
                        modifier = Modifier.weight(1f)
                    ) {
                        navController.navigate(Screen.Files.route)
                    }

                    QuickActionCard(
                        title = "Receive",
                        icon = Icons.Default.Download,
                        accentColor = VexyronCyan,
                        modifier = Modifier.weight(1f)
                    ) {
                        navController.navigate(Screen.Files.route)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Mirror Screen",
                        icon = Icons.Default.Cast,
                        accentColor = VexyronOrange,
                        modifier = Modifier.weight(1f)
                    ) {
                        navController.navigate(Screen.Mirroring.route)
                    }

                    QuickActionCard(
                        title = "Clipboard Sync",
                        icon = Icons.Default.ContentCopy,
                        accentColor = VexyronGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        navController.navigate(Screen.Clipboard.route)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Remote Terminal",
                        icon = Icons.Default.Terminal,
                        accentColor = VexyronCyan,
                        modifier = Modifier.weight(1f)
                    ) {
                        navController.navigate(Screen.Terminal.route)
                    }

                    QuickActionCard(
                        title = "Notifications",
                        icon = Icons.Default.Notifications,
                        accentColor = VexyronPurple,
                        modifier = Modifier.weight(1f)
                    ) {
                        navController.navigate(Screen.Notifications.route)
                    }
                }
            }
        }

        // 5. DEVICE STATUS SECTION
        item {
            Column {
                Text(
                    text = "Device Status",
                    color = VexyronTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusMetricCard(
                        title = "Battery",
                        value = "$batteryLevel%",
                        subtext = "Healthy • Android",
                        icon = Icons.Default.BatteryChargingFull,
                        tint = VexyronGreen,
                        modifier = Modifier.weight(1f)
                    )

                    StatusMetricCard(
                        title = "Network",
                        value = if (isConnected) "Wi-Fi LAN" else "Local Socket",
                        subtext = "Port 45876",
                        icon = Icons.Default.Wifi,
                        tint = VexyronCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. RECENT ACTIVITY SECTION
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        color = VexyronTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "View All",
                        color = VexyronCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { navController.navigate(Screen.Files.route) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecentActivityRow(
                        title = "IMG_20261120_Desktop.png",
                        subtitle = "Received • 4.2 MB",
                        time = "10m ago",
                        icon = Icons.Default.Image,
                        iconTint = VexyronCyan
                    )

                    RecentActivityRow(
                        title = "Project_Archive.tar.gz",
                        subtitle = "Sent to Desktop • 18.4 MB",
                        time = "1h ago",
                        icon = Icons.Default.FolderZip,
                        iconTint = VexyronPurple
                    )

                    RecentActivityRow(
                        title = "Pairing Handshake Confirmed",
                        subtitle = "VEXYRON Desktop (Linux)",
                        time = "Today",
                        icon = Icons.Default.CheckCircle,
                        iconTint = VexyronGreen
                    )
                }
            }
        }
    }

    // Direct IP Connection Dialog
    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            containerColor = VexyronCardBg,
            title = {
                Text(
                    text = "Connect by IP Address",
                    color = VexyronTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your laptop's LAN IP address (typically 192.168.0.118, or 10.0.2.2 for Android Studio emulator).",
                        color = VexyronTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { manualIpInput = it },
                        placeholder = { Text("e.g. 192.168.0.118", color = VexyronTextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VexyronCyan,
                            unfocusedBorderColor = VexyronBorder,
                            focusedTextColor = VexyronTextPrimary,
                            unfocusedTextColor = VexyronTextPrimary,
                            focusedContainerColor = VexyronSurfaceElevated,
                            unfocusedContainerColor = VexyronSurfaceElevated
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VexyronPrimaryButton(
                    text = "Connect",
                    onClick = {
                        if (manualIpInput.isNotBlank()) {
                            vm.connectToIp(manualIpInput.trim())
                            showIpDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                VexyronSecondaryButton(
                    text = "Cancel",
                    onClick = { showIpDialog = false }
                )
            }
        )
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    VexyronCard(
        modifier = modifier.clickable { onClick() },
        contentPadding = 14.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                color = VexyronTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StatusMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    VexyronCard(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = VexyronTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                color = VexyronTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                color = VexyronTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun RecentActivityRow(
    title: String,
    subtitle: String,
    time: String,
    icon: ImageVector,
    iconTint: Color
) {
    VexyronCard(
        contentPadding = 12.dp,
        backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    color = VexyronTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = VexyronTextSecondary,
                    fontSize = 12.sp
                )
            }

            Text(
                text = time,
                color = VexyronTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
