package androidx.lifecycle

import android.app.Application

/** Desktop counterpart of androidx.lifecycle.AndroidViewModel. */
abstract class AndroidViewModel(private val application: Application) : ViewModel() {
    @Suppress("UNCHECKED_CAST")
    fun <T : Application> getApplication(): T = application as T
}
