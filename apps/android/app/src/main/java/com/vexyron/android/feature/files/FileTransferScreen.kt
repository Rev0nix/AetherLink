package com.vexyron.android.feature.files

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.vexyron.android.service.network.ConnectionManager
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary

data class TransferItem(
    val id: String,
    val name: String,
    val size: String,
    val isUpload: Boolean,
    var progress: Float,
    var speed: String,
    var isPaused: Boolean = false,
    var isCompleted: Boolean = false
)

@Composable
fun FileTransferScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("All") }
    val liveIncomingTransfers by ConnectionManager.liveTransfers.collectAsState()
    val transfers = remember {
        mutableStateListOf<TransferItem>()
    }

    val allTransfers = remember(transfers.toList(), liveIncomingTransfers) {
        val incoming = liveIncomingTransfers.map { lt ->
            TransferItem(
                id = lt.fileId,
                name = lt.fileName,
                size = if (lt.totalBytes > 0) "${lt.totalBytes / 1024} KB" else "",
                isUpload = !lt.isIncoming,
                progress = lt.progress,
                speed = lt.speed,
                isCompleted = lt.isCompleted
            )
        }
        val incomingIds = incoming.map { it.id }.toSet()
        val localOnly = transfers.filter { it.id !in incomingIds }
        incoming + localOnly
    }

    // Android Storage Access Framework File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEachIndexed { index, uri ->
            val fileName = resolveFileName(context, uri)
            val transferId = System.currentTimeMillis().toString() + index
            val initialItem = TransferItem(
                id = transferId,
                name = fileName,
                size = "Reading...",
                isUpload = true,
                progress = 0.05f,
                speed = "Preparing..."
            )
            transfers.add(0, initialItem)

            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        withContext(Dispatchers.Main) {
                            val idx = transfers.indexOfFirst { it.id == transferId }
                            if (idx != -1) transfers[idx] = transfers[idx].copy(speed = "Cannot open file")
                        }
                        return@launch
                    }

                    val totalBytes = resolveFileSize(context, uri)
                    val fileId = UUID.randomUUID().toString()
                    val chunkSize = 32 * 1024
                    val totalChunks = if (totalBytes <= 0) 1 else ((totalBytes + chunkSize - 1) / chunkSize).toInt()

                    val ready = ConnectionManager.sendFileRequest(
                        fileId = fileId,
                        fileName = fileName,
                        fileSize = totalBytes,
                        mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    )

                    if (ready) {
                        val buffer = ByteArray(chunkSize)
                        var chunkIndex = 0
                        var sentBytes = 0L

                        inputStream.use { stream ->
                            while (true) {
                                val read = stream.read(buffer)
                                if (read <= 0) break

                                val chunkSlice = if (read == buffer.size) buffer else buffer.copyOfRange(0, read)
                                val base64 = android.util.Base64.encodeToString(chunkSlice, android.util.Base64.NO_WRAP)

                                val ok = ConnectionManager.sendFileChunk(fileId, chunkIndex, totalChunks, base64)
                                if (!ok) {
                                    throw java.io.IOException("Socket connection closed during file stream")
                                }

                                sentBytes += read
                                val progress = if (totalBytes > 0) (sentBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 1f

                                withContext(Dispatchers.Main) {
                                    val idx = transfers.indexOfFirst { it.id == transferId }
                                    if (idx != -1) {
                                        transfers[idx] = transfers[idx].copy(
                                            progress = progress,
                                            speed = "Chunk ${chunkIndex + 1}/$totalChunks",
                                            size = "${sentBytes / 1024} / ${totalBytes / 1024} KB"
                                        )
                                    }
                                }

                                chunkIndex++
                                delay(2)
                            }
                        }

                        ConnectionManager.sendFileComplete(fileId, fileName)

                        withContext(Dispatchers.Main) {
                            val idx = transfers.indexOfFirst { it.id == transferId }
                            if (idx != -1) {
                                transfers[idx] = transfers[idx].copy(
                                    progress = 1.0f,
                                    isCompleted = true,
                                    speed = "Saved to Desktop"
                                )
                            }
                            Toast.makeText(context, "$fileName saved to Desktop!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            val idx = transfers.indexOfFirst { it.id == transferId }
                            if (idx != -1) {
                                transfers[idx] = transfers[idx].copy(
                                    speed = "Offline",
                                    size = "${totalBytes / 1024} KB"
                                )
                            }
                            Toast.makeText(context, "Connect to desktop to transfer files", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val idx = transfers.indexOfFirst { it.id == transferId }
                        if (idx != -1) {
                            transfers[idx] = transfers[idx].copy(speed = "Error")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VexyronBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            VexyronHeader(
                title = "Files",
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VexyronSurfaceElevated)
                                .border(1.dp, VexyronBorder, CircleShape)
                                .clickable {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Upload",
                                tint = VexyronCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }

        // CATEGORY SELECTOR CARDS
        item {
            Text(
                text = "Categories",
                color = VexyronTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryPill(
                    title = "Images",
                    icon = Icons.Default.Image,
                    tint = VexyronPurple,
                    modifier = Modifier.weight(1f)
                ) { filePickerLauncher.launch(arrayOf("image/*")) }

                CategoryPill(
                    title = "Videos",
                    icon = Icons.Default.VideoFile,
                    tint = VexyronCyan,
                    modifier = Modifier.weight(1f)
                ) { filePickerLauncher.launch(arrayOf("video/*")) }

                CategoryPill(
                    title = "Docs",
                    icon = Icons.Default.Description,
                    tint = VexyronGreen,
                    modifier = Modifier.weight(1f)
                ) { filePickerLauncher.launch(arrayOf("application/*", "text/*")) }

                CategoryPill(
                    title = "Audio",
                    icon = Icons.Default.AudioFile,
                    tint = VexyronOrange,
                    modifier = Modifier.weight(1f)
                ) { filePickerLauncher.launch(arrayOf("audio/*")) }
            }
        }

        // ACTIVE TRANSFERS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Transfers",
                    color = VexyronTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                VexyronStatusBadge(
                    text = "${allTransfers.count { !it.isCompleted }} Active",
                    indicatorColor = VexyronCyan
                )
            }
        }

        val activeList = allTransfers.filter { !it.isCompleted }
        if (activeList.isEmpty()) {
            item {
                VexyronCard(backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.4f)) {
                    Text(
                        text = "No active transfers. Select files to send to desktop.",
                        color = VexyronTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(activeList) { transfer ->
                ActiveTransferCard(
                    transfer = transfer,
                    onTogglePause = {
                        transfer.isPaused = !transfer.isPaused
                    },
                    onCancel = {
                        transfers.removeAll { it.id == transfer.id }
                    }
                )
            }
        }

        // TRANSFER HISTORY
        item {
            Text(
                text = "Transfer History",
                color = VexyronTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val historyList = allTransfers.filter { it.isCompleted }
        items(historyList) { transfer ->
            TransferHistoryRow(transfer = transfer)
        }
    }
}

@Composable
fun CategoryPill(
    title: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    VexyronCard(
        modifier = modifier.clickable { onClick() },
        contentPadding = 12.dp,
        backgroundColor = VexyronSurfaceElevated
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                color = VexyronTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ActiveTransferCard(
    transfer: TransferItem,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit
) {
    VexyronCard(borderColor = VexyronCyan.copy(alpha = 0.4f)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (transfer.isUpload) VexyronPurple.copy(alpha = 0.15f)
                            else VexyronCyan.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transfer.isUpload) Icons.Default.Upload else Icons.Default.Download,
                        contentDescription = null,
                        tint = if (transfer.isUpload) VexyronPurple else VexyronCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transfer.name,
                        color = VexyronTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transfer.size} • ${transfer.speed}",
                        color = VexyronTextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(VexyronSurfaceElevated)
                            .clickable { onTogglePause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transfer.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume",
                            tint = VexyronTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(VexyronSurfaceElevated)
                            .clickable { onCancel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel",
                            tint = VexyronTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { transfer.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = VexyronCyan,
                trackColor = VexyronSurfaceElevated,
            )
        }
    }
}

@Composable
fun TransferHistoryRow(transfer: TransferItem) {
    VexyronCard(
        contentPadding = 12.dp,
        backgroundColor = VexyronSurfaceElevated.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VexyronGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = VexyronGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transfer.name,
                    color = VexyronTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (transfer.isUpload) "Uploaded to Desktop • ${transfer.size}"
                    else "Received • ${transfer.size}",
                    color = VexyronTextMuted,
                    fontSize = 11.sp
                )
            }

            VexyronStatusBadge(
                text = "Completed",
                indicatorColor = VexyronGreen
            )
        }
    }
}

private fun resolveFileName(context: android.content.Context, uri: Uri): String {
    var name: String? = null
    try {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx != -1) {
                    name = cursor.getString(idx)
                }
            }
        }
    } catch (_: Exception) {}

    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
    }

    if (name.isNullOrBlank()) {
        val mime = context.contentResolver.getType(uri) ?: ""
        val ext = when {
            mime.contains("jpeg") || mime.contains("jpg") -> ".jpg"
            mime.contains("png") -> ".png"
            mime.contains("pdf") -> ".pdf"
            mime.contains("mp4") -> ".mp4"
            else -> ""
        }
        name = "transfer_${System.currentTimeMillis()}$ext"
    }

    return name.replace("[/\\\\:*?\"<>|]".toRegex(), "_")
}

private fun resolveFileSize(context: android.content.Context, uri: Uri): Long {
    var size = -1L
    try {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (idx != -1 && !cursor.isNull(idx)) {
                    size = cursor.getLong(idx)
                }
            }
        }
    } catch (_: Exception) {}

    if (size <= 0) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                size = pfd.statSize
            }
        } catch (_: Exception) {}
    }
    return if (size >= 0) size else 0L
}
