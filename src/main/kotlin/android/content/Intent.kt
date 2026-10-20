package android.content

import android.net.Uri

/**
 * Desktop stand-in for android.content.Intent. Only the pieces used by the app are modelled;
 * ACTION_SEND requests are turned into clipboard copies / explorer shortcuts by Context.startActivity.
 */
class Intent {
    var action: String? = null
    var type: String? = null
    var data: Uri? = null
    var flags: Int = 0
    val extras: MutableMap<String, Any?> = linkedMapOf()
    var componentClass: Class<*>? = null

    constructor()

    constructor(action: String) {
        this.action = action
    }

    constructor(context: Context, cls: Class<*>) {
        this.componentClass = cls
    }

    fun putExtra(name: String, value: Any?): Intent {
        extras[name] = value
        return this
    }

    fun putExtra(name: String, value: String?): Intent {
        extras[name] = value
        return this
    }

    fun addFlags(flags: Int): Intent {
        this.flags = this.flags or flags
        return this
    }

    fun setType(type: String?): Intent {
        this.type = type
        return this
    }

    fun getStringExtra(name: String): String? = extras[name]?.toString()

    companion object {
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"

        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"

        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_SINGLE_TOP = 0x20000000
        const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
        const val FLAG_GRANT_READ_URI_PERMISSION = 0x00000001

        fun createChooser(target: Intent, title: CharSequence?): Intent {
            val chooser = Intent(target.action ?: ACTION_SEND)
            chooser.extras.putAll(target.extras)
            chooser.type = target.type
            if (title != null) chooser.putExtra(EXTRA_SUBJECT, title.toString())
            return chooser
        }
    }
}
