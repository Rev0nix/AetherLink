package com.aetherlink.android.domain.usecase

import com.aetherlink.android.domain.model.Device
import com.aetherlink.android.domain.repository.DiscoveryRepository

class DiscoverDevicesUseCase(
    private val repository: DiscoveryRepository
) {

    suspend operator fun invoke(): List<Device> {
        return repository.startDiscovery()
    }
}