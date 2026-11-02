package dev.m4s4t0.kushacres.art

import dev.m4s4t0.kushacres.model.Farmer
import kotlin.math.abs
import kotlin.math.pow

/**
 * The player's farmer at four times the detail of [People.figurePx]: the same [People.P_W]×[People.P_H] footprint
 * drawn on a 56×104 grid, so the face, hair, clothes and shoes all get real shape. Lit from the upper left.
 */
internal object Figure {
    const val K = 4
    const val W = People.P_W * K
    const val H = People.P_H * K
    private const val CX = 28
    private const val TOP = 46
    private const val WAIST = 64
    private const val HIP = 72
    private const val ANKLE = 96
    private val WHITE = 0xFFFFFFFF.toInt()
    private val INK = 0xFF1E1418.toInt()
    private val GOLD = 0xFFF2C440.toInt()

    /** Hats that leave the top of the head (and any bun on it) showing. */
    private val OPEN_HATS = setOf("None", "Heart band", "Leaf crown", "Bunny ears", "Visor", "Party hat")

    private enum class Sleeve { NONE, SHORT, LONG, PUFFY }
    private fun sleeve(o: String) = when (o) {
        "Tank top", "Sundress", "Halter", "Overalls" -> Sleeve.NONE
        "Tee", "Crop top", "Polo", "Jersey", "Dress", "Tie-dye" -> Sleeve.SHORT
        "Blouse", "Peasant top", "Puffer", "Kimono", "Off-shoulder" -> Sleeve.PUFFY
        else -> Sleeve.LONG
    }

    private class Body(val sh: Int, val wa: Int, val hi: Int, val fem: Boolean, val chin: Float)
    private fun body(b: Int) = when (b) {
        0 -> Body(14, 12, 11, false, 2.8f)
        1 -> Body(11, 8, 12, true, 1.8f)
        else -> Body(12, 10, 11, false, 2.2f)
    }

    private fun lt(c: Int, t: Float = 0.25f) = mix(c, WHITE, t)
    private fun dk(c: Int, f: Float = 0.74f) = shade(c, f)
    private fun inE(x: Int, y: Int, cx: Int, cy: Int, rx: Int, ry: Int): Boolean {
        val dx = (x - cx).toFloat() / (rx + 0.5f); val dy = (y - cy).toFloat() / (ry + 0.5f)
        return dx * dx + dy * dy <= 1f
    }
    private fun tri(i: Int, n: Int): Int { val m = Math.floorMod(i, 2 * n); return if (m < n) m else 2 * n - m }

    private inline fun Px.fill(x0: Int, y0: Int, x1: Int, y1: Int, inside: (Int, Int) -> Boolean, c: (Int, Int) -> Int) {
        for (y in maxOf(0, y0)..minOf(H - 1, y1)) for (x in maxOf(0, x0)..minOf(W - 1, x1)) if (inside(x, y)) set(x, y, c(x, y))
    }

    /** Lights the left edge and darkens the right edge of every horizontal run of exactly [c] in rows [y0]..[y1]. */
    private fun Px.round(y0: Int, y1: Int, c: Int, l: Int = lt(c, 0.18f), d: Int = dk(c, 0.78f), x0: Int = 0, x1: Int = W - 1) {
        for (y in y0..y1) {
            var x = x0
            while (x <= x1) {
                if (get(x, y) != c) { x++; continue }
                var e = x
                while (e + 1 <= x1 && get(e + 1, y) == c) e++
                if (e - x >= 4) { set(x, y, l); if (e - x >= 7) set(x + 1, y, l); for (i in maxOf(e - 2, x + 2)..e) set(i, y, d) }
                x = e + 1
            }
        }
    }

    private fun Px.tint(cx: Int, cy: Int, rx: Int, ry: Int, c: Int, a: Float) {
        for (y in cy - ry..cy + ry) for (x in cx - rx..cx + rx) {
            val d = (x - cx) * (x - cx) / (rx * rx + 0.5f) + (y - cy) * (y - cy) / (ry * ry + 0.5f)
            if (d <= 1f && get(x, y) != 0) set(x, y, mix(get(x, y), c, a * (1f - d * 0.5f)))
        }
    }

    fun px(look: Farmer, frame: Int, outline: Boolean = true): Px {
        val px = Px(W, H, 1)
        val b = body(look.body)
        val step = frame == 1
        val skin = Farmer.SKINS[look.skin]
        val hairC = Farmer.HAIR_COLORS[look.hairColor]
        val outfit = Farmer.OUTFITS[look.outfit]
        val hair = Farmer.HAIRS[look.hair]
        val hat = Farmer.HATS[look.hat]
        val covered = hat !in OPEN_HATS
        backHair(px, hair, hairC, covered)
        legs(px, look, b, step, skin, outfit)
        torso(px, look, b, skin, outfit)
        arms(px, look, b, step, skin, outfit)
        if (look.has(3)) necklace(px)
        head(px, look, b, skin, hairC)
        frontHair(px, hair, hairC, skin, covered, hat != "None")
        hat(px, look, hat)
        top(px, look, hat)
        if (outline) { px.outline(Sprites.OUT); px.outline(Sprites.OUT) }
        return px
    }

    // ---- Hair ----

    private fun hairShade(c: Int): (Int, Int) -> Int {
        val d = dk(c, 0.72f); val l = lt(c, 0.3f); val dd = dk(c, 0.55f)
        return { x, y ->
            when {
                x >= 38 || (x >= 36 && y > 30) -> d
                Math.floorMod(x * 5 + y / 3, 11) == 0 -> d
                y in 14..17 && x in 19..27 -> l
                Math.floorMod(x * 3 + y / 4, 13) == 0 -> l
                y > 60 && Math.floorMod(x, 4) == 0 -> dd
                else -> c
            }
        }
    }

    private fun backHair(px: Px, style: String, c: Int, covered: Boolean) {
        val hc = hairShade(c); val d = dk(c, 0.7f)
        val back: (Int, Int) -> Int = { x, y -> if (Math.floorMod(x * 5 + y / 3, 7) == 0) dk(c, 0.55f) else d }
        when (style) {
            "Long", "Bangs" -> {
                val end = if (style == "Long") 64 else 58
                px.fill(13, 18, 43, end, { x, y -> abs(x - CX) <= 15 - maxOf(0, y - (end - 5)) }, back)
            }
            "Wavy", "Mermaid" -> {
                val end = if (style == "Wavy") 68 else 92
                px.fill(10, 18, 46, end, { x, y -> abs(x - CX) <= 15 + tri(y / 3, 2) - maxOf(0, y - (end - 4)) * 2 }) { x, y ->
                    if (style == "Mermaid" && Math.floorMod(x + y / 5, 9) == 0) lt(c, 0.35f) else back(x, y)
                }
            }
            "Side swept" -> px.fill(13, 18, 43, 52, { x, y -> abs(x - CX) <= 15 - maxOf(0, y - 48) }, back)
            "Shag" -> px.fill(12, 18, 44, 54, { x, y -> abs(x - CX) <= 16 && y <= 46 + Math.floorMod(x * 3, 8) }, back)
            "Bob" -> px.fill(13, 18, 43, 42, { x, y -> abs(x - CX) <= 15 }, back)
            "Mullet" -> px.fill(15, 20, 41, 54, { x, y -> abs(x - CX) <= 13 - maxOf(0, y - 50) }, back)
            "Curls" -> {
                val bumps = listOf(14 to 22, 12 to 32, 13 to 42, 17 to 50, 23 to 54, 33 to 54, 39 to 50, 43 to 42, 44 to 32, 42 to 22, 20 to 13, 28 to 10, 36 to 13)
                for ((bx, by) in bumps) px.fill(bx - 7, by - 7, bx + 7, by + 7, { x, y -> inE(x, y, bx, by, 6, 6) }) { x, y -> if (inE(x, y, bx - 2, by - 2, 2, 2)) lt(c, 0.2f) else hc(x, y) }
            }
            "Afro" -> px.fill(6, 0, 50, 44, { x, y -> inE(x, y, CX, 22, 21, 21) && y <= 42 }) { x, y ->
                if (Math.floorMod(x * 7 + y * 3, 9) == 0) dk(c, 0.75f) else if (x < 20 && y < 20) lt(c, 0.15f) else if (x > 40) dk(c, 0.8f) else c
            }
            "Beehive" -> if (!covered) px.fill(14, 0, 42, 22, { x, y -> inE(x, y, CX, 11, 13, 12) }, hc)
            "Ponytail" -> {
                px.fill(34, 14, 52, 58, { x, y -> y >= 16 && abs(x - (40 + tri((y - 16) / 4, 3) + (y - 16) / 6)) <= 4 - maxOf(0, y - 52) }, hc)
                px.rect(36, 20, 7, 3, dk(c, 0.5f))
            }
            "Pigtails" -> for (s in listOf(-1, 1)) {
                val bx = CX + s * 15
                px.fill(bx - 8, 24, bx + 8, 58, { x, y -> abs(x - (bx + s * (y - 26) / 5)) <= 4 - maxOf(0, y - 52) }, hc)
                px.rect(bx - 3 + s, 28, 6, 3, 0xFFFF6AA0.toInt())
            }
            "Dreads" -> for (sx in 13..43 step 3) px.fill(sx, 18, sx + 1, 58 + Math.floorMod(sx * 7, 6), { _, _ -> true }) { x, y -> if (y % 9 == 0) GOLD else if (x == sx) c else d }
        }
    }

    private fun frontHair(px: Px, style: String, c: Int, skin: Int, covered: Boolean, hatted: Boolean) {
        val hc = hairShade(c)
        val skull = { x: Int, y: Int -> inE(x, y, CX, 25, 13, 13) }
        fun cap(line: (Int) -> Int, color: (Int, Int) -> Int = hc) = px.fill(14, 10, 42, 40, { x, y -> skull(x, y) && y < line(x) }, color)
        fun locks(end: Int, wave: Boolean = false) {
            for (s in listOf(-1, 1)) {
                val x0 = if (s < 0) 14 else 38
                px.fill(x0 - 2, 20, x0 + 6, end, { x, y ->
                    val o = if (wave) tri(y / 3, 2) - 1 else 0
                    x - o in x0..x0 + 4 - maxOf(0, y - end + 3) && y >= 20
                }, hc)
            }
        }
        val parted: (Int) -> Int = { x -> 17 + (x - CX) * (x - CX) / 12 }
        val pulled: (Int) -> Int = { x -> 16 + (x - CX) * (x - CX) / 14 }
        when (style) {
            "Short" -> { cap({ x -> 18 + (x - CX) * (x - CX) / 16 }); px.fill(19, 16, 30, 20, { x, y -> y == 18 + (x - 19) / 4 && x < 30 }) { _, _ -> dk(c, 0.7f) } }
            "Long" -> { cap(parted); locks(58); px.set(CX, 13, dk(c, 0.6f)); px.set(CX, 14, dk(c, 0.6f)) }
            "Wavy" -> { cap(parted); locks(62, wave = true) }
            "Mermaid" -> { cap(parted); locks(80, wave = true) }
            "Bangs" -> { cap({ x -> if (abs(x - CX) <= 10) 23 + (x % 3) / 2 else 30 }); locks(54) }
            "Bob" -> {
                cap({ x -> if (abs(x - CX) <= 10) 22 else 44 })
                locks(40)
                px.fill(13, 39, 43, 42, { x, y -> abs(x - CX) in 11..15 && y <= 41 }) { _, _ -> dk(c, 0.75f) }
            }
            "Side swept" -> { cap({ x -> 16 + (x - 14) * 10 / 28 + (x - CX) * (x - CX) / 30 }); locks(50) }
            "Shag" -> { cap({ x -> 21 + Math.floorMod(x * 5, 4) + if (abs(x - CX) > 9) 10 else 0 }); locks(46) }
            "Pixie" -> { cap({ x -> 17 + (x - 16) / 4 + (x - CX) * (x - CX) / 20 }); px.set(38, 22, c); px.set(17, 23, c) }
            "Bun", "Ponytail" -> {
                cap(pulled)
                if (style == "Bun" && !covered) px.fill(20, 0, 36, 14, { x, y -> inE(x, y, CX, 8, 6, 6) }) { x, y -> if (inE(x, y, 26, 6, 2, 2)) lt(c, 0.3f) else if (Math.floorMod(x + y, 4) == 0) dk(c, 0.8f) else c }
            }
            "Top knot" -> {
                cap(pulled)
                if (!covered) { px.fill(22, 2, 34, 12, { x, y -> inE(x, y, CX, 7, 4, 4) }, hc); px.rect(25, 11, 7, 2, dk(c, 0.55f)) }
            }
            "Space buns" -> {
                cap(parted)
                if (!covered) for (bx in listOf(17, 39)) px.fill(bx - 7, 4, bx + 7, 20, { x, y -> inE(x, y, bx, 12, 6, 6) }) { x, y -> if (inE(x, y, bx - 2, 10, 2, 2)) lt(c, 0.3f) else hc(x, y) }
            }
            "Pigtails", "Braids" -> {
                cap(parted)
                if (style == "Braids") for (s in listOf(-1, 1)) {
                    val bx = CX + s * 11
                    px.fill(bx - 4, 30, bx + 4, 72, { x, y -> abs(x - bx) <= 3 - (if (y > 68) 1 else 0) }) { x, y ->
                        val r = Math.floorMod(y, 6); val side = if (Math.floorMod(y / 6, 2) == 0) x < bx else x > bx
                        if (r == 0) dk(c, 0.6f) else if (side) lt(c, 0.2f) else c
                    }
                    px.rect(bx - 2, 70, 5, 2, 0xFFFF6AA0.toInt())
                }
            }
            "Curls" -> cap({ x -> 20 + Math.floorMod(x * 3, 5) - if (abs(x - CX) > 10) 0 else 1 + if (abs(x - CX) > 9) 10 else 0 })
            "Afro" -> cap({ x -> 18 + (x - CX) * (x - CX) / 20 })
            "Beehive" -> { cap(pulled); px.fill(14, 14, 42, 18, { x, y -> skull(x, y) && y < pulled(x) }, hc) }
            "Mullet" -> { cap({ x -> 18 + (x - CX) * (x - CX) / 16 }) }
            "Dreads" -> {
                cap(parted)
                for (sx in listOf(14, 17, 39, 42)) px.fill(sx, 22, sx + 1, 52, { _, _ -> true }) { x, y -> if (y % 9 == 0) GOLD else if (x == sx) c else dk(c, 0.7f) }
            }
            "Mohawk" -> {
                val fuzz = mix(skin, c, 0.35f)
                cap({ x -> 19 + (x - CX) * (x - CX) / 16 }) { _, _ -> fuzz }
                if (!hatted) px.fill(23, 0, 33, 22, { x, y -> abs(x - CX) <= 4 - maxOf(0, (12 - y) / 4) && y >= 2 }) { x, y -> if (x <= 25) lt(c, 0.3f) else if (x >= 31) dk(c, 0.7f) else c }
                else px.fill(14, 10, 42, 22, { x, y -> skull(x, y) && y < 18 && abs(x - CX) <= 4 }, hc)
            }
            "Undercut" -> {
                val fuzz = mix(skin, c, 0.4f)
                cap({ x -> 19 + (x - CX) * (x - CX) / 16 }) { _, _ -> fuzz }
                px.fill(14, 8, 42, 24, { x, y -> inE(x, y, CX - 1, 16, 11, 7) && y < 22 - (x - 18) / 5 }, hc)
            }
            "Buzz" -> {
                val fuzz = mix(skin, c, 0.6f)
                cap({ x -> 17 + (x - CX) * (x - CX) / 16 }) { x, y -> if (Math.floorMod(x + y, 3) == 0) dk(fuzz, 0.9f) else fuzz }
            }
            "Bald" -> { px.tint(23, 16, 3, 2, WHITE, 0.4f) }
        }
    }

    // ---- Legs, bottoms and shoes ----

    private fun legs(px: Px, look: Farmer, b: Body, step: Boolean, skin: Int, outfit: String) {
        val pants = Farmer.CLOTH[look.pants]
        val dress = outfit in Farmer.DRESSES
        val bottom = if (dress) "" else Farmer.BOTTOMS[look.bottom]
        val lift = if (step) 5 else 0; val swing = if (step) 3 else 0
        val skirt = bottom == "Skirt" || bottom == "Mini skirt"
        val pantsTo = when (bottom) { "Jeans", "Cargo pants", "Leggings" -> ANKLE; "Shorts" -> 82; else -> 0 }
        val cloth = if (bottom == "Leggings") dk(pants, 0.8f) else pants
        val legs = listOf(
            intArrayOf(CX - b.hi, HIP, CX - 1, HIP, CX - 2 - swing, ANKLE - lift, CX - 8 - swing, ANKLE - lift),
            intArrayOf(CX + 1, HIP, CX + b.hi, HIP, CX + 8, ANKLE, CX + 2, ANKLE),
        )
        val narrow = if (bottom == "Leggings" || !(pantsTo > 0)) 1 else 0
        for ((i, pts) in legs.withIndex()) {
            val p = pts.copyOf()
            if (narrow > 0) { p[0] += 1; p[2] -= 1; p[4] -= 0; p[6] += 0 }
            px.poly(p) { _, y -> if (y <= pantsTo) cloth else skin }
            val inner = if (i == 0) CX - 1 else CX + 1
            if (bottom == "Jeans") px.line(inner + (if (i == 0) -3 else 3), HIP + 4, inner + (if (i == 0) -4 else 4), ANKLE - 3, lt(pants, 0.15f))
            if (bottom == "Cargo pants") {
                val ox = if (i == 0) CX - b.hi + 1 else CX + b.hi - 5
                px.rect(ox, 80, 4, 6, dk(pants, 0.85f)); px.rect(ox, 80, 4, 1, dk(pants, 0.65f))
            }
            if (bottom == "Leggings") px.line(inner + (if (i == 0) -5 else 5), HIP + 4, inner + (if (i == 0) -6 else 6), ANKLE - 4, lt(cloth, 0.25f))
        }
        px.round(HIP, ANKLE, cloth); px.round(HIP, ANKLE, skin)
        if (bottom == "Shorts") px.fill(0, 81, W - 1, 82, { x, y -> px.get(x, y) != 0 && px.get(x, y) != skin }) { _, _ -> dk(pants, 0.7f) }
        if (bottom == "Jeans") px.fill(0, ANKLE - 2, W - 1, ANKLE, { x, y -> px.get(x, y) != 0 }) { _, _ -> lt(pants, 0.12f) }
        // Knees on bare legs.
        if (pantsTo < 84) { px.set(CX - 6 - swing / 2, 86 - lift / 2, dk(skin, 0.86f)); px.set(CX + 5, 86, dk(skin, 0.86f)) }
        if (skirt) {
            val end = if (bottom == "Skirt") 88 else 80
            px.poly(intArrayOf(CX - b.wa - 1, HIP - 4, CX + b.wa + 1, HIP - 4, CX + b.hi + 5, end, CX - b.hi - 5, end)) { x, y ->
                when { y >= end - 1 -> lt(pants, 0.2f); Math.floorMod(x - CX, 5) == 0 && y > HIP -> dk(pants, 0.8f); else -> pants }
            }
            px.round(HIP - 4, end, pants)
        }
        // Feet.
        val sc = Farmer.CLOTH[look.shoeColor]
        val shoe = Farmer.SHOES[look.shoes]
        val tall = pantsTo < ANKLE || bottom == "Leggings"
        for ((i, pts) in legs.withIndex()) {
            val ax0 = pts[6]; val ax1 = pts[4]; val ay = pts[5]
            foot(px, shoe, sc, skin, ax0 - 1, ax1 + 1, ay, tall, i == 0)
        }
    }

    private fun foot(px: Px, shoe: String, c: Int, skin: Int, x0: Int, x1: Int, y: Int, tall: Boolean, left: Boolean) {
        val l = lt(c, 0.25f); val d = dk(c, 0.7f)
        val toe = if (left) -1 else 1
        fun sole(col: Int) = px.rect(x0 - (if (left) 1 else 0), y + 5, x1 - x0 + 2, 2, col)
        fun upper(from: Int, col: Int) = px.fill(x0 - 1, from, x1 + 1, y + 5, { x, yy -> x in x0..x1 || (yy >= y + 2 && x == (if (left) x0 - 1 else x1 + 1)) }) { x, _ ->
            when { x <= x0 + 1 -> lt(col, 0.2f); x >= x1 - 1 -> dk(col, 0.78f); else -> col }
        }
        when (shoe) {
            "Boots" -> {
                upper(if (tall) y - 8 else y, c); sole(0xFF2A1E18.toInt())
                for (k in 0..2) px.set((x0 + x1) / 2, y - 1 + k * 2 - (if (tall) 4 else 0), l)
            }
            "Sneakers" -> {
                upper(y, c); sole(0xFFF4F2EC.toInt()); px.rect(x0, y + 4, x1 - x0 + 1, 1, 0xFFDAD6CC.toInt())
                px.rect(x0 + 1, y + 1, x1 - x0 - 1, 1, WHITE); px.set(x0 + 2, y + 3, l); px.set(x1 - 2, y + 3, d)
            }
            "Sandals" -> {
                upper(y, skin); sole(0xFF8A5A30.toInt())
                px.rect(x0, y + 1, x1 - x0 + 1, 1, c); px.rect(x0, y + 3, x1 - x0 + 1, 1, c)
                px.set(x0 + 1 + (if (left) 0 else x1 - x0 - 2), y + 4, mix(skin, 0xFFE87A9A.toInt(), 0.4f))
            }
            "Cowboy boots" -> {
                upper(if (tall) y - 10 else y, c); sole(0xFF3A2418.toInt())
                px.set(if (left) x0 - 2 else x1 + 2, y + 5, d)
                if (tall) { px.line(x0 + 1, y - 8, (x0 + x1) / 2, y - 4, l); px.line((x0 + x1) / 2, y - 4, x1 - 1, y - 8, l) }
                px.rect(x0, y, x1 - x0 + 1, 1, d)
            }
            "Wellies" -> {
                upper(if (tall) y - 14 else y, c); sole(dk(c, 0.45f))
                px.line(x0 + 1, if (tall) y - 12 else y + 1, x0 + 1, y + 3, lt(c, 0.5f))
                if (tall) px.rect(x0, y - 14, x1 - x0 + 1, 1, d)
            }
        }
        px.set(if (left) x0 else x1, y + 3 + toe * 0, 0)
    }

    // ---- Torso and outfits ----

    private fun torsoPts(b: Body, top: Int = TOP, bottom: Int = HIP + 2) = intArrayOf(
        CX - b.sh + 3, top, CX + b.sh - 3, top, CX + b.sh, top + 4, CX + b.wa, WAIST, CX + b.hi, bottom, CX - b.hi, bottom, CX - b.wa, WAIST, CX - b.sh, top + 4,
    )

    private fun torso(px: Px, look: Farmer, b: Body, skin: Int, outfit: String) {
        val shirt = Farmer.CLOTH[look.shirt]; val pants = Farmer.CLOTH[look.pants]
        val sD = dk(shirt, 0.72f); val sL = lt(shirt, 0.22f)
        val bottom = Farmer.BOTTOMS[look.bottom]
        val dress = outfit in Farmer.DRESSES
        val pts = torsoPts(b)
        val tie = intArrayOf(0xFFFF5A5A.toInt(), 0xFFFFB03A.toInt(), 0xFFF4E04A.toInt(), 0xFF5AD06A.toInt(), 0xFF4AA0F0.toInt(), 0xFFB060E0.toInt())
        val pattern: (Int, Int) -> Int = { x, y ->
            when (outfit) {
                "Flannel" -> {
                    val vx = Math.floorMod(x, 7) < 2; val hy = Math.floorMod(y, 7) < 2
                    if (vx && hy) dk(shirt, 0.5f) else if (vx || hy) sD else if (Math.floorMod(x, 7) == 4) sL else shirt
                }
                "Tie-dye" -> tie[Math.floorMod(((x - CX) * (x - CX) + (y - 58) * (y - 58)) / 18 + look.shirt, 6)]
                "Sweater" -> if (Math.floorMod(x, 3) == 0) dk(shirt, 0.88f) else shirt
                "Puffer" -> if (Math.floorMod(y - TOP, 6) == 5) dk(shirt, 0.7f) else if (Math.floorMod(y - TOP, 6) == 0) sL else shirt
                "Leather jacket" -> 0xFF2A2428.toInt()
                "Lab coat" -> 0xFFF2F2EE.toInt()
                "Kimono" -> if (Math.floorMod(x * 3 + y * 5, 23) == 0) lt(pants, 0.3f) else shirt
                "Sundress" -> if (Math.floorMod(x + y * 2, 9) == 0) lt(shirt, 0.5f) else shirt
                else -> shirt
            }
        }
        val base = pattern(CX, 60)
        // Skin first for the tops that leave shoulders or a midriff bare.
        px.poly(pts) { _, _ -> skin }
        px.round(TOP, HIP, skin)
        when (outfit) {
            "Tank top", "Sundress" -> {
                val strap = if (outfit == "Sundress") 1 else 2
                px.poly(pts) { x, y -> if (y >= TOP + 8 || abs(abs(x - CX) - (b.sh - 5)) < strap) pattern(x, y) else 0 }
                px.fill(0, TOP + 8, W - 1, TOP + 10, { x, y -> px.get(x, y) == pattern(x, y) && y == TOP + 8 + abs(x - CX) / 6 - 1 }) { x, y -> skin }
            }
            "Halter" -> px.poly(pts) { x, y -> if (y >= TOP + 9 || abs(x - CX) <= (TOP + 9 - y) / 2 + 1 && abs(x - CX) >= (TOP + 9 - y) / 2 - 2) pattern(x, y) else 0 }
            "Off-shoulder" -> {
                px.poly(pts) { x, y -> if (y >= TOP + 6) pattern(x, y) else 0 }
                px.fill(0, TOP + 6, W - 1, TOP + 7, { x, y -> px.get(x, y) == shirt }) { x, _ -> if (x % 3 == 0) sD else sL }
            }
            "Crop top" -> {
                px.poly(pts) { x, y -> if (y <= 60) pattern(x, y) else 0 }
                px.rect(CX - b.wa, 60, b.wa * 2 + 1, 1, sD)
                px.set(CX, 66, dk(skin, 0.7f)); px.set(CX, 67, dk(skin, 0.8f))
            }
            "Peasant top" -> px.poly(pts) { x, y -> if (inE(x, y, CX, TOP, 9, 4)) 0 else pattern(x, y) }
            else -> px.poly(pts) { x, y -> pattern(x, y) }
        }
        if (outfit !in setOf("Flannel", "Tie-dye", "Sweater", "Puffer", "Kimono")) px.round(TOP, HIP + 2, base)
        // A soft line for the bust on a feminine frame.
        if (b.fem && outfit != "Puffer") {
            for (s in listOf(-1, 1)) for (k in 0..3) px.set(CX + s * (2 + k), 56 + (if (k == 0 || k == 3) 0 else 1), dk(px.get(CX + s * (2 + k), 56).takeIf { it != 0 } ?: base, 0.86f))
        }
        val neck = dk(skin, 0.86f)
        when (outfit) {
            "Tee", "Dress", "Jersey" -> {
                px.fill(CX - 5, TOP, CX + 5, TOP + 5, { x, y -> inE(x, y, CX, TOP, 4, 3) }) { _, _ -> neck }
                px.fill(CX - 6, TOP, CX + 6, TOP + 5, { x, y -> !inE(x, y, CX, TOP, 4, 3) && inE(x, y, CX, TOP, 5, 4) }) { _, _ -> sD }
                if (outfit == "Jersey") {
                    val n = Farmer.CLOTH[(look.shirt + 6) % Farmer.CLOTH.size]
                    px.map(CX - 3, 53, arrayOf("XXXXXX", "....XX", "...XX.", "..XX..", "..XX..", "..XX.."), mapOf('X' to n))
                    px.rect(CX - b.sh + 1, TOP + 3, 3, 2, n); px.rect(CX + b.sh - 3, TOP + 3, 3, 2, n)
                }
            }
            "Overalls" -> {
                px.fill(CX - 5, TOP, CX + 5, TOP + 5, { x, y -> inE(x, y, CX, TOP, 4, 3) }) { _, _ -> neck }
                px.poly(intArrayOf(CX - 7, 52, CX + 7, 52, CX + b.wa, WAIST, CX + b.hi, HIP + 2, CX - b.hi, HIP + 2, CX - b.wa, WAIST)) { x, _ -> when { x <= CX - 6 -> lt(pants, 0.15f); x >= CX + 6 -> dk(pants, 0.8f); else -> pants } }
                px.line(CX - 6, 52, CX - 8, TOP, pants); px.line(CX + 6, 52, CX + 8, TOP, dk(pants, 0.85f))
                px.rect(CX - 6, 53, 2, 2, GOLD); px.rect(CX + 5, 53, 2, 2, GOLD)
                px.rect(CX - 3, 56, 7, 5, dk(pants, 0.85f))
            }
            "Hoodie" -> {
                px.fill(CX - 8, TOP - 2, CX + 8, TOP + 4, { x, y -> inE(x, y, CX, TOP, 8, 4) && !inE(x, y, CX, TOP, 4, 2) }) { x, _ -> if (x > CX + 3) sD else dk(shirt, 0.85f) }
                px.line(CX - 2, TOP + 3, CX - 2, TOP + 10, 0xFFF4F4F0.toInt()); px.line(CX + 2, TOP + 3, CX + 2, TOP + 9, 0xFFE8E4DC.toInt())
                px.poly(dk(shirt, 0.86f), CX - 7, 60, CX + 7, 60, CX + 9, 68, CX - 9, 68)
                px.line(CX - 7, 60, CX + 7, 60, sD)
            }
            "Flannel", "Denim jacket", "Cardigan", "Leather jacket", "Lab coat" -> {
                val inner = when (outfit) { "Leather jacket" -> shirt; "Lab coat" -> shirt; else -> 0xFFF0EEE6.toInt() }
                val jacket = pattern(CX, 50)
                px.poly(intArrayOf(CX - 3, TOP, CX + 3, TOP, CX + 3, HIP + 2, CX - 3, HIP + 2)) { x, _ -> if (x >= CX + 2) dk(inner, 0.85f) else inner }
                px.fill(CX - 4, TOP, CX + 4, TOP + 3, { x, y -> inE(x, y, CX, TOP, 3, 2) }) { _, _ -> neck }
                // Lapels.
                px.poly(intArrayOf(CX - 7, TOP, CX - 3, TOP, CX - 3, TOP + 10)) { _, _ -> dk(jacket, 0.82f) }
                px.poly(intArrayOf(CX + 3, TOP, CX + 7, TOP, CX + 3, TOP + 10)) { _, _ -> dk(jacket, 0.65f) }
                px.line(CX - 4, TOP + 10, CX - 4, HIP + 1, dk(jacket, 0.6f)); px.line(CX + 4, TOP + 10, CX + 4, HIP + 1, dk(jacket, 0.6f))
                when (outfit) {
                    "Denim jacket" -> { px.rect(CX - 10, 52, 5, 4, dk(jacket, 0.85f)); px.rect(CX + 6, 52, 5, 4, dk(jacket, 0.8f)); px.line(CX - b.wa, 66, CX + b.wa, 66, lt(jacket, 0.3f)) }
                    "Cardigan" -> for (k in 0..3) px.set(CX - 5, 52 + k * 5, lt(jacket, 0.5f))
                    "Leather jacket" -> { px.line(CX - 4, TOP + 10, CX - 4, HIP, 0xFFB8BCC4.toInt()); px.line(CX - 10, 50, CX - 6, 58, 0xFF4A4448.toInt()) }
                    "Lab coat" -> { px.rect(CX - 10, 62, 5, 5, 0xFFDCDCD6.toInt()); px.rect(CX + 6, 50, 4, 1, 0xFF3A6AC0.toInt()) }
                    "Flannel" -> {}
                }
            }
            "Blouse", "Polo" -> {
                px.fill(CX - 4, TOP, CX + 4, TOP + 4, { x, y -> inE(x, y, CX, TOP, 3, 3) }) { _, _ -> neck }
                px.poly(lt(shirt, 0.35f), CX - 7, TOP, CX - 1, TOP + 2, CX - 3, TOP + 5)
                px.poly(lt(shirt, 0.1f), CX + 1, TOP + 2, CX + 7, TOP, CX + 3, TOP + 5)
                for (k in 0 until if (outfit == "Polo") 2 else 4) px.set(CX, TOP + 6 + k * 5, lt(shirt, 0.55f))
                if (outfit == "Polo") px.rect(CX - 9, 53, 2, 2, Farmer.CLOTH[(look.shirt + 3) % Farmer.CLOTH.size])
            }
            "Sweater" -> {
                px.fill(CX - 6, TOP, CX + 6, TOP + 4, { x, y -> inE(x, y, CX, TOP, 5, 3) }) { x, y -> if (inE(x, y, CX, TOP, 3, 2)) neck else dk(shirt, 0.82f) }
                px.fill(0, HIP - 1, W - 1, HIP + 2, { x, y -> px.get(x, y) != 0 && abs(x - CX) <= b.hi }) { x, _ -> if (x % 2 == 0) dk(shirt, 0.8f) else dk(shirt, 0.92f) }
            }
            "Kimono" -> {
                px.poly(intArrayOf(CX - 6, TOP, CX + 6, TOP, CX, 58)) { _, _ -> neck }
                px.line(CX - 6, TOP, CX + 1, 60, lt(shirt, 0.4f)); px.line(CX + 6, TOP, CX, 58, dk(shirt, 0.7f))
                px.fill(0, 61, W - 1, 66, { x, y -> px.get(x, y) != 0 && abs(x - CX) <= b.wa + 2 }) { x, y -> if (y == 61) lt(pants, 0.2f) else if (x == CX + 4) dk(pants, 0.7f) else pants }
            }
            "Puffer" -> px.fill(CX - 6, TOP - 2, CX + 6, TOP + 3, { x, y -> abs(x - CX) <= 5 && !(abs(x - CX) <= 2 && y >= TOP) }) { x, _ -> if (x > CX) sD else shirt }
            "Peasant top" -> px.fill(0, TOP, W - 1, TOP + 6, { x, y -> !inE(x, y, CX, TOP, 9, 4) && inE(x, y, CX, TOP, 10, 5) && px.get(x, y) != 0 }) { x, _ -> if (x % 3 == 0) pants else sL }
            "Tie-dye" -> px.fill(CX - 5, TOP, CX + 5, TOP + 5, { x, y -> inE(x, y, CX, TOP, 4, 3) }) { _, _ -> neck }
        }
        // Waistband for anything with legs showing below a top.
        if (!dress && outfit != "Overalls" && outfit != "Lab coat" && bottom != "Skirt" && bottom != "Mini skirt") {
            val belt = if (bottom == "Leggings") dk(pants, 0.7f) else dk(pants, 0.85f)
            px.poly(intArrayOf(CX - b.hi, HIP - 1, CX + b.hi, HIP - 1, CX + b.hi, HIP + 2, CX - b.hi, HIP + 2)) { x, _ -> if (x >= CX + b.hi - 2) dk(belt, 0.8f) else belt }
            if (bottom == "Jeans" || bottom == "Cargo pants") px.rect(CX - 1, HIP - 1, 2, 2, GOLD)
        }
        if (dress) {
            val end = if (outfit == "Sundress") 86 else 90
            px.poly(intArrayOf(CX - b.wa, WAIST, CX + b.wa, WAIST, CX + b.hi + 7, end, CX - b.hi - 7, end)) { x, y ->
                when { y >= end - 1 -> sD; Math.floorMod(x - CX + (y - WAIST) / 6, 7) == 0 && y > WAIST + 4 -> dk(pattern(x, y), 0.86f); else -> pattern(x, y) }
            }
            px.round(WAIST, end, base)
            px.rect(CX - b.wa, WAIST - 1, b.wa * 2 + 1, 2, if (outfit == "Dress") sL else dk(shirt, 0.8f))
        }
        if (outfit == "Lab coat") {
            val coat = 0xFFF2F2EE.toInt()
            for (s in listOf(-1, 1)) px.poly(intArrayOf(CX + s * 4, HIP, CX + s * (b.hi + 1), HIP, CX + s * (b.hi + 3), 86, CX + s * 4, 86)) { x, _ -> if (s > 0 && x >= CX + b.hi) dk(coat, 0.8f) else if (s > 0) dk(coat, 0.93f) else coat }
        }
    }

    // ---- Arms ----

    private fun arms(px: Px, look: Farmer, b: Body, step: Boolean, skin: Int, outfit: String) {
        val shirt = when (outfit) { "Leather jacket" -> 0xFF2A2428.toInt(); "Lab coat" -> 0xFFF2F2EE.toInt(); else -> Farmer.CLOTH[look.shirt] }
        val sl = sleeve(outfit)
        val stripe = if (outfit == "Jersey") Farmer.CLOTH[(look.shirt + 6) % Farmer.CLOTH.size] else 0
        for (s in listOf(-1, 1)) {
            val drop = if (step && s < 0) 3 else if (step) -1 else 0
            val sx = CX + s * b.sh
            val wrist = 66 + drop
            val sleeveTo = when (sl) { Sleeve.NONE -> TOP; Sleeve.SHORT -> TOP + 11; else -> wrist - 1 }
            val start = if (outfit == "Off-shoulder") TOP + 5 else TOP
            val wide = sl == Sleeve.PUFFY
            val pts = intArrayOf(sx - 2 * s, TOP, sx + 3 * s, TOP + 3, sx + 3 * s + (if (wide) s else 0), wrist, sx - 2 * s, wrist)
            px.poly(pts) { x, y ->
                val inner = if (s < 0) x == minOf(pts[0], pts[4]) else x == maxOf(pts[0], pts[4])
                when {
                    y < start -> skin
                    y <= sleeveTo -> when {
                        outfit == "Tie-dye" -> intArrayOf(0xFFFF5A5A.toInt(), 0xFFF4E04A.toInt(), 0xFF4AA0F0.toInt())[Math.floorMod(y / 3, 3)]
                        stripe != 0 && y in TOP + 7..TOP + 8 -> stripe
                        y >= sleeveTo - 1 && sl != Sleeve.NONE -> dk(shirt, 0.8f)
                        else -> shirt
                    }
                    else -> skin
                }
            }
            // Shade the arm: lit on the outside of the left arm, dark on the right.
            val ax0 = minOf(pts[0], pts[2], pts[4], pts[6]); val ax1 = maxOf(pts[0], pts[2], pts[4], pts[6])
            for (y in TOP..wrist) {
                val row = (ax0..ax1).filter { px.get(it, y) != 0 }
                if (row.size < 3) continue
                val edge = if (s < 0) row.first() else row.last()
                px.set(edge, y, if (s < 0) lt(px.get(edge, y), 0.15f) else dk(px.get(edge, y), 0.8f))
            }
            // Hand.
            val hx = (pts[4] + pts[6]) / 2
            val glove = look.has(7)
            val hand = if (glove) 0xFFC8A060.toInt() else skin
            px.fill(hx - 3, wrist, hx + 3, wrist + 6, { x, y -> inE(x, y, hx, wrist + 3, 2, 3) }) { x, _ -> if (x > hx) dk(hand, 0.85f) else hand }
            if (glove) px.rect(hx - 2, wrist, 5, 1, dk(hand, 0.7f))
            if (look.has(5)) { px.rect(hx - 3, wrist - 2, 6, 1, GOLD); px.set(hx - 2, wrist - 1, 0xFF4AB8C0.toInt()); px.set(hx, wrist - 1, 0xFFE87A9A.toInt()); px.set(hx + 2, wrist - 1, 0xFFE8C860.toInt()) }
            if (look.has(6)) {
                val ink = 0xFF2A4A5A.toInt()
                for (y in TOP + 2 until wrist - 2) {
                    val x = (pts[0] + pts[6]) / 2 + s + tri(y / 2, 2) - 1
                    if (px.get(x, y) == skin || px.get(x, y) == lt(skin, 0.15f) || px.get(x, y) == dk(skin, 0.8f)) px.set(x, y, ink)
                    if (y % 6 == 0 && px.get(x + 1, y) == skin) px.set(x + 1, y, 0xFF3A7A4A.toInt())
                }
            }
        }
    }

    private fun necklace(px: Px) {
        for (x in CX - 5..CX + 5) px.set(x, TOP + 2 + (5 * 5 - (x - CX) * (x - CX)) / 7, GOLD)
        px.set(CX, TOP + 6, 0xFF5AC03A.toInt()); px.set(CX - 1, TOP + 6, 0xFF3A8A2A.toInt()); px.set(CX + 1, TOP + 6, 0xFF3A8A2A.toInt()); px.set(CX, TOP + 7, 0xFF3A8A2A.toInt())
    }

    // ---- Head and face ----

    private fun head(px: Px, look: Farmer, b: Body, skin: Int, hairC: Int) {
        val s = dk(skin, 0.86f); val d = dk(skin, 0.7f); val l = lt(skin, 0.14f)
        // Neck with a shadow under the chin.
        px.poly(intArrayOf(CX - 4, 36, CX + 4, 36, CX + 4, TOP + 1, CX - 4, TOP + 1)) { x, y -> if (x >= CX + 2 || y <= 42) s else skin }
        // Ears.
        for (sx in listOf(-1, 1)) {
            val ex = CX + sx * 12
            px.fill(ex - 3, 22, ex + 3, 34, { x, y -> inE(x, y, ex, 28, 2, 4) }) { _, _ -> if (sx > 0) s else skin }
            px.set(ex, 27, d); px.set(ex, 28, d)
        }
        // Face: a round crown narrowing to the chin; [Body.chin] is the curve power, higher is squarer.
        px.fill(14, 12, 42, 42, { x, y ->
            if (y <= 28) inE(x, y, CX, 26, 11, 13)
            else { val t = (y - 28) / 13.5f; t <= 1f && abs(x - CX) <= 11.5f * (1f - t.pow(b.chin)).pow(1f / b.chin) }
        }) { x, y ->
            when {
                x >= 36 || (y >= 38 && x >= 31) -> s
                x <= 20 && y in 25..33 -> l
                else -> skin
            }
        }
        val eyeY = 27
        // Eyeshadow under everything else on the lids.
        val shadow = Farmer.SHADOWS[look.shadow]
        if (shadow != 0) { px.tint(23, eyeY - 1, 4, 2, shadow, 0.55f); px.tint(33, eyeY - 1, 4, 2, shadow, 0.55f) }
        // Blush, freckles, a beauty mark.
        if (look.blush == 1) { px.tint(20, 33, 3, 2, 0xFFF06A86.toInt(), 0.35f); px.tint(36, 33, 3, 2, 0xFFF06A86.toInt(), 0.35f) }
        if (look.freckles == 1) for ((fx, fy) in listOf(19 to 31, 21 to 32, 20 to 34, 23 to 32, 33 to 32, 35 to 31, 36 to 33, 37 to 32, 27 to 31, 30 to 31)) px.set(fx, fy, dk(skin, 0.72f))
        if (look.mole == 1) px.set(33, 37, 0xFF3A2418.toInt())
        eyes(px, look, eyeY, skin)
        // Brows.
        val bc = dk(hairC, 0.8f)
        val by = eyeY - 4
        for (sx in listOf(-1, 1)) {
            val x0 = if (sx < 0) 19 else 31
            val rows = when (Farmer.BROWS[look.brows]) {
                "Thin" -> listOf(intArrayOf(1, 1, 0, 0, 0, 1))
                "Bold" -> listOf(intArrayOf(1, 0, 0, 0, 0, 1), intArrayOf(0, 1, 1, 1, 1, 1))
                "Arched" -> listOf(intArrayOf(1, 1, 0, 0, 1, 2))
                else -> listOf(intArrayOf(1, 0, 0, 0, 0, 1))
            }
            for ((r, row) in rows.withIndex()) for (i in 0..5) {
                val xi = if (sx < 0) x0 + i else x0 + 5 - i
                if (rows.size == 1 || r == 0) px.set(xi, by + row[i] + r, bc) else if (row[i] == 1) px.set(xi, by + 1 + r, bc)
            }
        }
        // Nose.
        px.set(29, 30, s); px.set(29, 31, s); px.set(27, 32, s); px.set(28, 33, d); px.set(30, 33, d); px.set(27, 30, l)
        // Mouth: a small smile, or coloured lips.
        val lip = Farmer.LIPS[look.lips]
        if (lip == 0) {
            for (x in 26..30) px.set(x, 36, dk(skin, 0.6f))
            px.set(25, 35, dk(skin, 0.7f)); px.set(31, 35, dk(skin, 0.7f)); px.set(28, 37, dk(skin, 0.82f))
        } else {
            for (x in 25..31) px.set(x, 35, if (x == 28) dk(lip, 0.8f) else lip)
            for (x in 26..30) px.set(x, 36, dk(lip, 0.65f))
            for (x in 26..30) px.set(x, 37, lip)
            px.set(27, 37, lt(lip, 0.45f))
        }
        // Facial hair and eyewear.
        val hc = hairC; val hd = dk(hairC, 0.72f)
        when (Farmer.FACES[look.face]) {
            "Beard" -> px.fill(15, 30, 41, 43, { x, y -> px.get(x, y) != 0 && y >= 33 && abs(x - CX) <= 12 && !(y in 35..37 && abs(x - CX) <= 3 && y != 34) }) { x, y ->
                if (Math.floorMod(x * 3 + y, 5) == 0) hd else if (x > 34) hd else hc
            }
            "Moustache" -> { for (x in 24..32) px.set(x, 34, hc); for (x in 23..25) px.set(x, 35, hc); for (x in 31..33) px.set(x, 35, hd); px.set(28, 34, hd) }
            "Goatee" -> { for (x in 25..31) px.set(x, 34, hc); px.fill(24, 38, 32, 42, { x, y -> abs(x - CX) <= 3 - (y - 38) / 3 && px.get(x, y) != 0 }) { x, _ -> if (x > CX) hd else hc } }
            "Stubble" -> px.fill(15, 32, 41, 43, { x, y -> px.get(x, y) != 0 && y >= 33 && Math.floorMod(x + y, 2) == 0 && !(y in 35..37 && abs(x - CX) <= 3) && abs(x - CX) <= 11 }) { x, y -> mix(px.get(x, y), hc, 0.4f) }
            "Glasses" -> for (ex in listOf(23, 33)) {
                val f = 0xFF3A2A20.toInt()
                for (x in ex - 4..ex + 4) { px.set(x, eyeY - 2, f); px.set(x, eyeY + 4, f) }
                for (y in eyeY - 2..eyeY + 4) { px.set(ex - 4, y, f); px.set(ex + 4, y, f) }
                px.set(CX, eyeY, f); px.set(CX - 1, eyeY - 1, f); px.set(CX + 1, eyeY - 1, f)
                px.set(ex - 3, eyeY - 1, 0x66FFFFFF)
            }
            "Round specs" -> for (ex in listOf(23, 33)) {
                for (y in eyeY - 4..eyeY + 6) for (x in ex - 5..ex + 5) {
                    val r = (x - ex) * (x - ex) + (y - eyeY - 1) * (y - eyeY - 1)
                    if (r in 14..22) px.set(x, y, GOLD)
                }
                px.line(CX - 1, eyeY, CX + 1, eyeY, GOLD)
            }
            "Shades", "Heart shades" -> {
                val heart = Farmer.FACES[look.face] == "Heart shades"
                val lens = if (heart) 0xFFE83A7A.toInt() else 0xFF14100E.toInt()
                for (ex in listOf(23, 33)) {
                    if (heart) {
                        px.map(ex - 4, eyeY - 2, arrayOf(".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."), mapOf('X' to lens))
                        px.set(ex - 3, eyeY - 1, 0xFFFFB0D0.toInt())
                    } else {
                        px.fill(ex - 4, eyeY - 2, ex + 4, eyeY + 3, { x, y -> !(y == eyeY + 3 && (x == ex - 4 || x == ex + 4)) }) { _, _ -> lens }
                        px.set(ex - 3, eyeY - 1, 0xFF6A7AA0.toInt()); px.set(ex - 2, eyeY - 1, 0xFF4A5A80.toInt())
                    }
                }
                px.line(CX - 1, eyeY - 1, CX + 1, eyeY - 1, lens)
                px.line(14, eyeY - 1, 18, eyeY - 1, dk(lens, 0.7f)); px.line(38, eyeY - 1, 42, eyeY - 1, dk(lens, 0.7f))
            }
        }
        if (look.has(4)) { px.set(31, 33, GOLD); px.set(31, 34, lt(GOLD, 0.4f)) }
        if (look.has(0)) for (ex in listOf(15, 41)) { px.set(ex, 33, GOLD); px.set(ex - 1, 34, GOLD); px.set(ex + 1, 34, GOLD); px.set(ex, 35, dk(GOLD, 0.8f)) }
    }

    private fun eyes(px: Px, look: Farmer, y: Int, skin: Int) {
        val iris = Farmer.EYE_COLORS[look.eyeColor]
        val rows = when (Farmer.EYES[look.eyes]) {
            "Almond" -> arrayOf("KKKKKK.", ".WHIIWK", "..WIPW.", "...ss..")
            "Big" -> arrayOf(".KKKKK.", "KHHIIWK", "KWHIPWK", "KWIPIWK", ".WWIWW.")
            "Sleepy" -> arrayOf(".......", ".LLLLL.", "KKKKKKK", ".WIPIW.")
            "Cat-eye" -> arrayOf("K......", ".KKKKKK", ".KWHIPW", "..WIIW.")
            else -> arrayOf(".KKKKK.", "KWHIIWK", "KWIPIWK", ".WWIWW.")
        }
        val pal = mapOf(
            'K' to INK, 'W' to 0xFFF6F2EE.toInt(), 'I' to iris, 'P' to 0xFF100C0C.toInt(), 'H' to WHITE,
            's' to dk(skin, 0.88f), 'L' to dk(skin, 0.9f),
        )
        px.map(20, y - 1, rows, pal)
        px.map(30, y - 1, Array(rows.size) { rows[it].reversed() }, pal)
        if (look.lashes == 1) {
            px.set(19, y - 2, INK); px.set(18, y - 1, INK); px.set(20, y - 2, INK)
            px.set(37, y - 2, INK); px.set(38, y - 1, INK); px.set(36, y - 2, INK)
        }
    }

    // ---- Hats and things on top ----

    private fun hat(px: Px, look: Farmer, hat: String) {
        val shirt = Farmer.CLOTH[look.shirt]; val pants = Farmer.CLOTH[look.pants]
        fun dome(cx: Int, cy: Int, rx: Int, ry: Int, cut: Int, c: Int) = px.fill(cx - rx - 1, cy - ry - 1, cx + rx + 1, cut, { x, y -> inE(x, y, cx, cy, rx, ry) && y <= cut }) { x, y ->
            when { x >= cx + rx - 3 -> dk(c, 0.75f); x <= cx - rx + 4 && y < cy -> lt(c, 0.22f); else -> c }
        }
        fun brim(cx: Int, cy: Int, rx: Int, ry: Int, c: Int) = px.fill(cx - rx - 1, cy - ry - 1, cx + rx + 1, cy + ry + 1, { x, y -> inE(x, y, cx, cy, rx, ry) }) { x, y ->
            when { y > cy -> dk(c, 0.7f); x >= cx + rx - 5 -> dk(c, 0.85f); else -> c }
        }
        when (hat) {
            "Cap" -> { dome(CX, 18, 13, 9, 19, pants); px.fill(14, 17, 46, 22, { x, y -> inE(x, y, CX + 4, 19, 16, 2) && y >= 18 }) { _, _ -> dk(pants, 0.7f) }; px.rect(CX - 1, 8, 3, 2, dk(pants, 0.8f)); px.tint(CX - 3, 13, 3, 2, WHITE, 0.4f) }
            "Straw hat" -> {
                val c = 0xFFE8C66A.toInt()
                brim(CX, 17, 23, 4, c); dome(CX, 13, 10, 9, 15, c); px.rect(18, 12, 21, 3, 0xFFC0402A.toInt())
                px.fill(4, 13, 52, 22, { x, y -> px.get(x, y) == c && Math.floorMod(x + y * 2, 5) == 0 }) { _, _ -> dk(c, 0.85f) }
            }
            "Beanie" -> {
                val c = Farmer.CLOTH[(look.shirt + 4) % Farmer.CLOTH.size]
                dome(CX, 18, 14, 11, 20, c)
                px.fill(0, 7, W - 1, 20, { x, y -> px.get(x, y) == c && x % 3 == 0 }) { _, _ -> dk(c, 0.88f) }
                px.fill(13, 17, 43, 21, { x, y -> inE(x, y, CX, 19, 15, 2) }) { x, _ -> if (x % 2 == 0) dk(c, 0.7f) else dk(c, 0.82f) }
                px.disc(CX, 5, 3, 0xFFF4F4F0.toInt()); px.set(CX + 2, 7, 0xFFD8D8D0.toInt())
            }
            "Bandana" -> { dome(CX, 19, 13, 8, 20, pants); px.fill(14, 10, 42, 20, { x, y -> px.get(x, y) == pants && Math.floorMod(x * 3 + y * 2, 7) == 0 }) { _, _ -> 0xFFF4F4F0.toInt() }; px.poly(dk(pants, 0.75f), 40, 18, 46, 20, 47, 28, 43, 26) }
            "Cowboy", "Cowgirl" -> {
                val c = if (hat == "Cowboy") 0xFF8A5A30.toInt() else mix(shirt, 0xFFF4E8D8.toInt(), 0.35f)
                px.poly(intArrayOf(17, 17, 18, 5, 24, 7, CX, 4, 32, 7, 38, 5, 39, 17)) { x, _ -> if (x >= 35) dk(c, 0.75f) else if (x <= 20) lt(c, 0.2f) else c }
                px.rect(17, 14, 23, 3, if (hat == "Cowboy") dk(c, 0.5f) else 0xFFF4F4F0.toInt())
                brim(CX, 18, 22, 3, c)
                px.poly(c, 4, 13, 8, 17, 4, 18); px.poly(dk(c, 0.8f), 52, 13, 48, 17, 52, 18)
                if (hat == "Cowgirl") { px.set(CX, 15, GOLD); px.set(CX - 1, 15, GOLD); px.set(CX + 1, 15, GOLD); px.set(CX, 14, GOLD) }
            }
            "Bucket" -> { dome(CX, 15, 11, 8, 17, pants); px.poly(intArrayOf(15, 17, 41, 17, 46, 23, 10, 23)) { x, _ -> if (x > 38) dk(pants, 0.7f) else dk(pants, 0.85f) } }
            "Witch" -> {
                val c = 0xFF3A2450.toInt()
                px.poly(intArrayOf(19, 16, 27, 2, 36, -2, 33, 4, 37, 16)) { x, _ -> if (x >= 32) dk(c, 0.75f) else c }
                px.rect(19, 12, 18, 4, 0xFFFF8A1E.toInt()); px.rect(26, 12, 4, 4, GOLD); px.rect(27, 13, 2, 2, 0xFFFF8A1E.toInt())
                brim(CX, 18, 23, 3, c)
            }
            "Party hat" -> {
                px.poly(intArrayOf(19, 18, CX, 0, 37, 18)) { x, y -> if (Math.floorMod(y + x / 3, 6) < 2) 0xFF3A3AA0.toInt() else if (x > 32) 0xFFE0B020.toInt() else 0xFFFFD23A.toInt() }
                px.disc(CX, 1, 2, 0xFFFF4A7A.toInt())
            }
            "Heart band" -> {
                val p = 0xFFFF4A7A.toInt()
                px.fill(14, 12, 42, 18, { x, y -> inE(x, y, CX, 24, 13, 11) && !inE(x, y, CX, 25, 13, 11) }) { _, _ -> p }
                px.map(CX - 4, 3, arrayOf(".XX.XX.", "XLXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."), mapOf('X' to p, 'L' to 0xFFFFC0D8.toInt()))
            }
            "Leprechaun", "Pilgrim", "Uncle Sam" -> {
                val c = when (hat) { "Leprechaun" -> 0xFF2E9A3A.toInt(); "Pilgrim" -> 0xFF4A4038.toInt(); else -> 0xFFF4F4F0.toInt() }
                val band = when (hat) { "Leprechaun" -> 0xFF1E1610.toInt(); "Pilgrim" -> 0xFF6A4A30.toInt(); else -> 0xFF3A5AC0.toInt() }
                val narrow = if (hat == "Pilgrim") 1 else 0
                px.poly(intArrayOf(18 + narrow * 2, 0, 38 - narrow * 2, 0, 38, 16, 18, 16)) { x, _ ->
                    if (hat == "Uncle Sam" && Math.floorMod(x, 6) < 3) 0xFFE03A3A.toInt() else if (x >= 35) dk(c, 0.75f) else if (x <= 20) lt(c, 0.2f) else c
                }
                px.rect(18, 12, 21, 4, band)
                if (hat == "Uncle Sam") for (x in 20..36 step 4) px.set(x, 13, WHITE) else { px.rect(25, 11, 7, 6, GOLD); px.rect(27, 13, 3, 2, band) }
                brim(CX, 17, if (hat == "Uncle Sam") 17 else 16, 2, if (hat == "Uncle Sam") band else c)
            }
            "Leaf crown" -> {
                val g = 0xFF5AC03A.toInt(); val d = 0xFF3A8A2A.toInt()
                px.fill(14, 12, 42, 18, { x, y -> inE(x, y, CX, 24, 13, 10) && !inE(x, y, CX, 25, 13, 10) }) { _, _ -> d }
                for (lx in listOf(16, 21, CX, 35, 40)) px.poly(intArrayOf(lx - 2, 15 - (if (lx == CX) 2 else 0), lx, 6 + abs(lx - CX) / 3, lx + 2, 15 - (if (lx == CX) 2 else 0))) { x, _ -> if (x <= lx) g else d }
            }
            "Bunny ears" -> {
                val w = 0xFFF4F0F4.toInt(); val p = 0xFFFFA0C0.toInt()
                px.fill(14, 12, 42, 18, { x, y -> inE(x, y, CX, 24, 13, 11) && !inE(x, y, CX, 25, 13, 11) }) { _, _ -> w }
                for ((ex, tilt) in listOf(21 to 0, 35 to 2)) {
                    px.fill(ex - 5, 0, ex + 5, 16, { x, y -> inE(x, y, ex + (if (tilt > 0) (12 - y) / 5 else 0), 6, 3, 9) }) { x, _ -> if (x > ex + 1) shade(w, 0.88f) else w }
                    px.fill(ex - 3, 0, ex + 3, 14, { x, y -> inE(x, y, ex + (if (tilt > 0) (12 - y) / 5 else 0), 6, 1, 6) }) { _, _ -> p }
                }
            }
            "Pumpkin" -> {
                val o = 0xFFFF8A1E.toInt()
                dome(CX, 13, 15, 9, 21, o)
                for (rx in listOf(21, CX, 35)) px.line(rx, 5, rx, 21, dk(o, 0.8f))
                px.rect(CX - 1, 1, 3, 4, 0xFF3A8A2A.toInt()); px.rect(CX + 2, 2, 3, 2, 0xFF5AC03A.toInt())
                px.poly(0xFF2A1408.toInt(), 20, 14, 24, 14, 22, 11); px.poly(0xFF2A1408.toInt(), 32, 14, 36, 14, 34, 11)
            }
            "Santa" -> {
                val r = 0xFFE03A3A.toInt(); val w = 0xFFF4F4F0.toInt()
                px.poly(intArrayOf(16, 17, 24, 4, 34, 2, 44, 10, 46, 20, 42, 14, 40, 17)) { x, _ -> if (x > 34) dk(r, 0.8f) else if (x < 22) lt(r, 0.15f) else r }
                px.fill(13, 15, 43, 21, { x, y -> inE(x, y, CX, 18, 15, 3) }) { x, _ -> if (x > 38) shade(w, 0.88f) else w }
                px.disc(46, 21, 3, w)
            }
            "Sun hat" -> {
                val c = mix(0xFFF4E2B0.toInt(), shirt, 0.2f)
                brim(CX, 18, 26, 5, c); dome(CX, 13, 10, 8, 16, c)
                px.rect(18, 13, 21, 3, pants); px.poly(pants, 36, 14, 42, 22, 38, 23, 35, 16)
            }
            "Beret" -> { px.fill(10, 6, 44, 20, { x, y -> inE(x, y, CX - 3, 14, 15, 6) }) { x, y -> if (y > 16) dk(pants, 0.72f) else if (x < 18) lt(pants, 0.2f) else pants }; px.rect(CX - 4, 6, 2, 3, dk(pants, 0.7f)) }
            "Headscarf" -> {
                val c = shirt
                px.fill(12, 8, 44, 34, { x, y -> inE(x, y, CX, 21, 14, 12) && (y <= 20 || abs(x - CX) >= 11) }) { x, y ->
                    if (Math.floorMod(x * 2 + y * 3, 9) == 0) lt(c, 0.6f) else if (x > 38) dk(c, 0.75f) else c
                }
                px.poly(dk(c, 0.8f), 38, 30, 46, 34, 44, 40, 40, 34)
            }
            "Visor" -> {
                val c = 0xFFF4F4F0.toInt()
                px.fill(14, 15, 42, 19, { x, y -> inE(x, y, CX, 24, 13, 9) && !inE(x, y, CX, 26, 13, 9) }) { _, _ -> c }
                px.fill(12, 17, 44, 23, { x, y -> inE(x, y, CX + 2, 20, 15, 2) && y >= 19 }) { _, _ -> pants }
            }
        }
    }

    private fun top(px: Px, look: Farmer, hat: String) {
        val petals = 0xFFFF8AB8.toInt(); val mid = 0xFFFFD23A.toInt()
        fun flower(x: Int, y: Int, p: Int = petals) { px.set(x, y - 1, p); px.set(x - 1, y, p); px.set(x + 1, y, p); px.set(x, y + 1, p); px.set(x, y, mid); px.set(x + 1, y + 1, dk(p, 0.8f)) }
        if (look.has(1)) { flower(18, 19); px.set(16, 20, 0xFF5AC03A.toInt()); px.set(17, 21, 0xFF3A8A2A.toInt()) }
        if (look.has(2) && (hat in OPEN_HATS)) {
            val cols = intArrayOf(petals, 0xFFFFFFFF.toInt(), 0xFFB088E0.toInt(), 0xFFFF6A4A.toInt(), 0xFF4AB8C0.toInt())
            for ((i, fx) in (16..40 step 4).withIndex()) {
                val fy = 14 + ((fx - CX) * (fx - CX)) / 40
                px.set(fx + 2, fy, 0xFF3A8A2A.toInt())
                flower(fx, fy, cols[i % cols.size])
            }
        }
    }
}
