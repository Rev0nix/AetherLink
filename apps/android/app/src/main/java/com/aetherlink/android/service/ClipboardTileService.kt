package com.aetherlink.android.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.aetherlink.android.feature.clipboard.ClipboardSyncActivity
import com.aetherlink.android.service.network.ConnectionManager

@RequiresApi(Build.VERSION_CODES.N)
class ClipboardTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        if (ConnectionManager.isConnected) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Sync to Laptop"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "AetherLink Offline"
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, ClipboardSyncActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
