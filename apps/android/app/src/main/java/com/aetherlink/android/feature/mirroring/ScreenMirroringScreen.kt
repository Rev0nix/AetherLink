package com.aetherlink.android.feature.mirroring

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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aetherlink.android.core.components.AetherCard
import com.aetherlink.android.core.components.AetherHeader
import com.aetherlink.android.core.components.AetherPrimaryButton
import com.aetherlink.android.core.components.AetherSecondaryButton
import com.aetherlink.android.core.components.AetherStatusBadge
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGreen
import com.aetherlink.android.core.designsystem.AetherOrange
import com.aetherlink.android.core.designsystem.AetherRed
import com.aetherlink.android.core.designsystem.AetherSurfaceElevated
import com.aetherlink.android.core.designsystem.AetherTextMuted
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary
import com.aetherlink.android.domain.model.DeviceState
import com.aetherlink.android.viewmodel.HomeViewModel

@Composable
fun ScreenMirroringScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    var isMirroringActive by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf("60 FPS") }
    var isFullscreen by remember { mutableStateOf(false) }

    val isConnected = vm.state == DeviceState.CONNECTED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            AetherHeader(
                title = "Screen Mirroring",
                onBack = { navController.popBackStack() }
            )
        }

        // DESKTOP STREAMING STATUS CARD
        item {
            AetherCard(
                borderColor = if (isMirroringActive) AetherOrange.copy(alpha = 0.5f) else AetherBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isMirroringActive) AetherOrange.copy(alpha = 0.15f)
                                        else AetherSurfaceElevated
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMirroringActive) Icons.Default.CastConnected else Icons.Default.Cast,
                                    contentDescription = null,
                                    tint = if (isMirroringActive) AetherOrange else AetherTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = if (isMirroringActive) "Streaming to Desktop"
                                    else if (isConnected) "Ready to Mirror"
                                    else "No Desktop Connected",
                                    color = AetherTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isConnected) "${vm.devices.firstOrNull()?.name ?: "Desktop"} • WebRTC Ready"
                                    else "Pair device first to enable mirroring",
                                    color = AetherTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        AetherStatusBadge(
                            text = if (isMirroringActive) "Live" else if (isConnected) "Standby" else "Offline",
                            indicatorColor = if (isMirroringActive) AetherOrange else if (isConnected) AetherGreen else AetherRed
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isMirroringActive) {
                        AetherSecondaryButton(
                            text = "Stop Mirroring",
                            onClick = { isMirroringActive = false },
                            modifier = Modifier.fillMaxWidth(),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = AetherRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    } else {
                        AetherPrimaryButton(
                            text = "Start Mirroring",
                            onClick = { isMirroringActive = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isConnected,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Cast,
                                    contentDescription = null,
                                    tint = AetherTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // MIRRORING SETTINGS
        item {
            Text(
                text = "Stream Configuration",
                color = AetherTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            AetherCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Resolution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = AetherCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Resolution",
                                color = AetherTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("720p", "1080p", "4K").forEach { res ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedResolution == res) AetherCyan.copy(alpha = 0.2f) else AetherSurfaceElevated)
                                        .border(1.dp, if (selectedResolution == res) AetherCyan else AetherBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedResolution = res }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = res,
                                        color = if (selectedResolution == res) AetherCyan else AetherTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // FPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = AetherGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Frame Rate",
                                color = AetherTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("30 FPS", "60 FPS").forEach { fps ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedFps == fps) AetherGreen.copy(alpha = 0.2f) else AetherSurfaceElevated)
                                        .border(1.dp, if (selectedFps == fps) AetherGreen else AetherBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedFps = fps }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = fps,
                                        color = if (selectedFps == fps) AetherGreen else AetherTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Fullscreen Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = AetherOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Auto Fullscreen on Desktop",
                                color = AetherTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Switch(
                            checked = isFullscreen,
                            onCheckedChange = { isFullscreen = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AetherTextPrimary,
                                checkedTrackColor = AetherCyan,
                                uncheckedThumbColor = AetherTextMuted,
                                uncheckedTrackColor = AetherSurfaceElevated
                            )
                        )
                    }
                }
            }
        }
    }
}
