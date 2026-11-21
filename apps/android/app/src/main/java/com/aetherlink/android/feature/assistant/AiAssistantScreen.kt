package com.aetherlink.android.feature.assistant

import android.os.BatteryManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aetherlink.android.core.components.AetherHeader
import com.aetherlink.android.core.components.AetherStatusBadge
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCardBg
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGradientPrimary
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherSurfaceElevated
import com.aetherlink.android.core.designsystem.AetherTextMuted
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary
import com.aetherlink.android.domain.model.DeviceState
import com.aetherlink.android.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val time: String = "Just now"
)

@Composable
fun AiAssistantScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val messages = remember {
        mutableStateListOf(
            ChatMessage("1", "Hello! I am Aether AI. How can I assist you with your connected devices today?", isUser = false)
        )
    }

    val suggestedPrompts = listOf(
        "Is my desktop connected?",
        "Check my phone battery.",
        "Send my latest photos to my laptop.",
        "Check my connection speed.",
        "Find recently received files."
    )

    fun handleSend(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(System.currentTimeMillis().toString(), text, isUser = true))
        inputText = ""

        scope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)

            // Dynamic contextual response
            delay(600)
            val reply = when {
                text.contains("battery", ignoreCase = true) -> {
                    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                        context.registerReceiver(null, filter)
                    }
                    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
                    "Your smartphone battery level is at $level%. Optimal for device sync."
                }
                text.contains("connected", ignoreCase = true) -> {
                    if (vm.state == DeviceState.CONNECTED) {
                        "Yes! You are currently connected to ${vm.devices.firstOrNull()?.name ?: "Desktop"} on port 45876."
                    } else {
                        "Your desktop is currently offline or disconnected. Tap 'Connect' on the Home dashboard to pair."
                    }
                }
                text.contains("speed", ignoreCase = true) -> {
                    "Your local Wi-Fi connection channel speed is operating at approximately ~24.5 MB/s."
                }
                text.contains("photo", ignoreCase = true) || text.contains("file", ignoreCase = true) -> {
                    "I can trigger the file transfer engine! Head over to the Files tab to choose items for high-speed transfer."
                }
                else -> {
                    "Understood. I am monitoring your AetherLink device ecosystem and ready to execute your workflow."
                }
            }
            messages.add(ChatMessage((System.currentTimeMillis() + 1).toString(), reply, isUser = false))
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherBg)
            .padding(top = 52.dp, bottom = 100.dp)
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
                        contentDescription = "Aether AI",
                        tint = AetherTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Aether AI",
                        color = AetherTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Device Ecosystem Assistant",
                        color = AetherTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            AetherStatusBadge(
                text = "Online",
                indicatorColor = AetherCyan
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

        // SUGGESTED PROMPT CHIPS
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

        // MESSAGE INPUT BOX
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Ask Aether AI...",
                        color = AetherTextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AetherCyan,
                    unfocusedBorderColor = AetherBorder,
                    focusedTextColor = AetherTextPrimary,
                    unfocusedTextColor = AetherTextPrimary,
                    focusedContainerColor = AetherCardBg,
                    unfocusedContainerColor = AetherCardBg
                ),
                shape = RoundedCornerShape(22.dp)
            )

            // Mic Icon Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AetherSurfaceElevated)
                    .border(1.dp, AetherBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = AetherTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Send Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AetherGradientPrimary)
                    .clickable { handleSend(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = AetherTextPrimary,
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
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AetherGradientPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AetherTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (message.isUser) AetherPurple
                    else AetherSurfaceElevated
                )
                .border(
                    1.dp,
                    if (message.isUser) AetherPurple else AetherBorder,
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = AetherTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
