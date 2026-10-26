// Desktop stand-ins for Context and SharedPreferences: each preferences file is a properties file in
// ~/.local/share/kush-acres, written whole (to a temp file, then moved over) on every apply.
package android.content

import java.io.File
import java.util.Properties

open class Context {
    open val applicationContext: Context get() = this
    open val packageName = "dev.m4s4t0.kushacres"
    val dataDir: File get() = Context.dataDir
    val cacheDir: File get() = File(dataDir, "cache").apply { mkdirs() }

    fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        synchronized(prefs) { prefs.getOrPut(name) { SharedPreferences(File(dataDir, "$name.properties")) } }

    companion object {
        const val MODE_PRIVATE = 0
        private val prefs = HashMap<String, SharedPreferences>()

        val dataDir: File by lazy {
            val base = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: (System.getProperty("user.home") + "/.local/share")
            File(base, "kush-acres").apply { mkdirs() }
        }
    }
}

class SharedPreferences internal constructor(private val file: File) {
    private val props = Properties().apply { if (file.exists()) runCatching { file.inputStream().use { load(it) } } }

    fun getString(key: String, def: String?): String? = synchronized(props) { props.getProperty(key) } ?: def
    fun getInt(key: String, def: Int) = getString(key, null)?.toIntOrNull() ?: def
    fun getLong(key: String, def: Long) = getString(key, null)?.toLongOrNull() ?: def
    fun getFloat(key: String, def: Float) = getString(key, null)?.toFloatOrNull() ?: def
    fun getBoolean(key: String, def: Boolean) = getString(key, null)?.toBooleanStrictOrNull() ?: def
    fun contains(key: String) = synchronized(props) { props.containsKey(key) }

    fun edit() = Editor()

    inner class Editor {
        private val put = LinkedHashMap<String, String?>()
        fun putString(key: String, v: String?) = apply { put[key] = v }
        fun putInt(key: String, v: Int) = apply { put[key] = v.toString() }
        fun putLong(key: String, v: Long) = apply { put[key] = v.toString() }
        fun putFloat(key: String, v: Float) = apply { put[key] = v.toString() }
        fun putBoolean(key: String, v: Boolean) = apply { put[key] = v.toString() }
        fun remove(key: String) = apply { put[key] = null }

        fun commit(): Boolean = synchronized(props) {
            for ((k, v) in put) if (v == null) props.remove(k) else props.setProperty(k, v)
            runCatching {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.outputStream().use { props.store(it, null) }
                java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
            }.isSuccess
        }

        fun apply() { commit() }
    }
}
