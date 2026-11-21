package com.aetherlink.android.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object InAppUpdateInstaller {
    private const val TAG = "InAppUpdateInstaller"

    var isDownloading by mutableStateOf(false)
        private set

    var downloadProgress by mutableFloatStateOf(0f)
        private set

    var statusText by mutableStateOf("")
        private set

    var pendingInstallFile: File? = null

    fun checkPendingInstall(context: Context) {
        val file = pendingInstallFile ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
                pendingInstallFile = null
                installApk(context, file)
            }
        } else {
            pendingInstallFile = null
            installApk(context, file)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    pendingInstallFile = apkFile
                    Toast.makeText(context, "Please grant permission to install updates", Toast.LENGTH_LONG).show()
                    val permissionIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            Toast.makeText(context, "Could not open installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun startDirectDownloadAndInstall(context: Context, downloadUrl: String) {
        if (isDownloading) return

        isDownloading = true
        downloadProgress = 0f
        statusText = "Connecting to update server..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var currentUrl = downloadUrl
                var conn: HttpURLConnection
                var redirects = 0

                // Follow redirects manually across domains (GitHub releases redirect to AWS S3/Azure Blob CDN)
                while (true) {
                    val url = URL(currentUrl)
                    conn = url.openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 30000
                    conn.readTimeout = 60000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) AetherLink-Updater")
                    conn.setRequestProperty("Accept", "*/*")

                    val code = conn.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
                        code == HttpURLConnection.HTTP_MOVED_PERM ||
                        code == HttpURLConnection.HTTP_SEE_OTHER ||
                        code == 307 || code == 308
                    ) {
                        val location = conn.getHeaderField("Location") ?: throw IOException("Redirect without Location header")
                        currentUrl = URL(URL(currentUrl), location).toString()
                        conn.disconnect()
                        redirects++
                        if (redirects > 10) throw IOException("Too many redirects: $redirects")
                    } else if (code in 200..299) {
                        break
                    } else {
                        throw IOException("Server returned HTTP $code")
                    }
                }

                val contentLength = conn.contentLengthLong

                val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updateDir, "1-AetherLink-Android.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val inputStream: InputStream = conn.inputStream
                val outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(65536)
                var bytesRead: Int
                var totalRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    if (contentLength > 0) {
                        downloadProgress = (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                    }

                    val mbRead = totalRead / (1024f * 1024f)
                    val mbTotal = if (contentLength > 0) contentLength / (1024f * 1024f) else 13.7f
                    val percent = if (contentLength > 0) (downloadProgress * 100).toInt() else 0
                    statusText = String.format("Downloading: %.1f MB / %.1f MB (%d%%)", mbRead, mbTotal, percent)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    isDownloading = false
                    statusText = "Download complete! Opening package installer..."
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct in-app download failed", e)
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    statusText = "Download failed: ${e.localizedMessage ?: e.message}"
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
