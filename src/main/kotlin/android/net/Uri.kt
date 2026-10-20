package android.net

import java.io.File
import java.net.URI

/** Minimal desktop replacement for android.net.Uri. */
class Uri private constructor(private val raw: String) {

    val path: String
        get() = when {
            raw.startsWith("file://") -> runCatching { File(URI(raw)).absolutePath }.getOrElse { raw.removePrefix("file://") }
            raw.startsWith("content://") -> raw.removePrefix("content://").substringAfter('/', "")
            else -> raw
        }

    val lastPathSegment: String? get() = path.substringAfterLast('/', "").ifBlank { null }

    override fun toString(): String = raw

    override fun equals(other: Any?): Boolean = other is Uri && other.raw == raw

    override fun hashCode(): Int = raw.hashCode()

    companion object {
        fun parse(uriString: String): Uri = Uri(uriString)

        fun fromFile(file: File): Uri = Uri("file://${file.absolutePath}")
    }
}
