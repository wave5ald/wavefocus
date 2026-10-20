package androidx.activity.result.contract

import android.net.Uri
import com.example.desktop.platform.FileDialogs

/** Desktop replacement for androidx.activity.result.contract.ActivityResultContract. */
abstract class ActivityResultContract<I, O> {
    abstract fun execute(input: I): O
}

/**
 * Mirrors the Android names used by the app:
 *  - ActivityResultContracts.CreateDocument(mime)
 *  - ActivityResultContracts.GetContent()
 *  - ActivityResultContracts.RequestPermission()
 */
class ActivityResultContracts {

    class CreateDocument(private val mimeType: String = "application/json") :
        ActivityResultContract<String, Uri?>() {
        override fun execute(input: String): Uri? {
            val file = FileDialogs.saveFile("Save file", input) ?: return null
            return Uri.fromFile(file)
        }
    }

    class GetContent(private val mimeType: String = "*/*") :
        ActivityResultContract<String, Uri?>() {
        override fun execute(input: String): Uri? {
            val file = FileDialogs.openFile("Open file", null) ?: return null
            return Uri.fromFile(file)
        }
    }

    class RequestPermission : ActivityResultContract<String, Boolean>() {
        override fun execute(input: String): Boolean = true
    }
}
