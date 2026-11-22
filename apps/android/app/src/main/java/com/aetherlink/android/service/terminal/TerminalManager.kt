package com.aetherlink.android.service.terminal

import android.util.Log
import com.aetherlink.android.protocol.Message
import com.aetherlink.android.protocol.MessageType
import com.aetherlink.android.service.network.ConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

enum class TerminalEntryType {
    INPUT,
    STDOUT,
    STDERR,
    SYSTEM
}

data class TerminalEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: TerminalEntryType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object TerminalManager {
    private const val TAG = "TerminalManager"

    private val _entries = MutableStateFlow<List<TerminalEntry>>(
        listOf(
            TerminalEntry(
                type = TerminalEntryType.SYSTEM,
                text = "VEXYRON Remote Terminal\nCross-platform remote shell for connected Windows & Linux laptops.\nConnecting to host session..."
            )
        )
    )
    val entries: StateFlow<List<TerminalEntry>> = _entries.asStateFlow()

    private val _cwd = MutableStateFlow("~")
    val cwd: StateFlow<String> = _cwd.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun initSession() {
        if (ConnectionManager.isConnected) {
            // Request host info and current working directory
            val msg = Message(
                type = MessageType.TERMINAL_EXEC,
                requestId = UUID.randomUUID().toString(),
                payload = buildJsonObject {
                    put("command", "")
                    put("cwd", _cwd.value)
                }
            )
            ConnectionManager.client?.send(msg)
        }
    }

    fun executeCommand(rawCommand: String) {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) return

        if (trimmed.equals("clear", ignoreCase = true) || trimmed.equals("cls", ignoreCase = true)) {
            clear()
            return
        }

        // Add to history
        val currentHistory = _history.value.toMutableList()
        currentHistory.remove(trimmed)
        currentHistory.add(trimmed)
        _history.value = currentHistory

        // Append input prompt
        val prompt = "${_cwd.value}$ $trimmed"
        val currentEntries = _entries.value.toMutableList()
        currentEntries.add(TerminalEntry(type = TerminalEntryType.INPUT, text = prompt))
        _entries.value = currentEntries

        if (!ConnectionManager.isConnected) {
            currentEntries.add(
                TerminalEntry(
                    type = TerminalEntryType.STDERR,
                    text = "Error: No laptop connected. Ensure your phone and laptop are paired and connected on Wi-Fi."
                )
            )
            _entries.value = currentEntries
            return
        }

        _isExecuting.value = true

        val msg = Message(
            type = MessageType.TERMINAL_EXEC,
            requestId = UUID.randomUUID().toString(),
            payload = buildJsonObject {
                put("command", trimmed)
                put("cwd", _cwd.value)
            }
        )

        val ok = ConnectionManager.client?.send(msg) ?: false
        if (!ok) {
            _isExecuting.value = false
            currentEntries.add(
                TerminalEntry(
                    type = TerminalEntryType.STDERR,
                    text = "Failed to deliver command to desktop socket."
                )
            )
            _entries.value = currentEntries
        }
    }

    fun onTerminalOutput(
        command: String,
        stdout: String,
        stderr: String,
        exitCode: Int,
        newCwd: String
    ) {
        Log.d(TAG, "onTerminalOutput: exitCode=$exitCode, cwd=$newCwd")
        _isExecuting.value = false

        if (newCwd.isNotBlank()) {
            _cwd.value = newCwd
        }

        if (stdout.trim() == "[CLEAR]") {
            clear()
            return
        }

        val current = _entries.value.toMutableList()

        if (stdout.isNotEmpty()) {
            current.add(TerminalEntry(type = TerminalEntryType.STDOUT, text = stdout.trimEnd()))
        }

        if (stderr.isNotEmpty()) {
            current.add(TerminalEntry(type = TerminalEntryType.STDERR, text = stderr.trimEnd()))
        }

        if (current.size > 200) {
            // Keep recent 200 lines to preserve mobile memory
            _entries.value = current.takeLast(200)
        } else {
            _entries.value = current
        }
    }

    fun clear() {
        _entries.value = listOf(
            TerminalEntry(
                type = TerminalEntryType.SYSTEM,
                text = "Console cleared. Host: ${_cwd.value}"
            )
        )
    }
}
