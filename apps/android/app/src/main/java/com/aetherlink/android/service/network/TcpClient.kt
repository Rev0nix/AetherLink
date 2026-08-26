package com.aetherlink.android.service.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class TcpClient(
    private val host: String,
    private val port: Int
) {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    fun connect(): Boolean {
        return try {
            socket = Socket(host, port)
            writer = PrintWriter(socket!!.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun send(message: String) {
        writer?.println(message)
    }

    fun receive(): String? {
        return reader?.readLine()
    }

    fun disconnect() {
        reader?.close()
        writer?.close()
        socket?.close()
    }
}