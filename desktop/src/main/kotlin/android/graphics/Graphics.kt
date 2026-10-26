// Desktop stand-ins for the bits of android.graphics the game draws with, on Skia. Only what the shared code
// calls is here, with Android's behaviour: colours are unpremultiplied ARGB ints, Paint() filters bitmaps by default,
// PorterDuff MULTIPLY is Skia's MODULATE.
package android.graphics

import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect as SkRect
import org.jetbrains.skia.SamplingMode
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Bitmap private constructor(w: Int, h: Int) {
    enum class Config { ARGB_8888 }

    /** The pixels; replaced when the bitmap is reconfigured to another size. */
    var sk: org.jetbrains.skia.Bitmap = alloc(w, h); private set
    var allocationByteCount = w * h * 4; private set
    val width get() = sk.width
    val height get() = sk.height

    private var image: Image? = null

    /** A snapshot for drawing onto other bitmaps; taken again after this one is drawn on. */
    internal fun image(): Image = image ?: Image.makeFromBitmap(sk).also { image = it }
    internal fun touched() { image = null }

    fun reconfigure(w: Int, h: Int, config: Config) {
        if (w == width && h == height) return
        sk = alloc(w, h); image = null
        allocationByteCount = maxOf(allocationByteCount, w * h * 4)
    }

    fun eraseColor(c: Int) { touched(); sk.erase(c) }

    fun getPixel(x: Int, y: Int): Int = IntArray(1).also { getPixels(it, 0, 1, x, y, 1, 1) }[0]

    fun getPixels(out: IntArray, offset: Int, stride: Int, x: Int, y: Int, w: Int, h: Int) {
        val bytes = sk.readPixels(ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL), w * 4, x, y) ?: return
        val ints = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
        for (j in 0 until h) ints.position(j * w).get(out, offset + j * stride, w)
    }

    companion object {
        private fun alloc(w: Int, h: Int) = org.jetbrains.skia.Bitmap().apply { allocPixels(ImageInfo.makeN32Premul(w, h)); erase(0) }

        fun createBitmap(w: Int, h: Int, config: Config) = Bitmap(w, h)

        /** From unpremultiplied ARGB pixels, row by row. */
        fun createBitmap(px: IntArray, w: Int, h: Int, config: Config): Bitmap {
            val b = Bitmap(w, h)
            val buf = ByteBuffer.allocate(w * h * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until w * h) buf.putInt(px[i])
            b.sk.installPixels(ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL), buf.array(), w * 4)
            // Back to the premultiplied kind Skia can draw into.
            val premul = alloc(w, h)
            org.jetbrains.skia.Canvas(premul).use { it.drawImage(Image.makeFromBitmap(b.sk), 0f, 0f) }
            b.sk = premul
            return b
        }

        /** The [w]×[h] part of [src] at [x],[y], scaled (and so mirrored) by [m]. */
        fun createBitmap(src: Bitmap, x: Int, y: Int, w: Int, h: Int, m: Matrix?, filter: Boolean): Bitmap {
            val sx = m?.sx ?: 1f; val sy = m?.sy ?: 1f
            val out = Bitmap(Math.round(w * Math.abs(sx)), Math.round(h * Math.abs(sy)))
            org.jetbrains.skia.Canvas(out.sk).use { c ->
                c.translate(if (sx < 0) out.width.toFloat() else 0f, if (sy < 0) out.height.toFloat() else 0f)
                c.scale(sx, sy)
                c.drawImageRect(src.image(), SkRect.makeXYWH(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat()),
                    SkRect.makeWH(w.toFloat(), h.toFloat()), if (filter) LINEAR else SamplingMode.DEFAULT, null, true)
            }
            return out
        }

        internal val LINEAR = FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE)
    }
}

class Matrix {
    var sx = 1f; private set
    var sy = 1f; private set
    fun setScale(x: Float, y: Float) { sx = x; sy = y }
}

class RectF(var left: Float = 0f, var top: Float = 0f, var right: Float = 0f, var bottom: Float = 0f) {
    fun set(l: Float, t: Float, r: Float, b: Float) { left = l; top = t; right = r; bottom = b }
    fun width() = right - left
    fun height() = bottom - top
    internal fun sk() = SkRect.makeLTRB(left, top, right, bottom)
}

class PorterDuff {
    enum class Mode(internal val blend: BlendMode) {
        SRC_OVER(BlendMode.SRC_OVER), ADD(BlendMode.PLUS), MULTIPLY(BlendMode.MODULATE), SCREEN(BlendMode.SCREEN),
        CLEAR(BlendMode.CLEAR), SRC(BlendMode.SRC), DST_IN(BlendMode.DST_IN), SRC_ATOP(BlendMode.SRC_ATOP),
    }
}

open class Xfermode
class PorterDuffXfermode(val mode: PorterDuff.Mode) : Xfermode()

class Typeface private constructor(internal val sk: org.jetbrains.skia.Typeface?) {
    companion object {
        const val NORMAL = 0
        const val BOLD = 1
        const val ITALIC = 2
        const val BOLD_ITALIC = 3
        val DEFAULT = Typeface(null)
        val MONOSPACE = Typeface(FontMgr.default.matchFamilyStyle("monospace", FontStyle.NORMAL))

        fun create(family: Typeface?, style: Int): Typeface {
            val fs = when (style) { BOLD -> FontStyle.BOLD; ITALIC -> FontStyle.ITALIC; BOLD_ITALIC -> FontStyle.BOLD_ITALIC; else -> FontStyle.NORMAL }
            val name = family?.sk?.familyName ?: "sans-serif"
            return Typeface(FontMgr.default.matchFamilyStyle(name, fs) ?: family?.sk)
        }
    }
}

open class Paint(flags: Int = 0) {
    enum class Style { FILL, STROKE, FILL_AND_STROKE }
    enum class Align { LEFT, CENTER, RIGHT }

    internal val sk = org.jetbrains.skia.Paint().apply { isAntiAlias = flags and ANTI_ALIAS_FLAG != 0; color = 0xFF000000.toInt() }

    var color: Int
        get() = sk.color
        set(v) { sk.color = v }
    /** 0..255, leaving the colour as it is. */
    var alpha: Int
        get() = sk.color ushr 24
        set(v) { sk.color = (sk.color and 0xFFFFFF) or (v.coerceIn(0, 255) shl 24) }
    var isAntiAlias: Boolean
        get() = sk.isAntiAlias
        set(v) { sk.isAntiAlias = v }
    var isFilterBitmap = true
    var style = Style.FILL
        set(v) { field = v; sk.mode = when (v) { Style.FILL -> PaintMode.FILL; Style.STROKE -> PaintMode.STROKE; Style.FILL_AND_STROKE -> PaintMode.STROKE_AND_FILL } }
    var strokeWidth: Float
        get() = sk.strokeWidth
        set(v) { sk.strokeWidth = v }
    var xfermode: Xfermode? = null
        set(v) { field = v; sk.blendMode = (v as? PorterDuffXfermode)?.mode?.blend ?: BlendMode.SRC_OVER }
    var textSize = 12f
    var typeface: Typeface? = null
    var textAlign = Align.LEFT

    private var font: Font? = null
    internal fun font(): Font {
        val f = font
        if (f != null && f.size == textSize && f.typeface === typeface?.sk) return f
        return Font(typeface?.sk, textSize).also { font = it }
    }

    fun measureText(text: String) = font().measureTextWidth(text)

    companion object {
        const val ANTI_ALIAS_FLAG = 1
        const val FILTER_BITMAP_FLAG = 2
    }
}

/** Draws with Skia onto a [Bitmap], or onto the window when it wraps the canvas Compose hands out. */
class Canvas(val sk: org.jetbrains.skia.Canvas, private val target: Bitmap? = null) {
    constructor(b: Bitmap) : this(org.jetbrains.skia.Canvas(b.sk), b)

    private fun draw() { target?.touched() }

    fun save(): Int = sk.save()
    fun restore() { sk.restore() }
    fun translate(dx: Float, dy: Float) { sk.translate(dx, dy) }
    fun scale(sx: Float, sy: Float) { sk.scale(sx, sy) }

    fun drawRect(l: Float, t: Float, r: Float, b: Float, p: Paint) { draw(); sk.drawRect(SkRect.makeLTRB(l, t, r, b), p.sk) }
    fun drawRect(r: RectF, p: Paint) { draw(); sk.drawRect(r.sk(), p.sk) }
    fun drawOval(l: Float, t: Float, r: Float, b: Float, p: Paint) { draw(); sk.drawOval(SkRect.makeLTRB(l, t, r, b), p.sk) }
    fun drawOval(r: RectF, p: Paint) { draw(); sk.drawOval(r.sk(), p.sk) }
    fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float, p: Paint) { draw(); sk.drawLine(x0, y0, x1, y1, p.sk) }
    fun drawColor(c: Int) { draw(); sk.clear(c) }

    fun drawBitmap(b: Bitmap, src: Rect?, dst: RectF, p: Paint?) {
        draw()
        val s = src?.let { SkRect.makeLTRB(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) }
            ?: SkRect.makeWH(b.width.toFloat(), b.height.toFloat())
        sk.drawImageRect(b.image(), s, dst.sk(), if (p?.isFilterBitmap == true) Bitmap.LINEAR else SamplingMode.DEFAULT, p?.sk, true)
    }

    fun drawBitmap(b: Bitmap, left: Float, top: Float, p: Paint?) =
        drawBitmap(b, null, RectF(left, top, left + b.width, top + b.height), p)

    fun drawText(text: String, x: Float, y: Float, p: Paint) {
        draw()
        val font = p.font()
        val dx = when (p.textAlign) { Paint.Align.LEFT -> 0f; Paint.Align.CENTER -> font.measureTextWidth(text) / 2; Paint.Align.RIGHT -> font.measureTextWidth(text) }
        sk.drawString(text, x - dx, y, font, p.sk)
    }
}

class Rect(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0)
