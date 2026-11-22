package com.vexyron.android.feature.notifications

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import com.vexyron.android.core.components.VexyronPrimaryButton
import com.vexyron.android.service.network.ConnectionManager
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
import com.vexyron.android.core.components.VexyronCard
import com.vexyron.android.core.components.VexyronHeader
import com.vexyron.android.core.components.VexyronPrimaryButton
import com.vexyron.android.core.components.VexyronSecondaryButton
import com.vexyron.android.core.components.VexyronStatusBadge
import com.vexyron.android.core.designsystem.VexyronBg
import com.vexyron.android.core.designsystem.VexyronBorder
import com.vexyron.android.core.designsystem.VexyronCyan
import com.vexyron.android.core.designsystem.VexyronGreen
import com.vexyron.android.core.designsystem.VexyronOrange
import com.vexyron.android.core.designsystem.VexyronPurple
import com.vexyron.android.core.designsystem.VexyronRed
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary

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
            AppNotificationRule("1", "WhatsApp", Icons.AutoMirrored.Filled.Message, VexyronGreen),
            AppNotificationRule("2", "Gmail", Icons.Default.Email, VexyronRed),
            AppNotificationRule("3", "Messages (SMS)", Icons.AutoMirrored.Filled.Message, VexyronCyan),
            AppNotificationRule("4", "Slack & Discord", Icons.Default.Notifications, VexyronPurple)
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
            .background(VexyronBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            VexyronHeader(
                title = "Notifications",
                onBack = { navController.popBackStack() }
            )
        }

        // SYNC TOGGLE
        item {
            VexyronCard(
                borderColor = if (isNotificationSyncEnabled) VexyronPurple.copy(alpha = 0.4f) else VexyronBorder
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
                                    if (isNotificationSyncEnabled) VexyronPurple.copy(alpha = 0.15f)
                                    else VexyronSurfaceElevated
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isNotificationSyncEnabled) VexyronPurple else VexyronTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Mirror Notifications",
                                color = VexyronTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isNotificationSyncEnabled) "Forwarding to VEXYRON Desktop" else "Mirroring disabled",
                                color = VexyronTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isNotificationSyncEnabled,
                        onCheckedChange = { isNotificationSyncEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VexyronTextPrimary,
                            checkedTrackColor = VexyronPurple,
                            uncheckedThumbColor = VexyronTextMuted,
                            uncheckedTrackColor = VexyronSurfaceElevated
                        )
                    )
                }
            }
        }

        // PERMISSION ACCESS CARD
        item {
            VexyronCard(
                backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = VexyronCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Notification Access Permission",
                            color = VexyronTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Android requires explicit Notification Listener authorization so VEXYRON can detect incoming alerts and relay them securely to your desktop.",
                        color = VexyronTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    VexyronSecondaryButton(
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
                color = VexyronTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            VexyronCard {
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
                                    color = VexyronTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            Checkbox(
                                checked = appSelection[app.id] == true,
                                onCheckedChange = { appSelection[app.id] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = VexyronCyan,
                                    uncheckedColor = VexyronBorder,
                                    checkmarkColor = VexyronBg
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            VexyronPrimaryButton(
                text = "Send Test Notification to Desktop",
                onClick = {
                    scope.launch {
                        val ok = ConnectionManager.sendNotification(
                            id = UUID.randomUUID().toString(),
                            appName = "VEXYRON",
                            packageName = "com.vexyron.android",
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
