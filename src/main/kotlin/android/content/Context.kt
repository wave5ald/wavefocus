package android.content

import android.os.PowerManagerImpl
import android.os.VibratorImpl
import android.os.VibratorManagerImpl
import com.example.desktop.platform.AppPaths
import com.example.desktop.platform.DesktopPlatform
import com.example.desktop.platform.ToastBus
import java.io.File

/**
 * Minimal JVM/desktop stand-in for android.content.Context.
 * Only the surface used by the Wave Focus sources is provided.
 */
open class Context {

    open val applicationContext: Context get() = this

    open val packageName: String get() = "com.aistudio.dailyroutine.xkqxxc"

    open val filesDir: File get() = AppPaths.filesDir

    open val cacheDir: File get() = AppPaths.cacheDir

    val contentResolver: ContentResolver get() = ContentResolver.instance

    fun getSharedPreferences(name: String, mode: Int): SharedPreferences = SharedPreferencesImpl.get(name)

    fun getDatabasePath(name: String): File = File(AppPaths.databasesDir, name)

    fun getSystemService(name: String): Any? = when (name) {
        CLIPBOARD_SERVICE -> ClipboardManager.instance
        POWER_SERVICE -> PowerManagerImpl
        VIBRATOR_SERVICE -> VibratorImpl
        VIBRATOR_MANAGER_SERVICE -> VibratorManagerImpl
        else -> null
    }

    fun getString(resId: Int): String = ""

    fun startActivity(intent: Intent) {
        when {
            intent.extras.containsKey(Intent.EXTRA_TEXT) -> {
                val text = intent.extras[Intent.EXTRA_TEXT]?.toString().orEmpty()
                DesktopPlatform.setClipboardText(text)
                ToastBus.show("Copied to clipboard: ${text.take(60)}${if (text.length > 60) "…" else ""}")
            }
            intent.extras.containsKey(Intent.EXTRA_STREAM) -> {
                val uri = intent.extras[Intent.EXTRA_STREAM]
                val path = uri?.toString()
                if (path != null) DesktopPlatform.openInExplorer(File(path))
            }
            intent.data != null -> {
                val data = intent.data!!.toString()
                if (data.startsWith("http")) DesktopPlatform.browse(data)
            }
            else -> ToastBus.show(intent.action ?: "Action completed")
        }
    }

    fun startService(intent: Intent) = Unit

    fun startForegroundService(intent: Intent) = Unit

    companion object {
        const val MODE_PRIVATE = 0
        const val MODE_APPEND = 32768

        const val CLIPBOARD_SERVICE = "clipboard"
        const val POWER_SERVICE = "power"
        const val VIBRATOR_SERVICE = "vibrator"
        const val VIBRATOR_MANAGER_SERVICE = "vibrator_manager"
        const val NOTIFICATION_SERVICE = "notification"
        const val LOCATION_SERVICE = "location"
    }
}
