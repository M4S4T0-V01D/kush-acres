package dev.m4s4t0.kushacres

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore

/** Save files for moving a farm: written to Download/KushAcres, opened with the system file picker. The desktop has its own version. */
object SaveFile {
    const val WHERE = "Download/KushAcres"

    /** Set by MainActivity: opens the system picker. */
    var open: (() -> Unit)? = null
    private var got: ((String) -> Unit)? = null

    /** Returns where it went, or null if it couldn't be written. */
    fun export(text: String, name: String): String? = runCatching {
        val r = Store.app.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, WHERE)
        }
        val uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        r.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: return null
        "$WHERE/$name"
    }.getOrNull()

    /** Lets the player pick a file; [then] gets its text, or nothing if they back out. */
    fun pick(then: (String) -> Unit) {
        got = then
        open?.invoke()
    }

    /** The picker's answer, from MainActivity. */
    fun picked(uri: Uri?) {
        val then = got ?: return
        got = null
        if (uri == null) return
        val text = runCatching { Store.app.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } }.getOrNull()
        then(text ?: "")
    }
}
