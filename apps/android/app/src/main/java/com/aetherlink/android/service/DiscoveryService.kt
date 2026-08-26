package com.aetherlink.android.service

import android.content.Context
import com.aetherlink.android.domain.model.Device
import kotlinx.coroutines.delay

class DiscoveryService(
    private val context: Context
) {

    suspend fun discoverDevices(): List<Device> {

        delay(2000)

        return listOf(
            Device(
                id = "desktop-001",
                name = "AetherLink Desktop",
                ipAddress = "192.168.1.100",
                isConnected = false
            )
        )
    }

    fun stopDiscovery() {
    }
}