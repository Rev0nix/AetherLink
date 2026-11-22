package com.vexyron.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vexyron.android.MainActivity
import com.vexyron.android.R
import com.vexyron.android.feature.clipboard.ClipboardSyncActivity
import com.vexyron.android.service.network.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VexyronService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var clipboardManager: ClipboardManager? = null
    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "VexyronService"
        private const val CHANNEL_ID = "vexyron_background_connection"
        private const val NOTIFICATION_ID = 45876
        const val ACTION_START = "com.vexyron.android.ACTION_START"
        const val ACTION_STOP = "com.vexyron.android.ACTION_STOP"
        const val ACTION_DISCONNECT = "com.vexyron.android.ACTION_DISCONNECT"
        const val ACTION_SYNC_CLIPBOARD = "com.vexyron.android.ACTION_SYNC_CLIPBOARD"

        fun start(context: Context) {
            try {
                val intent = Intent(context, VexyronService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Sent startForegroundService intent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VexyronService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, VexyronService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop VexyronService", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
        setupClipboardMonitoring()
        Log.d(TAG, "VexyronService created")
    }

    private fun setupClipboardMonitoring() {
        try {
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipListener = ClipboardManager.OnPrimaryClipChangedListener {
                try {
                    val clip = clipboardManager?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0)?.coerceToText(this)?.toString()
                        if (!text.isNullOrBlank() && text != ConnectionManager.lastSyncedText) {
                            Log.d(TAG, "Local clipboard changed, beaming to Laptop: ${text.take(30)}...")
                            serviceScope.launch {
                                ConnectionManager.sendClipboard(text)
                            }
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && android.provider.Settings.canDrawOverlays(this)) {
                        val syncIntent = Intent(this, ClipboardSyncActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        }
                        startActivity(syncIntent)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not read clipboard: ${e.message}")
                }
            }
            clipboardManager?.addPrimaryClipChangedListener(clipListener)
            Log.d(TAG, "Registered clipboard change listener")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register clipboard listener: ${e.message}")
        }
    }

    private fun teardownClipboardMonitoring() {
        try {
            clipListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }
        } catch (_: Exception) {}
        clipListener = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                Log.d(TAG, "User requested disconnect from notification")
                ConnectionManager.disconnect(applicationContext)
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping foreground service")
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_SYNC_CLIPBOARD -> {
                Log.d(TAG, "Notification action: Sync Clipboard triggered")
                serviceScope.launch {
                    try {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = cm?.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0)?.coerceToText(applicationContext)?.toString()
                            if (!text.isNullOrBlank()) {
                                ConnectionManager.sendClipboard(text)
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        applicationContext,
                                        "Pushed to Laptop: ${text.take(30)}...",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(applicationContext, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(applicationContext, "Clipboard is empty", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed syncing clipboard from action: ${e.message}")
                    }
                }
                return START_STICKY
            }
            else -> {
                val notification = buildForegroundNotification()
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    Log.d(TAG, "Foreground notification started successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "startForeground error", e)
                }
                return START_STICKY
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VEXYRON Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps wireless connection active with Laptop companion"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, VexyronService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val pendingDisconnect = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = Intent(this, ClipboardSyncActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingSync = PendingIntent.getActivity(
            this,
            2,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VEXYRON Connected")
            .setContentText("Connected to Laptop • Seamless file & clipboard sync")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .addAction(0, "📋 Sync to Laptop", pendingSync)
            .addAction(0, "Disconnect", pendingDisconnect)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "VEXYRON::ConnectionWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire(12 * 60 * 60 * 1000L) // 12 hours max
                }
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiLock == null) {
                @Suppress("DEPRECATION")
                wifiLock = wifiManager?.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "VEXYRON::ConnectionWifiLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
            Log.d(TAG, "Acquired WakeLock and WifiLock")
        } catch (e: Exception) {
            Log.w(TAG, "Error acquiring Wake/Wifi lock: ${e.message}")
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null

            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock = null
            Log.d(TAG, "Released WakeLock and WifiLock")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Wake/Wifi lock: ${e.message}")
        }
    }

    private fun stopForegroundService() {
        teardownClipboardMonitoring()
        releaseLocks()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        teardownClipboardMonitoring()
        releaseLocks()
        super.onDestroy()
        Log.d(TAG, "VexyronService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
