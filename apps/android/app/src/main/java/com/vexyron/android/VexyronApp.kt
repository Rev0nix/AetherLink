package com.vexyron.android

import android.app.Application
import android.util.Log
import com.vexyron.android.core.designsystem.ThemeManager
import com.vexyron.android.core.i18n.I18nManager

class VexyronApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("VEXYRON", "Application Started")
        ThemeManager.init(this)
        I18nManager.init(this)
    }
}
