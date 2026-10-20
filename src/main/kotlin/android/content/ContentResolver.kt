package android.content

import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Desktop replacement for ContentResolver: content:// URIs map to local files. */
class ContentResolver {

    fun openInputStream(uri: Uri): InputStream? = try {
        FileInputStream(uri.toFile())
    } catch (_: Exception) {
        null
    }

    fun openOutputStream(uri: Uri): OutputStream? = try {
        val file = uri.toFile()
        file.parentFile?.mkdirs()
        FileOutputStream(file)
    } catch (_: Exception) {
        null
    }

    fun openFileDescriptor(uri: Uri, mode: String) = null

    fun getType(uri: Uri): String? {
        val name = uri.toFile().name
        return when (name.substringAfterLast('.', "").lowercase()) {
            "json" -> "application/json"
            "txt" -> "text/plain"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> null
        }
    }

    private fun Uri.toFile(): File = File(this.path ?: this.toString().removePrefix("file://"))

    companion object {
        val instance: ContentResolver by lazy { ContentResolver() }
    }
}
