package android.os

/** Desktop stand-in for android.os.Build. */
class Build {
    object VERSION {
        const val SDK_INT: Int = 35
        const val RELEASE: String = "15"
        const val CODENAME: String = "REL"
    }

    object VERSION_CODES {
        const val LOLLIPOP = 21
        const val M = 23
        const val N = 24
        const val O = 26
        const val P = 28
        const val Q = 29
        const val R = 30
        const val S = 31
        const val TIRAMISU = 33
        const val UPSIDE_DOWN_CAKE = 34
    }

    companion object {
        val MANUFACTURER: String get() = "Microsoft"
        val MODEL: String get() = "Windows PC"
        val DEVICE: String get() = "desktop"
    }
}
