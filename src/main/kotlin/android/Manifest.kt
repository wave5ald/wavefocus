package android

/** Desktop stand-in for android.Manifest permission constants. */
class Manifest {
    object permission {
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
        const val ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
        const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
        const val INTERNET = "android.permission.INTERNET"
        const val VIBRATE = "android.permission.VIBRATE"
    }
}
