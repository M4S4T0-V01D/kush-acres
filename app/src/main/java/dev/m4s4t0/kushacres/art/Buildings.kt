package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.ROOF_COLORS
import dev.m4s4t0.kushacres.model.WALL_COLORS
import dev.m4s4t0.kushacres.model.WorkHouse
import kotlin.random.Random

/** Iso box: bottom corner at (cx, by), left face [wl] px wide, right face [wr] px wide, [h] px tall. */
internal fun box(px: Px, cx: Int, by: Int, wl: Int, wr: Int, h: Int, top: Int, left: Int, right: Int) {
    val lx = cx - wl; val ly = by - wl / 2
    val rx = cx + wr; val ry = by - wr / 2
    px.poly(left, lx, ly, cx, by, cx, by - h, lx, ly - h)
    px.poly(right, cx, by, rx, ry, rx, ry - h, cx, by - h)
    px.poly(top, lx, ly - h, cx, by - h, rx, ry - h, lx + wr, ly - wr / 2 - h)
}

/** The house (2×2 squares) and the facilities (1 square). */
object Buildings {
    private val OUT = Sprites.OUT

    const val SHACK_W = 76
    const val SHACK_H = 88
    /** The footprint's front (bottom) corner sits on this pixel. */
    const val SHACK_AX = 38
    const val SHACK_AY = 85

    private class Spec(val hw: Int, val h: Int, val rise: Int)
    private val SPECS = arrayOf(Spec(20, 12, 9), Spec(25, 14, 13), Spec(27, 15, 14), Spec(29, 24, 15), Spec(31, 18, 15), Spec(31, 27, 18),
        // Villa, castle, sky tower, palace.
        Spec(31, 29, 16), Spec(30, 30, 13), Spec(27, 46, 7), Spec(31, 32, 18))

    private val shackCache = HashMap<String, Bitmap>()

    /** Centres of the shack's front windows and door, in sprite (art) pixels: where light spills out at night. [back]: no door. */
    fun shackLights(lvl: Int, back: Boolean = false): List<Pair<Int, Int>> {
        val L = lvl.coerceIn(0, SPECS.size - 1)
        val hw = SPECS[L].hw; val h = SPECS[L].h
        val bx = SHACK_AX; val by = SHACK_AY - 16 + hw / 2
        val lx = bx - hw; val ly = by - hw / 2
        val dx = lx + hw * 55 / 100
        val wins = when (L) {
            0 -> listOf(lx + hw / 5 to 5)
            1, 2 -> listOf(lx + hw / 6 to 5, lx + hw * 8 / 10 to 5)
            3 -> listOf(lx + hw / 6 to 5, lx + hw * 8 / 10 to 5, lx + hw / 6 to 14, dx to 14, lx + hw * 8 / 10 to 14)
            4 -> listOf(lx + hw / 7 to 6, lx + hw * 3 / 10 to 6, lx + hw * 8 / 10 to 6)
            8 -> (1..4).flatMap { k -> listOf(lx + hw / 7 to 3 + k * 9, dx to 3 + k * 9, lx + hw * 8 / 10 to 3 + k * 9) } +
                listOf(lx + hw / 7 to 3, lx + hw * 8 / 10 to 3)
            else -> listOf(lx + hw / 7 to 5, lx + hw * 8 / 10 to 5, lx + hw / 7 to 16, dx to 16, lx + hw * 8 / 10 to 16)
        }
        val out = wins.map { (x, z) -> (x + 2) to (ly + (x + 2 - lx) / 2 - z - 2) }.toMutableList()
        if (!back) out += (dx + 2) to (ly + (dx + 2 - lx) / 2 - minOf(9, h - 3) / 2) // the door's gap
        return out
    }

    /** The shack seen from the front, or with [back] from behind: the long wall without its door and porch. */
    fun shack(lvl: Int, roof: Int, wall: Int, back: Boolean = false): Bitmap = shackCache.getOrPut("$lvl/$roof/$wall/$back") {
        val L = lvl.coerceIn(0, SPECS.size - 1)
        val sp = SPECS[L]
        val px = Px(SHACK_W, SHACK_H)
        val r = Random(L * 31 + 7)
        val hw = sp.hw; val h = sp.h
        val bx = SHACK_AX; val by = SHACK_AY - 16 + hw / 2
        val lx = bx - hw; val ly = by - hw / 2
        val rx = bx + hw; val ry = ly
        val ky = by - hw
        val wc = WALL_COLORS[wall.coerceIn(0, WALL_COLORS.size - 1)]
        val rc = ROOF_COLORS[roof.coerceIn(0, ROOF_COLORS.size - 1)]
        val stone = 0xFF8A8A90.toInt()

        fun tex(c: Int, x: Int, z: Int): Int = when (L) {
            0 -> if (x % 3 == 0) shade(c, 0.8f) else if (r.nextInt(30) == 0) 0xFF9A5A32.toInt() else c
            1 -> when (z % 3) { 0 -> shade(c, 0.68f); 1 -> shade(c, 1.08f); else -> c }
            2 -> if (z < 2) stone else c
            3 -> if (z < 2) stone else if (z % 2 == 0) shade(c, 0.9f) else c
            4 -> if (z < 3) stone else if (z % 3 == 0) shade(c, 0.85f) else c
            5 -> if (z % 4 == 0 || (x + (z / 4) * 3) % 7 == 0) shade(c, 0.86f) else c
            // Villa: smooth stucco over a stone footing.
            6 -> if (z < 3) stone else if (r.nextInt(9) == 0) shade(c, 1.06f) else c
            // Castle: big grey blocks, tinted by the paint.
            7 -> mix(stone, c, 0.25f).let { s -> if (z % 4 == 0 || (x + (z / 4) * 3) % 6 == 0) shade(s, 0.78f) else if ((x * 7 + z * 3) % 11 == 0) shade(s, 1.1f) else s }
            // Sky tower: a glass curtain on painted ribs.
            8 -> if (x % 5 == 0 || z % 9 == 0) c else if ((x + z) % 7 < 2) 0xFF9CCCF0.toInt() else 0xFF5A8CC0.toInt()
            // Palace: marble with gold bands.
            else -> if (z % 10 == 0 && z > 0) 0xFFE8C040.toInt() else if ((x * 5 + z * 9) % 23 == 0) shade(c, 0.92f) else c
        }
        val wl = wc; val wr = shade(wc, 0.78f)
        val gold = 0xFFE8C040.toInt()
        val fancy = L == 5 || L == 9
        px.poly(intArrayOf(lx, ly, bx, by, bx, by - h, lx, ly - h)) { x, y -> tex(wl, x, ly + (x - lx) / 2 - y) }
        px.poly(intArrayOf(bx, by, rx, ry, rx, ry - h, bx, by - h)) { x, y -> tex(wr, x, by - (x - bx) / 2 - y) }

        // Things on the walls: slanted rectangles that follow the face.
        fun onLeft(x0: Int, z0: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = x0 + i; px.set(x, ly + (x - lx) / 2 - z0 - j, c(i, j)) }
        }
        fun onRight(x0: Int, z0: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = x0 + i; px.set(x, by - (x - bx) / 2 - z0 - j, c(i, j)) }
        }
        val glass = 0xFF6AA8D8.toInt(); val glassHi = 0xFFB8E0FF.toInt(); val frame = 0xFFE8E0D0.toInt()
        fun window(x0: Int, z0: Int, right: Boolean = false, w: Int = 5, hh: Int = 5) {
            val f: (Int, Int) -> Int = { i, j ->
                when {
                    i == 0 || j == 0 || i == w - 1 || j == hh - 1 -> frame
                    i == w / 2 || j == hh / 2 -> shade(frame, 0.8f)
                    i == 1 && j == hh - 2 -> glassHi
                    else -> glass
                }
            }
            if (right) onRight(x0, z0, w, hh, f) else onLeft(x0, z0, w, hh, f)
        }
        val door = 0xFF6A4325.toInt()
        val dh = minOf(9, h - 3)
        val dx = lx + hw * 55 / 100
        if (!back) onLeft(dx, 0, 5, dh) { i, j -> if (i == 0 || i == 4 || j == dh - 1) shade(door, 0.7f) else if (i == 3 && j == dh / 2) 0xFFF2C440.toInt() else door }

        val wins = when (L) {
            0 -> listOf(lx + hw / 5 to 5)
            1, 2 -> listOf(lx + hw / 6 to 5, lx + hw * 8 / 10 to 5)
            3 -> listOf(lx + hw / 6 to 5, lx + hw * 8 / 10 to 5, lx + hw / 6 to 14, dx to 14, lx + hw * 8 / 10 to 14)
            4 -> listOf(lx + hw / 7 to 6, lx + hw * 3 / 10 to 6, lx + hw * 8 / 10 to 6)
            8 -> (1..4).flatMap { k -> listOf(lx + hw / 7 to 3 + k * 9, dx to 3 + k * 9, lx + hw * 8 / 10 to 3 + k * 9) } +
                listOf(lx + hw / 7 to 3, lx + hw * 8 / 10 to 3)
            else -> listOf(lx + hw / 7 to 5, lx + hw * 8 / 10 to 5, lx + hw / 7 to 16, dx to 16, lx + hw * 8 / 10 to 16)
        }
        if (L != 8) wins.forEach { (x, z) -> window(x, z) }
        else wins.forEach { (x, z) -> onLeft(x, z, 5, 4) { i, j -> if (i == 0 || j == 3) 0xFF3A5A80.toInt() else if (i == 1 && j == 2) glassHi else 0xFF8AC0E8.toInt() } }
        // The villa's balcony runs along the upper floor.
        if (L == 6) {
            onLeft(lx + 2, 13, hw - 4, 1) { _, _ -> 0xFFF4F0E8.toInt() }
            onLeft(lx + 2, 16, hw - 4, 1) { _, _ -> 0xFFF4F0E8.toInt() }
            for (i in 0 until hw - 4 step 3) onLeft(lx + 2 + i, 14, 1, 2) { _, _ -> 0xFFF4F0E8.toInt() }
        }
        if (L >= 2) window(bx + hw / 2 - 2, h - 8, right = true, w = 4, hh = 4)
        if (L == 2) wins.forEach { (x, z) ->
            onLeft(x - 1, z - 1, 7, 1) { _, _ -> 0xFF7A4E2A.toInt() }
            onLeft(x - 1, z, 7, 1) { i, _ -> if (i % 2 == 0) intArrayOf(0xFFE05A6A.toInt(), 0xFFF2D24A.toInt(), 0xFFF0F0F0.toInt())[(i / 2) % 3] else 0xFF4FA03C.toInt() }
        }

        // Roof: a gable with the ridge running along the front wall; the gable end sits on the right wall.
        val ltx = lx; val lty = ly - h
        val btx = bx; val bty = by - h
        val rtx = rx; val rty = ry - h
        val kty = ky - h
        val m1x = bx - hw / 2; val m1y = (lty + kty) / 2 - sp.rise
        val m2x = bx + hw / 2; val m2y = (bty + rty) / 2 - sp.rise
        val roofD = shade(rc, 0.72f)
        px.poly(roofD, m1x - 2, m1y - 1, m2x + 2, m2y + 1, rtx + 4, rty, bx, kty - 2)
        if (L >= 1) {
            val cx = m1x + (m2x - m1x) * 7 / 10 + 3
            val cy = m1y + (m2y - m1y) * 7 / 10
            px.rect(cx, cy - 6, 4, 10, 0xFF8A4A3A.toInt()); px.rect(cx, cy - 6, 4, 1, 0xFF4A2A22.toInt())
            px.rect(cx + 3, cy - 5, 1, 9, 0xFF6A3A2E.toInt())
        } else {
            px.rect(m2x - 6, m2y - 8, 2, 10, 0xFF9AA0A8.toInt())
        }
        px.poly(intArrayOf(btx, bty, rtx, rty, m2x, m2y)) { x, y -> tex(wr, x, by - (x - bx) / 2 - y) }
        val shingle: (Int, Int) -> Int = when (L) {
            0 -> { x, _ -> if (x % 3 == 0) shade(rc, 0.8f) else if (x % 3 == 1) shade(rc, 1.12f) else rc }
            else -> { x, y -> if (Math.floorMod(y * 2 - x, 6) < 2) shade(rc, 0.84f) else rc }
        }
        px.poly(intArrayOf(ltx - 4, lty, btx, bty + 2, m2x + 2, m2y + 1, m1x - 2, m1y - 1), shingle)
        px.line(m1x - 2, m1y - 1, m2x + 2, m2y + 1, shade(rc, 1.3f))
        val trim = if (fancy) gold else shade(rc, 0.55f)
        px.line(ltx - 4, lty, btx, bty + 2, trim)
        px.line(btx, bty + 2, m2x + 2, m2y + 1, trim)
        if (L >= 2) px.disc(m2x, (m2y + bty) / 2 + 2, 1, glass)

        // Porches.
        if (!back && (L == 1 || L in 4..6 || L == 9)) {
            val t0 = if (L == 1) dx - 4 else lx + 2
            val t1 = if (L == 1) dx + 9 else bx - 2
            val d = 6; val zp = minOf(h - 4, 11)
            fun wy(x: Int) = ly + (x - lx) / 2
            val floor = if (fancy || L == 6) 0xFFE8E0D0.toInt() else 0xFFA87A4A.toInt()
            px.poly(floor, t0, wy(t0), t1, wy(t1), t1 - d, wy(t1) + d / 2, t0 - d, wy(t0) + d / 2)
            val post = if (fancy || L == 6) 0xFFF4F0E8.toInt() else 0xFF6A4325.toInt()
            val n = if (L == 1) 2 else 4
            for (k in 0 until n) {
                val x = t0 - d + 1 + (t1 - t0) * k / (n - 1)
                val y = wy(x + d - 1) + d / 2
                px.rect(x, y - zp, if (fancy) 2 else 1, zp, post)
            }
            val proof = if (fancy) 0xFFF4F0E8.toInt() else shade(rc, 0.92f)
            px.poly(proof, t0, wy(t0) - zp - 1, t1, wy(t1) - zp - 1, t1 - d, wy(t1) + d / 2 - zp + 1, t0 - d, wy(t0) + d / 2 - zp + 1)
            px.line(t0 - d, wy(t0) + d / 2 - zp + 1, t1 - d, wy(t1) + d / 2 - zp + 1, if (fancy) gold else shade(rc, 0.6f))
        }
        // The castle's corner tower, with battlements and a pennant.
        if (L == 7) {
            val s0 = mix(stone, wc, 0.25f)
            val tx = lx + 4; val tb = ly + 4; val th = h + 16
            box(px, tx, tb, 5, 5, th, shade(s0, 1.1f), s0, shade(s0, 0.78f))
            for (k in 0..4 step 2) { px.rect(tx - 5 + k, tb - th - 5 + k / 2, 2, 3, s0); px.rect(tx + 1 + k, tb - th - 3 - k / 2, 2, 3, shade(s0, 0.78f)) }
            px.rect(tx - 2, tb - th - 8, 1, 6, 0xFF5A4030.toInt())
            px.rect(tx - 1, tb - th - 8, 4, 2, rc)
            for (k in 0 until 3) px.rect(tx - 3, tb - th + 6 + k * 7, 1, 3, 0xFF2A2420.toInt())
        }
        // The tower's antenna, with a red light on top.
        if (L == 8) {
            px.rect(m2x - 5, maxOf(1, m2y - 12), 1, 12, 0xFF9AA0A8.toInt())
            px.rect(m2x - 6, maxOf(0, m2y - 13), 3, 2, 0xFFFF4A3A.toInt())
        }
        // The palace's domes.
        if (L == 9) {
            val cx = (m1x + m2x) / 2; val cy = (m1y + m2y) / 2
            px.rect(cx - 5, cy - 2, 11, 4, 0xFFF4F0E8.toInt())
            px.disc(cx, cy - 4, 6, gold); px.disc(cx - 2, cy - 6, 2, 0xFFFFF0A0.toInt())
            px.rect(cx, cy - 14, 1, 5, gold); px.disc(cx, cy - 15, 1, gold)
            for (dx2 in intArrayOf(-12, 12)) {
                px.disc(cx + dx2, cy + dx2 / 3 - 1, 3, gold); px.set(cx + dx2 - 1, cy + dx2 / 3 - 3, 0xFFFFF0A0.toInt())
                px.rect(cx + dx2, cy + dx2 / 3 - 6, 1, 3, gold)
            }
        }
        px.outline(OUT)
        px.bitmap()
    }

    // ---- Facilities ----

    const val FAC_W = 32
    const val FAC_H = 40
    /** The square's centre sits on this pixel. */
    const val FAC_AX = 16
    const val FAC_AY = 31

    private val facCache = HashMap<Facility, Bitmap>()

    fun facility(f: Facility): Bitmap = facCache.getOrPut(f) {
        val px = Px(FAC_W, FAC_H)
        val cx = 16; val by = 37
        when (f) {
            Facility.ROLLER -> {
                val wood = 0xFFA8764A.toInt()
                px.rect(16, 31, 1, 7, shade(wood, 0.6f)); px.rect(5, 25, 1, 7, shade(wood, 0.6f)); px.rect(27, 25, 1, 7, shade(wood, 0.6f))
                box(px, cx, 31, 12, 12, 2, shade(wood, 1.15f), wood, shade(wood, 0.75f))
                px.rect(8, 21, 5, 3, 0xFFF4F4F0.toInt())
                for (k in 0..2) { px.line(13 + k * 2, 24 - k, 17 + k * 2, 22 - k, 0xFFF4F4F0.toInt()); px.set(17 + k * 2, 22 - k, 0xFFC86A3A.toInt()) }
                px.disc(22, 22, 2, 0xFF4FA03C.toInt()); px.set(21, 21, 0xFF9AD86A.toInt())
            }
            Facility.PRESS -> {
                val steel = 0xFF8A9098.toInt()
                box(px, cx, by, 11, 11, 4, shade(steel, 1.2f), steel, shade(steel, 0.7f))
                px.rect(7, 19, 2, 14, shade(steel, 0.8f)); px.rect(24, 19, 2, 14, shade(steel, 0.6f))
                px.rect(7, 17, 19, 3, shade(steel, 0.9f))
                px.rect(15, 11, 2, 9, 0xFFC8CCD4.toInt())
                px.rect(10, 10, 12, 2, 0xFFD8484A.toInt()); px.disc(10, 10, 1, 0xFF2C3038.toInt()); px.disc(21, 10, 1, 0xFF2C3038.toInt())
                px.rect(11, 24, 11, 2, 0xFF5C6068.toInt())
                px.rect(12, 27, 9, 2, 0xFF7A4E2A.toInt()); px.rect(12, 27, 9, 1, 0xFFA87A4A.toInt())
            }
            Facility.KITCHEN -> {
                val wall = 0xFFF0D8C0.toInt()
                px.rect(21, 4, 3, 10, 0xFF9AA0A8.toInt()); px.rect(21, 4, 3, 1, 0xFF5C6068.toInt())
                box(px, cx, by, 12, 12, 13, 0, wall, shade(wall, 0.78f))
                px.poly(intArrayOf(4, 18, 16, 24, 28, 18, 16, 12)) { x, y -> if (Math.floorMod(x - y * 2, 8) < 4) 0xFFE87AA8.toInt() else 0xFFF4F0F0.toInt() }
                for (i in 0 until 6) for (j in 0 until 5) px.set(7 + i, 32 + (i + 3) / 2 - 3 - j - 3, if (j == 0 || j == 4 || i == 0 || i == 5) 0xFF3C3038.toInt() else if ((i + j) % 3 == 0) 0xFFFFD25A.toInt() else 0xFFF08A3A.toInt())
                px.disc(22, 28, 2, 0xFFB07A4A.toInt()); px.set(22, 26, 0xFFE87AA8.toInt())
            }
            Facility.LAB -> {
                box(px, cx, by, 12, 12, 9, 0xFFE8ECF0.toInt(), 0xFFC8CCD4.toInt(), 0xFF8A9098.toInt())
                for (i in 0 until 12) px.set(4 + i, 32 + i / 2 - 1, if ((i / 2) % 2 == 0) 0xFFF2C440.toInt() else 0xFF2C3038.toInt())
                px.disc(10, 20, 3, 0xFF7AD8E8.toInt()); px.rect(9, 14, 2, 4, 0xFFB8ECF4.toInt()); px.set(9, 19, 0xFFE8FCFF.toInt())
                px.rect(18, 11, 3, 10, 0xFFE8B040.toInt()); px.rect(18, 11, 1, 10, 0xFFFFE08A.toInt())
                px.line(11, 14, 18, 12, 0xFF6C7078.toInt())
                px.disc(24, 20, 2, 0xFF9AD86A.toInt())
            }
            Facility.VAPE -> {
                box(px, cx, by, 12, 12, 10, 0xFF3A3A48.toInt(), 0xFF2A2A36.toInt(), 0xFF1E1E28.toInt())
                val neon = 0xFFC060FF.toInt()
                px.line(4, 21, 16, 27, neon); px.line(16, 27, 28, 21, neon)
                for (i in 0 until 5) for (j in 0 until 3) px.set(8 + i, 31 + (i + 4) / 2 - 2 - j - 3, 0xFF6AFF8A.toInt())
                px.rect(14, 4, 5, 3, 0xFF1C1A20.toInt())
                px.rect(14, 7, 5, 9, 0xFFC8CCD4.toInt()); px.rect(15, 8, 3, 7, 0xFFE8B040.toInt()); px.set(15, 8, 0xFFFFE89A.toInt())
                px.rect(14, 16, 5, 4, 0xFFF2C440.toInt()); px.rect(18, 16, 1, 4, 0xFFB07A1C.toInt())
            }
        }
        px.outline(OUT)
        px.bitmap()
    }

    // ---- Work buildings (same frame as the facilities) ----

    private val houseCache = HashMap<WorkHouse, Bitmap>()

    /** A pyramid roof over a box of half-widths [w] and wall height [h] standing on (cx, by). */
    private fun roof(px: Px, cx: Int, by: Int, w: Int, h: Int, rise: Int, c: Int) {
        val top = by - h; val ax = cx; val ay = top - w / 2 - rise
        px.poly(shade(c, 1.1f), cx - w - 1, top - w / 2, cx, top + 1, ax, ay)
        px.poly(shade(c, 0.78f), cx, top + 1, cx + w + 1, top - w / 2, ax, ay)
    }

    fun house(h: WorkHouse): Bitmap = houseCache.getOrPut(h) {
        val px = Px(FAC_W, FAC_H)
        val cx = 16; val by = 37
        when (h) {
            WorkHouse.TOWER -> {
                val wood = 0xFF8A5A34.toInt()
                for (lx in intArrayOf(7, 25)) px.rect(lx, 16, 1, 17, shade(wood, 0.7f))
                px.rect(16, 18, 1, 19, shade(wood, 0.6f))
                px.line(7, 30, 16, 22, shade(wood, 0.8f)); px.line(25, 30, 16, 22, shade(wood, 0.6f))
                val tank = 0xFF5A9AD8.toInt()
                px.rect(6, 6, 21, 11, tank); px.rect(6, 6, 7, 11, shade(tank, 1.15f)); px.rect(20, 6, 7, 11, shade(tank, 0.8f))
                for (y in intArrayOf(8, 12, 16)) px.rect(6, y, 21, 1, shade(tank, 0.65f))
                px.poly(0xFFB8C4CC.toInt(), 5, 6, 16, 1, 27, 6)
                px.rect(14, 33, 4, 2, 0xFF8FD4FF.toInt()); px.set(16, 30, 0xFF8FD4FF.toInt())
            }
            WorkHouse.GROW -> {
                val glass = 0xFFB8ECDA.toInt()
                box(px, cx, by, 12, 12, 10, 0, glass, shade(glass, 0.82f))
                for (i in 0..3) { px.rect(6 + i * 3, 25 + i, 2, 3, 0xFF4FA03C.toInt()); px.rect(19 + i * 2, 28 - i, 2, 3, 0xFF3E8A30.toInt()) }
                px.line(4, 22, 16, 28, 0xFFF4F4F0.toInt()); px.line(16, 28, 28, 22, 0xFFF4F4F0.toInt())
                roof(px, cx, by, 12, 10, 3, 0xFFE8B8F0.toInt())
                px.rect(12, 14, 8, 1, 0xFFFF7AE8.toInt())
            }
            WorkHouse.BUNK -> {
                val logs = 0xFFB07A4A.toInt()
                box(px, cx, by, 12, 12, 9, 0, logs, shade(logs, 0.75f))
                for (k in 0..3) { px.line(4, 25 + k * 2, 16, 31 + k * 2, shade(logs, 0.8f)) }
                px.rect(19, 25, 4, 7, 0xFF5A3A22.toInt()); px.set(22, 28, 0xFFF2C440.toInt())
                px.rect(7, 24, 3, 3, 0xFFFFE08A.toInt())
                roof(px, cx, by, 12, 9, 6, 0xFF3E6E4A.toInt())
                px.rect(21, 8, 2, 5, 0xFF7A7A80.toInt())
            }
            WorkHouse.TRIM -> {
                val board = 0xFFD8B87A.toInt()
                box(px, cx, by, 12, 12, 8, 0, board, shade(board, 0.75f))
                px.poly(0xFF3A2A1E.toInt(), 6, 28, 15, 33, 15, 26, 6, 21)
                for (i in 0..2) px.rect(8 + i * 3, 23 + i, 1, 4, 0xFF6AB84A.toInt())
                px.rect(19, 25, 6, 1, 0xFFC8CCD4.toInt()); px.rect(23, 24, 2, 3, 0xFFC8484A.toInt())
                roof(px, cx, by, 12, 8, 4, 0xFF9AA0A8.toInt())
            }
            WorkHouse.DRYING -> {
                val red = 0xFFB8443A.toInt()
                box(px, cx, by, 12, 12, 14, 0, red, shade(red, 0.75f))
                px.line(5, 20, 15, 25, 0xFFF4F0E8.toInt()); px.line(5, 30, 15, 35, 0xFFF4F0E8.toInt())
                px.poly(0xFF3A2A1E.toInt(), 7, 22, 14, 26, 14, 34, 7, 30)
                for (i in 0..2) { px.rect(8 + i * 2, 24 + i, 1, 3, 0xFF8A9A3A.toInt()) }
                roof(px, cx, by, 12, 14, 5, 0xFF5C4A3E.toInt())
                px.rect(14, 8, 4, 3, 0xFFF4F0E8.toInt())
            }
            WorkHouse.TOOLS -> {
                val steel = 0xFF7A8088.toInt()
                box(px, cx, by, 12, 12, 9, 0, 0xFF6A8A5A.toInt(), 0xFF4E6A42.toInt())
                px.rect(9, 22, 1, 9, 0xFFA87A4A.toInt()); px.rect(7, 22, 5, 1, steel)
                px.rect(12, 23, 1, 8, 0xFFA87A4A.toInt()); px.rect(11, 30, 3, 2, steel)
                px.rect(19, 26, 5, 4, 0xFFE8B040.toInt()); px.rect(19, 26, 5, 1, 0xFFFFE08A.toInt())
                roof(px, cx, by, 12, 9, 3, 0xFF8A3A2E.toInt())
            }
        }
        px.outline(OUT)
        px.bitmap()
    }
}
