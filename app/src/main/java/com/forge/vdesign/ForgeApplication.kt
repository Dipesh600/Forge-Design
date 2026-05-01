package com.forge.vdesign

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ForgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable aapt:attr and complex vectors across all Android versions
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
}
