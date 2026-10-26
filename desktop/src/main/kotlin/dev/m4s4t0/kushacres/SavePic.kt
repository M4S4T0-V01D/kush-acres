package dev.m4s4t0.kushacres

import android.graphics.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Saves pictures to ~/Pictures/KushAcres (the phone's version uses the gallery). */
object SavePic {
    /** Returns where it went, or null if it couldn't be saved. */
    fun save(bmp: Bitmap, name: String): String? = runCatching {
        val home = System.getProperty("user.home")
        val pics = File(home, "Pictures").takeIf { it.isDirectory } ?: File(home)
        val dir = File(pics, "KushAcres").apply { mkdirs() }
        val png = bmp.image().encodeToData(EncodedImageFormat.PNG) ?: return null
        val out = File(dir, name)
        out.writeBytes(png.bytes)
        out.path.replaceFirst(home, "~")
    }.getOrNull()
}
