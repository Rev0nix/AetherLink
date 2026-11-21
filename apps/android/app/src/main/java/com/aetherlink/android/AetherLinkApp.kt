package com.aetherlink.android

import android.app.Application
import android.util.Log
import com.aetherlink.android.core.designsystem.ThemeManager
import com.aetherlink.android.core.i18n.I18nManager

class AetherLinkApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("AetherLink", "Application Started")
        ThemeManager.init(this)
        I18nManager.init(this)
    }
}
