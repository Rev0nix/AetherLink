package com.aetherlink.android.service.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.util.Log

class NsdDiscoveryManager(
    context: Context
) {

    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun startDiscovery() {
        Log.d("AetherLink", "NSD Discovery Started")
    }

    fun stopDiscovery() {
        Log.d("AetherLink", "NSD Discovery Stopped")
    }
}