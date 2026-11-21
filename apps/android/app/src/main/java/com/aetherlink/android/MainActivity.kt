package com.aetherlink.android

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherLinkTheme
import com.aetherlink.android.core.designsystem.ThemeManager
import com.aetherlink.android.core.i18n.I18nManager
import com.aetherlink.android.core.update.AetherUpdateModal
import com.aetherlink.android.core.update.UpdateManager
import com.aetherlink.android.service.network.ConnectionManager
import com.aetherlink.android.ui.navigation.AppNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager.init(this)
        I18nManager.init(this)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Silent background check for updates on startup
        UpdateManager.checkForUpdates(this, manual = false)

        setContent {
            AetherLinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AetherBg
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation()
                        AetherUpdateModal()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndSyncClipboard()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkAndSyncClipboard()
        }
    }

    private fun checkAndSyncClipboard() {
        if (ConnectionManager.isConnected) {
            try {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = cm?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0)?.coerceToText(this)?.toString()
                    if (!text.isNullOrBlank() && text != ConnectionManager.lastSyncedText) {
                        CoroutineScope(Dispatchers.IO).launch {
                            ConnectionManager.sendClipboard(text)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
