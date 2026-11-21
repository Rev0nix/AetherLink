package com.aetherlink.android.feature.clipboard

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.aetherlink.android.service.network.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val text: String? = when (intent?.action) {
                Intent.ACTION_PROCESS_TEXT -> {
                    intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                }
                Intent.ACTION_SEND -> {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                        ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                }
                else -> {
                    intent?.getStringExtra(Intent.EXTRA_TEXT)
                }
            }

            if (!text.isNullOrBlank()) {
                // 1. Put on local phone clipboard so user can also paste locally if they want
                try {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("AetherLink", text))
                } catch (e: Exception) {
                    Log.w("ProcessTextActivity", "Error setting local clip: ${e.message}")
                }

                // 2. Beam directly to Laptop companion
                if (ConnectionManager.isConnected) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val sent = ConnectionManager.sendClipboard(text)
                        withContext(Dispatchers.Main) {
                            if (sent) {
                                Toast.makeText(
                                    applicationContext,
                                    "Copied & Sent to Laptop!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Copied! (Send failed, check Laptop connection)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            finish()
                            overridePendingTransition(0, 0)
                        }
                    }
                    return
                } else {
                    Toast.makeText(
                        this,
                        "Copied! (Connect to Laptop to sync)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ProcessTextActivity", "Error processing text", e)
        }

        finish()
        overridePendingTransition(0, 0)
    }
}
