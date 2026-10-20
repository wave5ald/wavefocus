package android.widget

import android.content.Context
import com.example.desktop.platform.ToastBus

/** Desktop stand-in for android.widget.Toast -> snackbar message in the main window. */
class Toast private constructor(private val text: CharSequence) {

    fun show() {
        ToastBus.show(text)
    }

    fun cancel() = Unit

    fun setDuration(duration: Int): Toast = this

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

        fun makeText(context: Context?, text: CharSequence?, duration: Int): Toast =
            Toast(text?.toString().orEmpty())
    }
}
