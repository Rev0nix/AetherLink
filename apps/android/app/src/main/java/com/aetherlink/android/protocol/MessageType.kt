package com.aetherlink.android.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class MessageType {
    HELLO,
    WELCOME,
    DEVICE_INFO,
    PING,
    PONG,
    PAIR_REQUEST,
    PAIR_RESPONSE,
    FILE_REQUEST,
    FILE_CHUNK,
    FILE_COMPLETE,
    CLIPBOARD,
    NOTIFICATION,
    COMMAND,
    TERMINAL_EXEC,
    TERMINAL_OUTPUT,
    ERROR
}
