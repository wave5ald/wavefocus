package android.app

import android.content.Context

/** Desktop stand-in for android.app.Application (process wide context). */
open class Application : Context() {
    override val applicationContext: Context get() = this

    open fun onCreate() = Unit

    open fun onTerminate() = Unit
}
