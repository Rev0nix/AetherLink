package com.aetherlink.android.protocol

import kotlinx.serialization.Serializable

@Serializable
data class Message<T>(

    val type: MessageType,

    val payload: T
)