package com.aetherlink.android.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PairStatus {
    PENDING,
    PAIRED,
    CONFIRMED,
    REJECTED,
    ERROR
}

@Serializable
data class PairRequestPayload(
    @SerialName("device_id")
    val deviceId: String,

    @SerialName("device_name")
    val deviceName: String,

    val platform: String = "Android",

    val pin: String? = null
)

@Serializable
data class PairResponsePayload(
    val status: PairStatus,
    val pin: String? = null,
    val message: String = ""
)
