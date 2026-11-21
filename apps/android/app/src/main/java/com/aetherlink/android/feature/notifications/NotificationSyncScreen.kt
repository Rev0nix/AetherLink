package com.aetherlink.android.feature.notifications

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import com.aetherlink.android.core.components.AetherPrimaryButton
import com.aetherlink.android.service.network.ConnectionManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aetherlink.android.core.components.AetherCard
import com.aetherlink.android.core.components.AetherHeader
import com.aetherlink.android.core.components.AetherPrimaryButton
import com.aetherlink.android.core.components.AetherSecondaryButton
import com.aetherlink.android.core.components.AetherStatusBadge
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGreen
import com.aetherlink.android.core.designsystem.AetherOrange
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherRed
import com.aetherlink.android.core.designsystem.AetherSurfaceElevated
import com.aetherlink.android.core.designsystem.AetherTextMuted
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary

data class AppNotificationRule(
    val id: String,
    val appName: String,
    val icon: ImageVector,
    val iconTint: Color
)

@Composable
fun NotificationSyncScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isNotificationSyncEnabled by remember { mutableStateOf(true) }

    val appRules = remember {
        listOf(
            AppNotificationRule("1", "WhatsApp", Icons.AutoMirrored.Filled.Message, AetherGreen),
            AppNotificationRule("2", "Gmail", Icons.Default.Email, AetherRed),
            AppNotificationRule("3", "Messages (SMS)", Icons.AutoMirrored.Filled.Message, AetherCyan),
            AppNotificationRule("4", "Slack & Discord", Icons.Default.Notifications, AetherPurple)
        )
    }

    val appSelection = remember {
        mutableStateMapOf<String, Boolean>().apply {
            appRules.forEach { this[it.id] = true }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            AetherHeader(
                title = "Notifications",
                onBack = { navController.popBackStack() }
            )
        }

        // SYNC TOGGLE
        item {
            AetherCard(
                borderColor = if (isNotificationSyncEnabled) AetherPurple.copy(alpha = 0.4f) else AetherBorder
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
                                    if (isNotificationSyncEnabled) AetherPurple.copy(alpha = 0.15f)
                                    else AetherSurfaceElevated
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isNotificationSyncEnabled) AetherPurple else AetherTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Mirror Notifications",
                                color = AetherTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isNotificationSyncEnabled) "Forwarding to AetherLink Desktop" else "Mirroring disabled",
                                color = AetherTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isNotificationSyncEnabled,
                        onCheckedChange = { isNotificationSyncEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AetherTextPrimary,
                            checkedTrackColor = AetherPurple,
                            uncheckedThumbColor = AetherTextMuted,
                            uncheckedTrackColor = AetherSurfaceElevated
                        )
                    )
                }
            }
        }

        // PERMISSION ACCESS CARD
        item {
            AetherCard(
                backgroundColor = AetherSurfaceElevated.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = AetherCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Notification Access Permission",
                            color = AetherTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Android requires explicit Notification Listener authorization so AetherLink can detect incoming alerts and relay them securely to your desktop.",
                        color = AetherTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AetherSecondaryButton(
                        text = "Open System Permission Settings",
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // APP-SPECIFIC NOTIFICATION FILTERS
        item {
            Text(
                text = "App Notification Controls",
                color = AetherTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            AetherCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    appRules.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(app.iconTint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = app.icon,
                                        contentDescription = app.appName,
                                        tint = app.iconTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = app.appName,
                                    color = AetherTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            Checkbox(
                                checked = appSelection[app.id] == true,
                                onCheckedChange = { appSelection[app.id] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AetherCyan,
                                    uncheckedColor = AetherBorder,
                                    checkmarkColor = AetherBg
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            AetherPrimaryButton(
                text = "Send Test Notification to Desktop",
                onClick = {
                    scope.launch {
                        val ok = ConnectionManager.sendNotification(
                            id = UUID.randomUUID().toString(),
                            appName = "AetherLink",
                            packageName = "com.aetherlink.android",
                            title = "Sync Verified",
                            text = "Notification mirroring is active and verified between phone and PC."
                        )
                        if (ok) {
                            Toast.makeText(context, "Test notification mirrored to desktop!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Connect to desktop to mirror notifications", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
