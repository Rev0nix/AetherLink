package com.vexyron.android.domain.repository

import com.vexyron.android.domain.model.Device

interface DiscoveryRepository {

    suspend fun startDiscovery(): List<Device>

    fun stopDiscovery()
}