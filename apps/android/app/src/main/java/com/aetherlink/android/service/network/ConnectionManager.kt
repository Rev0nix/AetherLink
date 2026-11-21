package com.aetherlink.android.service.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.aetherlink.android.protocol.Message
import com.aetherlink.android.protocol.MessageType
import com.aetherlink.android.protocol.PairRequestPayload
import com.aetherlink.android.protocol.PairResponsePayload
import com.aetherlink.android.protocol.PairStatus
import com.aetherlink.android.service.AetherLinkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class FileTransferRequestPayload(
    @SerialName("file_id") val fileId: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("mime_type") val mimeType: String = ""
)

@Serializable
data class FileChunkPayload(
    @SerialName("file_id") val fileId: String,
    @SerialName("chunk_index") val chunkIndex: Int,
    @SerialName("total_chunks") val totalChunks: Int,
    @SerialName("data_base64") val dataBase64: String
)

@Serializable
data class FileCompletePayload(
    @SerialName("file_id") val fileId: String,
    @SerialName("file_name") val fileName: String,
    val status: String = "SUCCESS"
)

data class LiveTransfer(
    val fileId: String,
    val fileName: String,
    val totalBytes: Long,
    val transferredBytes: Long,
    val progress: Float,
    val isIncoming: Boolean,
    val isCompleted: Boolean,
    val speed: String = ""
)

data class AndroidClipboardItem(
    val id: String,
    val text: String,
    val timestamp: Long,
    val source: String,
    var isPinned: Boolean = false
)

object ConnectionManager {
    var client: TcpClient? = null
    var isConnected: Boolean = false
    private var isManualDisconnect: Boolean = false
    private var isReconnecting: Boolean = false

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var listeningJob: Job? = null
    private var keepAliveJob: Job? = null
    private val activeIncomingFiles = ConcurrentHashMap<String, File>()

    private val _liveTransfers = MutableStateFlow<List<LiveTransfer>>(emptyList())
    val liveTransfers: StateFlow<List<LiveTransfer>> = _liveTransfers.asStateFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _clipboardHistory = MutableStateFlow<List<AndroidClipboardItem>>(emptyList())
    val clipboardHistory: StateFlow<List<AndroidClipboardItem>> = _clipboardHistory.asStateFlow()

    @Volatile
    var lastSyncedText: String = ""
    @Volatile
    var lastSyncTimestamp: Long = 0L

    fun addClipboardItem(text: String, source: String = "Laptop"): AndroidClipboardItem {
        val item = AndroidClipboardItem(
            id = UUID.randomUUID().toString(),
            text = text,
            timestamp = System.currentTimeMillis(),
            source = source
        )
        _clipboardHistory.value = listOf(item) + _clipboardHistory.value.filter { it.text != text }.take(49)
        return item
    }

    fun removeClipboardItem(id: String) {
        _clipboardHistory.value = _clipboardHistory.value.filter { it.id != id }
    }

    fun togglePinClipboardItem(id: String) {
        _clipboardHistory.value = _clipboardHistory.value.map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
    }

    fun clearClipboardHistory() {
        _clipboardHistory.value = _clipboardHistory.value.filter { it.isPinned }
    }

    fun onConnected(context: Context, newClient: TcpClient) {
        client = newClient
        isConnected = true
        isManualDisconnect = false
        isReconnecting = false
        _connectionState.value = true

        AetherLinkService.start(context)
        startListening(context)
        startKeepAlive(context)
        Log.d("ConnectionManager", "onConnected: Background service and keep-alive active")
    }

    fun disconnect(context: Context) {
        Log.d("ConnectionManager", "disconnect: Manual user disconnect requested")
        isManualDisconnect = true
        isConnected = false
        _connectionState.value = false

        stopKeepAlive()
        stopListening()
        client?.disconnect()
        client = null
        AetherLinkService.stop(context)
    }

    fun startListening(context: Context) {
        stopListening()
        val c = client ?: return

        listeningJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d("ConnectionManager", "Starting resilient background TCP reader loop...")
            try {
                while (isActive && isConnected && c.isConnected()) {
                    val line = c.readLine()
                    if (line == null) {
                        Log.d("ConnectionManager", "TCP reader reached EOF or socket disconnected.")
                        break
                    }
                    if (line.isBlank()) {
                        continue
                    }
                    try {
                        val message = json.decodeFromString<Message>(line)
                        handleIncomingMessage(context.applicationContext, message)
                    } catch (e: Exception) {
                        Log.e("ConnectionManager", "Message parse error: ${e.message} for line: $line")
                    }
                }
            } catch (e: Exception) {
                Log.e("ConnectionManager", "TCP reader error: ${e.message}")
            } finally {
                Log.d("ConnectionManager", "TCP reader loop ended.")
                if (!isManualDisconnect && isConnected) {
                    handleConnectionLoss(context)
                }
            }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
    }

    fun startKeepAlive(context: Context) {
        stopKeepAlive()
        val c = client ?: return

        keepAliveJob = CoroutineScope(Dispatchers.IO).launch {
            var consecutiveFailures = 0
            while (isActive && isConnected) {
                delay(10_000)
                if (!isConnected || isManualDisconnect) break
                try {
                    val ping = Message(
                        type = MessageType.PING,
                        requestId = UUID.randomUUID().toString(),
                        payload = json.encodeToJsonElement(
                            mapOf("timestamp" to System.currentTimeMillis().toString())
                        )
                    )
                    val ok = c.send(ping)
                    if (ok) {
                        consecutiveFailures = 0
                    } else {
                        consecutiveFailures++
                        Log.w("ConnectionManager", "Keep-alive ping failed ($consecutiveFailures/3)")
                        if (consecutiveFailures >= 3) {
                            Log.w("ConnectionManager", "Keep-alive failed 3 times. Connection lost.")
                            handleConnectionLoss(context)
                            break
                        }
                    }
                } catch (e: Exception) {
                    consecutiveFailures++
                    Log.w("ConnectionManager", "Keep-alive ping exception ($consecutiveFailures/3): ${e.message}")
                    if (consecutiveFailures >= 3) {
                        handleConnectionLoss(context)
                        break
                    }
                }
            }
        }
    }

    fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }

    fun handleConnectionLoss(context: Context) {
        if (isManualDisconnect || isReconnecting) return
        Log.w("ConnectionManager", "handleConnectionLoss: Initiating auto-reconnect sequence...")
        isReconnecting = true
        isConnected = false
        _connectionState.value = false

        stopKeepAlive()
        stopListening()
        client?.disconnect()
        client = null

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("aetherlink_prefs", Context.MODE_PRIVATE)
            val lastIp = prefs.getString("last_connected_ip", null)
            val deviceId = prefs.getString("device_id", UUID.randomUUID().toString()) ?: UUID.randomUUID().toString()
            val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

            if (!lastIp.isNullOrBlank()) {
                var attempt = 0
                while (attempt < 15 && !isConnected && !isManualDisconnect) {
                    attempt++
                    Log.d("ConnectionManager", "Auto-reconnect attempt $attempt to $lastIp:45876...")
                    delay(3000)
                    if (isManualDisconnect) break

                    try {
                        val testClient = TcpClient(lastIp, 45876)
                        if (testClient.connect(timeoutMs = 3500)) {
                            // 1. Send HELLO
                            val hello = Message(
                                type = MessageType.HELLO,
                                requestId = UUID.randomUUID().toString(),
                                payload = json.encodeToJsonElement(mapOf("client" to "AetherLink Android AutoReconnect"))
                            )
                            testClient.send(hello)
                            val helloReply = testClient.receive()

                            if (helloReply?.type == MessageType.WELCOME) {
                                // 2. Send PAIR_REQUEST (already paired/trusted device)
                                val pairReq = Message(
                                    type = MessageType.PAIR_REQUEST,
                                    requestId = UUID.randomUUID().toString(),
                                    payload = json.encodeToJsonElement(
                                        PairRequestPayload(
                                            deviceId = deviceId,
                                            deviceName = deviceName,
                                            platform = "Android",
                                            pin = null
                                        )
                                    )
                                )
                                testClient.send(pairReq)
                                val pairReply = testClient.receive()

                                if (pairReply?.type == MessageType.PAIR_RESPONSE) {
                                    val resp: PairResponsePayload = json.decodeFromJsonElement(pairReply.payload)
                                    if (resp.status == PairStatus.PAIRED || resp.status == PairStatus.CONFIRMED) {
                                        Log.d("ConnectionManager", "Auto-reconnect successful!")
                                        isReconnecting = false
                                        onConnected(context.applicationContext, testClient)
                                        return@launch
                                    }
                                }
                            }
                            testClient.disconnect()
                        }
                    } catch (e: Exception) {
                        Log.d("ConnectionManager", "Auto-reconnect attempt $attempt failed: ${e.message}")
                    }
                }
            }
            isReconnecting = false
            AetherLinkService.stop(context.applicationContext)
        }
    }

    private suspend fun handleIncomingMessage(context: Context, message: Message) {
        when (message.type) {
            MessageType.FILE_REQUEST -> {
                try {
                    val req = json.decodeFromJsonElement<FileTransferRequestPayload>(message.payload)
                    val safeName = File(req.fileName).name.ifEmpty { "downloaded_file" }
                    Log.d("ConnectionManager", "Received FILE_REQUEST for $safeName (${req.fileSize} bytes)")

                    // Store chunks in application cache to avoid any Android Scoped Storage / EACCES permissions
                    val tempFile = File(context.cacheDir, "aether_recv_${req.fileId}.tmp")
                    if (tempFile.exists()) tempFile.delete()
                    tempFile.createNewFile()
                    activeIncomingFiles[req.fileId] = tempFile

                    // Track in live transfers
                    updateTransfer(
                        LiveTransfer(
                            fileId = req.fileId,
                            fileName = safeName,
                            totalBytes = req.fileSize,
                            transferredBytes = 0,
                            progress = 0.05f,
                            isIncoming = true,
                            isCompleted = false,
                            speed = "Receiving..."
                        )
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Receiving \"$safeName\" from Laptop...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // Reply ACK
                    val ack = Message(
                        type = MessageType.COMMAND,
                        requestId = message.requestId,
                        payload = json.encodeToJsonElement(
                            mapOf("status" to "READY", "file_id" to req.fileId)
                        )
                    )
                    client?.send(ack)
                } catch (e: Exception) {
                    Log.e("ConnectionManager", "Error handling FILE_REQUEST", e)
                }
            }

            MessageType.FILE_CHUNK -> {
                try {
                    val chunk = json.decodeFromJsonElement<FileChunkPayload>(message.payload)
                    val tempFile = activeIncomingFiles[chunk.fileId]
                    if (tempFile != null) {
                        val bytes = Base64.decode(chunk.dataBase64, Base64.NO_WRAP)
                        FileOutputStream(tempFile, true).use { fos ->
                            fos.write(bytes)
                        }

                        val progress = if (chunk.totalChunks > 0) {
                            (chunk.chunkIndex + 1).toFloat() / chunk.totalChunks
                        } else {
                            1.0f
                        }

                        val existing = _liveTransfers.value.find { it.fileId == chunk.fileId }
                        if (existing != null) {
                            val newTransferred = existing.transferredBytes + bytes.size
                            updateTransfer(
                                existing.copy(
                                    transferredBytes = newTransferred,
                                    progress = progress,
                                    speed = "Receiving chunk ${chunk.chunkIndex + 1}/${chunk.totalChunks}"
                                )
                            )
                        }
                    }

                    // Send ACK
                    val ack = Message(
                        type = MessageType.COMMAND,
                        requestId = message.requestId,
                        payload = json.encodeToJsonElement(
                            mapOf("status" to "CHUNK_ACK", "chunk" to chunk.chunkIndex.toString())
                        )
                    )
                    client?.send(ack)
                } catch (e: Exception) {
                    Log.e("ConnectionManager", "Error handling FILE_CHUNK", e)
                }
            }

            MessageType.FILE_COMPLETE -> {
                try {
                    val comp = json.decodeFromJsonElement<FileCompletePayload>(message.payload)
                    val safeName = File(comp.fileName).name.ifEmpty { "downloaded_file" }
                    Log.d("ConnectionManager", "FILE_COMPLETE received for $safeName")
                    val tempFile = activeIncomingFiles.remove(comp.fileId)

                    if (tempFile != null) {
                        // Transfer from cache directly into the public Downloads directory using MediaStore
                        saveFileToDownloads(context, tempFile, safeName)

                        val existing = _liveTransfers.value.find { it.fileId == comp.fileId }
                        if (existing != null) {
                            updateTransfer(
                                existing.copy(
                                    progress = 1.0f,
                                    isCompleted = true,
                                    speed = "Saved to Downloads"
                                )
                            )
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Received \"$safeName\" saved to Downloads!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    val ack = Message(
                        type = MessageType.COMMAND,
                        requestId = message.requestId,
                        payload = json.encodeToJsonElement(mapOf("status" to "COMPLETED"))
                    )
                    client?.send(ack)
                } catch (e: Exception) {
                    Log.e("ConnectionManager", "Error handling FILE_COMPLETE", e)
                }
            }

            MessageType.CLIPBOARD -> {
                try {
                    val payloadObj = message.payload as? JsonObject
                    val content = (payloadObj?.get("content") as? JsonPrimitive)?.content
                        ?: (message.payload as? JsonPrimitive)?.content

                    if (!content.isNullOrEmpty()) {
                        Log.d("ConnectionManager", "Received clipboard from Laptop (${content.length} chars)")
                        lastSyncedText = content
                        lastSyncTimestamp = System.currentTimeMillis()
                        addClipboardItem(content, "Laptop")

                        withContext(Dispatchers.Main) {
                            try {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("AetherLink", content))
                            } catch (e: Exception) {
                                Log.w("ConnectionManager", "Background setPrimaryClip: ${e.message}")
                            }
                            Toast.makeText(context, "Clipboard synced from Laptop", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ConnectionManager", "Error handling CLIPBOARD", e)
                }
            }

            MessageType.PING -> {
                val pong = Message(
                    type = MessageType.PONG,
                    requestId = message.requestId,
                    payload = json.encodeToJsonElement(
                        mapOf("timestamp" to System.currentTimeMillis().toString())
                    )
                )
                client?.send(pong)
            }

            MessageType.PONG -> {
                Log.d("ConnectionManager", "Heartbeat PONG confirmed from Laptop")
            }

            else -> {
                Log.d("ConnectionManager", "Unhandled message type: ${message.type}")
            }
        }
    }

    private fun updateTransfer(transfer: LiveTransfer) {
        val current = _liveTransfers.value.toMutableList()
        val index = current.indexOfFirst { it.fileId == transfer.fileId }
        if (index >= 0) {
            current[index] = transfer
        } else {
            current.add(0, transfer)
        }
        _liveTransfers.value = current
    }

    private fun saveFileToDownloads(context: Context, tempFile: File, fileName: String) {
        try {
            // Android 10+ (API 29+) Scoped Storage MediaStore write
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        tempFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    Log.d("ConnectionManager", "Successfully saved to MediaStore.Downloads: $fileName")
                    return
                }
            }

            // Direct file system fallback for Android 9 and below
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fileName)
            tempFile.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
            Log.d("ConnectionManager", "Saved directly to Downloads: ${destFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("ConnectionManager", "Failed to save to public Downloads, trying app files", e)
            try {
                val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val fallbackFile = File(fallbackDir, fileName)
                tempFile.copyTo(fallbackFile, overwrite = true)
                Log.d("ConnectionManager", "Saved to fallback dir: ${fallbackFile.absolutePath}")
            } catch (e2: Exception) {
                Log.e("ConnectionManager", "Fatal: could not save file anywhere", e2)
            }
        } finally {
            try {
                tempFile.delete()
            } catch (_: Exception) {}
        }
    }

    suspend fun sendClipboard(text: String): Boolean = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext false
        if (text.isBlank()) return@withContext false
        val now = System.currentTimeMillis()
        if (text == lastSyncedText && now - lastSyncTimestamp < 3000L) {
            return@withContext true
        }
        lastSyncedText = text
        lastSyncTimestamp = now
        addClipboardItem(text, "Phone")

        try {
            val payload = buildJsonObject {
                put("content", text)
                put("timestamp", now)
                put("source_device", Build.MODEL ?: "Android Phone")
            }
            val message = Message(
                type = MessageType.CLIPBOARD,
                requestId = UUID.randomUUID().toString(),
                payload = payload
            )
            c.send(message)
            Log.d("ConnectionManager", "Successfully pushed clipboard to Laptop (${text.length} chars)")
            true
        } catch (e: Exception) {
            Log.e("ConnectionManager", "Failed to send clipboard", e)
            false
        }
    }

    suspend fun sendNotification(
        id: String,
        appName: String,
        packageName: String,
        title: String,
        text: String
    ): Boolean = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext false
        try {
            val payload = buildJsonObject {
                put("id", id)
                put("app_name", appName)
                put("package_name", packageName)
                put("title", title)
                put("text", text)
                put("timestamp", System.currentTimeMillis())
            }
            val message = Message(
                type = MessageType.NOTIFICATION,
                requestId = UUID.randomUUID().toString(),
                payload = payload
            )
            c.send(message)
            true
        } catch (e: Exception) {
            Log.e("ConnectionManager", "Failed to send notification", e)
            false
        }
    }

    suspend fun sendFileRequest(
        fileId: String,
        fileName: String,
        fileSize: Long,
        mimeType: String
    ): Boolean = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext false
        try {
            val message = Message(
                type = MessageType.FILE_REQUEST,
                requestId = UUID.randomUUID().toString(),
                payload = json.encodeToJsonElement(
                    FileTransferRequestPayload(
                        fileId = fileId,
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType
                    )
                )
            )
            c.send(message)
            true
        } catch (e: Exception) {
            Log.e("ConnectionManager", "Failed to send file request", e)
            false
        }
    }

    suspend fun sendFileChunk(
        fileId: String,
        chunkIndex: Int,
        totalChunks: Int,
        base64Data: String
    ): Boolean = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext false
        try {
            val message = Message(
                type = MessageType.FILE_CHUNK,
                requestId = UUID.randomUUID().toString(),
                payload = json.encodeToJsonElement(
                    FileChunkPayload(
                        fileId = fileId,
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                        dataBase64 = base64Data
                    )
                )
            )
            c.send(message)
            true
        } catch (e: Exception) {
            Log.e("ConnectionManager", "Failed to send file chunk", e)
            false
        }
    }

    suspend fun sendFileComplete(fileId: String, fileName: String): Boolean = withContext(Dispatchers.IO) {
        val c = client ?: return@withContext false
        try {
            val message = Message(
                type = MessageType.FILE_COMPLETE,
                requestId = UUID.randomUUID().toString(),
                payload = json.encodeToJsonElement(
                    FileCompletePayload(
                        fileId = fileId,
                        fileName = fileName,
                        status = "SUCCESS"
                    )
                )
            )
            c.send(message)
            true
        } catch (e: Exception) {
            Log.e("ConnectionManager", "Failed to send file complete", e)
            false
        }
    }
}
