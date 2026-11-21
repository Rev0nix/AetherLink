package com.aetherlink.android.core.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.aetherlink.android.core.components.AetherPrimaryButton
import com.aetherlink.android.core.components.AetherSecondaryButton
import com.aetherlink.android.core.constants.AppConstants
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCardBg
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGreen
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherSurfaceElevated
import com.aetherlink.android.core.designsystem.AetherTextMuted
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary
import com.aetherlink.android.core.i18n.I18nManager

@Composable
fun AetherUpdateModal() {
    val context = LocalContext.current
    val info = UpdateManager.updateInfo

    if (UpdateManager.showDialog && info != null) {
        AlertDialog(
            onDismissRequest = {
                UpdateManager.dismissUpdate(context, info.version)
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, AetherBorder, RoundedCornerShape(28.dp)),
            containerColor = AetherCardBg,
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Bar with Icon and Dismiss button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AetherPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AetherCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(AetherGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = I18nManager.t("update.available"),
                                        color = AetherGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "AetherLink v${info.version}",
                                    color = AetherTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = { UpdateManager.dismissUpdate(context, info.version) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = AetherTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Version comparison badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AetherSurfaceElevated)
                            .border(1.dp, AetherBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CURRENT", color = AetherTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("v${AppConstants.VERSION}", color = AetherTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = AetherCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text("NEW RELEASE", color = AetherGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("v${info.version}", color = AetherTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Release notes preview
                    Column {
                        Text(
                            text = "Release Notes & Improvements",
                            color = AetherTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AetherSurfaceElevated.copy(alpha = 0.5f))
                                .border(1.dp, AetherBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = info.changelog.ifBlank { "Performance improvements and bug fixes." },
                                color = AetherTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (info.apkSize > 0) {
                        val sizeMb = String.format("%.1f MB", info.apkSize / (1024.0 * 1024.0))
                        Text(
                            text = "📦 Package: ${info.apkName ?: "1-AetherLink-Android.apk"} ($sizeMb)",
                            color = AetherTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                AetherPrimaryButton(
                    text = I18nManager.t("update.download"),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        UpdateManager.downloadAndInstall(context, info.apkDownloadUrl)
                    }
                )
            },
            dismissButton = {
                AetherSecondaryButton(
                    text = I18nManager.t("update.dismiss"),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        UpdateManager.dismissUpdate(context, info.version)
                    }
                )
            }
        )
    }
}
