package com.vexyron.android.data.repository

import com.vexyron.android.domain.model.Device
import com.vexyron.android.domain.repository.DiscoveryRepository
import com.vexyron.android.service.DiscoveryService

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