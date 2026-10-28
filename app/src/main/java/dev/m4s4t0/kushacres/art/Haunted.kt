package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import kotlin.random.Random

/** The Haunted Manor's map: the mansion itself, and the dead trees, cobwebs, toadstools and pumpkins in its woods. */
object Haunted {
    /** The mansion covers 4×4 squares; its footprint's front (bottom) corner sits on (AX, AY). */
    const val W = 136
    const val H = 184
    const val AX = W / 2
    const val AY = H - 3
    private const val CX = AX
    private const val BY = AY - 2

    private val SIDING = 0xFF4E4858.toInt()
    private val TRIM = 0xFF8C8478.toInt()
    private val ROOF = 0xFF2A2433.toInt()
    private val LIT = 0xFFF2C25A.toInt()
    private val GHOST = 0xFFB878F0.toInt()
    private val DARK = 0xFF161220.toInt()
    private val BOARD = 0xFF6A5040.toInt()

    /**
     * An iso box whose front corner is (cx, by): its left face runs [a] pixels across, its right face [b].
     * Lit windows are noted in [lit] as sprite pixels, for the night lights.
     */
    private class Box(val px: Px, val cx: Int, val by: Int, val a: Int, val b: Int, val lit: MutableList<IntArray>) {
        fun yl(x: Int) = by - (cx - x) / 2
        fun yr(x: Int) = by - (x - cx) / 2

        fun walls(h: Int, c: Int) {
            val siding: (Int, Int) -> Int = { col, z -> if (z % 3 == 0) shade(col, 0.82f) else col }
            px.poly(intArrayOf(cx - a, by - a / 2, cx, by, cx, by - h, cx - a, by - a / 2 - h)) { x, y -> siding(c, yl(x) - y) }
            px.poly(intArrayOf(cx, by, cx + b, by - b / 2, cx + b, by - b / 2 - h, cx, by - h)) { x, y -> siding(shade(c, 0.74f), yr(x) - y) }
            // Weather stains running down from the eaves, and a dark plinth.
            for (x in cx - a + 3 until cx + b - 2 step 7) {
                val y0 = if (x < cx) yl(x) else yr(x)
                for (j in 0 until 6 + (x * 7) % 9) px.set(x, y0 - h + 1 + j, shade(if (x < cx) c else shade(c, 0.74f), 0.85f))
            }
            onL(cx - a, 0, a, 2, shade(c, 0.6f)); onR(cx, 0, b, 2, shade(c, 0.45f))
        }

        fun onL(x0: Int, z0: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = x0 + i; px.set(x, yl(x) - z0 - j, c(i, j)) }
        }
        fun onR(x0: Int, z0: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = x0 + i; px.set(x, yr(x) - z0 - j, c(i, j)) }
        }
        fun onL(x0: Int, z0: Int, w: Int, hh: Int, c: Int) = onL(x0, z0, w, hh) { _, _ -> c }
        fun onR(x0: Int, z0: Int, w: Int, hh: Int, c: Int) = onR(x0, z0, w, hh) { _, _ -> c }

        /** A tall arched window: lit amber, lit violet, dark, or dark and boarded over. */
        fun window(x0: Int, z0: Int, glass: Int, right: Boolean = false, boarded: Boolean = false) {
            val w = 7; val hh = 11
            val f: (Int, Int) -> Int = { i, j ->
                val arch = j == hh - 1 && (i == 0 || i == w - 1)
                when {
                    arch -> 0
                    boarded && (j == 3 || j == 7) -> BOARD
                    i == 0 || i == w - 1 || j == 0 || j == hh - 1 || j == hh - 2 && (i == 1 || i == w - 2) -> TRIM
                    i == w / 2 || j == 5 -> shade(TRIM, 0.6f)
                    else -> glass
                }
            }
            if (right) onR(x0, z0, w, hh, f) else onL(x0, z0, w, hh, f)
            // Crooked shutters.
            val sh = shade(SIDING, 0.55f)
            if (right) { onR(x0 - 2, z0 + 1, 2, hh - 1, sh); onR(x0 + w, z0, 2, hh - 2, sh) } else { onL(x0 - 2, z0, 2, hh - 1, sh); onL(x0 + w, z0 + 1, 2, hh - 1, sh) }
            if (glass != DARK) lit += intArrayOf(x0 + w / 2, (if (right) yr(x0 + w / 2) else yl(x0 + w / 2)) - z0 - hh / 2, glass)
        }

        /** A gable roof on walls [h] high, the ridge running back from the middle of the left face. */
        fun gable(h: Int, rise: Int) {
            val e0x = cx; val e0y = by - h
            val e1x = cx - a - 2; val e1y = by - a / 2 - h + 1
            val e2x = cx + b + 2; val e2y = by - b / 2 - h + 1
            val r1x = cx - a / 2; val r1y = by - a / 4 - h - rise
            val r2x = r1x + b; val r2y = r1y - b / 2
            val e3x = cx - a + b; val e3y = by - a / 2 - b / 2 - h
            px.poly(shade(ROOF, 0.6f), e1x, e1y, r1x, r1y, r2x, r2y, e3x, e3y)
            // The gable end: siding up to a peak, with a round attic window.
            px.poly(intArrayOf(cx - a, by - a / 2 - h, cx, by - h, r1x, r1y + 2)) { x, y -> if ((yl(x) - y) % 3 == 0) shade(SIDING, 0.82f) else SIDING }
            px.disc(r1x, r1y + rise / 2 + 2, 3, TRIM); px.disc(r1x, r1y + rise / 2 + 2, 2, GHOST)
            lit += intArrayOf(r1x, r1y + rise / 2 + 2, GHOST)
            px.poly(intArrayOf(e0x, e0y + 1, e2x, e2y, r2x, r2y, r1x, r1y)) { x, y ->
                // Rows of slates, a few slipped or gone.
                val row = (y + (x - cx) / 2 + 400) % 4
                when {
                    row == 0 -> shade(ROOF, 0.7f)
                    (x * 13 + y * 7) % 41 == 0 -> shade(ROOF, 1.5f)
                    else -> shade(ROOF, 0.9f)
                }
            }
            // Bargeboards along the gable's edges.
            px.line(e1x, e1y, r1x, r1y, TRIM); px.line(r1x, r1y, e0x, e0y, TRIM)
        }

        /** A steep spire on walls [h] high, its tip leaning over by [lean]. */
        fun spire(h: Int, rise: Int, lean: Int) {
            val tx = cx - a / 2 + b / 2 + lean; val ty = by - a / 4 - b / 4 - h - rise
            val l = intArrayOf(cx - a - 2, by - a / 2 - h + 1); val f = intArrayOf(cx, by - h + 2); val r = intArrayOf(cx + b + 2, by - b / 2 - h + 1)
            px.poly(intArrayOf(l[0], l[1], f[0], f[1], tx, ty)) { x, y -> if ((y - (x - l[0]) / 2) % 3 == 0) shade(ROOF, 1.0f) else shade(ROOF, 1.15f) }
            px.poly(intArrayOf(f[0], f[1], r[0], r[1], tx, ty)) { x, y -> if ((y + (x - f[0]) / 2) % 3 == 0) shade(ROOF, 0.6f) else shade(ROOF, 0.72f) }
            // A bent weathervane.
            px.line(tx, ty, tx, ty - 7, 0xFF5A5450.toInt()); px.line(tx - 3, ty - 5, tx + 2, ty - 6, 0xFF5A5450.toInt())
        }
    }

    /** Lit windows, as (x, y, colour) in sprite pixels; filled in when the sprite is first drawn. */
    val windows: List<IntArray> get() { manor; return lit }
    private val lit = ArrayList<IntArray>()

    val manor: Bitmap by lazy {
        val px = Px(W, H)
        // The main house: two storeys of grey clapboard with a steep gable roof.
        val m = Box(px, CX + 8, BY - 8, 46, 40, lit)
        m.walls(42, SIDING)
        m.window(m.cx - 12, 5, LIT); m.window(m.cx - 44, 5, DARK, boarded = true)
        m.window(m.cx - 12, 25, DARK); m.window(m.cx - 44, 25, LIT)
        m.window(m.cx + 3, 5, DARK, right = true, boarded = true); m.window(m.cx + 29, 5, LIT, right = true)
        m.window(m.cx + 3, 25, LIT, right = true); m.window(m.cx + 16, 25, DARK, right = true); m.window(m.cx + 29, 25, DARK, right = true, boarded = true)
        // The front door under a sagging porch roof on posts, steps up to it.
        m.onR(m.cx + 15, 0, 9, 15) { i, j -> if (i == 0 || i == 8 || j == 14) shade(0xFF3A2228.toInt(), 0.7f) else if (i == 6 && j == 7) 0xFFC8A040.toInt() else 0xFF3A2228.toInt() }
        m.onR(m.cx + 11, 17, 18, 3) { i, j -> if (j == 0) shade(ROOF, 0.5f) else if ((i + j) % 4 == 0) shade(ROOF, 0.7f) else shade(ROOF, 0.9f) }
        for (k in 0..2) px.poly(shade(0xFF6A6460.toInt(), 1f - k * 0.12f),
            m.cx + 14 + k * 2, m.yr(m.cx + 14) + 1 + k, m.cx + 26 + k * 2, m.yr(m.cx + 26) + 1 + k, m.cx + 26 + k * 2, m.yr(m.cx + 26) + 2 + k, m.cx + 14 + k * 2, m.yr(m.cx + 14) + 2 + k)
        for (x in intArrayOf(m.cx + 13, m.cx + 26)) px.rect(x + 1, m.yr(x) - 16, 1, 18, TRIM)
        m.gable(42, 28)
        // Two leaning chimneys.
        for ((x, top, lean) in listOf(Triple(m.cx + 18, 84, 1), Triple(m.cx + 30, 92, -1))) {
            for (j in 0 until 16) px.rect(x + (j * lean) / 6, BY - top - j, 5, 1, if (j % 3 == 0) 0xFF5A3A34.toInt() else 0xFF6E463C.toInt())
            px.rect(x - 1 + (15 * lean) / 6, BY - top - 16, 7, 2, 0xFF3A2A28.toInt())
        }
        // The tower in front of the left face: tall, narrow, round-windowed, its spire leaning off true.
        val t = Box(px, CX - 24, BY - 9, 16, 16, lit)
        t.walls(66, shade(SIDING, 0.92f))
        t.window(t.cx - 12, 8, DARK, boarded = true); t.window(t.cx + 5, 8, DARK, right = true)
        t.window(t.cx - 12, 32, LIT); t.window(t.cx + 5, 50, GHOST, right = true)
        t.onL(t.cx - 16, 64, 16, 2, TRIM); t.onR(t.cx, 64, 16, 2, shade(TRIM, 0.7f))
        t.spire(66, 40, 3)
        // Ivy creeping up the corner of the house.
        val r = Random(7)
        for (k in 0 until 60) {
            val x = m.cx - 1 - r.nextInt(5); val z = r.nextInt(30)
            px.set(x, m.yl(x) - z, if (k % 3 == 0) 0xFF3A5A34.toInt() else 0xFF2A4428.toInt())
        }
        px.light(bands = false)
        px.outline(Sprites.OUT)
        // Cobwebs in the porch corner and under the tower's eaves, drawn after the outline so they stay fine.
        web(px, (m.cx + 12) * px.s, (m.yr(m.cx + 12) - 16) * px.s, 12, 0, 1)
        web(px, (t.cx - 1) * px.s, (t.yl(t.cx - 1) - 62) * px.s, 10, 1, 1)
        px.bitmap()
    }

    /** A spiderweb in the corner at fine pixel (fx, fy), [r] fine pixels long, opening toward ([dx], [dy]) signs. */
    private fun web(px: Px, fx: Int, fy: Int, r: Int, dx: Int, dy: Int) {
        val c = 0xC8E4E4EE.toInt()
        val sx = if (dx == 0) -1 else 1
        val spokes = 5
        val ends = (0 until spokes).map { k ->
            val ang = Math.PI / 2 * k / (spokes - 1)
            Pair(fx + (sx * Math.cos(ang) * r).toInt(), fy + (dy * Math.sin(ang) * r).toInt())
        }
        for ((ex, ey) in ends) fineLine(px, fx, fy, ex, ey, c)
        for (ring in 1..3) {
            val pts = ends.map { (ex, ey) -> Pair(fx + (ex - fx) * ring / 3, fy + (ey - fy) * ring / 3 + ring / 2) }
            for (k in 0 until pts.size - 1) fineLine(px, pts[k].first, pts[k].second, pts[k + 1].first, pts[k + 1].second, c)
        }
    }

    private fun fineLine(px: Px, x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        val n = maxOf(Math.abs(x1 - x0), Math.abs(y1 - y0), 1)
        for (i in 0..n) px.dot(x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, c)
    }

    private val treeCache = HashMap<Int, Bitmap>()
    /** A dead, gnarled tree, the same 26×40 as a living one; some are strung with cobwebs. */
    fun tree(v: Int): Bitmap = treeCache.getOrPut(v) {
        val px = Px(26, 40)
        val f = px.s
        val bark = if (v % 2 == 0) 0xFF4A3E40.toInt() else 0xFF3E3634.toInt()
        val r = Random(v * 31 + 5)
        val tips = ArrayList<IntArray>()
        // Thick limbs drawn as fine-pixel strokes that thin out toward the tips.
        fun limb(x: Float, y: Float, ang: Double, len: Float, w: Float, depth: Int) {
            val ex = x + (Math.cos(ang) * len).toFloat(); val ey = y - (Math.sin(ang) * len).toFloat()
            val steps = (len * f).toInt().coerceAtLeast(1)
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val bx = x + (ex - x) * t; val by = y + (ey - y) * t
                val ww = w * (1 - t * 0.45f)
                val half = (ww * f / 2).toInt()
                for (k in -half..half) px.dot((bx * f).toInt() + k, (by * f).toInt(), if (k == -half) shade(bark, 1.3f) else if (k == half) shade(bark, 0.7f) else bark)
            }
            if (depth == 0 || len < 2.5f) { tips += intArrayOf((ex * f).toInt(), (ey * f).toInt()); return }
            val n = if (depth >= 2) 2 else 1 + r.nextInt(2)
            for (k in 0 until n) {
                val turn = (if (k % 2 == 0) 1 else -1) * (0.35 + r.nextDouble() * 0.55)
                limb(ex, ey, ang + turn + (r.nextDouble() - 0.5) * 0.3, len * (0.55f + r.nextFloat() * 0.2f), w * 0.6f, depth - 1)
            }
        }
        // Roots splayed at the foot.
        for (k in -1..1 step 2) limb(13f, 38f, if (k < 0) Math.PI - 0.25 else 0.25, 3.5f, 1.6f, 0)
        tips.clear()
        val lean = (r.nextDouble() - 0.5) * 0.35
        limb(13f, 39f, Math.PI / 2 + lean, 13f + r.nextInt(4), 3.4f, 4)
        // A knot hole and a few clinging dead leaves.
        px.dot(13 * f, 31 * f, DARK); px.dot(13 * f + 1, 31 * f, DARK)
        if (v % 3 == 0) for (k in 0 until 4) tips.getOrNull(k * 3)?.let { (x, y) -> px.dot(x, y + 1, 0xFF8A5A2A.toInt()); px.dot(x + 1, y + 1, 0xFF6A4020.toInt()) }
        px.light()
        px.outline(Sprites.OUT)
        // Every third tree has a web strung across a fork.
        if (v % 3 == 1) {
            val c = 0xC0E0E0EA.toInt()
            val hub = intArrayOf(13 * f + (v % 2) * 6 - 3, 16 * f)
            val near = tips.sortedBy { (x, y) -> (x - hub[0]) * (x - hub[0]) + (y - hub[1]) * (y - hub[1]) }.take(5)
            for ((x, y) in near) fineLine(px, hub[0], hub[1], x, y, c)
            for (ring in 1..3) for (k in 0 until near.size - 1) {
                val a = near[k]; val b = near[k + 1]
                fineLine(px, hub[0] + (a[0] - hub[0]) * ring / 4, hub[1] + (a[1] - hub[1]) * ring / 4,
                    hub[0] + (b[0] - hub[0]) * ring / 4, hub[1] + (b[1] - hub[1]) * ring / 4, c)
            }
            // The spider.
            px.dot(hub[0] + 2, hub[1] + 3, DARK); px.dot(hub[0] + 3, hub[1] + 3, DARK); px.dot(hub[0] + 2, hub[1] + 4, DARK)
        }
        px.bitmap()
    }

    /** The anchor of the props below: they stand with their foot on (PROP_AX, PROP_AY). */
    const val PROP_AX = 8
    const val PROP_AY = 12

    private val propCache = HashMap<Int, Bitmap>()
    /**
     * Something on the forest floor, 16×14: 0 red toadstools, 1 glowing toadstools, 2 a pumpkin patch,
     * 3 a jack-o'-lantern, 4 a dead shrub hung with a web.
     */
    fun prop(k: Int): Bitmap = propCache.getOrPut(k) {
        val px = Px(16, 14)
        when (k) {
            0, 1 -> {
                val cap = if (k == 0) 0xFFC8342C.toInt() else 0xFF5AE0C8.toInt()
                for ((x, h, w) in listOf(Triple(5, 5, 3), Triple(10, 7, 4), Triple(12, 3, 2))) {
                    px.rect(x, PROP_AY - h, 1, h, 0xFFE8E0D0.toInt())
                    px.poly(cap, x - w, PROP_AY - h + 1, x + w + 1, PROP_AY - h + 1, x + w, PROP_AY - h - 1, x + 1, PROP_AY - h - w + 1, x - w + 1, PROP_AY - h - 1)
                    px.set(x - 1, PROP_AY - h - 1, if (k == 0) 0xFFF4F0E8.toInt() else 0xFFD8FFF4.toInt())
                    if (w > 2) px.set(x + 2, PROP_AY - h, if (k == 0) 0xFFF4F0E8.toInt() else 0xFFD8FFF4.toInt())
                }
            }
            2, 3 -> {
                val o = 0xFFE8761C.toInt()
                fun pumpkin(cx: Int, cy: Int, r: Int) {
                    for (dx in -r..r) for (dy in -r + 1..r - 1) {
                        if (dx * dx * (r - 1) * (r - 1) + dy * dy * r * r > r * r * (r - 1) * (r - 1)) continue
                        px.set(cx + dx, cy + dy, if (dx == 0 || Math.abs(dx) == r / 2 + 1) shade(o, 0.78f) else o)
                    }
                    px.rect(cx, cy - r + 1, 1, 2, 0xFF4F6A2A.toInt())
                }
                if (k == 2) { pumpkin(5, 10, 3); pumpkin(10, 9, 4); px.line(2, 12, 7, 11, 0xFF3A5A2A.toInt()) }
                else {
                    pumpkin(8, 8, 5)
                    val g = 0xFFFFD84A.toInt()
                    px.set(6, 7, g); px.set(10, 7, g); px.rect(6, 10, 5, 1, g); px.set(7, 11, g); px.set(9, 11, g)
                }
            }
            else -> {
                val stick = 0xFF4A3E3A.toInt()
                px.line(8, 13, 8, 6, stick); px.line(8, 9, 4, 4, stick); px.line(8, 8, 13, 3, stick); px.line(8, 11, 12, 8, stick); px.line(6, 7, 3, 7, stick)
            }
        }
        px.light()
        px.outline(Sprites.OUT)
        if (k == 4) {
            val c = 0xC8E4E4EE.toInt(); val f = px.s
            val hub = intArrayOf(8 * f, 6 * f)
            val ends = listOf(4 to 4, 13 to 3, 12 to 8, 3 to 7).map { (x, y) -> intArrayOf(x * f, y * f) }
            for (e in ends) fineLine(px, hub[0], hub[1], e[0], e[1], c)
            for (ring in 1..2) for (i in ends.indices) {
                val a = ends[i]; val b = ends[(i + 1) % ends.size]
                fineLine(px, hub[0] + (a[0] - hub[0]) * ring / 3, hub[1] + (a[1] - hub[1]) * ring / 3, hub[0] + (b[0] - hub[0]) * ring / 3, hub[1] + (b[1] - hub[1]) * ring / 3, c)
            }
        }
        px.bitmap()
    }
}
