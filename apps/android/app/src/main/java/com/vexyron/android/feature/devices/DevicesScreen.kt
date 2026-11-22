package com.vexyron.android.feature.devices

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.vexyron.android.core.components.VexyronCard
import com.vexyron.android.core.components.VexyronHeader
import com.vexyron.android.core.components.VexyronPrimaryButton
import com.vexyron.android.core.components.VexyronSecondaryButton
import com.vexyron.android.core.components.VexyronStatusBadge
import com.vexyron.android.core.designsystem.VexyronBg
import com.vexyron.android.core.designsystem.VexyronBorder
import com.vexyron.android.core.designsystem.VexyronCyan
import com.vexyron.android.core.designsystem.VexyronGreen
import com.vexyron.android.core.designsystem.VexyronRed
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary
import com.vexyron.android.domain.model.Device
import com.vexyron.android.domain.model.DeviceState
import com.vexyron.android.service.network.ConnectionManager
import com.vexyron.android.ui.navigation.Screen
import com.vexyron.android.viewmodel.HomeViewModel

@Composable
fun DevicesScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    val isConnected = vm.state == DeviceState.CONNECTED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VexyronBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // TOP APP HEADER
        item {
            VexyronHeader(
                title = "Devices",
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VexyronSurfaceElevated)
                                .border(1.dp, VexyronBorder, CircleShape)
                                .clickable { navController.navigate(Screen.Pairing.route) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = VexyronCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }

        // TOP ACTION BUTTONS (Pair New Device & Scan)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VexyronPrimaryButton(
                    text = "Pair New Device",
                    onClick = { navController.navigate(Screen.Pairing.route) },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = VexyronTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                VexyronSecondaryButton(
                    text = if (vm.state == DeviceState.SEARCHING) "Scanning..." else "Scan Network",
                    onClick = { vm.scanDevices() },
                    isLoading = vm.state == DeviceState.SEARCHING,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = VexyronTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // 1. ACTIVE / PAIRED DEVICE SECTION
        item {
            Text(
                text = "Paired Devices",
                color = VexyronTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (vm.devices.isEmpty()) {
            item {
                EmptyDevicesState(
                    onScan = { vm.scanDevices() }
                )
            }
        } else {
            items(vm.devices) { device ->
                DeviceItemCard(
                    device = device,
                    isConnected = isConnected,
                    onConnect = { vm.connectToDevice(device) },
                    onDisconnect = { vm.disconnect() },
                    onOpenTerminal = { navController.navigate(Screen.Terminal.route) }
                )
            }
        }

        // 2. NEARBY WI-FI / MDNS DISCOVERY SECTION (Properly Arranged Badge)
        item {
            Text(
                text = "Nearby Network Discovery",
                color = VexyronTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            VexyronCard(
                backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.4f)
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
                                .background(VexyronCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = VexyronCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Local mDNS Service",
                                color = VexyronTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Listening on _vexyron._tcp",
                                color = VexyronTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    VexyronStatusBadge(
                        text = if (vm.state == DeviceState.SEARCHING) "Scanning" else "Broadcasting",
                        indicatorColor = VexyronCyan
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceItemCard(
    device: Device,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenTerminal: () -> Unit = {}
) {
    VexyronCard(
        borderColor = if (isConnected) VexyronGreen.copy(alpha = 0.4f) else VexyronBorder
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            contentDescription = null,
                            tint = if (isConnected) VexyronGreen else VexyronTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = device.name,
                            color = VexyronTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${device.ipAddress} • Port 45876",
                            color = VexyronTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val hostOs = if (device.os != "Unknown") device.os else ConnectionManager.hostOs.value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isConnected && hostOs != "Unknown") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF0078D4).copy(alpha = 0.2f) else VexyronGreen.copy(alpha = 0.2f))
                                .border(1.dp, if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF0078D4) else VexyronGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (hostOs.equals("Windows", ignoreCase = true)) "Windows" else "Linux",
                                color = if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF60CDFF) else VexyronGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    VexyronStatusBadge(
                        text = if (isConnected) "Connected" else "Available",
                        indicatorColor = if (isConnected) VexyronGreen else VexyronCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isConnected) {
                    VexyronPrimaryButton(
                        text = "Terminal",
                        onClick = onOpenTerminal,
                        modifier = Modifier.weight(1f)
                    )
                    VexyronSecondaryButton(
                        text = "Disconnect",
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    VexyronPrimaryButton(
                        text = "Connect",
                        onClick = onConnect,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDevicesState(
    onScan: () -> Unit
) {
    VexyronCard(contentPadding = 24.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VexyronSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = VexyronTextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No Paired Devices Found",
                color = VexyronTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Scan your local Wi-Fi network or tap 'Pair New Device' to link your laptop.",
                color = VexyronTextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            VexyronSecondaryButton(
                text = "Scan Network",
                onClick = onScan,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
