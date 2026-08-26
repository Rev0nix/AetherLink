package com.aetherlink.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aetherlink.android.ui.components.StatusCard
import com.aetherlink.android.viewmodel.HomeViewModel
import com.aetherlink.android.domain.model.DeviceState
import android.util.Log

@Composable
fun HomeScreen() {

    val vm: HomeViewModel = viewModel()
    Log.d("AetherLink", "HomeScreen recomposed")
    Log.d("AetherLink", "Current state = ${vm.state}")
    val message = when (vm.state) {

        DeviceState.DISCONNECTED ->
            "No Desktop Connected"

        DeviceState.SEARCHING ->
            "Searching for desktop..."

        DeviceState.FOUND ->
            "Desktop Found"

        DeviceState.CONNECTED ->
            "Connected"
    }
    val deviceName =
        vm.devices.firstOrNull()?.name ?: "Unknown Device"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 56.dp,
                bottom = 24.dp
            )
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "AetherLink",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Android Companion",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        StatusCard(
            title = "Connection Status",
            message = message
        )
        Spacer(modifier = Modifier.height(20.dp))

        when (vm.state) {

            DeviceState.DISCONNECTED -> {

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.connectToDevice()
                    }
                ) {
                    Text("Connect")
                }
            }

            DeviceState.SEARCHING -> {

                Text(
                    text = "Searching for desktop...",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            DeviceState.FOUND -> {

                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.scanDevices()
                    }
                ) {
                    Text("Scan for Devices")
                }
            }

            DeviceState.CONNECTED -> {

                Text(
                    text = "Connected to $deviceName",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}