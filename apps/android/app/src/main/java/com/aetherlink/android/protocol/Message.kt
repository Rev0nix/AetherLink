package com.aetherlink.android.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.UUID

@Serializable
data class Message(
    val version: Int = 1,

    @SerialName("message_type")
    val type: MessageType,

    @SerialName("request_id")
    val requestId: String = UUID.randomUUID().toString(),

    val payload: JsonElement
)
