package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How many real pixels make one art pixel: sprites are drawn in art pixels but shapes are rasterized this much finer. */
const val S = 2

/**
 * A tiny ARGB pixel buffer to draw sprites into. Coordinates are art pixels; the buffer itself is [s] times finer, so
 * blocks stay chunky while lines, discs, polygons and outlines come out smooth. [dot] reaches single fine pixels.
 */
class Px(val w: Int, val h: Int, val s: Int = S) {
    val fw = w * s
    val fh = h * s
    val p = IntArray(fw * fh)

    /** One fine pixel. */
    fun dot(fx: Int, fy: Int, c: Int) {
        if (c == 0 || fx !in 0 until fw || fy !in 0 until fh) return
        p[fy * fw + fx] = c
    }

    fun set(x: Int, y: Int, c: Int) {
        if (c == 0 || x !in 0 until w || y !in 0 until h) return
        for (j in 0 until s) for (i in 0 until s) p[(y * s + j) * fw + x * s + i] = c
    }

    fun get(x: Int, y: Int): Int = if (x in 0 until w && y in 0 until h) p[y * s * fw + x * s] else 0

    fun rect(x: Int, y: Int, rw: Int, rh: Int, c: Int) {
        for (j in y until y + rh) for (i in x until x + rw) set(i, j, c)
    }

    /** A one-art-pixel-wide line, stepped at fine resolution so slopes stay smooth. */
    fun line(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        var x = x0 * s; var y = y0 * s
        val ex = x1 * s; val ey = y1 * s
        val dx = abs(ex - x); val dy = -abs(ey - y)
        val sx = if (x < ex) 1 else -1; val sy = if (y < ey) 1 else -1
        var err = dx + dy
        while (true) {
            for (j in 0 until s) for (i in 0 until s) dot(x + i, y + j, c)
            if (x == ex && y == ey) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    fun disc(cx: Int, cy: Int, r: Int, c: Int) {
        val fcx = (cx + 0.5f) * s; val fcy = (cy + 0.5f) * s
        val fr = (r + 0.45f) * s
        val lo = ((cy - r - 1) * s); val hi = ((cy + r + 2) * s)
        val l = ((cx - r - 1) * s); val rr = ((cx + r + 2) * s)
        for (fy in lo until hi) for (fx in l until rr) {
            val ddx = fx + 0.5f - fcx; val ddy = fy + 0.5f - fcy
            if (ddx * ddx + ddy * ddy <= fr * fr) dot(fx, fy, c)
        }
    }

    /** Fills a polygon given as x0,y0,x1,y1,... in art pixels, colouring each fine pixel with [c] (given art coordinates). */
    fun poly(pts: IntArray, c: (Int, Int) -> Int) {
        val n = pts.size / 2
        var y0 = Int.MAX_VALUE; var y1 = Int.MIN_VALUE
        for (i in 0 until n) { y0 = min(y0, pts[i * 2 + 1]); y1 = max(y1, pts[i * 2 + 1]) }
        val xs = FloatArray(n)
        for (fy in y0 * s until (y1 + 1) * s) {
            val yc = (fy + 0.5f) / s
            var k = 0
            for (i in 0 until n) {
                val ax = pts[i * 2].toFloat(); val ay = pts[i * 2 + 1].toFloat()
                val j = (i + 1) % n
                val bx = pts[j * 2].toFloat(); val by = pts[j * 2 + 1].toFloat()
                if ((ay <= yc) != (by <= yc)) xs[k++] = ax + (yc - ay) * (bx - ax) / (by - ay)
            }
            xs.sort(0, k)
            var i = 0
            while (i + 1 < k) {
                for (fx in (xs[i] * s).roundToInt() until ((xs[i + 1] + 1) * s).roundToInt()) {
                    val ax = Math.floorDiv(fx, s); val ay = Math.floorDiv(fy, s)
                    dot(fx, fy, c(ax, ay))
                }
                i += 2
            }
        }
    }

    fun poly(col: Int, vararg pts: Int) = poly(pts) { _, _ -> col }

    /** Draws a picture from rows of characters, each mapped to a colour (anything unmapped is clear). */
    fun map(x: Int, y: Int, rows: Array<String>, pal: Map<Char, Int>) {
        rows.forEachIndexed { j, row -> row.forEachIndexed { i, ch -> pal[ch]?.let { set(x + i, y + j, it) } } }
    }

    /** A thin dark outline around everything drawn so far, so sprites read against any ground. */
    fun outline(c: Int) {
        val src = p.copyOf()
        for (y in 0 until fh) for (x in 0 until fw) {
            if (src[y * fw + x] != 0) continue
            val n = (x > 0 && src[y * fw + x - 1] != 0) || (x < fw - 1 && src[y * fw + x + 1] != 0) ||
                (y > 0 && src[(y - 1) * fw + x] != 0) || (y < fh - 1 && src[(y + 1) * fw + x] != 0)
            if (n) p[y * fw + x] = c
        }
    }

    fun bitmap(): Bitmap = Bitmap.createBitmap(p, fw, fh, Bitmap.Config.ARGB_8888)
}

fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

/** Scales a colour's brightness (f < 1 darker, f > 1 lighter). */
fun shade(c: Int, f: Float): Int {
    val r = ((c shr 16 and 255) * f).toInt().coerceIn(0, 255)
    val g = ((c shr 8 and 255) * f).toInt().coerceIn(0, 255)
    val b = ((c and 255) * f).toInt().coerceIn(0, 255)
    return argb(c ushr 24, r, g, b)
}

fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(s: Int) = (((a shr s) and 255) * (1 - t) + ((b shr s) and 255) * t).toInt()
    return argb(255, ch(16), ch(8), ch(0))
}
