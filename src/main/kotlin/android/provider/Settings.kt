package android.provider

import android.content.Context
import com.example.desktop.platform.ToastBus

/** Desktop stand-in for android.provider.Settings (battery optimisation menus do not exist on Windows). */
object Settings {
    const val ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    const val ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"
    const val ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS"

    fun openBatterySettings(context: Context) {
        ToastBus.show("Windows power settings are managed by the operating system.")
    }

    fun openAppDetails(context: Context) {
        ToastBus.show("Wave Focus runs as a normal Windows desktop app.")
    }
}
