package com.aetherlink.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aetherlink.android.data.repository.DiscoveryRepositoryImpl
import com.aetherlink.android.domain.model.Device
import com.aetherlink.android.domain.model.DeviceState
import com.aetherlink.android.domain.usecase.DiscoverDevicesUseCase
import com.aetherlink.android.service.DiscoveryService
import com.aetherlink.android.service.network.TcpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val discoveryService =
        DiscoveryService(application.applicationContext)

    private val repository =
        DiscoveryRepositoryImpl(discoveryService)

    private val discoverDevicesUseCase =
        DiscoverDevicesUseCase(repository)

    var state by mutableStateOf(DeviceState.DISCONNECTED)
        private set

    var devices by mutableStateOf<List<Device>>(emptyList())
        private set

    fun scanDevices() {

        viewModelScope.launch {

            Log.d("AetherLink", "Starting device scan")

            state = DeviceState.SEARCHING

            devices = discoverDevicesUseCase()

            Log.d(
                "AetherLink",
                "Devices found = ${devices.size}"
            )

            state =
                if (devices.isNotEmpty())
                    DeviceState.FOUND
                else
                    DeviceState.DISCONNECTED

            Log.d("AetherLink", "State after scan = $state")
        }
    }

    fun connectToDevice() {

        Log.d("AetherLink", "===== CONNECT BUTTON PRESSED =====")

        viewModelScope.launch {

            Log.d("AetherLink", "Entered coroutine")

            state = DeviceState.SEARCHING

            Log.d("AetherLink", "State = $state")

            val connected = withContext(Dispatchers.IO) {

                try {

                    Log.d("AetherLink", "Creating TcpClient")

                    val client = TcpClient(
                        host = "172.20.161.249",
                        port = 45876
                    )

                    Log.d("AetherLink", "Trying to connect...")

                    if (!client.connect()) {

                        Log.e("AetherLink", "Connection failed")

                        return@withContext false
                    }

                    Log.d("AetherLink", "Connected!")

                    client.send("HELLO")

                    Log.d("AetherLink", "HELLO sent")

                    val reply = client.receive()

                    Log.d("AetherLink", "Server reply = $reply")

                    reply?.trim()?.startsWith("WELCOME") == true

                } catch (e: Exception) {

                    Log.e("AetherLink", "TCP Error", e)

                    false
                }
            }

            Log.d("AetherLink", "connected = $connected")

            if (connected) {

                Log.d("AetherLink", "Updating device list")

                devices = listOf(
                    Device(
                        id = "desktop-001",
                        name = "AetherLink Desktop",
                        ipAddress = "172.20.161.249",
                        isConnected = true
                    )
                )

                Log.d("AetherLink", "Changing state to CONNECTED")

                state = DeviceState.CONNECTED

                Log.d("AetherLink", "Current state = $state")

            } else {

                Log.d("AetherLink", "Changing state to DISCONNECTED")

                state = DeviceState.DISCONNECTED

                Log.d("AetherLink", "Current state = $state")
            }
        }
    }

    fun disconnect() {

        Log.d("AetherLink", "Disconnect pressed")

        devices = emptyList()

        state = DeviceState.DISCONNECTED

        Log.d("AetherLink", "Current state = $state")
    }
}