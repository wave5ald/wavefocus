package android.util

/** Desktop stand-in for android.util.Log -> stdout/stderr. */
object Log {
    fun v(tag: String, msg: String): Int = print("V", tag, msg)
    fun d(tag: String, msg: String): Int = print("D", tag, msg)
    fun i(tag: String, msg: String): Int = print("I", tag, msg)
    fun w(tag: String, msg: String): Int = print("W", tag, msg)
    fun w(tag: String, msg: String, tr: Throwable?): Int = print("W", tag, msg, tr)
    fun e(tag: String, msg: String): Int = print("E", tag, msg)
    fun e(tag: String, msg: String, tr: Throwable?): Int = print("E", tag, msg, tr)
    fun wtf(tag: String, msg: String): Int = print("WTF", tag, msg)

    private fun print(level: String, tag: String, msg: String, tr: Throwable? = null): Int {
        System.out.println("[$level/$tag] $msg")
        tr?.printStackTrace()
        return 0
    }
}
