package com.vexyron.android.feature.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.vexyron.android.service.FloatingSyncBubbleService
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vexyron.android.core.components.VexyronCard
import com.vexyron.android.core.components.VexyronHeader
import com.vexyron.android.core.components.VexyronPrimaryButton
import com.vexyron.android.core.components.VexyronSecondaryButton
import com.vexyron.android.core.designsystem.VexyronBg
import com.vexyron.android.core.designsystem.VexyronBorder
import com.vexyron.android.core.designsystem.VexyronCardBg
import com.vexyron.android.core.designsystem.VexyronCyan
import com.vexyron.android.core.designsystem.VexyronGreen
import com.vexyron.android.core.designsystem.VexyronOrange
import com.vexyron.android.core.designsystem.VexyronPurple
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary
import com.vexyron.android.service.network.AndroidClipboardItem
import com.vexyron.android.service.network.ConnectionManager
import kotlinx.coroutines.launch

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / 60000
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "$mins mins ago"
        mins < 1440 -> "${mins / 60} hours ago"
        else -> "${mins / 1440} days ago"
    }
}

@Composable
fun ClipboardScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSyncEnabled by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var manualText by remember { mutableStateOf("") }

    val liveHistory by ConnectionManager.clipboardHistory.collectAsState()
    val isConnected by ConnectionManager.connectionState.collectAsState()

    // When screen appears and sync is on, check if there's text on phone clipboard to auto-sync
    LaunchedEffect(Unit) {
        if (isSyncEnabled && isConnected) {
            try {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = cm?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val currentText = clip.getItemAt(0)?.coerceToText(context)?.toString()
                    if (!currentText.isNullOrBlank() && currentText != ConnectionManager.lastSyncedText) {
                        ConnectionManager.sendClipboard(currentText)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val filteredItems = liveHistory.filter {
        it.text.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VexyronBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            VexyronHeader(
                title = "Clipboard Sync",
                onBack = { navController.popBackStack() }
            )
        }

        // SYNC STATUS CARD
        item {
            VexyronCard(
                borderColor = if (isConnected && isSyncEnabled) VexyronGreen.copy(alpha = 0.4f) else VexyronBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isConnected && isSyncEnabled) VexyronGreen.copy(alpha = 0.15f)
                                    else VexyronSurfaceElevated
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (isConnected && isSyncEnabled) VexyronGreen else VexyronTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Auto Sync Clipboard",
                                color = VexyronTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isConnected) {
                                    if (isSyncEnabled) "Synchronizing with Laptop" else "Sync paused"
                                } else {
                                    "Laptop not connected"
                                },
                                color = if (isConnected && isSyncEnabled) VexyronGreen else VexyronTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isSyncEnabled,
                        onCheckedChange = { isSyncEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VexyronTextPrimary,
                            checkedTrackColor = VexyronGreen,
                            uncheckedThumbColor = VexyronTextMuted,
                            uncheckedTrackColor = VexyronSurfaceElevated
                        )
                    )
                }
            }
        }

        // FLOATING QUICK-SYNC BUBBLE
        item {
            var isBubbleEnabled by remember {
                mutableStateOf(FloatingSyncBubbleService.isRunning)
            }

            VexyronCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isBubbleEnabled) VexyronCyan.copy(alpha = 0.15f)
                                    else VexyronSurfaceElevated
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (isBubbleEnabled) VexyronCyan else VexyronTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Floating Sync Button",
                                color = VexyronTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Shows a 📋 bubble on screen edge to sync with 1 tap from inside any app",
                                color = VexyronTextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Switch(
                        checked = isBubbleEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    Toast.makeText(context, "Please allow 'Display over other apps' to enable the floating button", Toast.LENGTH_LONG).show()
                                    val overlayIntent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(overlayIntent)
                                } else {
                                    FloatingSyncBubbleService.start(context)
                                    isBubbleEnabled = true
                                }
                            } else {
                                FloatingSyncBubbleService.stop(context)
                                isBubbleEnabled = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VexyronTextPrimary,
                            checkedTrackColor = VexyronCyan,
                            uncheckedThumbColor = VexyronTextMuted,
                            uncheckedTrackColor = VexyronSurfaceElevated
                        )
                    )
                }
            }
        }

        // HOW TO SYNC CARD
        item {
            VexyronCard(
                borderColor = VexyronCyan.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = VexyronCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How to Copy from Phone to Laptop",
                            color = VexyronTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Highlight any text & tap 'Copy to Laptop' directly in the selection toolbar.\n• Or tap '📋 Sync to Laptop' in your notification drawer.\n• Or tap the Floating Sync Button while using any app.",
                        color = VexyronTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // SEND TEXT TO LAPTOP
        item {
            VexyronCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Send Text to Laptop",
                        color = VexyronTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        placeholder = { Text("Type or paste text to push to Laptop...", color = VexyronTextMuted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VexyronCyan,
                            unfocusedBorderColor = VexyronBorder,
                            focusedTextColor = VexyronTextPrimary,
                            unfocusedTextColor = VexyronTextPrimary,
                            focusedContainerColor = VexyronSurfaceElevated,
                            unfocusedContainerColor = VexyronSurfaceElevated
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Paste from phone OS clipboard button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VexyronSurfaceElevated)
                                .border(1.dp, VexyronBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = cm?.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val clipStr = clip.getItemAt(0)?.coerceToText(context)?.toString()
                                            if (!clipStr.isNullOrBlank()) {
                                                manualText = clipStr
                                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Unable to paste: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = VexyronCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste from Phone", color = VexyronTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Send Button
                        Box(modifier = Modifier.weight(1f)) {
                            VexyronPrimaryButton(
                                text = "Send to Laptop",
                                onClick = {
                                    if (manualText.isNotBlank()) {
                                        val textToSend = manualText
                                        manualText = ""
                                        scope.launch {
                                            val sent = ConnectionManager.sendClipboard(textToSend)
                                            if (sent) {
                                                Toast.makeText(context, "Pushed to Laptop clipboard!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Saved to history (Connect to Laptop to sync)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = VexyronTextPrimary, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        // SEARCH BAR
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search clipboard history...", color = VexyronTextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = VexyronTextMuted, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VexyronCyan,
                    unfocusedBorderColor = VexyronBorder,
                    focusedTextColor = VexyronTextPrimary,
                    unfocusedTextColor = VexyronTextPrimary,
                    focusedContainerColor = VexyronCardBg,
                    unfocusedContainerColor = VexyronCardBg
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // CLIPBOARD HISTORY HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clipboard History",
                    color = VexyronTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${filteredItems.size} items",
                    color = VexyronTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                VexyronCard(backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.3f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = VexyronTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching clipboard items" else "No clipboard history yet",
                            color = VexyronTextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copy text on your laptop or phone to sync instantly",
                            color = VexyronTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        items(filteredItems, key = { it.id }) { item ->
            VexyronCard(
                backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.isPinned) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = VexyronOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Source badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (item.source.contains("Laptop", ignoreCase = true)) VexyronCyan.copy(alpha = 0.15f)
                                        else VexyronPurple.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (item.source.contains("Laptop", ignoreCase = true)) "Laptop" else "Phone",
                                    color = if (item.source.contains("Laptop", ignoreCase = true)) VexyronCyan else VexyronPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = formatRelativeTime(item.timestamp),
                                color = VexyronTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Copy to Android OS clipboard button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(VexyronCardBg)
                                    .clickable {
                                        try {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("VEXYRON", item.text))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = VexyronCyan, modifier = Modifier.size(14.dp))
                            }

                            // Pin button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(VexyronCardBg)
                                    .clickable { ConnectionManager.togglePinClipboardItem(item.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pin",
                                    tint = if (item.isPinned) VexyronOrange else VexyronTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Delete button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(VexyronCardBg)
                                    .clickable { ConnectionManager.removeClipboardItem(item.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VexyronTextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = item.text,
                        color = VexyronTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // PRIVACY / PERMISSION EXPLANATION
        item {
            VexyronCard(backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.3f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = VexyronCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Android 10+ restricts background clipboard sniffing. When VEXYRON is open, clips are synced automatically; you can also use 'Paste from Phone' above to beam text at any time.",
                        color = VexyronTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
