package dev.m4s4t0.kushacres

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore

/** Saves pictures where the phone's gallery finds them (Pictures/KushAcres). The desktop has its own version. */
object SavePic {
    const val WHERE = "Pictures/KushAcres"

    /** Returns where it went, or null if it couldn't be saved. */
    fun save(bmp: Bitmap, name: String): String? = runCatching {
        val r = Store.app.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, WHERE)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = r.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        r.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return null
        r.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        "$WHERE/$name"
    }.getOrNull()
}
