package android.graphics

/**
 * Desktop stand-in for android.graphics.Color.
 * Only the colour-parsing surface used by the Wave Focus sources is provided.
 */
object Color {

    const val BLACK = -0x1000000
    const val WHITE = -0x1
    const val TRANSPARENT = 0

    /**
     * Parses "#RRGGBB", "#AARRGGBB" or the named "red"/"blue"/... forms,
     * returning an ARGB int exactly like the Android implementation.
     */
    @JvmStatic
    fun parseColor(colorString: String): Int {
        val text = colorString.trim()
        if (text.startsWith("#")) {
            var hex = text.substring(1)
            when (hex.length) {
                3 -> hex = hex.map { "$it$it" }.joinToString("")
                6 -> hex = "ff$hex"
                8 -> Unit
                else -> throw IllegalArgumentException("Unknown color: $colorString")
            }
            return hex.toLong(16).toInt()
        }
        return when (text.lowercase()) {
            "black" -> BLACK
            "white" -> WHITE
            "transparent" -> TRANSPARENT
            "red" -> -0x10000
            "green" -> -0xff0100
            "blue" -> -0xffff01
            "yellow" -> -0x100
            "cyan" -> -0xff0001
            "magenta" -> -0xff01
            "gray", "grey" -> -0x7f7f80
            "ltgray", "lightgray", "lightgrey" -> -0x2b2b2c
            "darkgray", "darkgrey" -> -0x454546
            else -> throw IllegalArgumentException("Unknown color: $colorString")
        }
    }
}
