package com.aetherlink.android.feature.clipboard

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.aetherlink.android.service.network.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClipboardSyncActivity : Activity() {

    private var hasSynced = false
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (!hasSynced) {
            hasSynced = true
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Safety timeout to ensure activity dismisses even if focus event is missed
        handler.postDelayed(timeoutRunnable, 2500)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasSynced) {
            hasSynced = true
            handler.removeCallbacks(timeoutRunnable)
            performSync()
        }
    }

    private fun performSync() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.coerceToText(this)?.toString()
                if (!text.isNullOrBlank()) {
                    if (ConnectionManager.isConnected) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val sent = ConnectionManager.sendClipboard(text)
                            withContext(Dispatchers.Main) {
                                if (sent) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Pushed to Laptop: ${text.take(30)}...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        applicationContext,
                                        "Failed to send to Laptop",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                finish()
                                overridePendingTransition(0, 0)
                            }
                        }
                        return
                    } else {
                        Toast.makeText(this, "Connect to Laptop first to sync", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ClipboardSyncActivity", "Error reading clipboard", e)
        }

        finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeoutRunnable)
        super.onDestroy()
    }
}
