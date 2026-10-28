package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.WorkHouse

/** Work buildings and storefronts that cover 2×2 squares, drawn in the shack's frame. */
object BigHouses {
    const val W = Buildings.SHACK_W
    const val H = Buildings.SHACK_H
    /** The footprint's front (bottom) corner sits on this pixel. */
    const val AX = Buildings.SHACK_AX
    const val AY = Buildings.SHACK_AY
    private const val CX = AX
    private const val BY = AY - 2

    private val cache = HashMap<String, Bitmap>()

    /** [h]'s sprite; a storefront looks like its [lvl]. */
    fun sprite(h: WorkHouse, lvl: Int = 1): Bitmap = cache.getOrPut(if (h == WorkHouse.STORE) "S${lvl.coerceIn(1, 5)}" else h.name) {
        val px = Px(W, H)
        when (h) {
            WorkHouse.STORE -> store(px, lvl.coerceIn(1, 5))
            WorkHouse.TOWER -> {}
            WorkHouse.GROW -> grow(px)
            WorkHouse.BUNK -> bunk(px)
            WorkHouse.TRIM -> trim(px)
            WorkHouse.DRYING -> drying(px)
            WorkHouse.TOOLS -> tools(px)
        }
        px.light(bands = false)
        px.outline(Sprites.OUT)
        px.bitmap()
    }

    /** Where a staffed building's window glow goes, in sprite pixels. */
    fun glow(h: WorkHouse): Pair<Int, Int> = when (h) {
        WorkHouse.BUNK -> 22 to 64
        WorkHouse.GROW -> CX to 52
        else -> CX - 14 to 62
    }

    /** An iso box [hw] half-wide standing with its front corner on (CX, BY), and things painted on its faces. */
    private class Box(val px: Px, val hw: Int = 28) {
        val lx = CX - hw; val ly = BY - hw / 2; val rx = CX + hw
        /** Ground line of the left (front) face at column x, and of the right face. */
        fun yl(x: Int) = ly + (x - lx) / 2
        fun yr(x: Int) = BY - (x - CX) / 2

        fun walls(h: Int, l: (Int, Int) -> Int, r: (Int, Int) -> Int) {
            px.poly(intArrayOf(lx, ly, CX, BY, CX, BY - h, lx, ly - h)) { x, y -> l(x, yl(x) - y) }
            px.poly(intArrayOf(CX, BY, rx, ly, rx, ly - h, CX, BY - h)) { x, y -> r(x, yr(x) - y) }
        }
        fun walls(h: Int, c: Int, tex: (Int, Int, Int) -> Int = { c0, _, _ -> c0 }) =
            walls(h, { x, z -> tex(c, x, z) }, { x, z -> tex(shade(c, 0.78f), x, z) })
        fun flat(h: Int, c: Int) = px.poly(c, lx, ly - h, CX, BY - h, rx, ly - h, CX, BY - hw - h)

        fun onL(x0: Int, z0: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = x0 + i; px.set(x, yl(x) - z0 - j, c(i, j)) }
        }
        fun onR(x0: Int, z0: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = x0 + i; px.set(x, yr(x) - z0 - j, c(i, j)) }
        }
        fun onL(x0: Int, z0: Int, w: Int, hh: Int, c: Int) = onL(x0, z0, w, hh) { _, _ -> c }
        fun onR(x0: Int, z0: Int, w: Int, hh: Int, c: Int) = onR(x0, z0, w, hh) { _, _ -> c }

        /** A framed window on the left face, or the right with [right]. */
        fun window(x0: Int, z0: Int, w: Int, hh: Int, glass: Int, frame: Int, right: Boolean = false) {
            val f: (Int, Int) -> Int = { i, j ->
                when {
                    i == 0 || j == 0 || i == w - 1 || j == hh - 1 -> frame
                    i == w / 2 -> shade(frame, 0.85f)
                    i == 1 && j == hh - 2 -> shade(glass, 1.35f)
                    else -> glass
                }
            }
            if (right) onR(x0, z0, w, hh, f) else onL(x0, z0, w, hh, f)
        }
        fun door(x0: Int, w: Int, hh: Int, c: Int, right: Boolean = false) {
            val f: (Int, Int) -> Int = { i, j -> if (i == 0 || i == w - 1 || j == hh - 1) shade(c, 0.7f) else if (i == w - 2 && j == hh / 2) 0xFFF2C440.toInt() else c }
            if (right) onR(x0, 0, w, hh, f) else onL(x0, 0, w, hh, f)
        }
        /** Green and white stripes along the left face at height z, and the right with [both]. */
        fun awning(z: Int, hh: Int, a: Int, b: Int, both: Boolean = true) {
            onL(lx - 1, z, CX - lx + 1, hh) { i, j -> if (j == 0) shade(a, 0.7f) else if (((lx - 1 + i) / 3) % 2 == 0) a else b }
            if (both) onR(CX, z, rx - CX + 2, hh) { i, j -> if (j == 0) shade(a, 0.6f) else shade(if (((CX + i) / 3) % 2 == 0) a else b, 0.85f) }
        }

        /** A pyramid roof on walls [h] high. */
        fun pyramid(h: Int, rise: Int, c: Int) {
            val ay = BY - hw / 2 - h - rise
            px.poly(intArrayOf(lx - 2, ly - h + 1, CX, BY - h + 2, CX, ay)) { x, y -> if ((y - (x - lx) / 2) % 3 == 0) shade(c, 0.95f) else shade(c, 1.1f) }
            px.poly(intArrayOf(CX, BY - h + 2, rx + 2, ly - h + 1, CX, ay)) { x, y -> if ((y + (x - CX) / 2) % 3 == 0) shade(c, 0.7f) else shade(c, 0.78f) }
        }

        /** A gable roof whose ridge runs along the left face; the gable end shows on the right face in [wall]. */
        fun gable(h: Int, rise: Int, c: Int, wall: (Int, Int) -> Int) {
            val mx = CX + hw / 2; val my = BY - hw / 4 - h - rise
            val m2x = mx - hw; val m2y = my - hw / 2
            px.poly(shade(c, 0.62f), mx, my, rx + 1, ly - h, CX, BY - hw - h, m2x, m2y)
            px.poly(intArrayOf(CX, BY - h, rx, ly - h, mx, my)) { x, y -> wall(x, yr(x) - y) }
            px.poly(intArrayOf(lx - 2, ly - h + 2, CX, BY - h + 2, mx + 1, my, m2x - 1, m2y)) { x, y ->
                if ((y - (x - lx) / 2 + 300) % 3 == 0) shade(c, 0.88f) else shade(c, 1.08f)
            }
        }
    }

    private val JARS = intArrayOf(0xFF6AD84A.toInt(), 0xFFB070F0.toInt(), 0xFF5CD0E0.toInt(), 0xFFF2C440.toInt())
    private val GREEN = 0xFF3FB84A.toInt()
    private val WHITE = 0xFFF4F0E8.toInt()
    private val NEON = 0xFFE070FF.toInt()
    private val DARK = 0xFF2A2230.toInt()

    /** A row of jars on a dark shelf on the left face. */
    private fun Box.shelf(x0: Int, x1: Int, z: Int) {
        onL(x0, z, x1 - x0 + 1, 7, DARK)
        for (x in x0 + 1 until x1 step 2) onL(x, z + 2, 1, 3) { _, j -> if (j == 2) WHITE else JARS[(x / 2) % 4] }
    }

    private fun store(px: Px, lvl: Int) {
        when (lvl) {
            1 -> {
                // A roadside stand: a counter, posts and a striped awning, crates of jars out front.
                val b = Box(px, 24)
                val wood = 0xFFB07A4A.toInt()
                for (x in intArrayOf(b.lx + 1, CX, b.rx - 1)) px.rect(x, (if (x < CX) b.yl(x) else b.yr(x)) - 30, 2, 30, shade(wood, 0.55f))
                px.rect(CX - 1, BY - b.hw - 30, 2, 18, shade(wood, 0.5f))
                b.walls(12, wood) { c, x, z -> if (z % 4 == 0) shade(c, 0.8f) else if (x % 9 == 0) shade(c, 0.9f) else c }
                b.flat(12, shade(wood, 1.2f))
                for (i in 0..7) px.rect(b.lx + 6 + i * 4, b.yl(b.lx + 6 + i * 4) - 16 - (i % 2), 2, 3, JARS[i % 4])
                b.onL(b.lx + 4, 3, 20, 6) { i, j -> if (i == 0 || j == 0 || i == 19 || j == 5) shade(WHITE, 0.8f) else if (j == 3 && i in 3..16) GREEN else WHITE }
                b.awning(28, 4, GREEN, WHITE)
                px.poly(shade(GREEN, 1.1f), b.lx - 1, b.ly - 31, CX, BY - 31, b.rx + 1, b.ly - 31, CX, BY - b.hw - 31)
                // Crates.
                Box(px, 7).let { k -> k.walls(5, 0xFF9A6A3A.toInt()) { c, _, z -> if (z == 2) shade(c, 0.75f) else c } }
            }
            2 -> {
                // A kiosk: a painted hut with a serving window, an awning and a sign up top.
                val b = Box(px)
                val paint = 0xFF4E8A5A.toInt()
                b.walls(28, paint) { c, x, z -> if (z < 2) 0xFF8A8A90.toInt() else if (x % 4 == 0) shade(c, 0.9f) else c }
                b.flat(28, shade(paint, 1.25f))
                b.onL(b.lx + 4, 8, 20, 13, DARK)
                b.shelf(b.lx + 5, b.lx + 22, 9)
                b.onL(b.lx + 3, 7, 22, 2, 0xFFB07A4A.toInt())
                b.door(CX - 7, 6, 15, 0xFF6A4325.toInt())
                b.awning(22, 4, GREEN, WHITE)
                b.window(CX + 8, 8, 9, 9, 0xFF8FD4F0.toInt(), WHITE, right = true)
                // The sign.
                px.rect(CX - 16, BY - 28 - 26, 32, 10, DARK); px.rect(CX - 15, BY - 28 - 25, 30, 8, WHITE)
                for (x in CX - 12..CX + 10 step 4) px.rect(x, BY - 28 - 22, 3, 2, GREEN)
                px.rect(CX - 10, BY - 28 - 16, 1, 5, DARK); px.rect(CX + 9, BY - 28 - 16, 1, 5, DARK)
            }
            3 -> {
                // A brick shop with a shop window, a door and a green cross.
                val b = Box(px)
                val brick = 0xFFC8A070.toInt()
                b.walls(26, brick) { c, x, z -> if (z % 3 == 0 || (x + (z / 3) * 3) % 6 == 0) shade(c, 0.86f) else c }
                b.window(b.lx + 3, 5, 15, 14, 0xFF8FD4F0.toInt(), 0xFF5A3A22.toInt())
                b.shelf(b.lx + 4, b.lx + 16, 6)
                b.door(b.lx + 20, 6, 16, 0xFF5A3A22.toInt())
                b.awning(19, 3, GREEN, WHITE, both = false)
                b.onR(CX + 13, 9, 3, 11, GREEN); b.onR(CX + 9, 13, 11, 3, GREEN)
                b.pyramid(26, 10, 0xFF5C6A72.toInt())
            }
            4 -> {
                // A store: white walls, big glass, a neon sign on the roof.
                val b = Box(px)
                val wall = 0xFFE8E0D0.toInt()
                b.walls(32, wall) { c, x, z -> if (z < 2) 0xFF8A8A90.toInt() else if (z % 8 == 0) shade(c, 0.92f) else c }
                b.flat(32, shade(wall, 0.65f))
                b.onL(b.lx + 2, 3, 18, 15, 0xFF8FD4F0.toInt())
                b.shelf(b.lx + 3, b.lx + 18, 4); b.shelf(b.lx + 3, b.lx + 18, 11)
                b.door(b.lx + 21, 6, 16, 0xFF3A6A8A.toInt())
                b.awning(20, 3, GREEN, WHITE)
                b.window(CX + 4, 4, 9, 12, 0xFF8FD4F0.toInt(), WHITE, right = true)
                b.window(CX + 16, 4, 9, 12, 0xFF8FD4F0.toInt(), WHITE, right = true)
                px.rect(CX - 18, BY - 32 - 24, 36, 11, DARK); px.rect(CX - 17, BY - 32 - 23, 34, 9, NEON)
                for (x in CX - 14..CX + 13 step 3) px.rect(x, BY - 32 - 20, 2, 3, WHITE)
                px.rect(CX - 12, BY - 32 - 13, 1, 6, DARK); px.rect(CX + 11, BY - 32 - 13, 1, 6, DARK)
            }
            else -> {
                // The flagship: three floors of glass and a neon leaf on top.
                val b = Box(px)
                val glass = 0xFF7AD0E8.toInt(); val frame = 0xFF3A3A48.toInt()
                b.walls(46, { x, z -> if ((x - b.lx) % 6 == 0 || z % 15 == 0) frame else if ((x + z) % 9 < 2) shade(glass, 1.2f) else glass },
                    { x, z -> if ((x - CX) % 6 == 0 || z % 15 == 0) frame else shade(glass, 0.75f) })
                b.flat(46, shade(frame, 1.3f))
                for (z in intArrayOf(1, 16, 31)) { b.shelf(b.lx + 2, b.lx + 9, z + 2); b.shelf(b.lx + 13, b.lx + 22, z + 2) }
                b.door(CX - 6, 6, 13, DARK)
                val lx = CX; val ly = BY - 46 - 30
                px.rect(lx, ly, 1, 14, GREEN)
                for (i in 1..6) { px.rect(lx - i - 2, ly + 3 + i, 3, 1, NEON); px.rect(lx + i, ly + 3 + i, 3, 1, NEON) }
                for (i in 1..3) { px.rect(lx - i * 2 - 3, ly + 10 + i, 3, 1, NEON); px.rect(lx + i * 2 + 1, ly + 10 + i, 3, 1, NEON) }
                px.rect(lx - 1, ly + 1, 3, 2, NEON); px.rect(lx, ly + 14, 1, 5, frame)
            }
        }
    }

    private fun grow(px: Px) {
        // A greenhouse: rows of plants behind glass under pink grow lights.
        val b = Box(px)
        val glass = 0xFFB8ECDA.toInt(); val frame = 0xFFF4F4F0.toInt()
        val l: (Int, Int) -> Int = { x, z -> if ((x - b.lx) % 7 == 0 || z == 0 || z == 11) frame else glass }
        b.walls(20, l) { x, z -> if ((x - CX) % 7 == 0 || z == 0 || z == 11) frame else shade(glass, 0.82f) }
        for (i in 0..8) b.onL(b.lx + 2 + i * 3, 2, 2, 5 + i % 3) { _, j -> if (j > 3) 0xFF6AB84A.toInt() else 0xFF3E8A30.toInt() }
        for (i in 0..8) b.onR(CX + 2 + i * 3, 2, 2, 4 + (i + 1) % 3, 0xFF2F6E28.toInt())
        b.door(CX - 8, 6, 14, 0xFF9AD8C0.toInt())
        b.gable(20, 13, 0xFFD8F4E8.toInt()) { x, z -> if ((x - CX) % 7 == 0) frame else shade(glass, 0.82f) }
        b.onL(b.lx + 2, 20, CX - b.lx - 3, 1, 0xFFFF7AE8.toInt())
    }

    private fun bunk(px: Px) {
        // A log cabin: warm windows, a door, a chimney.
        val b = Box(px)
        val logs = 0xFFB07A4A.toInt()
        val tex: (Int, Int, Int) -> Int = { c, x, z -> if (z % 3 == 0) shade(c, 0.72f) else if ((x * 3 + z) % 13 == 0) shade(c, 0.88f) else c }
        b.walls(20, logs, tex)
        b.window(b.lx + 4, 7, 7, 7, 0xFFFFE08A.toInt(), 0xFF5A3A22.toInt())
        b.window(CX - 10, 7, 7, 7, 0xFFFFE08A.toInt(), 0xFF5A3A22.toInt())
        b.door(b.lx + 14, 6, 14, 0xFF5A3A22.toInt())
        b.window(CX + 10, 8, 7, 6, 0xFFFFE08A.toInt(), 0xFF5A3A22.toInt(), right = true)
        px.rect(CX + 4, BY - 20 - 34, 5, 16, 0xFF7A7A80.toInt()); px.rect(CX + 3, BY - 20 - 35, 7, 2, 0xFF5A5A60.toInt())
        b.gable(20, 15, 0xFF3E6E4A.toInt()) { x, z -> tex(shade(logs, 0.78f), x, z) }
        // A woodpile by the wall.
        for (k in 0..2) px.rect(b.lx + 4 + k * 3, b.yl(b.lx + 4) + 1 - k * 2, 9 - k * 3, 2, if (k % 2 == 0) 0xFF8A5A34.toInt() else 0xFFC8935A.toInt())
    }

    private fun trim(px: Px) {
        // A board shed with an open bay: buds hang to dry over the trimming table.
        val b = Box(px)
        val board = 0xFFD8B87A.toInt()
        b.walls(18, board) { c, x, _ -> if (x % 4 == 0) shade(c, 0.82f) else c }
        b.onL(b.lx + 4, 0, 18, 15, 0xFF3A2A1E.toInt())
        for (i in 0..5) b.onL(b.lx + 6 + i * 3, 9 - i % 2, 2, 5) { _, j -> if (j == 4) 0xFF8A6A3A.toInt() else 0xFF6AB84A.toInt() }
        b.onL(b.lx + 5, 3, 16, 2, 0xFFA87A4A.toInt())
        b.onR(CX + 6, 9, 12, 2, 0xFFC8CCD4.toInt()); b.onR(CX + 16, 7, 4, 5, 0xFFC8484A.toInt())
        b.window(CX + 6, 4, 7, 4, 0xFF8FD4F0.toInt(), WHITE, right = true)
        b.pyramid(18, 9, 0xFF9AA0A8.toInt())
    }

    private fun drying(px: Px) {
        // A red barn: white trim, crossed doors on the gable end, buds hanging in the loft.
        val b = Box(px)
        val red = 0xFFB8443A.toInt()
        val tex: (Int, Int, Int) -> Int = { c, x, _ -> if (x % 3 == 0) shade(c, 0.85f) else c }
        b.walls(26, red, tex)
        b.onL(b.lx, 0, CX - b.lx, 1, WHITE); b.onL(b.lx, 25, CX - b.lx, 1, WHITE)
        b.onR(CX + 6, 0, 16, 18) { i, j -> if (i == 0 || i == 15 || j == 17 || i == j * 15 / 17 || i == 15 - j * 15 / 17 || i == 7) WHITE else shade(red, 0.7f) }
        b.onL(b.lx + 9, 14, 10, 9, 0xFF3A2A1E.toInt())
        for (i in 0..3) b.onL(b.lx + 10 + i * 2, 17 - i % 2, 1, 4, 0xFF8A9A3A.toInt())
        b.window(b.lx + 3, 5, 5, 6, 0xFF8FD4F0.toInt(), WHITE); b.window(b.lx + 20, 5, 5, 6, 0xFF8FD4F0.toInt(), WHITE)
        b.gable(26, 16, 0xFF5C4A3E.toInt()) { x, z -> tex(shade(red, 0.78f), x, z) }
        px.rect(CX + 12, BY - 26 - 21, 4, 3, WHITE)
    }

    private fun tools(px: Px) {
        // A green tool shed: tools on the wall, a workbench, a toolbox.
        val b = Box(px)
        val green = 0xFF6A8A5A.toInt()
        val steel = 0xFF7A8088.toInt(); val handle = 0xFFA87A4A.toInt()
        b.walls(18, green) { c, x, _ -> if (x % 5 == 0) shade(c, 0.85f) else c }
        b.onL(b.lx + 3, 3, 1, 12, handle); b.onL(b.lx + 1, 14, 5, 2, steel)
        b.onL(b.lx + 8, 3, 1, 11, handle); b.onL(b.lx + 7, 3, 3, 2, steel)
        b.onL(b.lx + 12, 4, 1, 10, handle); for (k in 0..2) b.onL(b.lx + 11 + k, 13 + k % 2, 1, 2, steel)
        b.door(CX - 9, 7, 14, 0xFF4E6A42.toInt())
        b.onR(CX + 4, 5, 18, 2, handle); b.onR(CX + 8, 7, 6, 4, 0xFFE8B040.toInt()); b.onR(CX + 8, 10, 6, 1, 0xFFFFE08A.toInt())
        b.gable(18, 11, 0xFF8A3A2E.toInt()) { x, z -> if (x % 5 == 0) shade(green, 0.65f) else shade(green, 0.78f) }
    }
}
