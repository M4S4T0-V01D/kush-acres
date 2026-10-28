package dev.m4s4t0.kushacres

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Save files for moving a farm: written to ~/Downloads/KushAcres (the phone's version uses Download/KushAcres). */
object SaveFile {
    private fun dir(): File {
        val home = System.getProperty("user.home")
        val down = File(home, "Downloads").takeIf { it.isDirectory } ?: File(home)
        return File(down, "KushAcres")
    }

    /** Returns where it went, or null if it couldn't be written. */
    fun export(text: String, name: String): String? = runCatching {
        val out = File(dir().apply { mkdirs() }, name)
        out.writeText(text)
        out.path.replaceFirst(System.getProperty("user.home"), "~")
    }.getOrNull()

    /** Lets the player pick a file; [then] gets its text, or nothing if they back out. */
    fun pick(then: (String) -> Unit) {
        val d = FileDialog(null as Frame?, "Open a Kush Acres save", FileDialog.LOAD)
        dir().takeIf { it.isDirectory }?.let { d.directory = it.path }
        d.isVisible = true
        val name = d.file ?: return
        then(runCatching { File(d.directory, name).readText() }.getOrDefault(""))
    }
}
