package com.aetherlink.android.protocol

enum class MessageType {
    HELLO,
    DEVICE_INFO,
    PAIR_REQUEST,
    PAIR_RESPONSE,
    PING,
    PONG,
    FILE_TRANSFER,
    CLIPBOARD,
    NOTIFICATION,
    COMMAND
}