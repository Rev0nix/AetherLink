package com.aetherlink.android.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aetherlink.android.core.constants.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val title: String,
    val changelog: String,
    val releaseUrl: String,
    val publishedAt: String,
    val apkDownloadUrl: String,
    val apkName: String,
    val apkSize: Long
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val PREFS_NAME = "aetherlink_prefs"
    private const val KEY_DISMISSED_VERSION = "dismissed_update_version"

    const val FALLBACK_APK_URL = "https://github.com/Rev0nix/AetherLink/releases/download/v2.2.6/1-AetherLink-Android.apk"
    const val FALLBACK_RELEASE_URL = "https://github.com/Rev0nix/AetherLink/releases/latest"

    var isChecking by mutableStateOf(false)
        private set

    var isUpdateAvailable by mutableStateOf(false)
        private set

    var showDialog by mutableStateOf(false)

    var updateInfo by mutableStateOf<UpdateInfo?>(null)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    fun checkForUpdates(
        context: Context,
        manual: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (isChecking) return

        isChecking = true
        statusMessage = "Checking for updates..."
        if (manual) {
            Toast.makeText(context, "Checking for latest release...", Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(AppConstants.RELEASES_API_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "AetherLink-Android-${AppConstants.VERSION}")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.use { it.readText() }
                    conn.disconnect()

                    val json = JSONObject(response)
                    val tagName = json.optString("tag_name", "").removePrefix("v")
                    val title = json.optString("name", "AetherLink v$tagName")
                    val body = json.optString("body", "Universal device synchronization improvements and optimizations.")
                    val htmlUrl = json.optString("html_url", FALLBACK_RELEASE_URL)
                    val publishedAt = json.optString("published_at", "")

                    var matchedApkUrl: String? = null
                    var matchedApkName: String? = null
                    var matchedApkSize: Long = 0

                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            val downloadUrl = asset.optString("browser_download_url", "")
                            val size = asset.optLong("size", 0)

                            if (name.endsWith(".apk", ignoreCase = true)) {
                                matchedApkUrl = downloadUrl
                                matchedApkName = name
                                matchedApkSize = size
                                if (name.contains("Android", ignoreCase = true)) {
                                    break
                                }
                            }
                        }
                    }

                    val hasNewer = isNewer(tagName, AppConstants.VERSION)

                    val info = UpdateInfo(
                        version = tagName.ifBlank { AppConstants.VERSION },
                        title = title,
                        changelog = body,
                        releaseUrl = htmlUrl,
                        publishedAt = publishedAt,
                        apkDownloadUrl = matchedApkUrl ?: FALLBACK_APK_URL,
                        apkName = matchedApkName ?: "1-AetherLink-Android.apk",
                        apkSize = matchedApkSize
                    )

                    withContext(Dispatchers.Main) {
                        updateInfo = info
                        isUpdateAvailable = hasNewer
                        isChecking = false

                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val dismissed = prefs.getString(KEY_DISMISSED_VERSION, null)

                        if (manual) {
                            showDialog = true
                        } else if (hasNewer && dismissed != tagName) {
                            showDialog = true
                        }

                        statusMessage = if (hasNewer) "Update available: v$tagName" else "Up to date (v${AppConstants.VERSION})"
                        onComplete?.invoke(hasNewer)
                    }
                } else {
                    conn.disconnect()
                    fallbackManualDialog(context, manual, onComplete)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                fallbackManualDialog(context, manual, onComplete)
            }
        }
    }

    private suspend fun fallbackManualDialog(
        context: Context,
        manual: Boolean,
        onComplete: ((Boolean) -> Unit)?
    ) {
        withContext(Dispatchers.Main) {
            isChecking = false
            statusMessage = "v${AppConstants.VERSION} (Official Release)"

            val fallbackInfo = UpdateInfo(
                version = AppConstants.VERSION,
                title = "AetherLink v${AppConstants.VERSION}",
                changelog = "Official release build.\n• Universal Android, Windows & Linux integration\n• High-speed Wi-Fi Direct sync & discovery\n• Multi-theme engine & full localization",
                releaseUrl = FALLBACK_RELEASE_URL,
                publishedAt = "Latest",
                apkDownloadUrl = FALLBACK_APK_URL,
                apkName = "1-AetherLink-Android.apk",
                apkSize = 13703517
            )
            updateInfo = fallbackInfo
            isUpdateAvailable = false

            if (manual) {
                showDialog = true
            }
            onComplete?.invoke(false)
        }
    }

    fun dismissUpdate(context: Context, version: String) {
        showDialog = false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_VERSION, version).apply()
    }

    fun downloadAndInstall(context: Context, targetUrl: String?) {
        val urlToOpen = targetUrl ?: updateInfo?.apkDownloadUrl ?: FALLBACK_APK_URL
        InAppUpdateInstaller.startDirectDownloadAndInstall(context, urlToOpen)
        showDialog = true
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = parseSemver(latest)
        val c = parseSemver(current)
        val maxLen = maxOf(l.size, c.size)
        for (i in 0 until maxLen) {
            val lp = l.getOrElse(i) { 0 }
            val cp = c.getOrElse(i) { 0 }
            if (lp > cp) return true
            if (lp < cp) return false
        }
        return false
    }

    private fun parseSemver(v: String): List<Int> {
        return v.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split("-", "+")[0]
            .split(".")
            .mapNotNull { it.toIntOrNull() }
    }
}
