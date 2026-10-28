package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Furn

/**
 * Furniture for the insides of your house and store, seen from the front and a little above. A floor square is
 * [CW] art pixels wide and [CD] deep; a floor piece's sprite is its footprint plus [up] pixels rising above it, drawn
 * with its bottom edge on the footprint's front. Wall pieces are [CW] per slot by [WH].
 */
object InteriorArt {
    private val OUT = Sprites.OUT
    const val CW = 16
    const val CD = 10
    const val WH = 16

    fun up(f: Furn) = when (f) {
        Furn.RUG -> 0
        Furn.COFFEE_TABLE -> 4
        Furn.CRATE_TABLE, Furn.BENCH, Furn.DINING, Furn.POOL, Furn.BED, Furn.HOT_TUB -> 6
        Furn.BEANBAG, Furn.RECORDS -> 8
        Furn.ARMCHAIR, Furn.COUCH, Furn.COUNTER, Furn.REGISTER, Furn.CASE -> 10
        Furn.PLANT, Furn.TV, Furn.FISH_TANK, Furn.JARS -> 14
        Furn.LAMP, Furn.FRIDGE, Furn.SHELVES, Furn.ATM -> 16
        Furn.BOOKSHELF, Furn.JUKEBOX -> 18
        Furn.ARCADE -> 20
        Furn.STATUE -> 22
        else -> 0
    }

    fun width(f: Furn) = f.w * CW
    fun height(f: Furn) = if (f.wall) WH else f.h * CD + up(f)

    private val cache = HashMap<Furn, Bitmap>()
    fun sprite(f: Furn): Bitmap = cache.getOrPut(f) { draw(f).bitmap() }

    private const val WOOD = 0xFF9A6A3E.toInt()
    private const val WOOD_D = 0xFF6A4325.toInt()
    private const val WOOD_L = 0xFFB88A58.toInt()
    private const val CREAM = 0xFFF0E6CC.toInt()
    private const val GLASS = 0xFFA8D8E8.toInt()
    private const val LEAF = 0xFF4A9A3A.toInt()
    private const val LEAF_D = 0xFF2E6A28.toInt()
    private const val GOLD = 0xFFE8C04A.toInt()
    private const val DARK = 0xFF2A2630.toInt()
    private const val METAL = 0xFFA8A8B0.toInt()

    /** A block seen from the front: a lit top face [depth] deep over a front face, [h] tall in all. */
    private fun Px.box(x: Int, y: Int, w: Int, h: Int, depth: Int, c: Int) {
        rect(x, y, w, depth, shade(c, 1.22f))
        rect(x, y + depth, w, h - depth, c)
    }

    /** A five-fingered pot leaf around [cx],[cy]. */
    private fun Px.potLeaf(cx: Int, cy: Int, r: Int, c: Int) {
        poly(c, cx, cy, cx - 1, cy - r, cx + 1, cy - r)
        poly(c, cx, cy, cx - r, cy - r / 2, cx - r + 1, cy - r / 2 - 2)
        poly(c, cx, cy, cx + r, cy - r / 2, cx + r - 1, cy - r / 2 - 2)
        poly(c, cx, cy, cx - r + 1, cy + 1, cx - r + 2, cy - 1)
        poly(c, cx, cy, cx + r - 1, cy + 1, cx + r - 2, cy - 1)
        line(cx, cy, cx, cy + r / 2, shade(c, 0.7f))
    }

    /** A 3×5 letter for signs: O, P, E or N. */
    private fun Px.letter(ch: Char, x: Int, y: Int, c: Int) {
        val rows = when (ch) {
            'O' -> arrayOf("###", "#.#", "#.#", "#.#", "###")
            'P' -> arrayOf("###", "#.#", "###", "#..", "#..")
            'E' -> arrayOf("###", "#..", "##.", "#..", "###")
            else -> arrayOf("#..#", "##.#", "#.##", "#..#", "#..#")
        }
        rows.forEachIndexed { j, r -> r.forEachIndexed { i, k -> if (k == '#') set(x + i, y + j, c) } }
    }

    private fun draw(f: Furn): Px {
        val w = width(f); val h = height(f)
        val px = Px(w, h)
        val b = h - 1 // bottom row
        when (f) {
            Furn.BEANBAG -> {
                oval(px, 8, b - 5, 6, 5, 0xFF8A4AB0.toInt()); oval(px, 7, b - 7, 3, 2, 0xFFA870D0.toInt())
            }
            Furn.CRATE_TABLE -> {
                px.box(2, b - 11, 12, 11, 3, WOOD)
                for (k in 0..2) px.line(2, b - 6 + k * 3, 13, b - 6 + k * 3, WOOD_D)
            }
            Furn.PLANT -> {
                px.box(4, b - 8, 8, 8, 2, 0xFFB8603A.toInt())
                px.potLeaf(8, b - 12, 6, LEAF); px.potLeaf(5, b - 16, 4, LEAF_D); px.potLeaf(11, b - 17, 4, LEAF)
            }
            Furn.RUG -> {
                px.rect(2, 2, 28, 16, 0xFFB04A4A.toInt()); px.rect(5, 5, 22, 10, 0xFFE0A050.toInt())
                px.rect(8, 7, 16, 6, 0xFF6A3A8A.toInt())
                for (x in 2..29 step 2) { px.set(x, 1, CREAM); px.set(x, 18, CREAM) }
            }
            Furn.ARMCHAIR -> {
                px.rect(2, b - 18, 12, 10, 0xFF3E7A6A.toInt())
                px.box(2, b - 9, 12, 7, 3, 0xFF4E8A7A.toInt())
                px.box(1, b - 12, 3, 10, 2, 0xFF2E6A5A.toInt()); px.box(12, b - 12, 3, 10, 2, 0xFF2E6A5A.toInt())
                px.rect(3, b - 2, 2, 2, WOOD_D); px.rect(11, b - 2, 2, 2, WOOD_D)
            }
            Furn.LAMP -> {
                px.box(5, b - 4, 6, 4, 1, METAL)
                px.poly(GLASS, 6, b - 5, 10, b - 5, 9, b - 22, 7, b - 22)
                oval(px, 8, b - 9, 1, 2, 0xFFE8504A.toInt()); oval(px, 8, b - 16, 1, 1, 0xFFF0804A.toInt())
                px.rect(6, b - 24, 4, 2, METAL)
            }
            Furn.COUCH -> {
                px.rect(3, b - 18, 42, 9, 0xFF8A4A3A.toInt())
                px.box(3, b - 10, 42, 8, 4, 0xFFA05A48.toInt())
                px.line(17, b - 10, 17, b - 6, 0xFF7A3A2A.toInt()); px.line(31, b - 10, 31, b - 6, 0xFF7A3A2A.toInt())
                px.box(0, b - 13, 4, 11, 2, 0xFF7A3A2A.toInt()); px.box(44, b - 13, 4, 11, 2, 0xFF7A3A2A.toInt())
                px.rect(2, b - 2, 2, 2, WOOD_D); px.rect(44, b - 2, 2, 2, WOOD_D)
            }
            Furn.COFFEE_TABLE -> {
                px.box(2, b - 9, 28, 6, 4, WOOD)
                px.rect(3, b - 3, 2, 3, WOOD_D); px.rect(27, b - 3, 2, 3, WOOD_D)
                px.disc(10, b - 8, 1, 0xFFE0E0E0.toInt()); px.rect(20, b - 8, 4, 2, 0xFF4A70B0.toInt())
            }
            Furn.BED -> {
                px.rect(1, 0, 30, 9, WOOD_D)
                px.box(1, 6, 30, h - 8, 12, CREAM)
                px.rect(3, 7, 11, 4, 0xFFFFFFFF.toInt()); px.rect(18, 7, 11, 4, 0xFFFFFFFF.toInt())
                px.rect(1, 13, 30, 9, 0xFF4A6AB0.toInt()); px.rect(1, 21, 30, b - 21, 0xFF3A5A9A.toInt())
            }
            Furn.RECORDS -> {
                px.box(1, b - 13, 14, 13, 4, WOOD)
                px.rect(2, b - 8, 12, 6, WOOD_D)
                oval(px, 7, b - 15, 4, 2, DARK); px.set(7, b - 15, 0xFFE05050.toInt()); px.line(12, b - 17, 10, b - 15, METAL)
            }
            Furn.BOOKSHELF -> {
                px.rect(1, 1, 30, b, WOOD_D)
                val cols = intArrayOf(0xFFC84A4A.toInt(), 0xFF4A7AC8.toInt(), 0xFFE8C04A.toInt(), 0xFF5AA04A.toInt(), 0xFF8A5AB0.toInt())
                for (shelf in 0..2) {
                    val y = 3 + shelf * 8
                    px.rect(3, y, 26, 6, 0xFF4A2E1A.toInt())
                    var x = 3
                    var k = shelf * 2
                    while (x < 27) {
                        val bw = 2 + k % 2; val low = if (k % 3 == 0) 1 else 0
                        px.rect(x, y + low, bw, 6 - low, cols[k % cols.size]); x += bw + 1; k++
                    }
                    px.rect(2, y + 6, 28, 1, WOOD)
                }
            }
            Furn.FRIDGE -> {
                px.rect(2, 1, 12, b, 0xFFE8E8EC.toInt())
                px.line(2, 9, 13, 9, 0xFFB0B0B8.toInt())
                px.rect(11, 4, 1, 3, METAL); px.rect(11, 12, 1, 6, METAL)
                px.potLeaf(6, 16, 3, LEAF)
            }
            Furn.TV -> {
                px.box(3, b - 8, 26, 8, 3, WOOD_D)
                px.rect(2, 0, 28, 15, DARK); px.rect(3, 1, 26, 12, 0xFF3A6AC8.toInt())
                px.rect(5, 3, 8, 3, 0xFF7AB0F0.toInt()); px.rect(15, 8, 10, 3, 0xFF5AA04A.toInt())
            }
            Furn.DINING -> {
                px.box(1, 4, 30, 14, 10, WOOD)
                px.rect(3, 18, 2, b - 17, WOOD_D); px.rect(27, 18, 2, b - 17, WOOD_D)
                oval(px, 9, 8, 3, 2, CREAM); oval(px, 22, 8, 3, 2, CREAM); px.disc(16, 6, 1, 0xFFE05A4A.toInt())
            }
            Furn.FISH_TANK -> {
                px.box(2, b - 7, 28, 7, 2, WOOD_D)
                px.rect(2, 1, 28, 15, GLASS); px.rect(2, 1, 28, 2, 0xFF7AB8D0.toInt())
                px.rect(3, 13, 26, 3, 0xFFD8C08A.toInt())
                px.line(7, 13, 7, 6, LEAF); px.line(24, 13, 23, 7, LEAF)
                px.rect(12, 7, 3, 2, 0xFFF08A3A.toInt()); px.rect(19, 10, 3, 2, 0xFFE0E040.toInt())
            }
            Furn.ARCADE -> {
                px.rect(2, 1, 12, b, 0xFF6A3AB0.toInt())
                px.rect(3, 3, 10, 3, 0xFFE8C04A.toInt())
                px.rect(3, 7, 10, 8, DARK); px.rect(4, 8, 8, 6, 0xFF3AC88A.toInt())
                px.box(1, 16, 14, 4, 2, 0xFF5A2A9A.toInt())
                px.set(4, 17, 0xFFE05050.toInt()); px.set(7, 17, 0xFF50A0E0.toInt()); px.set(10, 17, 0xFFE0E050.toInt())
            }
            Furn.POOL -> {
                px.box(1, 4, 46, 16, 12, 0xFF3A8A4A.toInt())
                px.rect(1, 4, 46, 2, WOOD); px.rect(1, 14, 46, 2, WOOD); px.rect(1, 4, 2, 12, WOOD); px.rect(45, 4, 2, 12, WOOD)
                px.rect(1, 16, 46, 4, WOOD_D)
                px.rect(3, 20, 3, b - 19, WOOD_D); px.rect(42, 20, 3, b - 19, WOOD_D)
                px.disc(14, 10, 1, CREAM); px.disc(30, 9, 1, 0xFFE05050.toInt()); px.disc(33, 11, 1, 0xFFE0C040.toInt()); px.disc(31, 12, 1, DARK)
            }
            Furn.JUKEBOX -> {
                px.disc(8, 7, 6, 0xFFC84A4A.toInt())
                px.rect(2, 7, 13, b - 7, 0xFFC84A4A.toInt())
                px.disc(8, 8, 4, 0xFFE8C04A.toInt()); px.disc(8, 8, 2, 0xFF5AC8E0.toInt())
                px.rect(4, 15, 9, 6, DARK); for (k in 0..2) px.line(5, 16 + k * 2, 11, 16 + k * 2, 0xFF8A8A90.toInt())
            }
            Furn.HOT_TUB -> {
                px.box(1, 2, 46, b - 1, 26, 0xFF8A5A3A.toInt())
                px.rect(4, 4, 40, 22, 0xFF4AB8D8.toInt())
                for (k in 0..6) px.disc(8 + k * 6, 8 + (k * 7) % 14, 1, 0xFFD8F4FC.toInt())
                for (x in 1..46 step 4) px.line(x, 28, x, b, WOOD_D)
            }
            Furn.STATUE -> {
                px.box(3, b - 7, 10, 7, 2, 0xFFD8D4CC.toInt())
                px.poly(GOLD, 5, b - 8, 11, b - 8, 10, b - 20, 6, b - 20)
                px.disc(8, b - 23, 3, GOLD)
                px.line(6, b - 18, 2, b - 25, GOLD); px.line(10, b - 18, 13, b - 25, GOLD)
                px.set(13, 2, 0xFFFFFFFF.toInt()); px.set(2, 5, 0xFFFFFFFF.toInt())
            }
            Furn.COUNTER -> {
                px.box(0, b - 17, 48, 17, 5, WOOD)
                px.rect(0, b - 10, 48, 3, 0xFF3A8A4A.toInt())
                px.potLeaf(24, b - 3, 3, 0xFF3A8A4A.toInt())
            }
            Furn.REGISTER -> {
                px.box(1, b - 11, 14, 11, 3, WOOD_D)
                px.poly(METAL, 4, b - 11, 12, b - 11, 11, b - 18, 5, b - 18)
                px.rect(5, b - 17, 6, 2, 0xFF3AC88A.toInt())
            }
            Furn.JARS -> {
                px.box(1, b - 9, 14, 9, 3, WOOD)
                for (k in 0..2) {
                    val x = 2 + k * 4
                    px.rect(x, b - 18 + k % 2 * 2, 4, 8 - k % 2 * 2, GLASS)
                    px.rect(x, b - 14, 4, 3, if (k == 1) 0xFF8AB04A.toInt() else LEAF)
                    px.rect(x, b - 19 + k % 2 * 2, 4, 1, DARK)
                }
            }
            Furn.SHELVES -> {
                px.rect(1, 1, 30, b, 0xFFE8E0D0.toInt())
                for (shelf in 0..2) {
                    val y = 3 + shelf * 8
                    px.rect(2, y + 6, 28, 1, METAL)
                    for (k in 0..4) {
                        val x = 3 + k * 6
                        if ((k + shelf) % 3 == 0) px.rect(x, y + 1, 4, 5, GLASS).also { px.rect(x, y + 3, 4, 3, LEAF) }
                        else px.rect(x, y + 2, 4, 4, if (k % 2 == 0) 0xFF4A7A3A.toInt() else 0xFFE8C04A.toInt())
                    }
                }
            }
            Furn.CASE -> {
                px.box(1, b - 9, 30, 9, 3, WOOD_D)
                px.rect(1, 1, 30, b - 9, GLASS); px.rect(1, 1, 30, 1, 0xFFE0F4FA.toInt())
                for (k in 0..3) { px.rect(4 + k * 7, b - 13, 4, 3, if (k % 2 == 0) LEAF else GOLD) }
            }
            Furn.BENCH -> {
                px.box(1, b - 10, 30, 6, 3, WOOD)
                px.rect(2, b - 4, 2, 4, WOOD_D); px.rect(28, b - 4, 2, 4, WOOD_D)
            }
            Furn.ATM -> {
                px.rect(2, 1, 12, b, 0xFF4A5A6A.toInt())
                px.rect(4, 3, 8, 6, 0xFF3AC88A.toInt())
                for (j in 0..1) for (i in 0..2) px.set(5 + i * 2, 11 + j * 2, METAL)
                px.rect(4, 16, 8, 1, DARK); px.rect(3, 20, 10, 2, GOLD)
            }
            // Wall pieces
            Furn.POSTER_LEAF -> {
                px.rect(2, 1, 12, 14, 0xFFE8E0A8.toInt()); px.potLeaf(8, 8, 5, LEAF)
            }
            Furn.POSTER_SUNSET -> {
                px.rect(2, 1, 12, 14, 0xFFF08A5A.toInt()); px.rect(2, 1, 12, 5, 0xFFE05A8A.toInt())
                px.disc(8, 8, 3, 0xFFF8D85A.toInt()); px.rect(2, 11, 12, 4, 0xFF5A3A6A.toInt())
                px.line(11, 14, 12, 5, DARK); px.poly(DARK, 12, 5, 9, 7, 15, 6)
            }
            Furn.POSTER_BAND -> {
                px.rect(2, 1, 12, 14, DARK)
                px.rect(4, 3, 8, 2, 0xFFE05050.toInt()); px.disc(8, 9, 2, 0xFFE8C04A.toInt())
                px.rect(4, 13, 8, 1, CREAM)
            }
            Furn.CLOCK -> {
                px.disc(8, 8, 6, WOOD_D); px.disc(8, 8, 5, CREAM)
                px.line(8, 8, 8, 4, DARK); px.line(8, 8, 11, 9, DARK)
            }
            Furn.WINDOW -> {
                px.rect(1, 1, 14, 14, CREAM); px.rect(2, 2, 12, 12, 0xFF8AC8F0.toInt())
                px.rect(2, 10, 12, 4, 0xFF6AB04A.toInt()); px.disc(11, 5, 1, 0xFFF8F0A0.toInt())
                px.line(8, 2, 8, 13, CREAM); px.line(2, 8, 13, 8, CREAM)
            }
            Furn.PAINTING -> {
                px.rect(2, 1, 28, 14, GOLD); px.rect(4, 3, 24, 10, 0xFF8AC0E0.toInt())
                px.poly(0xFF6A8A5A.toInt(), 4, 12, 12, 6, 20, 12); px.poly(0xFF4A7A4A.toInt(), 14, 12, 22, 5, 28, 12)
                px.disc(24, 5, 1, 0xFFF8F0A0.toInt())
            }
            Furn.TAPESTRY -> {
                px.rect(2, 1, 28, 13, 0xFF6A3A8A.toInt())
                px.disc(16, 7, 5, 0xFFE8A04A.toInt()); px.disc(16, 7, 3, 0xFFB04A6A.toInt()); px.disc(16, 7, 1, 0xFFE8C04A.toInt())
                for (x in 3..29 step 2) px.set(x, 14, 0xFFE8A04A.toInt())
            }
            Furn.NEON_LEAF -> {
                px.rect(1, 1, 14, 14, DARK); px.potLeaf(8, 8, 5, 0xFF5AF07A.toInt())
            }
            Furn.GOLD_RECORD -> {
                px.rect(1, 1, 14, 14, WOOD_D); px.rect(2, 2, 12, 12, 0xFF2A2A2A.toInt())
                px.disc(8, 8, 4, GOLD); px.disc(8, 8, 1, DARK)
            }
            Furn.NEON_OPEN -> {
                px.rect(2, 3, 28, 10, DARK)
                "OPEN".forEachIndexed { i, ch -> px.letter(ch, 6 + i * 5, 5, 0xFFFF5A8A.toInt()) }
            }
            Furn.MENU_BOARD -> {
                px.rect(1, 1, 30, 14, WOOD_D); px.rect(2, 2, 28, 12, 0xFF2A3A2E.toInt())
                for (k in 0..3) { px.line(4, 4 + k * 3, 4 + 10 + (k * 5) % 8, 4 + k * 3, CREAM); px.line(24, 4 + k * 3, 27, 4 + k * 3, GOLD) }
            }
        }
        px.light()
        px.outline(OUT)
        return px
    }
}
