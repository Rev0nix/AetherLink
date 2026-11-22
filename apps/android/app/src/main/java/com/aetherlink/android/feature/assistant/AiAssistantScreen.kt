package com.aetherlink.android.feature.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aetherlink.android.core.components.AetherStatusBadge
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCardBg
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGradientPrimary
import com.aetherlink.android.core.designsystem.AetherGreen
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherSurfaceElevated
import com.aetherlink.android.core.designsystem.AetherTextMuted
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary
import com.aetherlink.android.core.voice.VoiceManager
import com.aetherlink.android.domain.model.DeviceState
import com.aetherlink.android.service.network.ConnectionManager
import com.aetherlink.android.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val time: String = "Just now"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val hostOs by ConnectionManager.hostOs.collectAsState()
    val hostName by ConnectionManager.hostName.collectAsState()
    val isConnected = ConnectionManager.isConnected && vm.state == DeviceState.CONNECTED

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = WindowInsets.isImeVisible || imeBottom > 0

    val messages = remember {
        mutableStateListOf(
            ChatMessage("1", "Hello! I am Vex AI. I can control your laptop, open applications like Brave, Chrome or VS Code, check system status, and manage workflows.", isUser = false)
        )
    }

    fun buildAppLaunchCommand(rawText: String, isWindows: Boolean): Pair<String, String>? {
        val lower = rawText.lowercase().trim()
        val cleaned = lower
            .removePrefix("open ")
            .removePrefix("launch ")
            .removePrefix("start ")
            .removePrefix("run ")
            .replace(" in laptop", "")
            .replace(" on laptop", "")
            .replace(" in desktop", "")
            .replace(" on desktop", "")
            .trim()

        if (cleaned.isEmpty()) return null

        return if (isWindows) {
            when (cleaned) {
                "brave", "brave browser" -> Pair("Start-Process brave -ErrorAction SilentlyContinue || start brave", "Brave")
                "chrome", "google chrome" -> Pair("start chrome", "Google Chrome")
                "firefox", "mozilla firefox" -> Pair("start firefox", "Firefox")
                "code", "vscode", "vs code", "visual studio code" -> Pair("start code", "VS Code")
                "spotify" -> Pair("start spotify", "Spotify")
                "calc", "calculator" -> Pair("start calc", "Calculator")
                "notepad", "text editor" -> Pair("start notepad", "Notepad")
                "terminal", "cmd", "powershell" -> Pair("start powershell", "PowerShell")
                "files", "explorer", "file explorer" -> Pair("start explorer", "File Explorer")
                "settings" -> Pair("start ms-settings:", "Windows Settings")
                "youtube" -> Pair("start https://www.youtube.com", "YouTube")
                "google" -> Pair("start https://www.google.com", "Google")
                "github" -> Pair("start https://www.github.com", "GitHub")
                else -> {
                    if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
                        Pair("start $cleaned", cleaned)
                    } else {
                        Pair("start $cleaned", cleaned.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        } else {
            // Linux
            when (cleaned) {
                "brave", "brave browser" -> Pair(
                    "(nohup /snap/bin/brave >/dev/null 2>&1 &) || (nohup brave >/dev/null 2>&1 &) || (nohup brave-browser >/dev/null 2>&1 &) || (gtk-launch brave-browser >/dev/null 2>&1 &) || (gtk-launch brave_brave >/dev/null 2>&1 &)",
                    "Brave Browser"
                )
                "chrome", "google chrome" -> Pair(
                    "(nohup google-chrome-stable >/dev/null 2>&1 &) || (nohup google-chrome >/dev/null 2>&1 &) || (nohup chromium >/dev/null 2>&1 &) || (gtk-launch google-chrome >/dev/null 2>&1 &)",
                    "Google Chrome"
                )
                "firefox", "mozilla firefox" -> Pair(
                    "(nohup firefox >/dev/null 2>&1 &) || (gtk-launch firefox >/dev/null 2>&1 &)",
                    "Firefox"
                )
                "code", "vscode", "vs code", "visual studio code" -> Pair(
                    "(nohup code >/dev/null 2>&1 &) || (gtk-launch code >/dev/null 2>&1 &)",
                    "VS Code"
                )
                "spotify" -> Pair(
                    "(nohup spotify >/dev/null 2>&1 &) || (nohup /snap/bin/spotify >/dev/null 2>&1 &) || (gtk-launch spotify >/dev/null 2>&1 &)",
                    "Spotify"
                )
                "calc", "calculator" -> Pair(
                    "(nohup gnome-calculator >/dev/null 2>&1 &) || (nohup kcalc >/dev/null 2>&1 &)",
                    "Calculator"
                )
                "terminal", "bash" -> Pair(
                    "(nohup gnome-terminal >/dev/null 2>&1 &) || (nohup xterm >/dev/null 2>&1 &) || (nohup konsole >/dev/null 2>&1 &)",
                    "Terminal"
                )
                "files", "file manager", "nautilus" -> Pair(
                    "(xdg-open ~ >/dev/null 2>&1 &) || (nohup nautilus >/dev/null 2>&1 &)",
                    "File Manager"
                )
                "settings" -> Pair(
                    "(nohup gnome-control-center >/dev/null 2>&1 &)",
                    "System Settings"
                )
                "youtube" -> Pair("xdg-open \"https://www.youtube.com\" >/dev/null 2>&1 &", "YouTube")
                "google" -> Pair("xdg-open \"https://www.google.com\" >/dev/null 2>&1 &", "Google")
                "github" -> Pair("xdg-open \"https://www.github.com\" >/dev/null 2>&1 &", "GitHub")
                else -> {
                    if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
                        Pair("xdg-open \"$cleaned\" >/dev/null 2>&1 &", cleaned)
                    } else {
                        Pair(
                            "(gtk-launch $cleaned >/dev/null 2>&1 &) || (nohup $cleaned >/dev/null 2>&1 &) || (xdg-open $cleaned >/dev/null 2>&1 &)",
                            cleaned.replaceFirstChar { it.uppercase() }
                        )
                    }
                }
            }
        }
    }

    fun handleSend(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        messages.add(ChatMessage(System.currentTimeMillis().toString(), trimmed, isUser = true))
        inputText = ""

        scope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)

            val lower = trimmed.lowercase()
            val isWindows = hostOs.equals("Windows", ignoreCase = true)
            val osName = if (hostOs != "Unknown") hostOs else "Laptop"
            val targetName = if (hostName.isNotBlank() && hostName != "Laptop") hostName else osName

            // Check if user is asking to launch/open an application
            val isAppLaunchIntent = lower.startsWith("open ") ||
                    lower.startsWith("launch ") ||
                    lower.startsWith("start ") ||
                    lower.startsWith("run ") ||
                    lower == "brave" || lower == "chrome" || lower == "firefox" ||
                    lower == "code" || lower == "vscode" || lower == "spotify" ||
                    lower == "calculator" || lower == "terminal"

            val reply = if (isAppLaunchIntent) {
                if (!isConnected) {
                    "⚠️ Your laptop is currently disconnected. Tap 'Connect' on the Home screen to link via Wi-Fi, then I will open applications for you."
                } else {
                    val launchPair = buildAppLaunchCommand(trimmed, isWindows)
                    if (launchPair != null) {
                        val (cmd, appName) = launchPair
                        // Trigger background execution on the laptop
                        ConnectionManager.sendTerminalCommand(cmd, "~")
                        "🚀 Opening $appName on your $osName laptop ($targetName)..."
                    } else {
                        "I couldn't identify the application name. Try saying 'open brave', 'open chrome', or 'open vscode'."
                    }
                }
            } else when {
                lower.contains("battery") -> {
                    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                        context.registerReceiver(null, filter)
                    }
                    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
                    "🔋 Your smartphone battery is at $level%."
                }
                lower.contains("connect") || lower.contains("status") -> {
                    if (isConnected) {
                        "🟢 Connected to $osName laptop ($targetName) on port 45876. You can send commands or say 'open brave' to launch applications."
                    } else {
                        "🔴 Laptop is currently disconnected. Go to the Home screen and tap 'Connect' to pair via Wi-Fi."
                    }
                }
                lower.contains("speed") -> {
                    "⚡ Local Wi-Fi connection channel operating at ultra-low latency with transfer throughput up to ~25 MB/s."
                }
                lower.contains("photo") || lower.contains("file") -> {
                    "📁 You can beam files directly to your laptop's Downloads folder! Head over to the Files tab to send images, docs, and media."
                }
                lower.contains("help") -> {
                    "🤖 I am Vex AI! Here is what I can do:\n• Open apps on laptop: 'open brave', 'open chrome', 'open vscode', 'open calculator'\n• Open websites: 'open youtube', 'open github'\n• Check device status: 'is laptop connected', 'battery level'\n• Remote control: Use the Remote Terminal for full bash & PowerShell commands."
                }
                else -> {
                    if (isConnected) {
                        "Understood! I'm linked to your $osName laptop. You can ask me to open apps (e.g. 'open brave', 'open chrome') or use the Remote Terminal to run shell commands."
                    } else {
                        "Hello! I am Vex AI. Connect your phone to your laptop on the Home screen to launch applications, beam files, and sync clipboard seamlessly."
                    }
                }
            }

            delay(400)
            messages.add(ChatMessage((System.currentTimeMillis() + 1).toString(), reply, isUser = false))
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val voiceManager = remember {
        VoiceManager(
            context = context,
            onPartial = { partial ->
                inputText = partial
            },
            onFinalResult = { result ->
                inputText = result
                handleSend(result)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    fun onMicClick() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            voiceManager.startListening()
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && messages.isNotEmpty()) {
            delay(150)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestedPrompts = listOf(
        "Open brave on laptop",
        "Open chrome on laptop",
        "Open vscode on laptop",
        "Is laptop connected?",
        "Check my phone battery"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherBg)
            .statusBarsPadding()
            .padding(top = 16.dp)
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = if (isKeyboardOpen) 8.dp else 92.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AetherGradientPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Vex AI",
                        tint = AetherTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Vex AI",
                        color = AetherTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isConnected) "Connected • ${if (hostOs != "Unknown") hostOs else "Laptop"}" else "Device Ecosystem Assistant",
                        color = if (isConnected) AetherGreen else AetherTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            AetherStatusBadge(
                text = if (isConnected) "Online" else "Ready",
                indicatorColor = if (isConnected) AetherGreen else AetherCyan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CHAT MESSAGES LIST
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }
        }

        // SUGGESTED PROMPT CHIPS (Hide when typing for maximum chat visibility)
        if (!isKeyboardOpen) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestedPrompts) { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AetherSurfaceElevated)
                            .border(1.dp, AetherBorder, RoundedCornerShape(20.dp))
                            .clickable { handleSend(prompt) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = AetherTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // MESSAGE INPUT BOX
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = if (voiceManager.isListening) "Listening... speak now" else "Ask Vex AI (e.g. open brave)...",
                        color = if (voiceManager.isListening) AetherGreen else AetherTextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { handleSend(inputText) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (voiceManager.isListening) AetherGreen else AetherCyan,
                    unfocusedBorderColor = AetherBorder,
                    focusedTextColor = AetherTextPrimary,
                    unfocusedTextColor = AetherTextPrimary,
                    focusedContainerColor = AetherCardBg,
                    unfocusedContainerColor = AetherCardBg
                ),
                shape = RoundedCornerShape(22.dp)
            )

            // Mic Icon Button
            val isListening = voiceManager.isListening
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListening) AetherCyan.copy(alpha = 0.2f) else AetherSurfaceElevated)
                    .border(
                        width = if (isListening) 2.dp else 1.dp,
                        color = if (isListening) AetherGreen else AetherBorder,
                        shape = CircleShape
                    )
                    .clickable { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isListening) "Listening" else "Voice Input",
                    tint = if (isListening) AetherGreen else AetherTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Send Icon Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) AetherCyan else AetherSurfaceElevated)
                    .clickable(enabled = inputText.isNotBlank()) { handleSend(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) AetherBg else AetherTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(if (message.isUser) AetherCyan.copy(alpha = 0.2f) else AetherCardBg)
                .border(
                    width = 1.dp,
                    color = if (message.isUser) AetherCyan.copy(alpha = 0.4f) else AetherBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) AetherTextPrimary else AetherTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
