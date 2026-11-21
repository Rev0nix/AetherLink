package com.aetherlink.android.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
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
    val apkDownloadUrl: String?,
    val apkName: String?,
    val apkSize: Long
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val PREFS_NAME = "aetherlink_prefs"
    private const val KEY_DISMISSED_VERSION = "dismissed_update_version"

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
                    val body = json.optString("body", "No changelog provided.")
                    val htmlUrl = json.optString("html_url", AppConstants.RELEASES_WEB_URL)
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
                        version = tagName,
                        title = title,
                        changelog = body,
                        releaseUrl = htmlUrl,
                        publishedAt = publishedAt,
                        apkDownloadUrl = matchedApkUrl ?: htmlUrl,
                        apkName = matchedApkName ?: "1-AetherLink-Android.apk",
                        apkSize = matchedApkSize
                    )

                    withContext(Dispatchers.Main) {
                        updateInfo = info
                        isUpdateAvailable = hasNewer
                        isChecking = false

                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val dismissed = prefs.getString(KEY_DISMISSED_VERSION, null)

                        if (hasNewer) {
                            statusMessage = "Update available: v$tagName"
                            if (manual || dismissed != tagName) {
                                showDialog = true
                            }
                        } else {
                            statusMessage = "You're up to date! (v${AppConstants.VERSION})"
                            if (manual) {
                                Toast.makeText(context, "AetherLink is up to date (v${AppConstants.VERSION})", Toast.LENGTH_SHORT).show()
                            }
                        }
                        onComplete?.invoke(hasNewer)
                    }
                } else {
                    conn.disconnect()
                    withContext(Dispatchers.Main) {
                        isChecking = false
                        statusMessage = "Could not check for updates"
                        if (manual) {
                            Toast.makeText(context, "Could not check for updates ($responseCode)", Toast.LENGTH_SHORT).show()
                        }
                        onComplete?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                withContext(Dispatchers.Main) {
                    isChecking = false
                    statusMessage = "Update check failed: ${e.message}"
                    if (manual) {
                        Toast.makeText(context, "Update check failed", Toast.LENGTH_SHORT).show()
                    }
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun dismissUpdate(context: Context, version: String) {
        showDialog = false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_VERSION, version).apply()
    }

    fun downloadAndInstall(context: Context, targetUrl: String?) {
        val urlToOpen = targetUrl ?: updateInfo?.apkDownloadUrl ?: updateInfo?.releaseUrl
        if (!urlToOpen.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open download link: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        showDialog = false
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
