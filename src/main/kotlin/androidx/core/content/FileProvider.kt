package androidx.core.content

import android.content.Context
import android.net.Uri
import java.io.File

/** Desktop stand-in for androidx.core.content.FileProvider (plain file URIs). */
object FileProvider {
    fun getUriForFile(context: Context, authority: String, file: File): Uri = Uri.fromFile(file)
}
