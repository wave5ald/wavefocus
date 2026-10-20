package com.example.util

import android.content.Context
import com.example.desktop.platform.ToastBus

/** Desktop counterpart: Windows does not aggressively kill background apps, nothing to whitelist. */
object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean = true

    fun requestIgnoreBatteryOptimizations(context: Context) {
        ToastBus.show("Windows keeps Wave Focus running normally in the background.")
    }
}
