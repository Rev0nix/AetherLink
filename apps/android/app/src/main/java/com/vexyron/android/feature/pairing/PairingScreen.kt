package com.vexyron.android.feature.pairing

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
import com.vexyron.android.core.components.VexyronCard
import com.vexyron.android.core.components.VexyronHeader
import com.vexyron.android.core.components.VexyronPrimaryButton
import com.vexyron.android.core.components.VexyronSecondaryButton
import com.vexyron.android.core.designsystem.VexyronBg
import com.vexyron.android.core.designsystem.VexyronBorder
import com.vexyron.android.core.designsystem.VexyronCardBg
import com.vexyron.android.core.designsystem.VexyronCyan
import com.vexyron.android.core.designsystem.VexyronGreen
import com.vexyron.android.core.designsystem.VexyronPurple
import com.vexyron.android.core.designsystem.VexyronRed
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary
import com.vexyron.android.core.utils.QrCodeScannerHelper
import com.vexyron.android.domain.model.DeviceState
import com.vexyron.android.viewmodel.HomeViewModel

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
            .background(VexyronBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            VexyronHeader(
                title = "Pair Device",
                onBack = { navController.popBackStack() }
            )
        }

        // SUCCESS STATE (If already paired / connected)
        if (vm.state == DeviceState.CONNECTED) {
            item {
                VexyronCard(
                    borderColor = VexyronGreen.copy(alpha = 0.5f),
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
                                .background(VexyronGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Paired",
                                tint = VexyronGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Successfully Paired!",
                            color = VexyronTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Connected securely to ${vm.devices.firstOrNull()?.name ?: "Desktop"}",
                            color = VexyronTextSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        VexyronPrimaryButton(
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
                        .background(VexyronSurfaceElevated)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isQrMode) VexyronCardBg else VexyronSurfaceElevated)
                            .border(
                                1.dp,
                                if (!isQrMode) VexyronCyan.copy(alpha = 0.5f) else VexyronSurfaceElevated,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isQrMode = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PIN Handshake",
                            color = if (!isQrMode) VexyronTextPrimary else VexyronTextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isQrMode) VexyronCardBg else VexyronSurfaceElevated)
                            .border(
                                1.dp,
                                if (isQrMode) VexyronCyan.copy(alpha = 0.5f) else VexyronSurfaceElevated,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isQrMode = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "QR Scanner & IP",
                            color = if (isQrMode) VexyronTextPrimary else VexyronTextMuted,
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
                            .background(VexyronRed.copy(alpha = 0.12f))
                            .border(1.dp, VexyronRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = VexyronRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = scanError ?: vm.errorMessage ?: "",
                                color = VexyronTextPrimary,
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
                    VexyronCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VexyronPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pin,
                                        contentDescription = null,
                                        tint = VexyronPurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "Secure PIN Verification",
                                        color = VexyronTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Mutual code handshake with desktop",
                                        color = VexyronTextMuted,
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
                                        .background(VexyronSurfaceElevated)
                                        .border(1.dp, VexyronCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Verification Code",
                                            color = VexyronTextMuted,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = vm.pairingPin ?: "----",
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = VexyronCyan,
                                            letterSpacing = 8.sp
                                        )
                                    }
                                }

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
                            } else {
                                Text(
                                    text = "Ensure VEXYRON Desktop is running on your laptop. Tap below to search and initiate handshake.",
                                    color = VexyronTextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                VexyronPrimaryButton(
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
                    VexyronCard {
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
                                    .background(VexyronSurfaceElevated)
                                    .border(1.dp, VexyronCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .clickable { launchScanner() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(VexyronCyan.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Scanner",
                                            tint = VexyronCyan,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Tap to Launch Camera Scanner",
                                        color = VexyronTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Scan the QR code shown on VEXYRON Desktop",
                                        color = VexyronTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action button to start scanner explicitly
                            VexyronPrimaryButton(
                                text = "Open Camera Scanner",
                                onClick = { launchScanner() },
                                modifier = Modifier.fillMaxWidth(),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = VexyronTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Direct IP Fallback Section
                            Text(
                                text = "Or connect manually by IP address:",
                                color = VexyronTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = directIpInput,
                                onValueChange = { directIpInput = it },
                                placeholder = { Text("192.168.0.118", color = VexyronTextMuted) },
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

                            Spacer(modifier = Modifier.height(12.dp))

                            VexyronSecondaryButton(
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
                                        tint = VexyronCyan,
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
                VexyronCard(
                    backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.4f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = VexyronGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "End-to-end encrypted local channel. No data leaves your Wi-Fi network.",
                            color = VexyronTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
