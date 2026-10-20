package android.os

import android.content.Context
import android.content.Intent
import com.example.desktop.platform.ToastBus

/** Desktop stand-in for android.os.PowerManager (battery optimisation is meaningless on Windows). */
class PowerManager {

    fun isIgnoringBatteryOptimizations(packageName: String): Boolean = true

    fun newWakeLock(levelAndFlags: Int, tag: String): WakeLock = WakeLock()

    class WakeLock {
        var isHeld: Boolean = false
            private set

        fun setReferenceCounted(referenceCounted: Boolean) = Unit

        fun acquire(timeout: Long = 0L) {
            isHeld = true
        }

        fun release() {
            isHeld = false
        }
    }

    companion object {
        const val PARTIAL_WAKE_LOCK = 1
        const val SCREEN_DIM_WAKE_LOCK = 6
        const val SCREEN_BRIGHT_WAKE_LOCK = 10
        const val FULL_WAKE_LOCK = 26
    }
}

val PowerManagerImpl = PowerManager()
