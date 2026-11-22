package com.vexyron.android.domain.model

data class Device(
    val id: String,
    val name: String,
    val ipAddress: String,
    val isConnected: Boolean,
    val os: String = "Unknown"
)