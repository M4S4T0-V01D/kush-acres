package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Holiday

/** The small currency icons: Pot Bucks and each holiday's token. 12×12 like the other icons. */
object Coins {
    private val OUT = Sprites.OUT
    private val white = 0xFFF4F4F0.toInt()

    /** A green bill with a leaf on it. */
    val bucks: Bitmap by lazy {
        Px(12, 12).apply {
            val g = 0xFF5AC05A.toInt()
            rect(0, 2, 12, 8, shade(g, 0.8f)); rect(1, 3, 10, 6, g)
            rect(1, 3, 10, 1, shade(g, 1.2f))
            disc(6, 6, 2, shade(g, 0.6f)); set(6, 5, 0xFFC8FF9A.toInt()); set(5, 6, 0xFFC8FF9A.toInt()); set(7, 6, 0xFFC8FF9A.toInt())
            set(2, 4, 0xFFC8FF9A.toInt()); set(9, 7, 0xFFC8FF9A.toInt())
            outline(OUT)
        }.bitmap()
    }

    /** A cut purple gem with a green glint: Kush Gems, for special buildings. */
    val gem: Bitmap by lazy {
        Px(12, 12).apply {
            val g = 0xFFB060F0.toInt()
            poly(shade(g, 0.7f), 1, 4, 11, 4, 6, 11)
            poly(g, 3, 4, 9, 4, 6, 10)
            poly(shade(g, 1.3f), 3, 1, 9, 1, 11, 4, 1, 4)
            rect(4, 2, 4, 1, 0xFFE8C8FF.toInt())
            set(5, 5, 0xFFC8FF9A.toInt()); set(4, 3, white)
            outline(OUT)
        }.bitmap()
    }

    /** A little green delivery van, for orders on the road. */
    val van: Bitmap by lazy {
        Px(14, 12).apply {
            val g = 0xFF4AA04A.toInt()
            rect(1, 3, 8, 6, g); rect(9, 5, 4, 4, shade(g, 0.9f)); rect(1, 3, 8, 1, shade(g, 1.25f))
            rect(10, 6, 2, 2, 0xFF9AD0F0.toInt()); rect(3, 5, 4, 2, 0xFFF4E8C0.toInt()); set(5, 5, g)
            disc(4, 9, 1, 0xFF222222.toInt()); disc(10, 9, 1, 0xFF222222.toInt())
            outline(OUT)
        }.bitmap()
    }

    private val cache = HashMap<Holiday, Bitmap>()

    fun token(h: Holiday): Bitmap = cache.getOrPut(h) {
        val px = Px(12, 12)
        with(px) {
            when (h) {
                Holiday.HALLOWEEN -> {
                    poly(0xFFFFF4D0.toInt(), 4, 1, 8, 1, 6, 3)
                    poly(0xFFFF8A1E.toInt(), 3, 2, 9, 2, 8, 6, 4, 6)
                    poly(0xFFFFD23A.toInt(), 4, 6, 8, 6, 6, 11)
                    poly(white, 4, 1, 8, 1, 7, 2, 5, 2)
                    set(5, 4, 0xFFFFC080.toInt())
                }
                Holiday.CHRISTMAS -> {
                    for (k in 0..8) set(7, 3 + k, if (k % 3 == 0) 0xFFE03A3A.toInt() else white)
                    for ((k, p) in listOf(7 to 2, 6 to 1, 5 to 1, 4 to 1, 3 to 2, 3 to 3).withIndex()) set(p.first, p.second, if (k % 2 == 0) 0xFFE03A3A.toInt() else white)
                }
                Holiday.NEWYEAR -> {
                    val cs = intArrayOf(0xFFFFD23A.toInt(), 0xFFFF5AB0.toInt(), 0xFF4AA0F0.toInt(), 0xFF6AE04A.toInt())
                    rect(2, 2, 3, 2, cs[0]); rect(7, 1, 2, 3, cs[1]); rect(1, 7, 2, 3, cs[2]); rect(5, 5, 3, 2, cs[3])
                    rect(8, 8, 3, 2, cs[0]); rect(4, 9, 2, 2, cs[1]); rect(9, 4, 2, 2, cs[2])
                }
                Holiday.VALENTINE -> {
                    val c = 0xFFFF4A7A.toInt()
                    disc(4, 4, 3, c); disc(8, 4, 3, c); poly(c, 1, 5, 11, 5, 6, 11)
                    set(3, 3, 0xFFFFC0D8.toInt())
                }
                Holiday.STPATRICK -> {
                    val c = 0xFF3AC04A.toInt()
                    disc(6, 3, 2, c); disc(3, 6, 2, c); disc(9, 6, 2, shade(c, 0.85f)); line(6, 7, 8, 11, 0xFF2E7A2A.toInt())
                    set(6, 2, 0xFFA0F08A.toInt())
                }
                Holiday.FOURTWENTY -> {
                    val c = 0xFF6AB83A.toInt()
                    disc(6, 6, 4, c); disc(4, 5, 2, shade(c, 1.15f)); disc(8, 8, 2, shade(c, 0.8f))
                    set(5, 7, 0xFFE07A2A.toInt()); set(8, 4, 0xFFE07A2A.toInt()); set(4, 4, white); set(7, 8, 0xFFA05AE0.toInt())
                }
                Holiday.EASTER -> {
                    for (j in 0 until 12) for (i in 0 until 12) {
                        val dx = (i - 5.5) / 4.2; val dy = (j - 6.5) / (if (j < 6) 5.8 else 4.8)
                        if (dx * dx + dy * dy <= 1) set(i, j, when (j) { 4 -> 0xFFFFE07A.toInt(); 8 -> 0xFF7AE0C0.toInt(); else -> 0xFFC0A0FF.toInt() })
                    }
                    set(4, 3, white)
                }
                Holiday.JULY4 -> {
                    val c = 0xFFFFD23A.toInt()
                    line(6, 0, 6, 11, c); line(0, 6, 11, 6, c); line(2, 2, 10, 10, 0xFFE03A3A.toInt()); line(10, 2, 2, 10, 0xFF4A90F0.toInt())
                    disc(6, 6, 1, white)
                }
                Holiday.THANKSGIVING -> {
                    disc(6, 7, 4, 0xFFB07030.toInt()); disc(5, 6, 2, 0xFFC8884A.toInt())
                    rect(1, 3, 10, 2, 0xFF6A4228.toInt()); rect(2, 2, 8, 1, 0xFF8A5A34.toInt()); rect(6, 0, 1, 2, 0xFF4A3220.toInt())
                }
            }
            outline(OUT)
        }
        px.bitmap()
    }
}
