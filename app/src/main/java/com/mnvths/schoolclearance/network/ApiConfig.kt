// file: network/ApiConfig.kt
package com.mnvths.schoolclearance.network

import android.os.Build

object ApiConfig {

    // IMPORTANT: Replace this with your computer's actual local IP address
    private const val REAL_DEVICE_IP = "192.168.1.2"

    private const val EMULATOR_IP = "10.0.2.2"
    private const val PORT = 3000

    // This function checks for common emulator properties
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    // The BASE_URL that your whole app will use
    val BASE_URL = if (isEmulator()) {
        "http://$EMULATOR_IP:$PORT"
    } else {
        "http://$REAL_DEVICE_IP:$PORT"
    }
}

