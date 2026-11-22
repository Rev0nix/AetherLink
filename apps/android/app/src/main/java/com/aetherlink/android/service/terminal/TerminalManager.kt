package com.aetherlink.android.service.terminal

import android.util.Log
import com.aetherlink.android.service.network.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            CoroutineScope(Dispatchers.IO).launch {
                ConnectionManager.sendTerminalCommand("", _cwd.value)
            }
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

        // Append input prompt based on host OS
        val hostOs = ConnectionManager.hostOs.value
        val prompt = if (hostOs.equals("Windows", ignoreCase = true)) {
            "${_cwd.value}> $trimmed"
        } else {
            "${_cwd.value}$ $trimmed"
        }
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

        CoroutineScope(Dispatchers.IO).launch {
            val ok = ConnectionManager.sendTerminalCommand(trimmed, _cwd.value)
            if (!ok) {
                withContext(Dispatchers.Main) {
                    _isExecuting.value = false
                    val failedEntries = _entries.value.toMutableList()
                    failedEntries.add(
                        TerminalEntry(
                            type = TerminalEntryType.STDERR,
                            text = "Failed to deliver command to desktop socket. Check that laptop app is running."
                        )
                    )
                    _entries.value = failedEntries
                }
            }
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
        val hostOs = ConnectionManager.hostOs.value
        val hostName = ConnectionManager.hostName.value
        val banner = if (hostOs != "Unknown") {
            "VEXYRON Remote Terminal ($hostOs)\nHost: $hostName | Working Dir: ${_cwd.value}"
        } else {
            "Console cleared. Host: ${_cwd.value}"
        }
        _entries.value = listOf(
            TerminalEntry(
                type = TerminalEntryType.SYSTEM,
                text = banner
            )
        )
    }
}
