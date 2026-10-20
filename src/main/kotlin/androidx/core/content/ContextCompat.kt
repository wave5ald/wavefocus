package androidx.core.content

import android.content.Context
import android.content.pm.PackageManager

/** Desktop stand-in: all permissions are effectively granted. */
object ContextCompat {
    fun checkSelfPermission(context: Context, permission: String): Int = PackageManager.PERMISSION_GRANTED
}
