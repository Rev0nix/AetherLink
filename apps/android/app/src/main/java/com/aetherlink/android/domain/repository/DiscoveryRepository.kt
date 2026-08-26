package com.aetherlink.android.domain.repository

import com.aetherlink.android.domain.model.Device

interface DiscoveryRepository {

    suspend fun startDiscovery(): List<Device>

    fun stopDiscovery()
}