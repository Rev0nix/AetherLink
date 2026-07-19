package com.aetherlink.android

import android.app.Application
import android.util.Log

class AetherLinkApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.i("AetherLink", "Application Started")
    }
}