package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.Supply

/** Storefronts on Main Street and icons for goods and supplies. */
object Town {
    private val OUT = Sprites.OUT

    const val FRONT_W = 64
    const val FRONT_H = 56
    private val fronts = HashMap<Keeper, Bitmap>()

    fun front(k: Keeper): Bitmap = fronts.getOrPut(k) {
        val px = Px(FRONT_W, FRONT_H)
        data class Look(val wall: Int, val awnA: Int, val awnB: Int, val trim: Int)
        val l = when (k) {
            Keeper.VIOLET -> Look(0xFF4E8A50.toInt(), 0xFF8A4AC8.toInt(), 0xFFE8D8F4.toInt(), 0xFF2E5A32.toInt())
            Keeper.DEX -> Look(0xFF9A6A3E.toInt(), 0xFFD03A3A.toInt(), 0xFFF4F0E8.toInt(), 0xFF5A3A1E.toInt())
            Keeper.HANK -> Look(0xFFA84A3A.toInt(), 0xFFF08A2A.toInt(), 0xFFF4E8D0.toInt(), 0xFF6A2A20.toInt())
            Keeper.MONA -> Look(0xFFE8DCC0.toInt(), 0xFF2A3A6A.toInt(), 0xFFF4F4F0.toInt(), 0xFFA89878.toInt())
            Keeper.GUS -> Look(0xFF4A7AB8.toInt(), 0xFFF2C440.toInt(), 0xFFF4F0E0.toInt(), 0xFF24406A.toInt())
            Keeper.SHADE -> Look(0xFF2A2634.toInt(), 0xFF3A2A50.toInt(), 0xFF1A1622.toInt(), 0xFF120E1A.toInt())
        }
        // Wall with texture.
        px.rect(2, 8, 60, 48, l.wall)
        for (y in 8 until 56) for (x in 2 until 62) {
            val brick = k == Keeper.HANK && (y % 4 == 0 || (x + (y / 4 % 2) * 4) % 8 == 0)
            val plank = k == Keeper.DEX && x % 6 == 0
            val grime = k == Keeper.SHADE && (x * 7 + y * 13) % 11 == 0
            if (brick || plank || grime) px.set(x, y, shade(l.wall, 0.8f))
        }
        // Cornice and sign board.
        px.rect(0, 2, 64, 8, l.trim); px.rect(0, 2, 64, 1, shade(l.trim, 1.3f))
        px.rect(10, 3, 44, 5, shade(l.trim, 0.7f))
        // Scalloped striped awning.
        for (y in 18..23) for (x in 4..59) px.set(x, y, if ((x - 4) / 7 % 2 == 0) l.awnA else l.awnB)
        for (i in 0 until 8) { val cx = 7 + i * 7; px.rect(cx - 3, 24, 7, 1, if (i % 2 == 0) l.awnA else l.awnB); px.rect(cx - 2, 25, 5, 1, if (i % 2 == 0) l.awnA else l.awnB) }
        px.rect(4, 18, 56, 1, shade(l.awnA, 1.25f))
        // Display window.
        val glass = if (k == Keeper.SHADE) 0xFF1A1A22.toInt() else 0xFF8ACCE8.toInt()
        px.rect(5, 29, 30, 20, l.trim); px.rect(6, 30, 28, 18, glass)
        if (k != Keeper.SHADE) { px.line(8, 46, 14, 31, shade(glass, 1.15f)); px.line(10, 46, 16, 31, shade(glass, 1.15f)) }
        window(px, k)
        px.rect(4, 49, 32, 2, shade(l.trim, 1.2f))
        // Door.
        val door = if (k == Keeper.SHADE) 0xFF6A6E78.toInt() else shade(l.trim, 1.1f)
        px.rect(40, 29, 18, 27, l.trim); px.rect(42, 31, 14, 25, door)
        if (k == Keeper.SHADE) {
            for (y in 33..53 step 5) px.rect(42, y, 14, 1, 0xFF4A4E58.toInt())
            px.rect(46, 36, 6, 2, 0xFF14141A.toInt())
        } else px.rect(44, 33, 10, 8, glass)
        px.set(53, 44, 0xFFF2C440.toInt()); px.set(53, 45, 0xFFF2C440.toInt())
        // Neon trim on the black market.
        if (k == Keeper.SHADE) {
            val neon = 0xFFD86AF0.toInt()
            px.rect(10, 3, 44, 1, neon); px.rect(10, 7, 44, 1, neon); px.rect(10, 3, 1, 5, neon); px.rect(53, 3, 1, 5, neon)
            px.rect(40, 27, 18, 1, neon)
        }
        px.outline(OUT)
        px.bitmap()
    }

    private fun window(px: Px, k: Keeper) {
        when (k) {
            Keeper.VIOLET -> for (i in 0..2) {
                val x = 11 + i * 9
                px.rect(x - 2, 43, 5, 4, 0xFFB86A3A.toInt()); px.rect(x - 3, 43, 7, 1, 0xFFD08A4A.toInt())
                Sprites.fanLeaf(px, x, 42, -Math.PI / 2, 5, 0xFF4FA03C.toInt(), 0xFF8AD06A.toInt())
            }
            Keeper.DEX -> for (i in 0..3) {
                val x = 8 + i * 7
                px.rect(x, 40, 5, 7, 0xFFDCEEF0.toInt()); px.rect(x + 1, 42, 3, 4, 0xFF6AB04A.toInt()); px.rect(x, 39, 5, 1, 0xFF8A5A2A.toInt())
            }
            Keeper.HANK -> {
                px.rect(9, 33, 1, 12, 0xFF8A5A2A.toInt()); px.rect(7, 33, 5, 3, 0xFF9AA0A8.toInt())
                px.rect(17, 36, 8, 8, 0xFF3E8EDC.toInt()); px.rect(19, 34, 4, 2, 0xFF3E8EDC.toInt()); px.rect(20, 38, 2, 2, 0xFFDCEEF0.toInt())
                px.rect(28, 34, 2, 12, 0xFF9AA0A8.toInt()); px.rect(27, 34, 4, 2, 0xFF9AA0A8.toInt())
            }
            Keeper.MONA -> for (i in 0..1) {
                val x = 8 + i * 13
                px.rect(x, 33, 11, 12, 0xFFF4F4F0.toInt())
                px.poly(0xFFC8484A.toInt(), x + 1, 39, x + 5, 35, x + 10, 39)
                px.rect(x + 2, 39, 7, 4, 0xFFE8C66A.toInt()); px.rect(x + 4, 40, 2, 3, 0xFF5A3A1E.toInt())
            }
            Keeper.GUS -> {
                px.rect(10, 33, 20, 13, 0xFFF4F0E0.toInt())
                px.rect(12, 35, 16, 2, 0xFFD03A3A.toInt())
                for (y in 39..43 step 2) px.rect(12, y, 16, 1, 0xFF8A8A92.toInt())
            }
            Keeper.SHADE -> {
                val wood = 0xFF6A4A2A.toInt()
                px.poly(wood, 6, 33, 34, 39, 34, 42, 6, 36)
                px.poly(wood, 6, 42, 34, 36, 34, 39, 6, 45)
                px.set(20, 38, 0xFF9AA0A8.toInt()); px.set(20, 41, 0xFF9AA0A8.toInt())
            }
        }
    }

    // ---- Goods and supply icons (12×12) ----

    private val goodsIcons = HashMap<Facility, Bitmap>()

    /** Icon for what a facility makes. */
    fun goods(f: Facility): Bitmap = goodsIcons.getOrPut(f) {
        Sprites.icon(when (f) {
            Facility.ROLLER -> arrayOf(
                "          rr",
                "         rk ",
                "        wwk ",
                "       wwwk ",
                "      wwwk  ",
                "     wwwk   ",
                "    wwwk    ",
                "   wwwk     ",
                "  nnnk      ",
                "  nnk       ",
                "  kk        ",
                "            ")
            Facility.PRESS -> arrayOf(
                "            ",
                "            ",
                "   kkkkkk   ",
                "  knnnnnnk  ",
                " knNNNNNNnk ",
                " knNnNNNNnk ",
                " knNNNNnNnk ",
                " knNNNNNNnk ",
                "  knnnnnnk  ",
                "   kkkkkk   ",
                "            ",
                "            ")
            Facility.KITCHEN -> arrayOf(
                "            ",
                "    kkkk    ",
                "  kknnnnkk  ",
                " knnNnnNnnk ",
                " knnnnnnnnk ",
                "knnGnnnnGnnk",
                "knnnnnGnnnnk",
                " knnNnnnNnk ",
                " knnnnnnnnk ",
                "  kknnnnkk  ",
                "    kkkk    ",
                "            ")
            Facility.LAB -> arrayOf(
                "            ",
                "    kkkk    ",
                "   kyyyyk   ",
                "  kyYYYYyk  ",
                " kyYwYYYYyk ",
                " kyYYYYoYyk ",
                " kyYYYooYyk ",
                "  kyYYYYyk  ",
                "   kyyyyk   ",
                "    kkkk    ",
                "            ",
                "            ")
            Facility.VAPE -> arrayOf(
                "     kk     ",
                "    kssk    ",
                "    kSSk    ",
                "    kyyk    ",
                "    kYYk    ",
                "    kYwk    ",
                "    kYYk    ",
                "    kyyk    ",
                "    kssk    ",
                "    kSSk    ",
                "    kssk    ",
                "     kk     ")
        })
    }

    private val supplyIcons = HashMap<Supply, Bitmap>()

    fun supply(s: Supply): Bitmap = supplyIcons.getOrPut(s) {
        Sprites.icon(when (s) {
            Supply.PAPERS -> arrayOf(
                "            ",
                "  kkkkkkkk  ",
                " kwwwwwwwwk ",
                " kwwwwwwwwk ",
                " kGGGGGGGGk ",
                " kGwwGGwwGk ",
                " kGGGGGGGGk ",
                " kGGGGGGGGk ",
                " kGGGGGGGGk ",
                "  kkkkkkkk  ",
                "            ",
                "            ")
            Supply.ICE -> arrayOf(
                "            ",
                "   kkkkk    ",
                "  kwwwwbk   ",
                " kwbbbbbBk  ",
                " kwbwbbbBk  ",
                " kwbbbbbBkk ",
                " kwbbbbBkwk ",
                " kkkkkkkwbk ",
                "     kwbbBk ",
                "     kbbBBk ",
                "      kkkk  ",
                "            ")
            Supply.BUTTER -> arrayOf(
                "            ",
                "            ",
                "    kkkkkk  ",
                "   kyyyyyyk ",
                "  kyywyyyYk ",
                " kkkkkkkkYk ",
                " kyyyyyyykk ",
                " kyyyyyyYk  ",
                " kYYYYYYYk  ",
                "  kkkkkkk   ",
                "            ",
                "            ")
            Supply.BUTANE -> arrayOf(
                "     kk     ",
                "    kssk    ",
                "   kkkkkk   ",
                "  kbbbbbbk  ",
                "  kbwbbbBk  ",
                "  kbwbbbBk  ",
                "  krrrrrrk  ",
                "  kbbbbbBk  ",
                "  kbbbbbBk  ",
                "  kbbbbbBk  ",
                "   kkkkkk   ",
                "            ")
            Supply.CARTS -> arrayOf(
                "            ",
                "  kk  kk    ",
                " kssk kssk  ",
                " kwwk kwwk  ",
                " kwwk kwwk  ",
                " kwwk kwwk  ",
                " kSSk kSSk  ",
                " kssk kssk  ",
                " kssk kssk  ",
                "  kk  kk    ",
                "            ",
                "            ")
        })
    }
}
