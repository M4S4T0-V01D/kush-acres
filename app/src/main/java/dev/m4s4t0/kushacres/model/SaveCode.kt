package dev.m4s4t0.kushacres.model

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

/** A save squeezed into one line of text, to carry a farm between the phone and the desktop game. */
object SaveCode {
    private const val HEAD = "KUSH1:"

    fun pack(json: String): String {
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out, Deflater(Deflater.BEST_COMPRESSION)).use { it.write(json.toByteArray()) }
        return HEAD + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    /** The save inside [text] (a code, a file holding one, or a plain save), or null if there isn't one. */
    fun unpack(text: String): String? = runCatching {
        val t = text.trim()
        if (t.startsWith("{")) return@runCatching t
        val at = t.indexOf(HEAD).takeIf { it >= 0 } ?: return null
        val body = t.substring(at + HEAD.length).filterNot { it.isWhitespace() }
        InflaterInputStream(Base64.getUrlDecoder().decode(body).inputStream()).use { String(it.readBytes()) }
    }.getOrNull()?.takeIf { it.startsWith("{") }

    /** The farm in [json], loaded on the side to check it and show what it holds; null if it won't load. */
    fun peek(json: String): Farm? {
        val keep = Strains.scaleLevel
        return try { runCatching { Farm().also { Save.read(json, it) } }.getOrNull() } finally { Strains.scaleLevel = keep }
    }
}
