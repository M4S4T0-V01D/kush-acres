package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.Generator
import dev.m4s4t0.kushacres.model.Special
import dev.m4s4t0.kushacres.model.Vehicle
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
        px.light(bands = false)
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
        px.light(bands = false)
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
        px.light(bands = false)
        px.outline(OUT)
        px.bitmap()
    }

    // ---- Generators and specials (same frame again) ----

    private val genCache = HashMap<Generator, Bitmap>()
    private val specCache = HashMap<Special, Bitmap>()

    /** A lightning bolt, [c] coloured, with its top at (x, y). */
    private fun bolt(px: Px, x: Int, y: Int, c: Int) = px.poly(c, x + 2, y, x - 1, y + 4, x + 1, y + 4, x - 1, y + 8, x + 4, y + 3, x + 2, y + 3, x + 4, y)

    fun generator(g: Generator): Bitmap = genCache.getOrPut(g) {
        val px = Px(FAC_W, FAC_H)
        val cx = 16; val by = 37
        when (g) {
            Generator.GAS -> {
                val red = 0xFFD8483A.toInt()
                px.rect(7, 31, 18, 2, 0xFF5C6068.toInt())
                box(px, cx, by - 3, 8, 8, 9, shade(red, 1.2f), red, shade(red, 0.7f))
                px.rect(10, 26, 4, 3, 0xFF2C3038.toInt()); px.set(11, 27, 0xFFFFD23A.toInt())
                px.rect(20, 16, 2, 7, 0xFF7A8088.toInt()); px.disc(21, 13, 2, 0x99C8C8C8.toInt()); px.disc(24, 9, 2, 0x66C8C8C8.toInt())
                px.rect(13, 20, 6, 1, 0xFF2C3038.toInt())
            }
            Generator.DIESEL -> {
                val y = 0xFFF2B830.toInt()
                box(px, cx, by, 12, 12, 11, shade(y, 1.15f), y, shade(y, 0.7f))
                for (k in 0..3) px.line(6, 25 + k * 2, 14, 29 + k * 2, 0xFF3A3020.toInt())
                px.rect(21, 7, 3, 12, 0xFF3A3A40.toInt()); px.rect(20, 6, 5, 2, 0xFF2A2A30.toInt())
                px.disc(22, 3, 2, 0x88505050.toInt())
                bolt(px, 19, 24, 0xFF2C2418.toInt())
            }
            Generator.SOLAR -> {
                val frame = 0xFF9AA0A8.toInt()
                for (lx in intArrayOf(8, 16, 24)) px.rect(lx, 22, 1, 12, shade(frame, 0.7f))
                val cell = 0xFF2E5AA8.toInt()
                px.poly(intArrayOf(2, 22, 16, 29, 30, 22, 16, 13)) { x, yy -> if ((x + yy * 2) % 6 == 0 || (x - yy * 2 + 60) % 6 == 0) 0xFF8AB8F0.toInt() else if (x < 16) shade(cell, 1.15f) else cell }
                px.line(2, 22, 16, 13, 0xFFD8E8F8.toInt()); px.line(16, 13, 30, 22, 0xFFB8C8E0.toInt())
                px.rect(14, 33, 5, 3, 0xFF3A3A40.toInt()); bolt(px, 15, 33, 0xFFFFD23A.toInt())
            }
            Generator.WIND -> {
                val w = 0xFFF0F0F0.toInt()
                px.rect(10, 34, 12, 2, 0xFF8A9098.toInt())
                px.poly(w, 15, 10, 17, 10, 18, 35, 14, 35); px.rect(17, 10, 1, 25, 0xFFC8CCD4.toInt())
                px.rect(14, 7, 5, 4, 0xFFD8DCE0.toInt())
                px.poly(w, 16, 9, 15, 0, 17, 0); px.poly(w, 16, 9, 4, 14, 5, 16); px.poly(0xFFD8DCE0.toInt(), 16, 9, 28, 15, 27, 17)
                px.disc(16, 9, 1, 0xFF5C6068.toInt()); px.set(16, 1, 0xFFFF4A3A.toInt())
            }
            Generator.STATION -> {
                val c = 0xFFB8BCC4.toInt()
                box(px, cx, by, 12, 12, 9, shade(c, 1.15f), c, shade(c, 0.72f))
                px.poly(0xFFD8D8D0.toInt(), 5, 26, 7, 8, 13, 8, 15, 26); px.rect(7, 8, 6, 2, 0xFFE8E8E0.toInt())
                px.poly(0xFFA8A8A0.toInt(), 13, 8, 15, 26, 15, 8)
                px.disc(10, 5, 3, 0xAAF0F0F0.toInt()); px.disc(13, 2, 2, 0x77F0F0F0.toInt())
                for (k in 0..3) px.rect(22, 6 + k * 4, 3, 2, if (k % 2 == 0) 0xFFE8483A.toInt() else 0xFFF4F4F0.toInt())
                px.rect(22, 22, 3, 6, 0xFF8A9098.toInt())
                bolt(px, 19, 27, 0xFFFFD23A.toInt())
            }
            Generator.FUSION -> {
                box(px, cx, by, 12, 12, 6, 0xFF4A4A5A.toInt(), 0xFF3A3A48.toInt(), 0xFF2A2A36.toInt())
                val glow = 0xFF5AF0FF.toInt()
                px.disc(16, 18, 10, 0xFF2A2A36.toInt()); px.disc(16, 18, 8, glow); px.disc(16, 18, 6, 0xFF2A2A36.toInt())
                px.disc(16, 18, 3, 0xFFE8FFFF.toInt()); px.disc(16, 18, 2, 0xFFFFFFFF.toInt())
                px.rect(5, 18, 3, 1, 0xFFB060F0.toInt()); px.rect(24, 18, 3, 1, 0xFFB060F0.toInt())
                px.set(12, 12, 0xFFC8FFFF.toInt()); px.set(20, 24, 0xFFC8FFFF.toInt())
            }
        }
        px.light(bands = false)
        px.outline(OUT)
        px.bitmap()
    }

    fun special(sp: Special): Bitmap = specCache.getOrPut(sp) {
        val px = Px(FAC_W, FAC_H)
        val cx = 16; val by = 37
        val red = 0xFFD8402E.toInt(); val gold = 0xFFF2C440.toInt(); val green = 0xFF3EA040.toInt()
        when (sp) {
            Special.VAULT -> {
                val conc = 0xFF9A9A92.toInt()
                box(px, cx, by, 12, 12, 10, shade(conc, 1.15f), conc, shade(conc, 0.72f))
                px.disc(10, 27, 4, 0xFF6C7078.toInt()); px.disc(10, 27, 3, 0xFFB8BCC4.toInt()); px.line(8, 27, 12, 27, 0xFF5C6068.toInt()); px.line(10, 25, 10, 29, 0xFF5C6068.toInt())
                px.disc(16, 21, 4, green); px.disc(15, 19, 2, 0xFF7AD85A.toInt()); px.rect(16, 21, 1, 4, 0xFF2E6A2A.toInt())
                px.rect(21, 26, 4, 2, 0xFF7AFF7A.toInt())
            }
            Special.WELL -> {
                val stone = 0xFF8A8478.toInt()
                px.disc(16, 30, 8, shade(stone, 0.7f)); px.disc(16, 28, 8, stone); px.disc(16, 27, 6, 0xFF2E6AA8.toInt()); px.set(14, 26, 0xFF8FD4FF.toInt())
                for (k in 0..3) px.set(9 + k * 4, 31 - (k % 2), shade(stone, 1.25f))
                px.rect(7, 12, 1, 15, 0xFF7A4E2A.toInt()); px.rect(24, 12, 1, 15, 0xFF5A3A22.toInt())
                px.poly(0xFFB8443A.toInt(), 4, 14, 16, 6, 28, 14, 26, 15, 16, 9, 6, 15)
                px.rect(8, 15, 16, 1, 0xFF5A3A22.toInt()); px.rect(15, 16, 1, 6, 0xFFC8B89A.toInt()); px.rect(14, 21, 3, 3, 0xFF9A6A3A.toInt())
            }
            Special.RADIO -> {
                val wood = 0xFFA8764A.toInt()
                box(px, cx, by, 10, 10, 8, 0, wood, shade(wood, 0.72f))
                roof(px, cx, by, 10, 8, 2, 0xFF5C4A3E.toInt())
                for ((k, c) in listOf(red, gold, green).withIndex()) px.line(6, 28 + k, 15, 32 + k, c)
                px.rect(21, 2, 1, 26, 0xFF9AA0A8.toInt()); px.line(18, 20, 21, 8, 0xFF9AA0A8.toInt()); px.line(24, 20, 21, 8, 0xFF7A8088.toInt())
                for (y in intArrayOf(8, 13, 18)) px.rect(19, y, 5, 1, 0xFF7A8088.toInt())
                px.disc(21, 1, 1, 0xFFFF3A3A.toInt())
                px.line(24, 3, 27, 1, 0xFFF4F4F0.toInt()); px.line(25, 5, 29, 3, 0xFFF4F4F0.toInt())
            }
            Special.DEPOT -> {
                val wall = 0xFF5A8AB8.toInt()
                box(px, cx, by, 12, 12, 11, shade(wall, 1.2f), wall, shade(wall, 0.72f))
                px.poly(0xFFC8CCD4.toInt(), 6, 24, 14, 28, 14, 35, 6, 31)
                for (k in 0..3) px.line(6, 26 + k * 2, 14, 30 + k * 2, 0xFF8A9098.toInt())
                px.rect(18, 22, 8, 3, 0xFFF4F0E8.toInt()); px.rect(19, 23, 2, 1, green); px.rect(22, 23, 3, 1, red)
                px.rect(20, 28, 5, 3, 0xFF4AA04A.toInt()); px.disc(21, 31, 1, 0xFF222222.toInt()); px.disc(24, 30, 1, 0xFF222222.toInt())
            }
            Special.TERRACE -> {
                val st = 0xFFB0A088.toInt()
                for (k in 0..2) {
                    val w = 12 - k * 3
                    box(px, cx, by - k * 5, w, w, 4, 0xFF4E8A3A.toInt(), st, shade(st, 0.72f))
                    for (i in 0 until w / 2) { px.disc(cx - w + 3 + i * 2, by - k * 5 - 6 - (w - 3 - i * 2) / 2 + i / 2, 1, if (i % 2 == 0) green else 0xFF6AC84A.toInt()) }
                    px.line(cx, by - k * 5 - 4, cx + w - 1, by - k * 5 - 4 - (w - 1) / 2, 0xFF5AB0F0.toInt())
                }
            }
            Special.DISPENSARY -> {
                val wall = 0xFFF4F0E8.toInt()
                box(px, cx, by, 12, 12, 11, 0, wall, shade(wall, 0.8f))
                roof(px, cx, by, 12, 11, 2, green)
                px.poly(0xFF7AC8E8.toInt(), 6, 25, 14, 29, 14, 34, 6, 30)
                px.rect(19, 25, 4, 7, 0xFF3E6E4A.toInt()); px.set(22, 28, gold)
                px.rect(14, 4, 5, 13, green); px.rect(11, 8, 11, 5, green); px.rect(15, 5, 3, 11, 0xFF7AFF7A.toInt()); px.rect(12, 9, 9, 3, 0xFF7AFF7A.toInt())
            }
            Special.DOME -> {
                px.disc(16, 31, 12, 0xFF6A6A70.toInt())
                px.disc(16, 26, 12, 0x99B8ECF4.toInt())
                for (k in -2..2) px.line(16 + k * 5, 14, 16 + k * 6, 31, 0xFFE8F4F8.toInt())
                for (y in intArrayOf(19, 25)) px.line(5, y, 27, y, 0xFFE8F4F8.toInt())
                px.disc(16, 30, 5, green); px.disc(11, 31, 3, 0xFF6AC84A.toInt()); px.disc(22, 31, 3, 0xFF4E8A3A.toInt())
                px.disc(10, 19, 2, 0xFFFFFFFF.toInt()); px.disc(24, 7, 3, gold); px.set(23, 6, 0xFFFFF0A0.toInt())
            }
            Special.CURING -> {
                val wood = 0xFF7A4E2A.toInt()
                box(px, cx, by, 12, 12, 10, 0, wood, shade(wood, 0.72f))
                roof(px, cx, by, 12, 10, 4, 0xFF8A3A2E.toInt())
                for (k in 0..2) { px.rect(6 + k * 3, 25 + k, 2, 4, 0xFFB8ECF4.toInt()); px.rect(6 + k * 3, 24 + k, 2, 1, gold); px.set(6 + k * 3, 27 + k, green) }
                px.rect(19, 25, 5, 7, 0xFF3A2A1E.toInt()); px.rect(19, 28, 5, 1, 0xFF5A3A22.toInt())
                px.rect(13, 9, 6, 3, 0xFFF4F0E8.toInt())
            }
            Special.FACTORY -> {
                val brick = 0xFFA84A3A.toInt()
                box(px, cx, by, 12, 12, 11, 0, brick, shade(brick, 0.72f))
                for (k in 0..2) px.poly(0xFF5C6068.toInt(), 4 + k * 4, 22 + k * 2, 8 + k * 4, 18 + k * 2, 8 + k * 4, 24 + k * 2)
                px.poly(shade(brick, 0.85f), 16, 26, 28, 20, 28, 15, 16, 21)
                for (k in 0..1) px.rect(19 + k * 4, 25 - k * 2, 2, 3, 0xFFFFE08A.toInt())
                px.rect(23, 3, 3, 14, 0xFF8A3A2E.toInt()); px.rect(23, 3, 3, 1, 0xFF2A2A30.toInt())
                px.disc(25, 1, 2, 0x88C8C8C8.toInt())
                px.disc(10, 28, 2, gold); px.set(10, 28, 0xFF7A5A1E.toInt())
            }
            Special.SUBSTATION -> {
                px.poly(0xFF7A7A72.toInt(), 4, 31, 16, 37, 28, 31, 16, 25)
                for (i in 0..12) { px.set(4 + i, 31 + i / 2 - 4, 0xFF9AA0A8.toInt()); px.set(16 + i, 37 - i / 2 - 4, 0xFF9AA0A8.toInt()) }
                for (i in 0..12 step 2) { px.rect(4 + i, 27 + i / 2, 1, 4, 0xFF8A9098.toInt()); px.rect(16 + i, 33 - i / 2, 1, 4, 0xFF6C7078.toInt()) }
                box(px, 12, 31, 4, 4, 9, 0xFFA8B0B8.toInt(), 0xFF8A9098.toInt(), 0xFF5C6068.toInt())
                box(px, 21, 30, 4, 4, 7, 0xFFA8B0B8.toInt(), 0xFF8A9098.toInt(), 0xFF5C6068.toInt())
                for (x in intArrayOf(10, 12, 14)) { px.rect(x, 15, 1, 6, 0xFFC86A3A.toInt()); px.set(x, 14, 0xFFE8E8E0.toInt()) }
                px.line(10, 14, 22, 12, 0xFF2C3038.toInt()); px.rect(21, 12, 1, 9, 0xFF6C7078.toInt())
                bolt(px, 19, 22, 0xFFFFD23A.toInt())
            }
            Special.ACADEMY -> {
                val wall = 0xFFE8DCC0.toInt()
                box(px, cx, by, 12, 12, 10, 0, wall, shade(wall, 0.78f))
                for (k in 0..2) { px.rect(6 + k * 3, 23 + k, 1, 7, 0xFFFFFFFF.toInt()); px.rect(19 + k * 3, 30 - k * 2 - 6, 1, 7, shade(wall, 0.9f)) }
                px.poly(0xFF3A5A9A.toInt(), 3, 21, 16, 14, 29, 21, 16, 28)
                px.poly(0xFF2C4478.toInt(), 16, 28, 29, 21, 29, 22, 16, 29)
                px.rect(14, 8, 5, 3, 0xFF2C2C34.toInt()); px.poly(0xFF2C2C34.toInt(), 10, 9, 16, 6, 22, 9, 16, 11)
                px.line(21, 9, 23, 14, gold); px.set(23, 15, gold)
            }
            Special.EXCHANGE -> {
                val marble = 0xFFF0ECE4.toInt()
                box(px, cx, by, 12, 12, 3, marble, shade(marble, 0.9f), shade(marble, 0.7f))
                for (k in 0..3) { px.rect(6 + k * 3, 20 + k * 1, 2, 11, marble); px.rect(18 + k * 3, 23 - k, 2, 11, shade(marble, 0.8f)) }
                px.poly(marble, 2, 20, 16, 10, 30, 20, 16, 23); px.poly(shade(marble, 0.75f), 16, 23, 30, 20, 30, 21, 16, 24)
                px.disc(16, 16, 2, gold)
                px.line(5, 8, 9, 5, 0xFF4AE04A.toInt()); px.line(9, 5, 12, 7, 0xFF4AE04A.toInt()); px.line(12, 7, 15, 2, 0xFF4AE04A.toInt())
            }
            Special.TEMPLE -> {
                val w = 0xFFE8D8B0.toInt()
                box(px, cx, by, 12, 12, 4, shade(w, 1.1f), w, shade(w, 0.72f))
                box(px, cx, by - 4, 8, 8, 9, 0, red, shade(red, 0.72f))
                for ((k, c) in listOf(red, gold, green).withIndex()) { px.line(8, 21 + k * 2, 16, 25 + k * 2, c); px.line(16, 25 + k * 2, 24, 21 + k * 2, shade(c, 0.8f)) }
                px.poly(green, 5, 20, 16, 4, 27, 20, 16, 26)
                px.poly(shade(green, 0.7f), 16, 26, 27, 20, 27, 21, 16, 27)
                px.disc(16, 5, 2, gold); px.set(15, 4, 0xFFFFF0A0.toInt())
                px.rect(14, 28, 4, 5, 0xFF3A2A1E.toInt())
            }
            Special.MINE -> {
                val rock = 0xFF7A6E62.toInt()
                px.poly(rock, 2, 32, 8, 16, 16, 10, 24, 14, 30, 30, 16, 37)
                px.poly(shade(rock, 0.75f), 16, 37, 30, 30, 24, 14, 16, 22)
                px.poly(0xFF1C1610.toInt(), 9, 33, 10, 25, 15, 23, 17, 30)
                px.rect(9, 24, 1, 9, 0xFF8A5A34.toInt()); px.rect(16, 22, 1, 9, 0xFF8A5A34.toInt()); px.line(9, 24, 16, 22, 0xFF8A5A34.toInt())
                px.rect(17, 31, 6, 3, 0xFF5C6068.toInt()); px.rect(18, 29, 4, 2, 0xFFB060F0.toInt()); px.set(19, 29, 0xFFE8C8FF.toInt())
                px.set(21, 18, 0xFFB060F0.toInt()); px.set(11, 17, 0xFFD8A0FF.toInt()); px.set(25, 24, 0xFFB060F0.toInt())
            }
            Special.MONUMENT -> {
                val st = 0xFFE8E0D0.toInt()
                box(px, cx, by, 12, 12, 3, shade(st, 1.1f), st, shade(st, 0.72f))
                box(px, cx, by - 3, 7, 7, 5, shade(gold, 1.1f), gold, shade(gold, 0.7f))
                val leaf = 0xFFFFD23A.toInt()
                px.rect(16, 6, 1, 20, shade(gold, 0.6f))
                px.poly(leaf, 16, 2, 14, 12, 16, 22, 18, 12)
                px.poly(leaf, 16, 20, 7, 10, 9, 15); px.poly(shade(leaf, 0.85f), 16, 20, 25, 10, 23, 15)
                px.poly(leaf, 16, 22, 5, 17, 9, 20); px.poly(shade(leaf, 0.85f), 16, 22, 27, 17, 23, 20)
                px.set(15, 6, 0xFFFFFFC0.toInt()); px.set(9, 12, 0xFFFFFFC0.toInt())
            }
        }
        px.light(bands = false)
        px.outline(OUT)
        px.bitmap()
    }

    const val VEH_W = 68
    const val VEH_H = 60
    /** The 2×2 footprint's front (bottom) corner sits on this pixel. */
    const val VEH_AX = 34
    const val VEH_AY = 56

    private val vehicleCache = HashMap<Vehicle, Bitmap>()

    /** A vehicle parked across a 2×2 footprint, its long side along the left face. */
    fun vehicle(v: Vehicle): Bitmap = vehicleCache.getOrPut(v) {
        val px = Px(VEH_W, VEH_H)
        // Body box centred on the footprint: [wl] long, [wr] wide.
        val wl = 28; val wr = 14
        val cx = VEH_AX + (wl - wr) / 2; val by = VEH_AY - 16 + (wl + wr) / 4
        val lx = cx - wl; val ly = by - wl / 2
        /** A strip on the left face from [d0] to [d1] along it, [z0] to [z1] up. */
        fun face(d0: Int, d1: Int, z0: Int, z1: Int, c: Int) =
            px.poly(c, lx + d0, ly + d0 / 2 - z0, lx + d1, ly + d1 / 2 - z0, lx + d1, ly + d1 / 2 - z1, lx + d0, ly + d0 / 2 - z1)
        fun wheel(d: Int, r: Int) {
            val x = lx + d; val y = ly + d / 2
            px.rect(x - r, y - r - 1, r * 2 + 1, r * 2 + 1, 0xFF26221E.toInt())
            px.rect(x - r / 2, y - r / 2 - 1, r + 1, r + 1, 0xFF8A8A90.toInt())
        }
        val glass = 0xFF9ED8F0.toInt()
        when (v) {
            Vehicle.RV -> {
                val c = 0xFFF0E6CC.toInt()
                box(px, cx, by - 3, wl, wr, 16, shade(c, 1.06f), c, shade(c, 0.78f))
                face(1, wl - 1, 5, 7, 0xFF3E7D8A.toInt()); face(1, wl - 1, 8, 9, 0xFFE0703A.toInt())
                for (d in intArrayOf(4, 10)) face(d, d + 4, 11, 16, glass)
                face(16, 20, 3, 15, 0xFF8A6238.toInt())
                box(px, cx + 3, by - 6, 6, 8, 3, 0xFFC8C8CC.toInt(), 0xFFA0A0A8.toInt(), 0xFF80808A.toInt()) // roof vent
                wheel(6, 3); wheel(22, 3)
            }
            Vehicle.TRUCK -> {
                val c = 0xFFE84A4A.toInt()
                box(px, cx, by - 3, wl, wr, 18, 0xFFF4F4F0.toInt(), c, shade(c, 0.75f))
                face(5, 20, 7, 15, 0xFF2A1E18.toInt())
                face(4, 21, 15, 18, 0xFFF2C440.toInt())
                for (d in 6..18 step 4) face(d, d + 2, 15, 18, 0xFFFFFFFF.toInt())
                face(7, 10, 8, 11, 0xFFFFC870.toInt()); face(13, 17, 8, 10, 0xFF6AB84A.toInt())
                face(22, 26, 10, 15, glass)
                wheel(6, 3); wheel(23, 3)
            }
            Vehicle.TRACTOR -> {
                val c = 0xFF4FA03C.toInt()
                box(px, cx - 2, by - 5, 18, 10, 8, shade(c, 1.15f), c, shade(c, 0.75f))
                box(px, cx - 14, by - 11, 9, 9, 18, 0xFFF2C440.toInt(), shade(glass, 0.9f), shade(glass, 0.7f))
                px.rect(cx - 6, by - 26, 2, 7, 0xFF3A3A3E.toInt())
                wheel(7, 6); wheel(22, 3)
                px.rect(lx + 27, ly + 3, 6, 2, 0xFFF2C440.toInt())
            }
            Vehicle.VAN -> {
                val c = 0xFFF4F4F0.toInt()
                box(px, cx, by - 3, wl, wr, 17, shade(c, 0.95f), c, shade(c, 0.78f))
                face(0, wl, 6, 9, 0xFF3A6EC8.toInt())
                box(px, cx + 2, by - 13, 6, 6, 4, 0xFFC89A5A.toInt(), 0xFFB07A4A.toInt(), 0xFF8A5A34.toInt()) // parcel logo up top
                face(22, 27, 10, 16, glass)
                face(8, 14, 11, 15, 0xFFB07A4A.toInt())
                wheel(6, 3); wheel(22, 3)
            }
        }
        px.light(bands = false)
        px.outline(OUT)
        px.bitmap()
    }
}
