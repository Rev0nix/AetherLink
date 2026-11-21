package com.aetherlink.android.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
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
import com.aetherlink.android.protocol.Message
import com.aetherlink.android.protocol.MessageType
import com.aetherlink.android.protocol.PairRequestPayload
import com.aetherlink.android.protocol.PairResponsePayload
import com.aetherlink.android.protocol.PairStatus
import com.aetherlink.android.service.DiscoveryService
import com.aetherlink.android.service.network.ConnectionManager
import com.aetherlink.android.service.network.TcpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.UUID

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

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

    var pairingPin by mutableStateOf<String?>(null)
        private set

    var pairingMessage by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var activeClient: TcpClient? = null

    init {
        viewModelScope.launch {
            ConnectionManager.connectionState.collect { connected ->
                if (connected) {
                    state = DeviceState.CONNECTED
                    devices = devices.map { it.copy(isConnected = true) }
                } else if (state == DeviceState.CONNECTED) {
                    state = DeviceState.DISCONNECTED
                    devices = devices.map { it.copy(isConnected = false) }
                }
            }
        }
    }

    private fun getOrCreateDeviceId(): String {
        val prefs = getApplication<Application>()
            .getSharedPreferences("aetherlink_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id.isNullOrBlank()) {
            val androidId = try {
                android.provider.Settings.Secure.getString(
                    getApplication<Application>().contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
            } catch (_: Exception) {
                null
            }
            id = if (!androidId.isNullOrBlank()) "android_$androidId" else UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.lowercase().contains("emulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk")
    }

    fun clearError() {
        errorMessage = null
    }

    fun scanDevices() {
        viewModelScope.launch {
            Log.d("AetherLink", "Starting wireless device scan")
            state = DeviceState.SEARCHING
            errorMessage = null

            try {
                val found = discoverDevicesUseCase()
                Log.d("AetherLink", "Wireless devices found = ${found.size}")
                devices = found

                if (found.isNotEmpty()) {
                    state = DeviceState.FOUND
                } else {
                    state = DeviceState.DISCONNECTED
                    errorMessage = "No desktop found. Tap 'Direct IP' to connect directly (192.168.0.118)."
                }
            } catch (e: Exception) {
                Log.e("AetherLink", "Discovery failed", e)
                devices = emptyList()
                state = DeviceState.DISCONNECTED
                errorMessage = "Discovery error: ${e.message}"
            }
        }
    }

    fun connectToIp(ip: String, port: Int = 45876) {
        viewModelScope.launch {
            Log.d("AetherLink", "Direct connection to $ip:$port")
            errorMessage = null
            val directDevice = Device(
                id = "$ip:$port",
                name = if (ip == "10.0.2.2") "AetherLink Desktop (Host)" else "AetherLink Desktop ($ip)",
                ipAddress = ip,
                isConnected = false
            )
            devices = listOf(directDevice)
            connectToDevice(directDevice)
        }
    }

    fun connectToDevice(device: Device? = null) {
        viewModelScope.launch {
            Log.d("AetherLink", "===== WIRELESS PROTOCOL CONNECT =====")
            errorMessage = null

            var target = device ?: devices.firstOrNull()

            if (target == null) {
                state = DeviceState.SEARCHING
                if (isEmulator()) {
                    Log.d("AetherLink", "Defaulting to emulator host 10.0.2.2")
                    target = Device("10.0.2.2:45876", "AetherLink Desktop (Host)", "10.0.2.2", false)
                    devices = listOf(target)
                } else {
                    Log.d("AetherLink", "No target device, scanning first")
                    val found = discoverDevicesUseCase()
                    devices = found
                    target = found.firstOrNull()

                    if (target == null) {
                        Log.d("AetherLink", "No desktop found on LAN")
                        state = DeviceState.DISCONNECTED
                        errorMessage = "No desktop found. Tap 'Direct IP' or allow port in Ubuntu: sudo ufw allow 45876/tcp"
                        return@launch
                    }
                }
            }

            state = DeviceState.SEARCHING
            val targetDevice = target

            withContext(Dispatchers.IO) {
                try {
                    activeClient?.disconnect()

                    val client = TcpClient(
                        host = targetDevice.ipAddress,
                        port = 45876
                    )

                    if (!client.connect(timeoutMs = 4500)) {
                        Log.e("AetherLink", "TCP connection failed to ${targetDevice.ipAddress}:45876")
                        withContext(Dispatchers.Main) {
                            state = DeviceState.DISCONNECTED
                            errorMessage = "Cannot reach ${targetDevice.ipAddress}:45876. Ensure laptop app is running and Ubuntu firewall allows port 45876."
                        }
                        return@withContext
                    }

                    activeClient = client

                    // 1. Send HELLO
                    val hello = Message(
                        type = MessageType.HELLO,
                        requestId = UUID.randomUUID().toString(),
                        payload = json.encodeToJsonElement(
                            mapOf("client" to "AetherLink Android")
                        )
                    )
                    client.send(hello)

                    val helloReply = client.receive()
                    Log.d("AetherLink", "HELLO reply: $helloReply")

                    if (helloReply?.type != MessageType.WELCOME) {
                        Log.e("AetherLink", "Invalid HELLO response")
                        client.disconnect()
                        activeClient = null
                        withContext(Dispatchers.Main) {
                            state = DeviceState.DISCONNECTED
                            errorMessage = "Desktop did not return WELCOME greeting"
                        }
                        return@withContext
                    }

                    // 2. Send PAIR_REQUEST
                    val pairRequest = Message(
                        type = MessageType.PAIR_REQUEST,
                        requestId = UUID.randomUUID().toString(),
                        payload = json.encodeToJsonElement(
                            PairRequestPayload(
                                deviceId = getOrCreateDeviceId(),
                                deviceName = getDeviceName(),
                                platform = "Android",
                                pin = null
                            )
                        )
                    )
                    client.send(pairRequest)

                    val pairReply = client.receive()
                    Log.d("AetherLink", "PAIR_REQUEST reply: $pairReply")

                    if (pairReply?.type == MessageType.PAIR_RESPONSE) {
                        val responsePayload: PairResponsePayload =
                            json.decodeFromJsonElement(pairReply.payload)

                        withContext(Dispatchers.Main) {
                            when (responsePayload.status) {
                                PairStatus.PAIRED, PairStatus.CONFIRMED -> {
                                    Log.d("AetherLink", "Device already trusted!")
                                    ConnectionManager.onConnected(getApplication(), client)

                                    // Save last connected IP
                                    getApplication<Application>()
                                        .getSharedPreferences("aetherlink_prefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("last_connected_ip", targetDevice.ipAddress)
                                        .putString("last_connected_name", targetDevice.name)
                                        .apply()

                                    devices = devices.map {
                                        if (it.id == targetDevice.id) it.copy(isConnected = true)
                                        else it.copy(isConnected = false)
                                    }
                                    state = DeviceState.CONNECTED
                                    errorMessage = null
                                }

                                PairStatus.PENDING -> {
                                    Log.d("AetherLink", "Pairing required, PIN: ${responsePayload.pin}")
                                    pairingPin = responsePayload.pin
                                    pairingMessage = responsePayload.message
                                    state = DeviceState.PAIRING
                                }

                                else -> {
                                    Log.e("AetherLink", "Pairing rejected: ${responsePayload.message}")
                                    state = DeviceState.DISCONNECTED
                                    errorMessage = "Pairing declined: ${responsePayload.message}"
                                }
                            }
                        }
                    } else {
                        Log.e("AetherLink", "Unexpected pair reply: ${pairReply?.type}")
                        client.disconnect()
                        activeClient = null
                        withContext(Dispatchers.Main) {
                            state = DeviceState.DISCONNECTED
                            errorMessage = "Unexpected response from desktop server"
                        }
                    }

                } catch (e: Exception) {
                    Log.e("AetherLink", "Socket error during connect", e)
                    withContext(Dispatchers.Main) {
                        state = DeviceState.DISCONNECTED
                        errorMessage = "Connection error: ${e.message}"
                    }
                }
            }
        }
    }

    fun confirmPairing() {
        viewModelScope.launch {
            val pin = pairingPin ?: return@launch
            val client = activeClient ?: return@launch

            withContext(Dispatchers.IO) {
                try {
                    val confirmMsg = Message(
                        type = MessageType.PAIR_REQUEST,
                        requestId = UUID.randomUUID().toString(),
                        payload = json.encodeToJsonElement(
                            PairRequestPayload(
                                deviceId = getOrCreateDeviceId(),
                                deviceName = getDeviceName(),
                                platform = "Android",
                                pin = pin
                            )
                        )
                    )
                    client.send(confirmMsg)

                    val reply = client.receive()
                    Log.d("AetherLink", "PAIR_CONFIRM reply: $reply")

                    withContext(Dispatchers.Main) {
                        if (reply?.type == MessageType.PAIR_RESPONSE) {
                            val payload: PairResponsePayload =
                                json.decodeFromJsonElement(reply.payload)

                            if (payload.status == PairStatus.PAIRED || payload.status == PairStatus.CONFIRMED) {
                                Log.d("AetherLink", "Pairing confirmed!")
                                ConnectionManager.onConnected(getApplication(), client)

                                // Save last connected IP
                                devices.firstOrNull()?.let { d ->
                                    getApplication<Application>()
                                        .getSharedPreferences("aetherlink_prefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("last_connected_ip", d.ipAddress)
                                        .putString("last_connected_name", d.name)
                                        .apply()
                                }

                                devices = devices.map { it.copy(isConnected = true) }
                                state = DeviceState.CONNECTED
                                pairingPin = null
                                errorMessage = null
                            } else {
                                state = DeviceState.DISCONNECTED
                                pairingPin = null
                                errorMessage = "PIN verification failed"
                            }
                        } else {
                            state = DeviceState.DISCONNECTED
                            pairingPin = null
                            errorMessage = "Verification timeout"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AetherLink", "Failed to confirm pairing", e)
                    withContext(Dispatchers.Main) {
                        state = DeviceState.DISCONNECTED
                        pairingPin = null
                        errorMessage = "Pairing error: ${e.message}"
                    }
                }
            }
        }
    }

    fun cancelPairing() {
        activeClient?.disconnect()
        activeClient = null
        pairingPin = null
        state = DeviceState.DISCONNECTED
    }

    fun disconnect() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ConnectionManager.disconnect(getApplication())
                activeClient = null
            }

            devices = devices.map { it.copy(isConnected = false) }
            state = DeviceState.DISCONNECTED
            pairingPin = null
            errorMessage = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryService.stopDiscovery()
        // Do not disconnect activeClient here so background connection stays alive when switching apps
    }
}
