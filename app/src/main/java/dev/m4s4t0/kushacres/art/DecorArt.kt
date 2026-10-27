package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Decor
import kotlin.math.PI
import kotlin.random.Random

internal fun oval(px: Px, cx: Int, cy: Int, rx: Int, ry: Int, c: Int) {
    for (j in -ry..ry) for (i in -rx..rx) {
        val d = i * i / (rx * rx + 0.5f) + j * j / (ry * ry + 0.5f)
        if (d <= 1f) px.set(cx + i, cy + j, c)
    }
}

/** Garden decor (32×32, square centre on [AX],[AY]) and path tiles. */
object DecorArt {
    private val OUT = Sprites.OUT
    const val W = 32
    const val H = 32
    const val AX = 16
    const val AY = 26

    private val cache = HashMap<Decor, Bitmap>()

    fun sprite(d: Decor): Bitmap = cache.getOrPut(d) {
        if (d.path) return@getOrPut path(d)
        val px = Px(W, H)
        val r = Random(d.ordinal * 13 + 5)
        val wood = 0xFF9A6A3E.toInt(); val woodD = 0xFF6A4325.toInt()
        val stone = 0xFFA8A8B0.toInt()
        when (d) {
            Decor.FLOWERS -> {
                px.poly(0xFF6A4226.toInt(), 6, 26, 16, 21, 26, 26, 16, 31)
                val cols = intArrayOf(0xFFE05A6A.toInt(), 0xFFF2D24A.toInt(), 0xFFF0F0F0.toInt(), 0xFFB070E0.toInt(), 0xFFF08A3A.toInt())
                repeat(9) {
                    val x = 9 + r.nextInt(15); val y = 23 + r.nextInt(6)
                    px.line(x, y, x, y - 3, 0xFF3F8A3A.toInt()); px.disc(x, y - 4, 1, cols[r.nextInt(cols.size)])
                }
            }
            Decor.BENCH -> {
                px.poly(wood, 11, 17, 27, 25, 27, 19, 11, 11)
                px.line(11, 14, 27, 22, woodD)
                px.rect(8, 21, 1, 5, woodD); px.rect(22, 28, 1, 3, woodD); px.rect(26, 25, 1, 4, woodD)
                px.poly(shade(wood, 1.15f), 7, 19, 23, 27, 27, 25, 11, 17)
                px.poly(woodD, 7, 19, 23, 27, 23, 29, 7, 21)
            }
            Decor.GNOME -> {
                px.rect(13, 24, 6, 2, 0xFF5A3A22.toInt())
                px.rect(12, 17, 8, 7, 0xFF3E6ED0.toInt()); px.rect(12, 17, 2, 7, 0xFF5A8AE8.toInt())
                px.poly(0xFFF4F4F0.toInt(), 12, 14, 20, 14, 16, 22)
                px.rect(14, 11, 4, 3, 0xFFF0C090.toInt()); px.set(16, 13, 0xFFE07070.toInt())
                px.set(14, 12, OUT); px.set(17, 12, OUT)
                px.poly(0xFFD8484A.toInt(), 11, 11, 21, 11, 16, 1)
            }
            Decor.SCARECROW -> {
                px.rect(15, 10, 2, 17, woodD); px.rect(7, 13, 18, 2, woodD)
                px.poly(intArrayOf(11, 12, 20, 12, 21, 21, 10, 21)) { x, y -> if ((x / 2 + y / 2) % 2 == 0) 0xFFC8484A.toInt() else 0xFF8A2A2E.toInt() }
                for (k in 0..3) px.set(11 + k * 3, 22, 0xFFE8C66A.toInt())
                px.rect(5, 12, 2, 3, 0xFFE8C66A.toInt()); px.rect(25, 12, 2, 3, 0xFFE8C66A.toInt())
                px.disc(16, 8, 3, 0xFFD8B888.toInt()); px.set(15, 8, OUT); px.set(17, 8, OUT); px.line(15, 10, 17, 10, woodD)
                px.rect(10, 5, 12, 1, 0xFFC89A42.toInt()); px.rect(13, 2, 6, 3, 0xFFE8C66A.toInt())
            }
            Decor.BARREL -> {
                oval(px, 16, 26, 6, 2, woodD)
                px.poly(intArrayOf(10, 13, 22, 13, 22, 26, 10, 26)) { x, _ -> if (x % 3 == 0) shade(wood, 0.78f) else if (x < 13) shade(wood, 1.15f) else wood }
                px.rect(10, 15, 13, 1, 0xFF6C7078.toInt()); px.rect(10, 23, 13, 1, 0xFF6C7078.toInt())
                oval(px, 16, 13, 6, 2, woodD); oval(px, 16, 13, 5, 1, 0xFF4AA8E0.toInt()); px.set(14, 13, 0xFF9AD4FF.toInt())
            }
            Decor.FLAMINGO -> {
                val pink = 0xFFF27AAE.toInt()
                px.line(15, 18, 15, 26, 0xFFD85A8A.toInt()); px.line(17, 18, 18, 26, 0xFFD85A8A.toInt())
                oval(px, 17, 15, 5, 3, pink); px.line(15, 14, 19, 16, shade(pink, 0.8f))
                px.line(12, 14, 11, 8, pink); px.line(13, 14, 12, 8, pink)
                px.disc(12, 6, 2, pink); px.set(9, 7, OUT); px.set(10, 7, 0xFFF4F4F0.toInt()); px.set(12, 5, OUT)
            }
            Decor.FLAG -> {
                px.line(9, 2, 9, 26, 0xFF9AA0A8.toInt()); px.rect(8, 25, 3, 2, 0xFF6C7078.toInt())
                val st = intArrayOf(0xFFD8484A.toInt(), 0xFFF2D24A.toInt(), 0xFF3F9A3A.toInt())
                for (x in 10..23) for (y in 3..11) px.set(x, y + (if ((x / 3) % 2 == 0) 0 else 1), st[(y - 3) / 3])
            }
            Decor.COMPOST -> {
                px.disc(16, 17, 6, 0xFF5A3A22.toInt())
                repeat(10) { px.set(11 + r.nextInt(11), 12 + r.nextInt(6), if (it % 2 == 0) 0xFF6AB04A.toInt() else 0xFF8A5A33.toInt()) }
                val slat: Int = wood
                box(px, 16, 30, 10, 10, 7, 0xFF4A2E1A.toInt(), slat, shade(slat, 0.75f))
                for (y in 0..6 step 3) { px.line(6, 25 - y, 16, 30 - y, woodD); px.line(16, 30 - y, 26, 25 - y, shade(woodD, 0.8f)) }
            }
            Decor.BEEHIVE -> {
                val y = 0xFFE8B83A.toInt()
                px.rect(10, 22, 1, 5, woodD); px.rect(21, 22, 1, 5, woodD); px.rect(16, 25, 1, 5, woodD)
                box(px, 16, 25, 7, 7, 5, shade(y, 1.2f), y, shade(y, 0.75f))
                box(px, 16, 19, 7, 7, 5, shade(y, 1.2f), shade(y, 0.95f), shade(y, 0.7f))
                box(px, 16, 13, 8, 8, 2, 0xFFC8CCD4.toInt(), 0xFF9AA0A8.toInt(), 0xFF6C7078.toInt())
                px.rect(11, 21, 3, 1, OUT)
                for ((bx, by) in listOf(6 to 9, 25 to 12, 22 to 5)) { px.set(bx, by, 0xFFF2D24A.toInt()); px.set(bx + 1, by, OUT) }
            }
            Decor.TIKI -> {
                px.rect(15, 9, 3, 18, 0xFFC8A060.toInt()); px.rect(15, 9, 1, 18, 0xFFE0C080.toInt())
                for (yy in intArrayOf(13, 18, 23)) px.rect(15, yy, 3, 1, 0xFF8A6A3A.toInt())
                px.rect(13, 6, 7, 3, 0xFF4A2E1A.toInt())
                px.poly(0xFFF08A3A.toInt(), 13, 6, 20, 6, 18, 2, 17, 0, 15, 3)
                px.poly(0xFFFFE27A.toInt(), 15, 6, 18, 6, 17, 3, 16, 2)
            }
            Decor.DOG -> {
                val fur = 0xFFB07A4A.toInt()
                px.rect(14, 22, 2, 5, shade(fur, 0.8f)); px.rect(19, 22, 2, 5, shade(fur, 0.8f))
                oval(px, 18, 20, 5, 4, fur); px.line(23, 19, 26, 15, fur)
                px.disc(12, 14, 3, fur); px.rect(8, 15, 3, 2, shade(fur, 1.15f)); px.set(8, 15, OUT)
                px.rect(13, 11, 2, 4, shade(fur, 0.6f)); px.set(11, 13, OUT)
                px.rect(11, 17, 5, 1, 0xFFD8484A.toInt()); px.set(13, 18, 0xFFF2C440.toInt())
            }
            Decor.FROG -> {
                box(px, 16, 29, 8, 8, 3, shade(stone, 1.15f), stone, shade(stone, 0.75f))
                val jade = 0xFF5AA86A.toInt()
                oval(px, 16, 19, 6, 5, jade); oval(px, 15, 18, 4, 3, shade(jade, 1.15f))
                px.disc(13, 14, 2, jade); px.disc(19, 14, 2, jade); px.set(13, 14, OUT); px.set(19, 14, OUT)
                px.line(13, 20, 19, 20, shade(jade, 0.6f))
                px.rect(11, 23, 3, 2, shade(jade, 0.8f)); px.rect(19, 23, 3, 2, shade(jade, 0.8f))
                px.disc(16, 9, 1, 0xFFF2C440.toInt())
            }
            Decor.VAN -> {
                val teal = 0xFF3AB0A8.toInt(); val white = 0xFFF0ECE0.toInt()
                val cx = 17; val by = 30
                box(px, cx, by, 14, 8, 11, white, teal, shade(teal, 0.78f))
                // White upper half with windows on the long side, windshield on the front.
                px.poly(intArrayOf(cx - 14, by - 7 - 11, cx, by - 11, cx, by - 6, cx - 14, by - 7 - 5)) { _, _ -> shade(white, 0.95f) }
                px.poly(intArrayOf(cx, by - 11, cx + 8, by - 4 - 11, cx + 8, by - 4 - 6, cx, by - 6)) { _, _ -> shade(white, 0.8f) }
                for (k in 0..2) for (i in 0 until 3) for (j in 0 until 3) px.set(cx - 12 + k * 4 + i, by - 7 + (k * 4 + i + 2) / 2 - 7 - j, 0xFF6AA8D8.toInt())
                for (i in 0 until 6) for (j in 0 until 3) px.set(cx + 1 + i, by - 1 - (i + 1) / 2 - 7 - j, 0xFF8AC8F0.toInt())
                px.disc(cx - 7, by - 4, 1, 0xFFF27AAE.toInt()); px.set(cx - 7, by - 4, 0xFFF2D24A.toInt())
                px.disc(cx - 11, by - 4, 2, 0xFF2C3038.toInt()); px.disc(cx - 2, by, 2, 0xFF2C3038.toInt()); px.disc(cx + 6, by - 3, 2, 0xFF2C3038.toInt())
            }
            Decor.NEON -> {
                px.rect(15, 17, 2, 10, 0xFF3C4048.toInt())
                px.rect(7, 3, 18, 15, 0xFF1A1A22.toInt())
                val pink = 0xFFFF5AC8.toInt()
                for (x in 7..24) { px.set(x, 3, pink); px.set(x, 17, pink) }
                for (y in 3..17) { px.set(7, y, pink); px.set(24, y, pink) }
                Sprites.fanLeaf(px, 16, 14, -PI / 2, 6, 0xFF6AFF7A.toInt(), 0xFFCAFFD0.toInt())
            }
            Decor.LAVA -> {
                px.poly(0xFF5A5F6A.toInt(), 11, 27, 21, 27, 19, 21, 13, 21)
                px.poly(0xFF7A3AB8.toInt(), 13, 21, 19, 21, 21, 13, 18, 6, 14, 6, 11, 13)
                px.disc(16, 17, 2, 0xFFFF8A3A.toInt()); px.disc(17, 11, 1, 0xFFFFB04A.toInt()); px.disc(14, 19, 1, 0xFFFF6A3A.toInt())
                px.line(13, 12, 14, 8, 0xFFB08AE8.toInt())
                px.poly(0xFF5A5F6A.toInt(), 14, 6, 18, 6, 17, 2, 15, 2)
            }
            Decor.ALIEN -> {
                box(px, 16, 29, 7, 7, 3, shade(stone, 1.15f), stone, shade(stone, 0.75f))
                val g = 0xFF8ACB5A.toInt()
                px.rect(14, 15, 4, 9, g); px.line(13, 16, 10, 21, g); px.line(18, 16, 21, 21, g)
                oval(px, 16, 10, 5, 6, g)
                px.poly(OUT, 12, 9, 15, 10, 15, 12, 13, 11); px.poly(OUT, 20, 9, 17, 10, 17, 12, 19, 11)
                px.set(13, 10, 0xFFE8FCFF.toInt()); px.set(19, 10, 0xFFE8FCFF.toInt())
            }
            Decor.WINDMILL -> {
                px.poly(intArrayOf(12, 28, 20, 28, 18, 10, 14, 10)) { x, y -> if (y % 4 == 0) shade(wood, 0.8f) else if (x < 15) shade(wood, 1.1f) else wood }
                px.rect(15, 23, 2, 5, woodD)
                px.poly(0xFFB8483A.toInt(), 13, 10, 19, 10, 16, 6)
                val hx = 16; val hy = 9
                for ((dx, dy) in listOf(-9 to -6, 9 to 6, 6 to -9, -6 to 9)) {
                    px.line(hx, hy, hx + dx, hy + dy, woodD)
                    px.line(hx + dx / 3 + 1, hy + dy / 3, hx + dx + 1, hy + dy, 0xFFF0E8D8.toInt())
                }
                px.disc(hx, hy, 1, 0xFF4A3220.toInt())
            }
            Decor.KOI -> {
                oval(px, 16, 25, 13, 6, stone); oval(px, 16, 25, 11, 5, 0xFF3A7AB8.toInt()); oval(px, 15, 24, 8, 3, 0xFF4F94D0.toInt())
                px.rect(10, 25, 3, 1, 0xFFF08A3A.toInt()); px.set(9, 25, 0xFFF0F0F0.toInt())
                px.rect(19, 23, 3, 1, 0xFFF0F0F0.toInt()); px.set(22, 23, 0xFFE05A3A.toInt())
                px.rect(15, 14, 3, 11, shade(stone, 0.9f)); px.rect(15, 14, 1, 11, shade(stone, 1.1f))
                oval(px, 16, 14, 4, 2, stone)
                for (k in 0..2) { px.set(13 - k, 16 + k * 2, 0xFF9CD0F8.toInt()); px.set(19 + k, 16 + k * 2, 0xFF9CD0F8.toInt()) }
            }
            Decor.STATUE -> {
                px.poly(shade(stone, 0.8f), 9, 27, 16, 30, 23, 27, 16, 24); box(px, 16, 29, 6, 6, 6, shade(stone, 1.1f), stone, shade(stone, 0.78f))
                val g = 0xFFE8C040.toInt(); val gd = 0xFFB08A20.toInt()
                px.rect(15, 16, 2, 7, gd)
                px.poly(g, 16, 2, 18, 9, 16, 17, 14, 9)
                px.poly(g, 16, 16, 8, 8, 10, 7, 16, 14); px.poly(gd, 16, 16, 24, 8, 22, 7, 16, 14)
                px.poly(g, 16, 17, 9, 15, 10, 14, 16, 16); px.poly(gd, 16, 17, 23, 15, 22, 14, 16, 16)
                px.set(15, 6, 0xFFFFF4B0.toInt()); px.set(15, 7, 0xFFFFF4B0.toInt())
            }
            Decor.GREENHOUSE -> {
                val gl = 0xAA9CD8F0.toInt(); val gr = 0xAA6AA8D0.toInt(); val fr = 0xFFF0F0F0.toInt()
                box(px, 16, 30, 11, 11, 9, gl, gl, gr)
                for (k in 0..3) { px.rect(18 - k * 3, 26 - k, 1, 4, 0xFF4F9A3A.toInt()); px.disc(18 - k * 3, 25 - k, 1, 0xFF6BC04A.toInt()) }
                px.poly(0xBBC8ECFF.toInt(), 5, 16, 16, 21, 16, 13, 10, 10)
                px.poly(0xBB9CC8E8.toInt(), 16, 21, 27, 16, 21, 8, 16, 13)
                px.line(16, 13, 21, 8, fr); px.line(10, 10, 21, 8, fr); px.line(5, 16, 16, 21, fr); px.line(16, 21, 27, 16, fr)
                px.line(16, 21, 16, 30, fr); px.line(5, 16, 5, 25, fr); px.line(27, 16, 27, 25, fr)
            }
            Decor.TOTEM -> {
                val cols = intArrayOf(0xFFC8484A.toInt(), 0xFF3E8AD0.toInt(), 0xFF4FA03C.toInt(), 0xFFE8B83A.toInt())
                for (k in 0..3) {
                    val y = 24 - k * 6
                    px.rect(12, y - 5, 8, 6, cols[k]); px.rect(12, y - 5, 2, 6, shade(cols[k], 1.2f)); px.rect(18, y - 5, 2, 6, shade(cols[k], 0.75f))
                    px.set(14, y - 3, 0xFFF0F0F0.toInt()); px.set(17, y - 3, 0xFFF0F0F0.toInt()); px.line(14, y - 1, 17, y - 1, OUT)
                }
                px.poly(0xFFE8B83A.toInt(), 8, 7, 12, 4, 12, 8); px.poly(0xFFE8B83A.toInt(), 24, 7, 20, 4, 20, 8)
                px.poly(0xFF4FA03C.toInt(), 12, 1, 20, 1, 16, -2)
            }
            Decor.DISCO -> {
                px.rect(15, 12, 2, 16, 0xFF6A6A72.toInt()); px.rect(11, 27, 10, 2, 0xFF4A4A52.toInt())
                px.rect(10, 4, 12, 1, 0xFF6A6A72.toInt())
                for (j in -6..6) for (i in -6..6) if (i * i + j * j <= 36) {
                    val c = if (((i + 6) / 2 + (j + 6) / 2) % 2 == 0) 0xFFD8D8E0.toInt() else 0xFF8A8A98.toInt()
                    px.set(16 + i, 11 + j, if (i < -2 && j < -2) shade(c, 1.2f) else if (i + j > 5) shade(c, 0.7f) else c)
                }
                px.set(13, 8, 0xFFFFFFFF.toInt()); px.set(19, 12, 0xFFFF80E0.toInt()); px.set(14, 14, 0xFF80E0FF.toInt())
            }
            Decor.UFO -> {
                px.poly(0xFF4A3A2A.toInt(), 4, 27, 16, 31, 28, 27, 16, 23)
                oval(px, 16, 21, 12, 4, 0xFF9AA0AA.toInt()); oval(px, 16, 20, 12, 3, 0xFFC8CED8.toInt())
                oval(px, 16, 16, 6, 4, 0xCC7AE8D0.toInt()); px.set(14, 14, 0xFFE0FFF8.toInt())
                for (k in 0..4) px.set(7 + k * 4, 22, if (k % 2 == 0) 0xFF60FF90.toInt() else 0xFFFFE060.toInt())
                px.line(25, 24, 28, 28, 0xFF6A6A72.toInt())
            }
            Decor.CRYSTAL -> {
                px.poly(0xFF4A3A5A.toInt(), 8, 27, 16, 31, 24, 27, 16, 23)
                fun shard(x: Int, y: Int, w: Int, h: Int, c: Int) {
                    px.poly(c, x - w, y, x, y + 2, x, y - h, x - w, y - h + 3)
                    px.poly(shade(c, 0.72f), x, y + 2, x + w, y, x + w, y - h + 3, x, y - h)
                    px.poly(shade(c, 1.3f), x - w, y - h + 3, x, y - h, x + w, y - h + 3, x, y - h - 4)
                }
                shard(11, 27, 3, 8, 0xFF9A5AE0.toInt()); shard(22, 27, 3, 7, 0xFFB070F0.toInt())
                shard(16, 28, 4, 17, 0xFFC890FF.toInt())
                px.set(14, 16, 0xFFFFFFFF.toInt()); px.set(14, 17, 0xFFF0E0FF.toInt())
            }
            else -> holiday(px, d)
        }
        px.light()
        px.outline(OUT)
        px.bitmap()
    }

    /** The holiday shop pieces. */
    private fun holiday(px: Px, d: Decor) {
        val white = 0xFFF4F4F0.toInt(); val gold = 0xFFF2C440.toInt(); val dirt = 0xFF5A4A36.toInt()
        val grassC = 0xFF4F9A3A.toInt(); val leaf = 0xFF3E8A3A.toInt()
        fun base(c: Int = dirt) = px.poly(c, 6, 27, 16, 22, 26, 27, 16, 31)
        fun ball(cx: Int, cy: Int, r: Int, c: Int) {
            px.disc(cx, cy, r, c); px.disc(cx + r / 3, cy + r / 3, r - 1, shade(c, 0.8f)); px.disc(cx - r / 4, cy - r / 4, r - 2, c)
            px.set(cx - r / 2, cy - r / 2, mix(c, white, 0.6f))
        }
        when (d) {
            Decor.PUMPKIN -> {
                base()
                val o = 0xFFF0801E.toInt()
                for (k in -1..1) oval(px, 16 + k * 4, 22, 5, 6, if (k == 0) o else shade(o, if (k < 0) 1.1f else 0.8f))
                px.rect(15, 14, 2, 3, 0xFF4F7A2A.toInt())
                val glow = 0xFFFFE04A.toInt()
                px.poly(glow, 11, 21, 14, 21, 12, 18); px.poly(glow, 18, 21, 21, 21, 20, 18)
                px.poly(glow, 11, 24, 21, 24, 19, 27, 13, 27); px.set(14, 24, shade(o, 0.8f)); px.set(18, 26, shade(o, 0.8f))
            }
            Decor.TOMBSTONE -> {
                base(0xFF4A5A36.toInt())
                val s = 0xFF8A8A94.toInt()
                px.rect(10, 12, 12, 16, s); px.disc(16, 12, 6, s); px.rect(20, 10, 2, 18, shade(s, 0.75f))
                px.rect(10, 12, 2, 16, shade(s, 1.15f))
                px.rect(15, 13, 2, 8, shade(s, 0.6f)); px.rect(13, 15, 6, 2, shade(s, 0.6f))
                px.rect(9, 27, 14, 2, shade(s, 0.7f)); px.set(12, 24, 0xFF6A9A4A.toInt()); px.set(19, 26, 0xFF6A9A4A.toInt())
            }
            Decor.CAULDRON -> {
                base()
                px.rect(10, 27, 12, 2, 0xFFE0602A.toInt()); px.set(13, 26, 0xFFFFC040.toInt()); px.set(18, 26, 0xFFFFC040.toInt())
                oval(px, 16, 21, 8, 6, 0xFF2A2A30.toInt()); oval(px, 14, 19, 5, 3, 0xFF3A3A44.toInt())
                oval(px, 16, 15, 7, 2, 0xFF7AE04A.toInt()); px.rect(8, 15, 16, 1, 0xFF3A3A44.toInt())
                px.disc(13, 11, 1, 0xFF9AF06A.toInt()); px.disc(18, 8, 2, 0xAA9AF06A.toInt()); px.set(20, 13, 0xFFC0FF90.toInt())
            }
            Decor.XMASTREE -> {
                base()
                px.rect(14, 26, 4, 3, 0xFF6A4325.toInt())
                for ((k, y) in intArrayOf(26, 20, 14).withIndex()) {
                    val w = 10 - k * 2
                    px.poly(leaf, 16 - w, y, 16 + w, y, 16, y - 9)
                    px.poly(shade(leaf, 0.75f), 16, y, 16 + w, y, 16, y - 9)
                }
                for ((x, y, c) in listOf(Triple(11, 24, 0xFFE03A3A.toInt()), Triple(19, 22, 0xFF4AA0F0.toInt()), Triple(14, 18, gold), Triple(18, 15, 0xFFE03A3A.toInt()), Triple(15, 11, 0xFF4AA0F0.toInt())))
                    px.set(x, y, c)
                px.poly(gold, 16, 0, 18, 4, 16, 6, 14, 4)
            }
            Decor.SNOWMAN -> {
                px.poly(0xFFE8F0F8.toInt(), 6, 27, 16, 22, 26, 27, 16, 31)
                ball(16, 24, 6, white); ball(16, 15, 4, white); ball(16, 8, 3, white)
                px.set(15, 7, OUT); px.set(17, 7, OUT); px.rect(17, 8, 3, 1, 0xFFF08A2A.toInt())
                px.rect(12, 11, 8, 2, 0xFFE03A3A.toInt()); px.rect(18, 12, 2, 4, 0xFFC02A2A.toInt())
                px.set(16, 15, OUT); px.set(16, 17, OUT)
                px.rect(13, 3, 6, 2, 0xFF2A2A30.toInt()); px.rect(12, 5, 8, 1, 0xFF2A2A30.toInt())
                px.line(12, 15, 8, 12, 0xFF6A4325.toInt()); px.line(20, 15, 24, 12, 0xFF6A4325.toInt())
            }
            Decor.PRESENTS -> {
                base()
                fun box(x: Int, y: Int, w: Int, h: Int, c: Int, rib: Int) {
                    px.rect(x, y, w, h, c); px.rect(x + w - 2, y, 2, h, shade(c, 0.75f))
                    px.rect(x + w / 2 - 1, y, 2, h, rib); px.rect(x, y + h / 2, w, 1, rib)
                    px.disc(x + w / 2 - 2, y - 1, 1, rib); px.disc(x + w / 2 + 1, y - 1, 1, rib)
                }
                box(7, 20, 9, 8, 0xFFE03A3A.toInt(), gold); box(17, 22, 8, 7, 0xFF3AA04A.toInt(), white); box(12, 11, 8, 8, 0xFF4A7AE0.toInt(), 0xFFF06AB0.toInt())
            }
            Decor.BALLDROP -> {
                base(0xFF3A3A50.toInt())
                px.rect(15, 8, 2, 20, 0xFF8A8A9A.toInt()); px.rect(11, 27, 10, 2, 0xFF5A5A6A.toInt())
                for (j in -5..5) for (i in -5..5) if (i * i + j * j <= 25)
                    px.set(16 + i, 9 + j, if ((i + j) % 2 == 0) 0xFFE0E8F8.toInt() else if (i < 0 && j < 0) 0xFFFFE07A.toInt() else 0xFF8AA0E0.toInt())
                px.set(14, 7, white); px.set(22, 4, gold); px.set(9, 6, 0xFFFF6AB0.toInt())
            }
            Decor.CONFETTI -> {
                base()
                px.rect(10, 22, 12, 6, 0xFF4A4A58.toInt()); px.disc(11, 28, 2, 0xFF2A2A30.toInt()); px.disc(21, 28, 2, 0xFF2A2A30.toInt())
                px.poly(0xFFE03A8A.toInt(), 12, 22, 18, 22, 24, 10, 20, 8); px.poly(shade(0xFFE03A8A.toInt(), 0.75f), 18, 22, 24, 10, 22, 9)
                val cols = intArrayOf(gold, 0xFF4AA0F0.toInt(), 0xFF6AE04A.toInt(), 0xFFFF5A5A.toInt(), white)
                val r = Random(7)
                repeat(14) { px.set(16 + r.nextInt(15), r.nextInt(10), cols[it % cols.size]) }
            }
            Decor.HEARTARCH -> {
                base(grassC)
                val pink = 0xFFFF6A9A.toInt()
                px.rect(7, 12, 2, 16, 0xFFE8E0E8.toInt()); px.rect(23, 12, 2, 16, 0xFFC8C0C8.toInt())
                for (k in 0..6) { val x = 8 + k * 16 / 6; val y = 12 - (if (k in 2..4) 4 else if (k in 1..5) 2 else 0)
                    px.disc(x - 1, y, 1, pink); px.disc(x + 1, y, 1, pink); px.poly(pink, x - 2, y + 1, x + 3, y + 1, x + 1, y + 4) }
                px.disc(15, 18, 2, 0xFFE03A5A.toInt()); px.disc(18, 18, 2, 0xFFE03A5A.toInt()); px.poly(0xFFE03A5A.toInt(), 13, 19, 21, 19, 17, 24)
            }
            Decor.ROSES -> {
                base(grassC)
                oval(px, 16, 22, 9, 6, leaf); oval(px, 14, 20, 6, 4, shade(leaf, 1.15f))
                val r = Random(11)
                repeat(9) { val x = 9 + r.nextInt(15); val y = 17 + r.nextInt(9); px.disc(x, y, 1, 0xFFD02A4A.toInt()); px.set(x, y, 0xFFFF6A80.toInt()) }
            }
            Decor.RAINBOW -> {
                base(grassC)
                val cols = intArrayOf(0xFFE84A4A.toInt(), 0xFFF2A030.toInt(), 0xFFF2E04A.toInt(), 0xFF4AC04A.toInt(), 0xFF4A90F0.toInt(), 0xFF8A5AD0.toInt())
                for ((k, c) in cols.withIndex()) for (a in 0..40) {
                    val t = a / 40.0 * PI; val r = 12 - k
                    px.set((16 - r * kotlin.math.cos(t)).toInt(), (24 - r * kotlin.math.sin(t)).toInt(), c)
                }
                oval(px, 26, 26, 3, 2, 0xFF2A2A30.toInt()); px.rect(24, 24, 5, 1, gold); px.set(25, 23, gold); px.set(27, 23, 0xFFFFF0A0.toInt())
                oval(px, 5, 24, 3, 2, white)
            }
            Decor.CLOVER -> {
                base(0xFF3A7A2A.toInt())
                val c = 0xFF4AC04A.toInt()
                px.line(16, 26, 18, 18, 0xFF2E7A2A.toInt())
                fun heart(x: Int, y: Int, k: Float) { px.disc(x - 2, y, 3, shade(c, k)); px.disc(x + 2, y, 3, shade(c, k)); px.poly(shade(c, k), x - 5, y + 1, x + 5, y + 1, x, y + 6) }
                heart(16, 7, 1.1f); heart(10, 13, 1f); heart(22, 13, 0.8f)
                px.disc(16, 13, 2, shade(c, 0.9f)); px.set(14, 6, 0xFFA0F08A.toInt()); px.set(8, 12, 0xFFA0F08A.toInt())
                px.set(9, 25, gold); px.set(23, 26, gold)
            }
            Decor.JOINT -> {
                base()
                px.poly(white, 6, 26, 9, 28, 26, 14, 23, 12); px.poly(0xFFD8D0C0.toInt(), 9, 28, 26, 14, 25, 13, 8, 27)
                px.poly(0xFFC8A060.toInt(), 6, 26, 9, 28, 10, 27, 7, 25)
                px.disc(25, 13, 2, 0xFFFF6A2A.toInt()); px.set(25, 12, 0xFFFFE04A.toInt())
                px.disc(26, 8, 2, 0x99C8C8D0.toInt()); px.disc(24, 4, 3, 0x77C8C8D0.toInt())
            }
            Decor.BONG -> {
                base()
                val gl = 0xCC6AC8E8.toInt()
                oval(px, 16, 25, 6, 4, gl); px.rect(13, 6, 6, 18, gl); px.rect(13, 6, 2, 18, 0xDDB8F0FF.toInt())
                oval(px, 16, 26, 5, 2, 0xAA4AA05A.toInt()); px.rect(16, 5, 2, 1, 0xFF8AE0F8.toInt())
                px.line(20, 22, 24, 18, 0xFF9AA0A8.toInt()); px.disc(24, 17, 1, 0xFF6A6A72.toInt()); px.set(24, 16, 0xFF4F9A3A.toInt())
            }
            Decor.EGG -> {
                base(grassC)
                oval(px, 16, 19, 7, 10, 0xFFC0A0FF.toInt())
                px.rect(9, 15, 15, 2, 0xFFFFE07A.toInt()); px.rect(9, 22, 15, 2, 0xFF7AE0C0.toInt())
                for (x in intArrayOf(11, 15, 19)) px.set(x, 19, 0xFFFF7AB0.toInt())
                px.set(13, 12, white)
            }
            Decor.BUNNYSTATUE -> {
                base(grassC)
                px.rect(10, 26, 12, 3, 0xFF8A6A4A.toInt())
                oval(px, 16, 21, 6, 5, leaf); oval(px, 17, 13, 4, 4, leaf)
                px.rect(14, 3, 2, 8, leaf); px.rect(18, 2, 2, 9, shade(leaf, 0.8f))
                px.disc(10, 22, 2, shade(leaf, 1.2f)); px.set(18, 13, OUT)
            }
            Decor.FLAGPOLE -> {
                base(grassC)
                px.rect(9, 2, 1, 26, 0xFFC8C8D0.toInt()); px.disc(9, 2, 1, gold); px.rect(7, 27, 5, 2, 0xFF8A8A94.toInt())
                for (j in 0..6) px.rect(10, 3 + j, 14, 1, if (j % 2 == 0) 0xFFE03A3A.toInt() else white)
                px.rect(10, 3, 6, 4, 0xFF3A4AA0.toInt()); px.set(11, 4, white); px.set(13, 4, white); px.set(12, 5, white); px.set(14, 5, white)
            }
            Decor.GRILL -> {
                base()
                px.line(11, 22, 9, 29, 0xFF3A3A40.toInt()); px.line(21, 22, 23, 29, 0xFF3A3A40.toInt())
                oval(px, 16, 19, 8, 5, 0xFF2A2A30.toInt()); px.rect(8, 16, 17, 2, 0xFF6A6A72.toInt())
                px.rect(10, 15, 4, 1, 0xFF8A4A2A.toInt()); px.rect(17, 15, 4, 1, 0xFFA05A2A.toInt()); px.set(15, 15, 0xFFE03A3A.toInt())
                px.disc(14, 10, 2, 0x99C8C8D0.toInt()); px.disc(18, 6, 2, 0x66C8C8D0.toInt())
            }
            Decor.CORNUCOPIA -> {
                base()
                val w = 0xFFC8904A.toInt()
                px.poly(w, 24, 12, 26, 22, 14, 26, 10, 20); px.poly(shade(w, 0.75f), 26, 22, 14, 26, 16, 22)
                px.line(24, 12, 28, 8, w); px.set(28, 7, w)
                px.disc(10, 22, 3, 0xFFE03A3A.toInt()); px.disc(8, 26, 2, 0xFFF2A030.toInt()); px.disc(13, 26, 2, 0xFF8A4AA0.toInt())
                px.poly(0xFFF2D04A.toInt(), 5, 19, 10, 17, 6, 22); px.set(9, 20, white)
            }
            Decor.HAYBALE -> {
                base()
                val h = 0xFFE8C66A.toInt()
                px.rect(7, 16, 18, 12, h); px.rect(19, 16, 6, 12, shade(h, 0.8f)); px.rect(7, 14, 18, 2, shade(h, 1.1f))
                px.rect(7, 19, 18, 1, 0xFF8A5A30.toInt()); px.rect(7, 24, 18, 1, 0xFF8A5A30.toInt())
                val r = Random(3)
                repeat(10) { px.set(8 + r.nextInt(16), 15 + r.nextInt(12), shade(h, 0.7f)) }
                px.set(12, 13, h); px.set(16, 12, h); px.set(21, 13, shade(h, 0.85f))
            }
            Decor.SOUNDSYSTEM -> {
                base()
                val red = 0xFFD83A2E.toInt(); val yel = 0xFFF2C83A.toInt(); val grn = 0xFF2E9A3E.toInt()
                val cab = 0xFF2A2420.toInt()
                for ((x, y0) in listOf(6 to 13, 19 to 13)) {
                    px.rect(x, y0, 8, 15, cab); px.rect(x + 6, y0, 2, 15, shade(cab, 0.7f))
                    px.disc(x + 3, y0 + 4, 2, 0xFF5A5450.toInt()); px.set(x + 3, y0 + 4, cab)
                    px.disc(x + 3, y0 + 10, 3, 0xFF5A5450.toInt()); px.disc(x + 3, y0 + 10, 1, cab)
                }
                px.rect(13, 20, 7, 8, 0xFF3A3430.toInt())
                px.rect(13, 21, 7, 1, red); px.rect(13, 23, 7, 1, yel); px.rect(13, 25, 7, 1, grn)
                px.rect(6, 11, 21, 2, cab); px.rect(6, 11, 7, 1, red); px.rect(13, 11, 7, 1, yel); px.rect(20, 11, 7, 1, grn)
                px.set(10, 7, yel); px.set(12, 5, yel); px.set(22, 6, yel); px.set(24, 8, yel)
            }
            Decor.HAMMOCK -> {
                base(0xFF4A6A30.toInt())
                val post = 0xFF6A4325.toInt()
                px.rect(5, 9, 2, 19, post); px.rect(25, 9, 2, 19, post)
                px.disc(6, 7, 3, leaf); px.disc(26, 7, 3, leaf)
                val stripes = intArrayOf(0xFFD83A2E.toInt(), 0xFFF2C83A.toInt(), 0xFF2E9A3E.toInt())
                for (x in 7..24) {
                    val sag = ((x - 7) * (24 - x)) / 22
                    for (k in 0..2) px.set(x, 12 + sag + k, stripes[(x / 2) % 3].let { if (k == 2) shade(it, 0.75f) else it })
                }
                px.line(7, 12, 5, 11, white); px.line(24, 12, 26, 11, white)
            }
            Decor.STEELDRUM -> {
                base()
                val s = 0xFFC8CCD4.toInt()
                px.rect(10, 26, 1, 3, 0xFF4A4A50.toInt()); px.rect(21, 26, 1, 3, 0xFF4A4A50.toInt())
                px.rect(10, 15, 12, 11, shade(s, 0.85f)); px.rect(10, 15, 3, 11, s); px.rect(19, 15, 3, 11, shade(s, 0.65f))
                px.rect(10, 18, 12, 1, 0xFFD83A2E.toInt()); px.rect(10, 22, 12, 1, 0xFFF2C83A.toInt())
                oval(px, 16, 15, 6, 2, shade(s, 1.1f)); oval(px, 16, 15, 4, 1, shade(s, 0.75f))
                px.set(14, 15, white); px.set(18, 14, white)
                px.line(6, 9, 13, 13, 0xFF8A5A30.toInt()); px.line(26, 9, 19, 13, 0xFF8A5A30.toInt())
                px.disc(13, 13, 1, white); px.disc(19, 13, 1, white)
            }
            Decor.MURAL -> {
                base()
                val wall = 0xFFE8DCC0.toInt()
                px.rect(5, 6, 22, 21, wall); px.rect(5, 26, 22, 2, shade(wall, 0.7f))
                px.rect(6, 7, 20, 6, 0xFFD83A2E.toInt()); px.rect(6, 13, 20, 6, 0xFFF2C83A.toInt()); px.rect(6, 19, 20, 7, 0xFF2E9A3E.toInt())
                val l = 0xFF1E5A22.toInt()
                px.line(16, 24, 16, 11, l)
                for (k in 0..2) { px.line(16, 16 + k * 2, 10 + k, 11 + k * 3, l); px.line(16, 16 + k * 2, 22 - k, 11 + k * 3, l) }
                px.line(16, 20, 12, 22, l); px.line(16, 20, 20, 22, l)
            }
            Decor.LION -> {
                base(0xFF6A6A70.toInt())
                val st = 0xFFC8B070.toInt(); val mane = 0xFFB0702A.toInt()
                px.rect(8, 23, 16, 5, shade(st, 0.75f)); px.rect(8, 23, 16, 1, st)
                oval(px, 19, 19, 6, 4, st); px.rect(21, 21, 2, 3, shade(st, 0.8f)); px.rect(16, 21, 2, 3, shade(st, 0.8f))
                px.disc(11, 15, 6, mane); px.disc(12, 16, 5, shade(mane, 0.8f))
                for (k in 0..5) px.set(6 + k * 2, 9 + (k % 2), 0xFFD83A2E.toInt())
                px.disc(11, 15, 3, st); px.set(10, 14, OUT); px.set(12, 14, OUT); px.rect(10, 17, 3, 1, shade(st, 0.6f))
                px.line(25, 18, 27, 13, mane); px.disc(27, 12, 1, mane)
            }
            Decor.SHROOMS -> {
                base(0xFF3A4A2A.toInt())
                val stem = 0xFFF0E8D0.toInt()
                fun shroom(cx: Int, by: Int, h: Int, r: Int, cap: Int) {
                    px.rect(cx - 1, by - h, 3, h, stem); px.set(cx + 1, by - 1, shade(stem, 0.8f))
                    oval(px, cx, by - h, r, r / 2 + 1, cap); oval(px, cx + 1, by - h + 1, r - 1, r / 2, shade(cap, 0.8f))
                    oval(px, cx, by - h - 1, r - 1, r / 2, cap)
                    px.set(cx - r / 2, by - h - 1, white); px.set(cx + r / 2, by - h, white); px.set(cx, by - h - 2, white)
                }
                shroom(10, 28, 6, 4, 0xFFB05AE0.toInt()); shroom(22, 28, 5, 4, 0xFF3AB0D8.toInt())
                shroom(16, 29, 11, 6, 0xFFE04A6A.toInt())
            }
            Decor.BUDDHA -> {
                base(0xFF5A5A62.toInt())
                val g = 0xFFE0B040.toInt()
                px.rect(7, 25, 18, 3, 0xFF8A8A94.toInt()); px.rect(7, 25, 18, 1, 0xFFB0B0B8.toInt())
                oval(px, 16, 22, 8, 3, shade(g, 0.85f))
                oval(px, 16, 17, 5, 6, g); oval(px, 17, 18, 4, 5, shade(g, 0.85f)); oval(px, 15, 16, 3, 4, g)
                px.disc(16, 9, 3, g); px.set(16, 5, shade(g, 0.8f)); px.disc(17, 10, 2, shade(g, 0.85f)); px.disc(15, 8, 1, mix(g, white, 0.5f))
                px.set(15, 9, shade(g, 0.6f)); px.set(17, 9, shade(g, 0.6f))
                px.rect(13, 20, 6, 1, shade(g, 0.7f))
            }
            Decor.TIKIBAR -> {
                base()
                val wd = 0xFF8A5A30.toInt(); val thatch = 0xFFD8B060.toInt()
                px.rect(7, 12, 2, 16, shade(wd, 0.7f)); px.rect(23, 12, 2, 16, shade(wd, 0.7f))
                px.rect(6, 20, 20, 7, wd); px.rect(6, 20, 20, 1, shade(wd, 1.25f)); px.rect(20, 21, 6, 6, shade(wd, 0.75f))
                for (x in 7..24 step 3) px.line(x, 21, x, 26, shade(wd, 0.6f))
                px.poly(thatch, 3, 13, 16, 5, 29, 13)
                for (x in 4..28 step 2) px.line(x, 13, x, 15, shade(thatch, if (x % 4 == 0) 0.8f else 1f))
                px.rect(10, 17, 2, 3, 0xFF3AB0D8.toInt()); px.rect(15, 18, 2, 2, 0xFFF08A3A.toInt()); px.set(15, 17, 0xFF4F9A3A.toInt())
                px.rect(20, 17, 2, 3, 0xFFD83A2E.toInt())
            }
            else -> {}
        }
    }

    private fun top(x: Int) = 7 - (if (x < 16) x else 31 - x) / 2
    private fun bottom(x: Int) = 8 + (if (x < 16) x else 31 - x) / 2

    /** A grass square with a path laid on it (same size as the ground tiles). */
    private fun path(d: Decor): Bitmap {
        val tw = Sprites.TW; val th = Sprites.TH + Sprites.DEPTH
        val px = Px(tw, th)
        Sprites.grass(0, false).getPixels(px.p, 0, px.fw, 0, 0, px.fw, px.fh)
        val r = Random(d.ordinal)
        for (x in 1 until tw - 1) for (y in top(x) + 1 until bottom(x)) {
            if (d == Decor.BRICK) {
                val row = (y + x / 2) / 2
                val mortar = (y + x / 2) % 2 == 0 || ((x + row * 3) % 6 == 0)
                px.set(x, y, if (mortar) 0xFF9A8A7A.toInt() else if (r.nextInt(6) == 0) 0xFFA84A3A.toInt() else 0xFFC0604A.toInt())
            } else if (r.nextInt(10) < 7) {
                px.set(x, y, if (r.nextInt(5) == 0) 0xFF8A8A92.toInt() else 0xFFB8B8C0.toInt())
            }
        }
        return px.bitmap()
    }
}
