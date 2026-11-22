package com.vexyron.android.domain.usecase

import com.vexyron.android.domain.model.Device
import com.vexyron.android.domain.repository.DiscoveryRepository

class DiscoverDevicesUseCase(
    private val repository: DiscoveryRepository
) {

    suspend operator fun invoke(): List<Device> {
        return repository.startDiscovery()
    }
}