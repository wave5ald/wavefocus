package android.content.pm

/** Desktop stand-in for android.content.pm.PackageManager (permission checks always succeed). */
class PackageManager {
    companion object {
        const val PERMISSION_GRANTED = 0
        const val PERMISSION_DENIED = -1
        const val GET_META_DATA = 128
    }
}
