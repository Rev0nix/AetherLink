package com.vexyron.android.protocol

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(

    val id: String,

    val name: String,

    val os: String,

    val version: String,

    val ipAddress: String,

    val port: Int
)