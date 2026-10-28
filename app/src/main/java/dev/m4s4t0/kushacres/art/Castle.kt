package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap

/** Fort 20's stonework: a square of curtain wall or a wall tower, and the keep in the middle of the map. */
object Castle {
    /** Wall squares and towers are one tile across; their front (bottom) corner sits on (16, WALL_AY or TOWER_AY). */
    const val WALL_H = 48
    const val WALL_AY = WALL_H - 1
    const val TOWER_H = 86
    const val TOWER_AY = TOWER_H - 1
    /** How high the tower's top is above its front corner, for its torch. */
    const val TOWER_TOP = 46
    /** The keep covers 6×6 squares; its footprint's front corner sits on (KEEP_AX, KEEP_AY). */
    const val KEEP_W = 200
    const val KEEP_H = 232
    const val KEEP_AX = KEEP_W / 2
    const val KEEP_AY = KEEP_H - 3

    private val STONE = 0xFFA89E8A.toInt()
    private val SLATE = 0xFF3E4656.toInt()
    private val SLIT = 0xFF1A1618.toInt()
    private val LIT = 0xFFF2C25A.toInt()
    private val FLAG = 0xFF3AB04A.toInt()
    private val GOLD = 0xFFF2D04A.toInt()

    /** An iso box whose front corner is (cx, by), its left face [a] pixels across and its right face [b]. */
    private class Box(val px: Px, val cx: Int, val by: Int, val a: Int, val b: Int) {
        fun yl(x: Int) = by - (cx - x) / 2
        fun yr(x: Int) = by - (x - cx) / 2

        /** Dressed stone: courses five high, the joints staggered, a few blocks a shade off. */
        private fun stone(c: Int, x: Int, z: Int): Int {
            val row = z / 5
            return when {
                z % 5 == 0 || (x + row * 5 + 400) % 10 == 0 -> shade(c, 0.78f)
                ((x + row * 5 + 400) / 10 * 7 + row * 13) % 11 == 0 -> shade(c, 0.9f)
                ((x + row * 5 + 400) / 10 * 5 + row * 3) % 13 == 0 -> shade(c, 1.08f)
                else -> c
            }
        }

        fun walls(h: Int, c: Int = STONE) {
            px.poly(intArrayOf(cx - a, by - a / 2, cx, by, cx, by - h, cx - a, by - a / 2 - h)) { x, y -> stone(c, x, yl(x) - y) }
            px.poly(intArrayOf(cx, by, cx + b, by - b / 2, cx + b, by - b / 2 - h, cx, by - h)) { x, y -> stone(shade(c, 0.74f), x, yr(x) - y) }
            // Moss creeping up from the ground.
            for (x in cx - a until cx + b) if ((x * 7) % 5 < 2) {
                val y0 = if (x < cx) yl(x) else yr(x)
                for (j in 0 until 1 + (x * 13) % 3) px.set(x, y0 - j, if (x < cx) 0xFF5A7A3A.toInt() else 0xFF465E30.toInt())
            }
        }

        fun top(h: Int, c: Int = shade(STONE, 0.88f)) =
            px.poly(intArrayOf(cx, by - h, cx - a, by - a / 2 - h, cx - a + b, by - a / 2 - b / 2 - h, cx + b, by - b / 2 - h)) { x, y ->
                if ((y + (x - cx) / 2 + 400) % 6 == 0 || (y - (x - cx) / 2 + 400) % 6 == 0) shade(c, 0.85f) else c
            }

        fun onL(x0: Int, z0: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = x0 + i; px.set(x, yl(x) - z0 - j, c(i, j)) }
        }
        fun onR(x0: Int, z0: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = x0 + i; px.set(x, yr(x) - z0 - j, c(i, j)) }
        }

        /** A pointed cap on walls [h] high. */
        fun cone(h: Int, rise: Int) {
            val tx = cx - a / 2 + b / 2; val ty = by - a / 4 - b / 4 - h - rise
            val l = intArrayOf(cx - a - 2, by - a / 2 - h + 1); val f = intArrayOf(cx, by - h + 2); val r = intArrayOf(cx + b + 2, by - b / 2 - h + 1)
            px.poly(intArrayOf(l[0], l[1], f[0], f[1], tx, ty)) { x, y -> if ((y - (x - l[0]) / 2) % 3 == 0) SLATE else shade(SLATE, 1.2f) }
            px.poly(intArrayOf(f[0], f[1], r[0], r[1], tx, ty)) { x, y -> if ((y + (x - f[0]) / 2) % 3 == 0) shade(SLATE, 0.6f) else shade(SLATE, 0.75f) }
            pennant(tx, ty)
        }
    }

    /** A pole topped with a green pennant, its foot at (x, y). */
    private fun Box.pennant(x: Int, y: Int) {
        px.rect(x, y - 14, 1, 14, 0xFF4A4038.toInt())
        for (i in 0 until 9) px.rect(x + 1 + i, y - 14 + i / 3, 1, 5 - i / 2, if (i == 3 || i == 4) GOLD else FLAG)
    }

    /** Merlons on a box [h] high: little stone blocks at tile-fractions (u, v) of its top, back ones first. */
    private fun Box.merlons(h: Int, at: List<Pair<Float, Float>>, k: Int, mh: Int) {
        for ((u, v) in at.sortedBy { it.first + it.second }) {
            val x = cx - a + ((u + 1 - v) * a).toInt()
            val y = by - h - a + ((u + v) * a / 2).toInt()
            Box(px, x, y + k / 2, k, k).apply { walls(mh); top(mh) }
        }
    }

    private val cache = HashMap<Int, Bitmap>()

    /** One square of curtain wall, crenellated both sides so it reads the same whichever way it runs. */
    val wall: Bitmap get() = cache.getOrPut(0) {
        val px = Px(32, WALL_H)
        val b = Box(px, 16, WALL_AY, 16, 16)
        b.walls(22); b.top(22)
        b.merlons(22, listOf(0.25f to 0.25f, 0.75f to 0.25f, 0.25f to 0.75f, 0.75f to 0.75f), 4, 6)
        px.light(bands = false)
        px.outline(Sprites.OUT)
        px.bitmap()
    }

    /** A wall tower: twice the wall's height, arrow slits on each face, a crenellated top and a pennant. */
    val tower: Bitmap get() = cache.getOrPut(1) {
        val px = Px(32, TOWER_H)
        val b = Box(px, 16, TOWER_AY, 16, 16)
        b.walls(40); b.top(40)
        // A band of corbels under the parapet.
        b.onL(0, 34, 16, 2) { i, _ -> if (i % 3 == 0) shade(STONE, 0.6f) else shade(STONE, 0.85f) }
        b.onR(16, 34, 16, 2) { i, _ -> if (i % 3 == 0) shade(STONE, 0.45f) else shade(STONE, 0.65f) }
        b.onL(6, 14, 2, 8) { _, _ -> SLIT }; b.onR(23, 14, 2, 8) { _, _ -> SLIT }
        val ring = listOf(0.15f, 0.5f, 0.85f).flatMap { u -> listOf(0.15f, 0.5f, 0.85f).map { u to it } }.filter { it != 0.5f to 0.5f }
        b.merlons(40, ring.filter { it.first + it.second < 1f }, 3, 6)
        b.pennant(16, TOWER_AY - 40 - 8)
        b.merlons(40, ring.filter { it.first + it.second >= 1f }, 3, 6)
        px.light(bands = false)
        px.outline(Sprites.OUT)
        px.bitmap()
    }

    /** Lit windows on the keep, as (x, y, colour) in sprite pixels; filled in when the sprite is first drawn. */
    val windows: List<IntArray> get() { keep; return lit }
    private val lit = ArrayList<IntArray>()

    val keep: Bitmap by lazy {
        val px = Px(KEEP_W, KEEP_H)
        val cx = KEEP_AX; val fy = KEEP_AY - 16
        fun turret(x: Int, y: Int) = Box(px, x, y + 9, 18, 18).apply {
            walls(104); onL(this.cx - 18, 98, 18, 2) { _, _ -> shade(STONE, 0.6f) }; onR(this.cx, 98, 18, 2) { _, _ -> shade(STONE, 0.45f) }
            onL(this.cx - 11, 40, 2, 9) { _, _ -> SLIT }; onR(this.cx + 9, 40, 2, 9) { _, _ -> SLIT }
            onL(this.cx - 12, 72, 4, 8) { i, j -> if (j == 7 && (i == 0 || i == 3)) STONE else LIT }
            lit += intArrayOf(this.cx - 10, yl(this.cx - 10) - 76, LIT)
            cone(104, 30)
        }
        // The back turret, then the keep itself, then the turrets either side and in front.
        turret(cx, fy - 64)
        val k = Box(px, cx, fy, 64, 64)
        k.walls(78); k.top(78)
        // Two rows of windows, a few lit, and arrow slits between.
        for ((x, z, on) in listOf(Triple(cx - 50, 50, true), Triple(cx - 34, 50, false), Triple(cx - 50, 26, false))) {
            k.onL(x, z, 6, 10) { i, j -> if (j == 9 && (i == 0 || i == 5)) STONE else if (i == 0 || i == 5 || j == 0) shade(STONE, 0.6f) else if (on) LIT else SLIT }
            if (on) lit += intArrayOf(x + 3, k.yl(x + 3) - z - 5, LIT)
        }
        for ((x, z, on) in listOf(Triple(cx + 14, 50, true), Triple(cx + 30, 50, false), Triple(cx + 46, 50, true), Triple(cx + 30, 26, true))) {
            k.onR(x, z, 6, 10) { i, j -> if (j == 9 && (i == 0 || i == 5)) shade(STONE, 0.74f) else if (i == 0 || i == 5 || j == 0) shade(STONE, 0.45f) else if (on) LIT else SLIT }
            if (on) lit += intArrayOf(x + 3, k.yr(x + 3) - z - 5, LIT)
        }
        for (x in listOf(cx + 18, cx + 50)) k.onR(x, 22, 2, 9) { _, _ -> SLIT }
        // The great gate: an arch with the portcullis half up, and a green banner over it.
        k.onL(cx - 40, 0, 18, 26) { i, j ->
            val arch = j >= 21 && (minOf(i, 17 - i) < (j - 20))
            when {
                arch -> STONE
                i < 2 || i > 15 || j >= 24 - minOf(i, 17 - i) / 3 && j > 19 -> shade(STONE, 0.62f)
                j < 8 -> 0xFF0E0C0E.toInt()
                i % 3 == 1 || j % 3 == 0 -> 0xFF6A6460.toInt()
                else -> 0xFF161214.toInt()
            }
        }
        k.onL(cx - 36, 30, 10, 18) { i, j ->
            val leaf = j in 5..13 && (i == 4 || i == 5 || (j - 5) in (kotlin.math.abs(i - 4) * 2 - 1)..(kotlin.math.abs(i - 4) * 2 + 2) && i in 1..8)
            when { j == 17 -> GOLD; j == 0 && i % 2 == 0 -> STONE; leaf -> GOLD; else -> FLAG }
        }
        // Battlements all the way round the roof.
        val step = 0.125f
        val edge = (0..8).flatMap { i -> listOf(i * step to 0.03f, 0.03f to i * step, i * step to 0.97f, 0.97f to i * step) }.distinct()
        k.merlons(78, edge, 4, 7)
        turret(cx - 64, fy - 32)
        turret(cx + 64, fy - 32)
        turret(cx, fy)
        // A great flag flying from the roof.
        px.rect(cx + 2, fy - 64 - 78 - 34, 1, 34, 0xFF4A4038.toInt())
        for (i in 0 until 18) px.rect(cx + 3 + i, fy - 64 - 78 - 34 + i / 6, 1, 11 - i / 4, if (i in 6..9) GOLD else FLAG)
        px.light(bands = false)
        px.outline(Sprites.OUT)
        px.bitmap()
    }
}
