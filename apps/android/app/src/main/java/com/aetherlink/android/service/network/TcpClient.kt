package com.aetherlink.android.service.network

import android.util.Log
import com.aetherlink.android.protocol.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class TcpClient(
    private val host: String,
    private val port: Int
) {

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var reader: BufferedReader? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun connect(timeoutMs: Int = 4500): Boolean {
        return try {
            val s = Socket()
            s.keepAlive = true
            s.tcpNoDelay = true
            s.connect(InetSocketAddress(host, port), timeoutMs)
            // Use 0 so background listener can block indefinitely waiting for messages
            s.soTimeout = 0
            socket = s

            outputStream = s.getOutputStream()
            reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            true
        } catch (e: Exception) {
            Log.w("TcpClient", "Connection to $host:$port failed: ${e.message}")
            false
        }
    }

    fun send(message: Message): Boolean {
        synchronized(this) {
            return try {
                val s = socket ?: return false
                if (!s.isConnected || s.isClosed) return false
                val os = outputStream ?: return false

                val encoded = json.encodeToString(message) + "\n"
                os.write(encoded.toByteArray(Charsets.UTF_8))
                os.flush()
                true
            } catch (e: Exception) {
                Log.w("TcpClient", "Socket send error: ${e.message}")
                false
            }
        }
    }

    fun readLine(): String? {
        return try {
            reader?.readLine()
        } catch (e: SocketTimeoutException) {
            "" // Non-blocking / timeout: return empty string so caller doesn't treat as EOF
        } catch (e: Exception) {
            null // Genuine socket exception / EOF
        }
    }

    fun isConnected(): Boolean {
        val s = socket ?: return false
        return s.isConnected && !s.isClosed
    }

    fun receive(): Message? {
        return try {
            val line = reader?.readLine() ?: return null
            json.decodeFromString<Message>(line)
        } catch (e: SocketTimeoutException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun disconnect() {
        try {
            reader?.close()
        } catch (_: Exception) {}

        try {
            outputStream?.close()
        } catch (_: Exception) {}

        try {
            socket?.close()
        } catch (_: Exception) {}

        reader = null
        outputStream = null
        socket = null
    }
}
