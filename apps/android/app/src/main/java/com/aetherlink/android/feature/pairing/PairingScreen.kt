package com.aetherlink.android.feature.pairing

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aetherlink.android.core.components.AetherCard
import com.aetherlink.android.core.components.AetherHeader
import com.aetherlink.android.core.components.AetherPrimaryButton
import com.aetherlink.android.core.components.AetherSecondaryButton
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCardBg
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGreen
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherRed
import com.aetherlink.android.core.designsystem.AetherSurfaceElevated
import com.aetherlink.android.core.designsystem.AetherTextMuted
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary
import com.aetherlink.android.core.utils.QrCodeScannerHelper
import com.aetherlink.android.domain.model.DeviceState
import com.aetherlink.android.viewmodel.HomeViewModel

@Composable
fun PairingScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var isQrMode by remember { mutableStateOf(false) }
    var directIpInput by remember { mutableStateOf("192.168.0.118") }
    var scanError by remember { mutableStateOf<String?>(null) }

    val launchScanner = {
        scanError = null
        QrCodeScannerHelper.startScan(
            context = context,
            onSuccess = { ip, port ->
                directIpInput = ip
                Toast.makeText(context, "Scanned $ip:$port! Connecting...", Toast.LENGTH_SHORT).show()
                vm.connectToIp(ip, port)
            },
            onError = { err ->
                scanError = err
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            AetherHeader(
                title = "Pair Device",
                onBack = { navController.popBackStack() }
            )
        }

        // SUCCESS STATE (If already paired / connected)
        if (vm.state == DeviceState.CONNECTED) {
            item {
                AetherCard(
                    borderColor = AetherGreen.copy(alpha = 0.5f),
                    contentPadding = 28.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AetherGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Paired",
                                tint = AetherGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Successfully Paired!",
                            color = AetherTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Connected securely to ${vm.devices.firstOrNull()?.name ?: "Desktop"}",
                            color = AetherTextSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        AetherPrimaryButton(
                            text = "Back to Home",
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // MODE SELECTOR: PIN Handshake vs QR Scanner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AetherSurfaceElevated)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isQrMode) AetherCardBg else AetherSurfaceElevated)
                            .border(
                                1.dp,
                                if (!isQrMode) AetherCyan.copy(alpha = 0.5f) else AetherSurfaceElevated,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isQrMode = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PIN Handshake",
                            color = if (!isQrMode) AetherTextPrimary else AetherTextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isQrMode) AetherCardBg else AetherSurfaceElevated)
                            .border(
                                1.dp,
                                if (isQrMode) AetherCyan.copy(alpha = 0.5f) else AetherSurfaceElevated,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isQrMode = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "QR Scanner & IP",
                            color = if (isQrMode) AetherTextPrimary else AetherTextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Error Message Banner if any
            if (vm.errorMessage != null || scanError != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AetherRed.copy(alpha = 0.12f))
                            .border(1.dp, AetherRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AetherRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = scanError ?: vm.errorMessage ?: "",
                                color = AetherTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // PIN CODE HANDSHAKE VIEW
            if (!isQrMode) {
                item {
                    AetherCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AetherPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pin,
                                        contentDescription = null,
                                        tint = AetherPurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "Secure PIN Verification",
                                        color = AetherTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Mutual code handshake with desktop",
                                        color = AetherTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (vm.state == DeviceState.PAIRING && vm.pairingPin != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AetherSurfaceElevated)
                                        .border(1.dp, AetherCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Verification Code",
                                            color = AetherTextMuted,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = vm.pairingPin ?: "----",
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AetherCyan,
                                            letterSpacing = 8.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AetherSecondaryButton(
                                        text = "Cancel",
                                        onClick = { vm.cancelPairing() },
                                        modifier = Modifier.weight(1f)
                                    )
                                    AetherPrimaryButton(
                                        text = "Confirm & Trust",
                                        onClick = { vm.confirmPairing() },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Ensure AetherLink Desktop is running on your laptop. Tap below to search and initiate handshake.",
                                    color = AetherTextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                AetherPrimaryButton(
                                    text = if (vm.state == DeviceState.SEARCHING) "Connecting..."
                                           else if (vm.devices.isNotEmpty()) "Pair with ${vm.devices.first().name}"
                                           else "Scan & Connect Desktop",
                                    onClick = {
                                        if (vm.devices.isNotEmpty()) vm.connectToDevice()
                                        else vm.scanDevices()
                                    },
                                    isLoading = vm.state == DeviceState.SEARCHING,
                                    enabled = vm.state != DeviceState.SEARCHING,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else {
                // QR SCANNER & DIRECT IP CARD
                item {
                    AetherCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Clickable Scanner Launch Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AetherSurfaceElevated)
                                    .border(1.dp, AetherCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .clickable { launchScanner() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(AetherCyan.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Scanner",
                                            tint = AetherCyan,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Tap to Launch Camera Scanner",
                                        color = AetherTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Scan the QR code shown on AetherLink Desktop",
                                        color = AetherTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action button to start scanner explicitly
                            AetherPrimaryButton(
                                text = "Open Camera Scanner",
                                onClick = { launchScanner() },
                                modifier = Modifier.fillMaxWidth(),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = AetherTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Direct IP Fallback Section
                            Text(
                                text = "Or connect manually by IP address:",
                                color = AetherTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = directIpInput,
                                onValueChange = { directIpInput = it },
                                placeholder = { Text("192.168.0.118", color = AetherTextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AetherCyan,
                                    unfocusedBorderColor = AetherBorder,
                                    focusedTextColor = AetherTextPrimary,
                                    unfocusedTextColor = AetherTextPrimary,
                                    focusedContainerColor = AetherSurfaceElevated,
                                    unfocusedContainerColor = AetherSurfaceElevated
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            AetherSecondaryButton(
                                text = if (vm.state == DeviceState.SEARCHING) "Connecting..." else "Connect to IP",
                                onClick = {
                                    if (directIpInput.isNotBlank()) {
                                        vm.connectToIp(directIpInput.trim())
                                    }
                                },
                                isLoading = vm.state == DeviceState.SEARCHING,
                                enabled = vm.state != DeviceState.SEARCHING,
                                modifier = Modifier.fillMaxWidth(),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Lan,
                                        contentDescription = null,
                                        tint = AetherCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // SECURITY NOTICE CARD
            item {
                AetherCard(
                    backgroundColor = AetherSurfaceElevated.copy(alpha = 0.4f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = AetherGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "End-to-end encrypted local channel. No data leaves your Wi-Fi network.",
                            color = AetherTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
