package android.util

import java.util.Base64 as JBase64

/** Desktop stand-in for android.util.Base64. */
object Base64 {
    const val DEFAULT = 0
    const val NO_WRAP = 2
    const val NO_PADDING = 1
    const val URL_SAFE = 8

    private val encoder: JBase64.Encoder = JBase64.getEncoder()
    private val decoder: JBase64.Decoder = JBase64.getDecoder()

    fun encodeToString(input: ByteArray, flags: Int): String = encoder.encodeToString(input)

    fun decode(str: String, flags: Int): ByteArray = decoder.decode(str)
}
