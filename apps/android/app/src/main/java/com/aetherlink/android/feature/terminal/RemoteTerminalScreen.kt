package com.aetherlink.android.feature.terminal

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCardBg
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGreen
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherRed
import com.aetherlink.android.core.designsystem.AetherSurfaceElevated
import com.aetherlink.android.core.designsystem.AetherTextMuted
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary
import com.aetherlink.android.domain.model.DeviceState
import com.aetherlink.android.service.network.ConnectionManager
import com.aetherlink.android.service.terminal.TerminalEntryType
import com.aetherlink.android.service.terminal.TerminalManager
import com.aetherlink.android.viewmodel.HomeViewModel

@Composable
fun RemoteTerminalScreen(
    navController: NavController,
    vm: HomeViewModel
) {
    val entries by TerminalManager.entries.collectAsState()
    val cwd by TerminalManager.cwd.collectAsState()
    val isExecuting by TerminalManager.isExecuting.collectAsState()
    val history by TerminalManager.history.collectAsState()
    val hostOs by ConnectionManager.hostOs.collectAsState()
    val hostName by ConnectionManager.hostName.collectAsState()
    val isConnected = vm.state == DeviceState.CONNECTED && ConnectionManager.isConnected

    var commandInput by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

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
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Color(0xFF090D16))
    ) {
        // 1. TOP APP BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AetherCardBg)
                .border(1.dp, AetherBorder)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AetherTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AetherCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Terminal",
                            tint = AetherCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Remote Terminal",
                            color = AetherTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) AetherGreen else AetherRed)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isConnected) {
                                    val osStr = if (hostOs != "Unknown") hostOs else "Laptop"
                                    val nameStr = if (hostName.isNotBlank() && hostName != "Laptop") " ($hostName)" else ""
                                    "Connected • $osStr$nameStr"
                                } else "Disconnected",
                                color = if (isConnected) AetherGreen else AetherTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isConnected && hostOs != "Unknown") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF0078D4).copy(alpha = 0.25f) else AetherGreen.copy(alpha = 0.25f))
                                        .border(1.dp, if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF0078D4) else AetherGreen, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (hostOs.equals("Windows", ignoreCase = true)) "Windows" else "Linux",
                                        color = if (hostOs.equals("Windows", ignoreCase = true)) Color(0xFF60CDFF) else AetherGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { TerminalManager.clear() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Console",
                            tint = AetherTextMuted
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
                    .background(AetherRed.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = AetherRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No laptop connected. Tap Connect on Home screen to link via Wi-Fi.",
                    color = AetherRed,
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
                            color = AetherCyan,
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
        val isWindows = hostOs.equals("Windows", ignoreCase = true)
        val quickCommands = remember(isWindows) {
            if (isWindows) {
                listOf(
                    "dir", "pwd", "ipconfig", "whoami", "hostname",
                    "systeminfo", "Get-Process", "cd ..", "cd ~", "cls"
                )
            } else {
                listOf(
                    "ls -la", "pwd", "whoami", "uname -a", "df -h",
                    "free -m", "uptime", "git status", "cd ..", "cd ~", "clear"
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AetherCardBg)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AetherSurfaceElevated)
                        .border(1.dp, AetherBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            if (cmd == "clear") {
                                TerminalManager.clear()
                            } else {
                                TerminalManager.executeCommand(cmd)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cmd,
                        color = AetherTextSecondary,
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
                .background(AetherCardBg)
                .border(1.dp, AetherBorder)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                // Host CWD indicator
                val promptPrefix = if (hostOs.equals("Windows", ignoreCase = true)) "PS $cwd>" else "host:$cwd$"
                Text(
                    text = promptPrefix,
                    color = AetherCyan,
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
                            tint = AetherTextSecondary,
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
                            tint = AetherTextSecondary,
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
                                if (hostOs.equals("Windows", ignoreCase = true)) "Enter PowerShell command..." else "Enter Linux / bash command...",
                                color = AetherTextMuted,
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
                                TerminalManager.executeCommand(toRun)
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AetherCyan,
                            unfocusedBorderColor = AetherBorder,
                            focusedTextColor = AetherTextPrimary,
                            unfocusedTextColor = AetherTextPrimary,
                            focusedContainerColor = AetherSurfaceElevated,
                            unfocusedContainerColor = AetherSurfaceElevated
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
                            .background(if (commandInput.isNotBlank() && !isExecuting) AetherCyan else AetherSurfaceElevated)
                            .clickable(enabled = commandInput.isNotBlank() && !isExecuting) {
                                val toRun = commandInput
                                commandInput = ""
                                historyIndex = -1
                                TerminalManager.executeCommand(toRun)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AetherCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Command",
                                tint = if (commandInput.isNotBlank()) Color.Black else AetherTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
