package com.vexyron.android.feature.mirroring

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
import com.vexyron.android.core.components.VexyronCard
import com.vexyron.android.core.components.VexyronHeader
import com.vexyron.android.core.components.VexyronPrimaryButton
import com.vexyron.android.core.components.VexyronSecondaryButton
import com.vexyron.android.core.components.VexyronStatusBadge
import com.vexyron.android.core.designsystem.VexyronBg
import com.vexyron.android.core.designsystem.VexyronBorder
import com.vexyron.android.core.designsystem.VexyronCyan
import com.vexyron.android.core.designsystem.VexyronGreen
import com.vexyron.android.core.designsystem.VexyronOrange
import com.vexyron.android.core.designsystem.VexyronRed
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary
import com.vexyron.android.domain.model.DeviceState
import com.vexyron.android.viewmodel.HomeViewModel

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
            .background(VexyronBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            VexyronHeader(
                title = "Screen Mirroring",
                onBack = { navController.popBackStack() }
            )
        }

        // DESKTOP STREAMING STATUS CARD
        item {
            VexyronCard(
                borderColor = if (isMirroringActive) VexyronOrange.copy(alpha = 0.5f) else VexyronBorder
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
                                        if (isMirroringActive) VexyronOrange.copy(alpha = 0.15f)
                                        else VexyronSurfaceElevated
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMirroringActive) Icons.Default.CastConnected else Icons.Default.Cast,
                                    contentDescription = null,
                                    tint = if (isMirroringActive) VexyronOrange else VexyronTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = if (isMirroringActive) "Streaming to Desktop"
                                    else if (isConnected) "Ready to Mirror"
                                    else "No Desktop Connected",
                                    color = VexyronTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isConnected) "${vm.devices.firstOrNull()?.name ?: "Desktop"} • WebRTC Ready"
                                    else "Pair device first to enable mirroring",
                                    color = VexyronTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        VexyronStatusBadge(
                            text = if (isMirroringActive) "Live" else if (isConnected) "Standby" else "Offline",
                            indicatorColor = if (isMirroringActive) VexyronOrange else if (isConnected) VexyronGreen else VexyronRed
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isMirroringActive) {
                        VexyronSecondaryButton(
                            text = "Stop Mirroring",
                            onClick = { isMirroringActive = false },
                            modifier = Modifier.fillMaxWidth(),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = VexyronRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    } else {
                        VexyronPrimaryButton(
                            text = "Start Mirroring",
                            onClick = { isMirroringActive = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isConnected,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Cast,
                                    contentDescription = null,
                                    tint = VexyronTextPrimary,
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
                color = VexyronTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            VexyronCard {
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
                                tint = VexyronCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Resolution",
                                color = VexyronTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("720p", "1080p", "4K").forEach { res ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedResolution == res) VexyronCyan.copy(alpha = 0.2f) else VexyronSurfaceElevated)
                                        .border(1.dp, if (selectedResolution == res) VexyronCyan else VexyronBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedResolution = res }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = res,
                                        color = if (selectedResolution == res) VexyronCyan else VexyronTextSecondary,
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
                                tint = VexyronGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Frame Rate",
                                color = VexyronTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("30 FPS", "60 FPS").forEach { fps ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedFps == fps) VexyronGreen.copy(alpha = 0.2f) else VexyronSurfaceElevated)
                                        .border(1.dp, if (selectedFps == fps) VexyronGreen else VexyronBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedFps = fps }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = fps,
                                        color = if (selectedFps == fps) VexyronGreen else VexyronTextSecondary,
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
                                tint = VexyronOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Auto Fullscreen on Desktop",
                                color = VexyronTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Switch(
                            checked = isFullscreen,
                            onCheckedChange = { isFullscreen = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VexyronTextPrimary,
                                checkedTrackColor = VexyronCyan,
                                uncheckedThumbColor = VexyronTextMuted,
                                uncheckedTrackColor = VexyronSurfaceElevated
                            )
                        )
                    }
                }
            }
        }
    }
}
