package com.aetherlink.android.data.repository

import com.aetherlink.android.domain.model.Device
import com.aetherlink.android.domain.repository.DiscoveryRepository
import com.aetherlink.android.service.DiscoveryService

class DiscoveryRepositoryImpl(
    private val discoveryService: DiscoveryService
) : DiscoveryRepository {

    override suspend fun startDiscovery(): List<Device> {
        return discoveryService.discoverDevices()
    }

    override fun stopDiscovery() {
        discoveryService.stopDiscovery()
    }
}