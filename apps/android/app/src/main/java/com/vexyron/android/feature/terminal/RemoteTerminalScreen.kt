package com.vexyron.android.feature.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vexyron.android.core.components.VexyronPrimaryButton
import com.vexyron.android.core.components.VexyronSecondaryButton
import com.vexyron.android.core.designsystem.VexyronBorder
import com.vexyron.android.core.designsystem.VexyronCardBg
import com.vexyron.android.core.designsystem.VexyronCyan
import com.vexyron.android.core.designsystem.VexyronGreen
import com.vexyron.android.core.designsystem.VexyronRed
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary
import com.vexyron.android.domain.model.DeviceState
import com.vexyron.android.service.network.ConnectionManager
import com.vexyron.android.service.terminal.TerminalEntryType
import com.vexyron.android.service.terminal.TerminalManager
import com.vexyron.android.viewmodel.HomeViewModel

@Composable
fun RemoteTerminalScreen(
    navController: NavController,
    vm: HomeViewModel
) {
    val entries by TerminalManager.entries.collectAsState()
    val cwd by TerminalManager.cwd.collectAsState()
    val isExecuting by TerminalManager.isExecuting.collectAsState()
    val history by TerminalManager.history.collectAsState()
    val sudoPassword by TerminalManager.sudoPassword.collectAsState()
    val hostOs by ConnectionManager.hostOs.collectAsState()
    val hostName by ConnectionManager.hostName.collectAsState()
    val isConnected = vm.state == DeviceState.CONNECTED && ConnectionManager.isConnected

    var commandInput by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    // Sudo password dialog state
    var showSudoDialog by remember { mutableStateOf(false) }
    var pendingSudoCommand by remember { mutableStateOf<String?>(null) }
    var sudoDialogInput by remember { mutableStateOf("") }

    val isWindows = hostOs.equals("Windows", ignoreCase = true)

    fun executeWithSudoCheck(cmd: String) {
        val trimmed = cmd.trim()
            .replace("shotdown", "shutdown")
            .replace("rebot", "reboot")
        if (trimmed.isEmpty()) return

        if (!isWindows && trimmed.startsWith("sudo ", ignoreCase = true) && sudoPassword.isNullOrBlank()) {
            pendingSudoCommand = trimmed
            sudoDialogInput = ""
            showSudoDialog = true
        } else {
            TerminalManager.executeCommand(trimmed)
        }
    }

    // Initialize session and request current host working directory on launch
    LaunchedEffect(isConnected) {
        if (isConnected) {
            TerminalManager.initSession()
        }
    }

    // Auto-scroll to bottom on new output
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // 1. TOP APP BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VexyronCardBg)
                .border(1.dp, VexyronBorder)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VexyronTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Terminal icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(VexyronCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Terminal",
                        tint = VexyronCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title & Subtitle with weight(1f) to prevent pushing right action buttons off screen
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Remote Terminal",
                        color = VexyronTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) VexyronGreen else VexyronRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusText = if (isConnected) {
                            val os = if (hostOs != "Unknown") hostOs else "Laptop"
                            val name = if (hostName.isNotBlank() && hostName != "Laptop") " ($hostName)" else ""
                            "$os$name"
                        } else "Disconnected"
                        Text(
                            text = statusText,
                            color = if (isConnected) VexyronGreen else VexyronTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right action buttons: Sudo + Clear
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isWindows) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sudoPassword != null) Color(0xFFEAB308).copy(alpha = 0.2f) else VexyronSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (sudoPassword != null) Color(0xFFEAB308) else VexyronBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    sudoDialogInput = sudoPassword ?: ""
                                    pendingSudoCommand = null
                                    showSudoDialog = true
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Sudo Auth",
                                    tint = if (sudoPassword != null) Color(0xFFFACC15) else VexyronTextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Sudo",
                                    color = if (sudoPassword != null) Color(0xFFFACC15) else VexyronTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (sudoPassword != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = { TerminalManager.clear() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Console",
                            tint = VexyronTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. DISCONNECTED WARNING BANNER
        AnimatedVisibility(visible = !isConnected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VexyronRed.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = VexyronRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No laptop connected. Tap Connect on Home screen to link via Wi-Fi.",
                    color = VexyronRed,
                    fontSize = 11.sp
                )
            }
        }

        // 3. CONSOLE OUTPUT
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                when (entry.type) {
                    TerminalEntryType.INPUT -> {
                        Text(
                            text = entry.text,
                            color = VexyronCyan,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TerminalEntryType.STDOUT -> {
                        Text(
                            text = entry.text,
                            color = Color(0xFFD4D4D4),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                    TerminalEntryType.STDERR -> {
                        Text(
                            text = entry.text,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                    TerminalEntryType.SYSTEM -> {
                        Text(
                            text = entry.text,
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 4. QUICK COMMAND CHIPS TOOLBAR
        val quickCommands = remember(isWindows) {
            if (isWindows) {
                listOf(
                    "dir", "pwd", "ipconfig", "whoami", "hostname",
                    "systeminfo", "Get-Process", "cd ..", "cd ~", "cls"
                )
            } else {
                listOf(
                    "ls -la", "pwd", "whoami", "uname -a", "df -h",
                    "sudo apt update", "sudo systemctl status", "free -m",
                    "uptime", "git status", "cd ..", "cd ~", "clear"
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VexyronCardBg)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                val isSudoCmd = cmd.startsWith("sudo ")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSudoCmd) Color(0xFFEAB308).copy(alpha = 0.15f) else VexyronSurfaceElevated)
                        .border(
                            1.dp,
                            if (isSudoCmd) Color(0xFFEAB308).copy(alpha = 0.5f) else VexyronBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (cmd == "clear") {
                                TerminalManager.clear()
                            } else {
                                executeWithSudoCheck(cmd)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cmd,
                        color = if (isSudoCmd) Color(0xFFFACC15) else VexyronTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 5. INPUT CONSOLE PROMPT ROW
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VexyronCardBg)
                .border(1.dp, VexyronBorder)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                // Host CWD indicator
                val promptPrefix = if (isWindows) "PS $cwd>" else "host:$cwd$"
                Text(
                    text = promptPrefix,
                    color = VexyronCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History navigators
                    IconButton(
                        onClick = {
                            if (history.isNotEmpty()) {
                                if (historyIndex < history.size - 1) {
                                    historyIndex++
                                    val item = history[history.size - 1 - historyIndex]
                                    commandInput = item
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Previous Command",
                            tint = VexyronTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (historyIndex > 0) {
                                historyIndex--
                                val item = history[history.size - 1 - historyIndex]
                                commandInput = item
                            } else if (historyIndex == 0) {
                                historyIndex = -1
                                commandInput = ""
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Next Command",
                            tint = VexyronTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = {
                            commandInput = it
                            historyIndex = -1
                        },
                        placeholder = {
                            Text(
                                if (isWindows) "Enter PowerShell command..." else "Enter command (e.g. ls, sudo shutdown now)...",
                                color = VexyronTextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (commandInput.isNotBlank()) {
                                val toRun = commandInput
                                commandInput = ""
                                historyIndex = -1
                                executeWithSudoCheck(toRun)
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VexyronCyan,
                            unfocusedBorderColor = VexyronBorder,
                            focusedTextColor = VexyronTextPrimary,
                            unfocusedTextColor = VexyronTextPrimary,
                            focusedContainerColor = VexyronSurfaceElevated,
                            unfocusedContainerColor = VexyronSurfaceElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (commandInput.isNotBlank() && !isExecuting) VexyronCyan else VexyronSurfaceElevated)
                            .clickable(enabled = commandInput.isNotBlank() && !isExecuting) {
                                val toRun = commandInput
                                commandInput = ""
                                historyIndex = -1
                                executeWithSudoCheck(toRun)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = VexyronCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Command",
                                tint = if (commandInput.isNotBlank()) Color.Black else VexyronTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 6. SUDO PASSWORD DIALOG
    if (showSudoDialog) {
        AlertDialog(
            onDismissRequest = {
                showSudoDialog = false
                pendingSudoCommand = null
            },
            containerColor = VexyronCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFFACC15),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Laptop Sudo / Root Auth",
                        color = VexyronTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Running privileged sudo commands requires your laptop user password. It will be securely used for this connection session.",
                        color = VexyronTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    if (pendingSudoCommand != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pending: ${pendingSudoCommand}",
                            color = VexyronCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sudoDialogInput,
                        onValueChange = { sudoDialogInput = it },
                        placeholder = { Text("Enter laptop password", color = VexyronTextMuted, fontSize = 13.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val pass = sudoDialogInput.trim()
                            if (pass.isNotEmpty()) {
                                TerminalManager.setSudoPassword(pass)
                                showSudoDialog = false
                                pendingSudoCommand?.let { cmd ->
                                    pendingSudoCommand = null
                                    TerminalManager.executeCommand(cmd, pass)
                                }
                            }
                        }),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFACC15),
                            unfocusedBorderColor = VexyronBorder,
                            focusedTextColor = VexyronTextPrimary,
                            unfocusedTextColor = VexyronTextPrimary,
                            focusedContainerColor = VexyronSurfaceElevated,
                            unfocusedContainerColor = VexyronSurfaceElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VexyronPrimaryButton(
                    text = if (pendingSudoCommand != null) "Run Command" else "Save Password",
                    onClick = {
                        val pass = sudoDialogInput.trim()
                        if (pass.isNotEmpty()) {
                            TerminalManager.setSudoPassword(pass)
                            showSudoDialog = false
                            pendingSudoCommand?.let { cmd ->
                                pendingSudoCommand = null
                                TerminalManager.executeCommand(cmd, pass)
                            }
                        }
                    }
                )
            },
            dismissButton = {
                Row {
                    if (sudoPassword != null) {
                        VexyronSecondaryButton(
                            text = "Clear Saved",
                            onClick = {
                                TerminalManager.setSudoPassword(null)
                                sudoDialogInput = ""
                                showSudoDialog = false
                                pendingSudoCommand = null
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    VexyronSecondaryButton(
                        text = "Cancel",
                        onClick = {
                            showSudoDialog = false
                            pendingSudoCommand = null
                        }
                    )
                }
            }
        )
    }
}
