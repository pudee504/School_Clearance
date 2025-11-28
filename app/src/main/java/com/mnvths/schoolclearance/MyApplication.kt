package com.mnvths.schoolclearance

import android.app.Application
import com.mnvths.schoolclearance.BuildConfig // <-- ADD THIS IMPORT
import timber.log.Timber

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // This plants a "debug tree" that only logs when you are running
        // the app in debug mode. It's automatically removed for release builds.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}