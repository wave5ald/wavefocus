package android.content

import com.example.desktop.platform.AppPaths
import java.io.File
import java.util.Properties

interface SharedPreferences {
    fun getString(key: String, defValue: String?): String?
    fun getInt(key: String, defValue: Int): Int
    fun getLong(key: String, defValue: Long): Long
    fun getFloat(key: String, defValue: Float): Float
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun contains(key: String): Boolean
    fun getAll(): Map<String, *>
    fun edit(): Editor
    fun registerOnSharedPreferenceChangeListener(listener: (SharedPreferences, String?) -> Unit)

    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun putFloat(key: String, value: Float): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
        fun apply()
        fun commit(): Boolean
    }
}

/** File backed SharedPreferences implementation stored under %APPDATA%\WaveFocus\prefs. */
internal class SharedPreferencesImpl(private val name: String) : SharedPreferences {

    private val file: File = File(AppPaths.prefsDir, "$name.properties")
    private val props = Properties()
    private val listeners = mutableListOf<(SharedPreferences, String?) -> Unit>()

    init {
        if (file.exists()) {
            try {
                file.inputStream().use { props.load(it) }
            } catch (_: Exception) {
            }
        }
    }

    @Synchronized
    private fun persist() {
        try {
            file.outputStream().use { props.store(it, "Wave Focus preferences") }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    override fun getString(key: String, defValue: String?): String? = props.getProperty(key) ?: defValue

    @Synchronized
    override fun getInt(key: String, defValue: Int): Int =
        props.getProperty(key)?.toIntOrNull() ?: defValue

    @Synchronized
    override fun getLong(key: String, defValue: Long): Long =
        props.getProperty(key)?.toLongOrNull() ?: defValue

    @Synchronized
    override fun getFloat(key: String, defValue: Float): Float =
        props.getProperty(key)?.toFloatOrNull() ?: defValue

    @Synchronized
    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        props.getProperty(key)?.toBooleanStrictOrNull() ?: defValue

    @Synchronized
    override fun contains(key: String): Boolean = props.containsKey(key)

    @Synchronized
    override fun getAll(): Map<String, *> = props.entries.associate { it.key.toString() to it.value }

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: (SharedPreferences, String?) -> Unit) {
        listeners.add(listener)
    }

    private inner class EditorImpl : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, String?>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            pending[key] = value; return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            pending[key] = value.toString(); return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            pending[key] = value.toString(); return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            pending[key] = value.toString(); return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            pending[key] = value.toString(); return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            pending[key] = null; return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true; return this
        }

        override fun apply() {
            commit()
        }

        @Synchronized
        override fun commit(): Boolean {
            try {
                if (clearRequested) props.clear()
                for ((k, v) in pending) {
                    if (v == null) props.remove(k) else props.setProperty(k, v)
                }
                persist()
                listeners.forEach { runCatching { it(this@SharedPreferencesImpl, null) } }
                return true
            } catch (_: Exception) {
                return false
            }
        }
    }

    companion object {
        private val cache = mutableMapOf<String, SharedPreferencesImpl>()

        @Synchronized
        fun get(name: String): SharedPreferencesImpl = cache.getOrPut(name) { SharedPreferencesImpl(name) }
    }
}
