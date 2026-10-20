package android.content

import com.example.desktop.platform.DesktopPlatform

class ClipData private constructor(private val items: List<Item>) {

    val itemCount: Int get() = items.size

    fun getItemAt(index: Int): Item = items[index]

    class Item(val text: CharSequence?)

    companion object {
        fun newPlainText(label: CharSequence?, text: CharSequence?): ClipData =
            ClipData(listOf(Item(text)))
    }
}

/** Desktop replacement for android.content.ClipboardManager (system clipboard backed). */
class ClipboardManager internal constructor() {

    fun setPrimaryClip(clip: ClipData?) {
        val text = clip?.let { if (it.itemCount > 0) it.getItemAt(0).text?.toString() else null }
        DesktopPlatform.setClipboardText(text.orEmpty())
    }

    val primaryClip: ClipData?
        get() {
            val text = DesktopPlatform.clipboardText() ?: return null
            return ClipData.newPlainText("clipboard", text)
        }

    fun hasPrimaryClip(): Boolean = !DesktopPlatform.clipboardText().isNullOrEmpty()

    companion object {
        val instance = ClipboardManager()
    }
}
