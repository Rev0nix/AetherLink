package com.vexyron.android.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vexyron.android.data.repository.DiscoveryRepositoryImpl
import com.vexyron.android.domain.model.Device
import com.vexyron.android.domain.model.DeviceState
import com.vexyron.android.domain.usecase.DiscoverDevicesUseCase
import com.vexyron.android.protocol.Message
import com.vexyron.android.protocol.MessageType
import com.vexyron.android.protocol.PairRequestPayload
import com.vexyron.android.protocol.PairResponsePayload
import com.vexyron.android.protocol.PairStatus
import com.vexyron.android.service.DiscoveryService
import com.vexyron.android.service.network.ConnectionManager
import com.vexyron.android.service.network.TcpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
            .getSharedPreferences("vexyron_prefs", Context.MODE_PRIVATE)
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
            Log.d("VEXYRON", "Starting wireless device scan")
            state = DeviceState.SEARCHING
            errorMessage = null

            try {
                val found = discoverDevicesUseCase()
                Log.d("VEXYRON", "Wireless devices found = ${found.size}")
                devices = found

                if (found.isNotEmpty()) {
                    state = DeviceState.FOUND
                } else {
                    state = DeviceState.DISCONNECTED
                    errorMessage = "No desktop found. Tap 'Direct IP' to connect directly (192.168.0.118)."
                }
            } catch (e: Exception) {
                Log.e("VEXYRON", "Discovery failed", e)
                devices = emptyList()
                state = DeviceState.DISCONNECTED
                errorMessage = "Discovery error: ${e.message}"
            }
        }
    }

    fun connectToIp(ip: String, port: Int = 45876) {
        viewModelScope.launch {
            Log.d("VEXYRON", "Direct connection to $ip:$port")
            errorMessage = null
            val directDevice = Device(
                id = "$ip:$port",
                name = if (ip == "10.0.2.2") "VEXYRON Desktop (Host)" else "VEXYRON Desktop ($ip)",
                ipAddress = ip,
                isConnected = false
            )
            devices = listOf(directDevice)
            connectToDevice(directDevice)
        }
    }

    fun connectToDevice(device: Device? = null) {
        viewModelScope.launch {
            Log.d("VEXYRON", "===== WIRELESS PROTOCOL CONNECT =====")
            errorMessage = null

            var target = device ?: devices.firstOrNull()

            if (target == null) {
                state = DeviceState.SEARCHING
                if (isEmulator()) {
                    Log.d("VEXYRON", "Defaulting to emulator host 10.0.2.2")
                    target = Device("10.0.2.2:45876", "VEXYRON Desktop (Host)", "10.0.2.2", false)
                    devices = listOf(target)
                } else {
                    Log.d("VEXYRON", "No target device, scanning first")
                    val found = discoverDevicesUseCase()
                    devices = found
                    target = found.firstOrNull()

                    if (target == null) {
                        Log.d("VEXYRON", "No desktop found on LAN")
                        state = DeviceState.DISCONNECTED
                        errorMessage = "No desktop found. Tap 'Direct IP' or check Linux firewall helper in Settings."
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
                        Log.e("VEXYRON", "TCP connection failed to ${targetDevice.ipAddress}:45876")
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
                            mapOf("client" to "VEXYRON Android")
                        )
                    )
                    client.send(hello)

                    val helloReply = client.receive()
                    Log.d("VEXYRON", "HELLO reply: $helloReply")

                    if (helloReply?.type != MessageType.WELCOME) {
                        Log.e("VEXYRON", "Invalid HELLO response")
                        client.disconnect()
                        activeClient = null
                        withContext(Dispatchers.Main) {
                            state = DeviceState.DISCONNECTED
                            errorMessage = "Desktop did not return WELCOME greeting"
                        }
                        return@withContext
                    } else {
                        try {
                            val helloObj = helloReply.payload as? JsonObject
                            val os = (helloObj?.get("os") as? JsonPrimitive)?.content
                            val hostname = (helloObj?.get("hostname") as? JsonPrimitive)?.content
                            ConnectionManager.setHostInfo(os, hostname)
                        } catch (_: Exception) {}
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
                    Log.d("VEXYRON", "PAIR_REQUEST reply: $pairReply")

                    if (pairReply?.type == MessageType.PAIR_RESPONSE) {
                        try {
                            val pairObj = pairReply.payload as? JsonObject
                            val os = (pairObj?.get("os") as? JsonPrimitive)?.content
                            val hostname = (pairObj?.get("hostname") as? JsonPrimitive)?.content
                            ConnectionManager.setHostInfo(os, hostname)
                        } catch (_: Exception) {}

                        val responsePayload: PairResponsePayload =
                            json.decodeFromJsonElement(pairReply.payload)

                        withContext(Dispatchers.Main) {
                            when (responsePayload.status) {
                                PairStatus.PAIRED, PairStatus.CONFIRMED -> {
                                    Log.d("VEXYRON", "Device already trusted!")
                                    ConnectionManager.onConnected(getApplication(), client)

                                    val detectedOs = ConnectionManager.hostOs.value
                                    val detectedHost = ConnectionManager.hostName.value
                                    val friendlyName = if (detectedHost.isNotBlank() && detectedHost != "Laptop") {
                                        "$detectedHost ($detectedOs)"
                                    } else if (detectedOs != "Unknown") {
                                        "VEXYRON Desktop ($detectedOs)"
                                    } else {
                                        targetDevice.name
                                    }

                                    // Save last connected IP
                                    getApplication<Application>()
                                        .getSharedPreferences("vexyron_prefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("last_connected_ip", targetDevice.ipAddress)
                                        .putString("last_connected_name", friendlyName)
                                        .apply()

                                    devices = devices.map {
                                        if (it.id == targetDevice.id) it.copy(
                                            isConnected = true,
                                            name = friendlyName,
                                            os = detectedOs
                                        ) else it.copy(isConnected = false)
                                    }
                                    state = DeviceState.CONNECTED
                                    errorMessage = null
                                }

                                PairStatus.PENDING -> {
                                    Log.d("VEXYRON", "Pairing required, PIN: ${responsePayload.pin}")
                                    pairingPin = responsePayload.pin
                                    pairingMessage = responsePayload.message
                                    state = DeviceState.PAIRING
                                }

                                else -> {
                                    Log.e("VEXYRON", "Pairing rejected: ${responsePayload.message}")
                                    state = DeviceState.DISCONNECTED
                                    errorMessage = "Pairing declined: ${responsePayload.message}"
                                }
                            }
                        }
                    } else {
                        Log.e("VEXYRON", "Unexpected pair reply: ${pairReply?.type}")
                        client.disconnect()
                        activeClient = null
                        withContext(Dispatchers.Main) {
                            state = DeviceState.DISCONNECTED
                            errorMessage = "Unexpected response from desktop server"
                        }
                    }

                } catch (e: Exception) {
                    Log.e("VEXYRON", "Socket error during connect", e)
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
                    Log.d("VEXYRON", "PAIR_CONFIRM reply: $reply")

                    withContext(Dispatchers.Main) {
                        if (reply?.type == MessageType.PAIR_RESPONSE) {
                            try {
                                val replyObj = reply.payload as? JsonObject
                                val os = (replyObj?.get("os") as? JsonPrimitive)?.content
                                val hostname = (replyObj?.get("hostname") as? JsonPrimitive)?.content
                                ConnectionManager.setHostInfo(os, hostname)
                            } catch (_: Exception) {}

                            val payload: PairResponsePayload =
                                json.decodeFromJsonElement(reply.payload)

                            if (payload.status == PairStatus.PAIRED || payload.status == PairStatus.CONFIRMED) {
                                Log.d("VEXYRON", "Pairing confirmed!")
                                ConnectionManager.onConnected(getApplication(), client)

                                val detectedOs = ConnectionManager.hostOs.value
                                val detectedHost = ConnectionManager.hostName.value
                                val friendlyName = if (detectedHost.isNotBlank() && detectedHost != "Laptop") {
                                    "$detectedHost ($detectedOs)"
                                } else if (detectedOs != "Unknown") {
                                    "VEXYRON Desktop ($detectedOs)"
                                } else {
                                    "VEXYRON Desktop"
                                }

                                // Save last connected IP
                                devices.firstOrNull()?.let { d ->
                                    getApplication<Application>()
                                        .getSharedPreferences("vexyron_prefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("last_connected_ip", d.ipAddress)
                                        .putString("last_connected_name", friendlyName)
                                        .apply()
                                }

                                devices = devices.map { it.copy(isConnected = true, name = friendlyName, os = detectedOs) }
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
                    Log.e("VEXYRON", "Failed to confirm pairing", e)
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
