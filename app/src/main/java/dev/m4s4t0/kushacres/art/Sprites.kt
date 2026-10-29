package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Pheno
import dev.m4s4t0.kushacres.model.Strain
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Every picture in the game, drawn pixel by pixel in code and cached. Ground tiles are 2:1 isometric diamonds
 * [TW]×[TH] with [DEPTH] pixels of earth showing underneath, so the farm reads as a floating block of land.
 */
object Sprites {
    const val TW = 32
    const val TH = 16
    const val DEPTH = 5
    const val STAGES = 6
    /** Plant sprites are anchored at this pixel (bottom centre), which sits on the tile's centre. */
    const val PLANT_W = 32
    const val PLANT_H = 40
    const val PLANT_AX = 16
    const val PLANT_AY = 37

    internal val OUT = 0xFF1C1A14.toInt()

    // ---- Ground ----

    /** Top and bottom rows of the diamond in column x. */
    private fun top(x: Int) = 7 - (if (x < 16) x else 31 - x) / 2
    private fun bottom(x: Int) = 8 + (if (x < 16) x else 31 - x) / 2
    /** Whether art pixel x,y is on the diamond's top face. */
    private fun onTop(x: Int, y: Int) = x in 0 until TW && y in top(x)..bottom(x)

    private fun block(top: (Px, Int, Int, Random) -> Unit, sideL: Int, sideR: Int, lip: Int, seed: Int,
                      fine: (Px, Random) -> Unit = { _, _ -> }): Bitmap {
        val px = Px(TW, TH + DEPTH)
        val r = Random(seed)
        for (x in 0 until TW) {
            for (y in top(x)..bottom(x)) top(px, x, y, r)
            for (d in 1..DEPTH) {
                val base = if (x < 16) sideL else sideR
                val c = if (d == 1) lip else if (r.nextInt(7) == 0) shade(base, 0.85f) else base
                px.set(x, bottom(x) + d, c)
            }
        }
        // Earth strata and pebbles down the sides, finer than the art grid.
        for (fx in 0 until px.fw) {
            val x = fx / px.s
            val base = if (x < 16) sideL else sideR
            for (fy in (bottom(x) + 2) * px.s until (bottom(x) + DEPTH + 1) * px.s) {
                val n = r.nextInt(100)
                when {
                    n < 6 -> px.dot(fx, fy, shade(base, 1.18f))
                    n < 14 -> px.dot(fx, fy, shade(base, 0.82f))
                }
            }
            if (((fx / 3 + bottom(x)) % 5) == 0) px.dot(fx, (bottom(x) + 3) * px.s, shade(base, 0.75f))
        }
        fine(px, r)
        // Light on the top-left edge, a lip of shadow under the front edges.
        for (fx in 0 until px.fw) {
            val x = fx / px.s
            val ft = top(x) * px.s + if (x < 16) (px.s - 1 - fx % px.s) else fx % px.s
            if (x < 16) px.dot(fx, ft, shade(px.p[ft * px.fw + fx], 1.2f))
            val fb = (bottom(x) + 1) * px.s
            px.dot(fx, fb, shade(lip, 0.8f))
        }
        return px.bitmap()
    }

    private val grassCache = HashMap<Long, Bitmap>()
    /** A square of ground; [color] tints it for each farm (grass, sand, snow, ash…). */
    fun grass(v: Int, wild: Boolean, color: Int = if (wild) 0xFF3E7D3C.toInt() else 0xFF62AE45.toInt()): Bitmap =
        grassCache.getOrPut((color.toLong() shl 8) or (v * 2L + if (wild) 1 else 0)) {
        val base = color
        val flowers = intArrayOf(0xFFF4E36B.toInt(), 0xFFF2F2F2.toInt(), 0xFFE58AB8.toInt(), 0xFFA8B8F8.toInt())
        block({ px, x, y, r ->
            val n = r.nextInt(100)
            val c = when {
                n < 12 -> shade(base, 0.9f)
                n < 18 -> shade(base, 1.07f)
                else -> base
            }
            px.set(x, y, c)
        }, 0xFF8A5A33.toInt(), 0xFF6B4226.toInt(), shade(base, 0.8f), 100 + v * 7 + if (wild) 3 else 0) { px, r ->
            val s = px.s
            // Speckle, then little tufts of blades, then the odd flower.
            for (fy in 0 until (TH + 1) * s) for (fx in 0 until px.fw) {
                if (!onTop(fx / s, fy / s)) continue
                val n = r.nextInt(100)
                if (n < 9) px.dot(fx, fy, shade(base, 0.82f)) else if (n < 13) px.dot(fx, fy, shade(base, 1.16f))
            }
            repeat(if (wild) 16 else 11) {
                val fx = r.nextInt(3, px.fw - 3); val fy = r.nextInt(4, TH * s - 2)
                if (!onTop(fx / s, fy / s) || !onTop(fx / s, (fy - 3) / s)) return@repeat
                val dark = shade(base, 0.7f); val lt = shade(base, 1.3f)
                px.dot(fx, fy, dark); px.dot(fx, fy - 1, base); px.dot(fx, fy - 2, lt)
                px.dot(fx - 1, fy, dark); px.dot(fx - 2, fy - 1, lt)
                px.dot(fx + 1, fy, dark); px.dot(fx + 2, fy - 1, shade(base, 1.15f))
            }
            if (!wild && v != 0) repeat(r.nextInt(1, 3)) {
                val fx = r.nextInt(8, px.fw - 8); val fy = r.nextInt(8, TH * s - 8)
                if (!onTop(fx / s, fy / s)) return@repeat
                val c = flowers[r.nextInt(flowers.size)]
                px.dot(fx - 1, fy, c); px.dot(fx + 1, fy, c); px.dot(fx, fy - 1, c); px.dot(fx, fy + 1, shade(c, 0.85f))
                px.dot(fx, fy, 0xFFF2C440.toInt()); px.dot(fx, fy + 2, shade(base, 0.7f))
            }
        }
    }

    const val WATER_FRAMES = 4
    private val waterCache = HashMap<Int, Bitmap>()
    /** A square of water sunk a little below the ground, with ripples that drift over [WATER_FRAMES] frames. */
    fun water(frame: Int, deep: Boolean): Bitmap = waterCache.getOrPut(frame * 2 + if (deep) 1 else 0) {
        val base = if (deep) 0xFF2C6CA8.toInt() else 0xFF3E8CC8.toInt()
        val bank = 0xFF6B5034.toInt()
        block({ px, x, y, _ -> px.set(x, y, if (y == top(x)) shade(base, 0.7f) else base) }, bank, shade(bank, 0.8f), shade(base, 0.6f), 900 + frame) { px, r ->
            val s = px.s
            for (fy in 0 until (TH + 1) * s) for (fx in 0 until px.fw) {
                if (!onTop(fx / s, fy / s)) continue
                // Diagonal ripple bands that slide along with the frame.
                val ph = ((fx + fy * 2 + frame * 4) % 24 + 24) % 24
                when {
                    ph < 2 -> px.dot(fx, fy, shade(base, 1.22f))
                    ph in 12..13 -> px.dot(fx, fy, shade(base, 0.88f))
                }
            }
            repeat(3) {
                val fx = (r.nextInt(6, px.fw - 6) + frame * 3) % (px.fw - 6) + 3; val fy = r.nextInt(6, TH * s - 4)
                if (onTop(fx / s, fy / s)) { px.dot(fx, fy, 0xFFE8F4FF.toInt()); px.dot(fx + 1, fy, 0xFFB8DCF8.toInt()) }
            }
        }
    }

    private val rockCache = HashMap<Int, Bitmap>()
    /** A boulder sitting on the grass. */
    fun rock(v: Int): Bitmap = rockCache.getOrPut(v) {
        val px = Px(20, 16)
        val f = px.s.toFloat()
        val g = intArrayOf(0xFF8C8A84.toInt(), 0xFF9A8E7C.toInt(), 0xFF7E848C.toInt())[v % 3]
        val r = Random(v + 77)
        val lumps = listOf(Triple(10f, 9.5f, 5.6f), Triple(6f, 11f, 3.6f + v % 2), Triple(14f, 11.2f, 3.4f))
        for ((cx, cy, rad) in lumps) {
            for (fy in ((cy - rad - 1) * f).toInt()..((cy + rad) * f).toInt()) for (fx in ((cx - rad - 1) * f).toInt()..((cx + rad + 1) * f).toInt()) {
                val dx = ((fx + 0.5f) / f - cx) / rad; val dy = ((fy + 0.5f) / f - cy) / (rad * 0.85f)
                if (dx * dx + dy * dy > 1 || (fy + 0.5f) / f > 14.5f) continue
                val lv = -0.6f * dx - 0.7f * dy + (r.nextFloat() - 0.5f) * 0.25f
                px.dot(fx, fy, when { lv > 0.55f -> shade(g, 1.3f); lv > 0.1f -> shade(g, 1.1f); lv > -0.4f -> g; else -> shade(g, 0.72f) })
            }
        }
        // A crack and a bit of moss.
        for (k in 0 until 6) px.dot(((9 + k * 0.5f) * f).toInt(), ((7 + k) * f / 1.6f + 4 * f).toInt(), shade(g, 0.6f))
        if (v % 2 == 0) for (k in 0 until 5) px.dot(((7 + k) * f).toInt(), (6.2f * f).toInt() + k % 2, 0xFF5E8A3A.toInt())
        px.outline(OUT)
        px.bitmap()
    }

    private val plotCache = HashMap<Pair<Boolean, Int?>, Bitmap>()
    /** A plot; [dirt] swaps the dry soil colour (wet soil is the same colour, darker). */
    fun plot(wet: Boolean, dirt: Int? = null): Bitmap = plotCache.getOrPut(wet to dirt) {
        val soil = when {
            dirt != null -> if (wet) shade(dirt, 0.58f) else dirt
            wet -> 0xFF5A3720.toInt()
            else -> 0xFF9C6A3C.toInt()
        }
        val wood = 0xFF7A5230.toInt()
        val px = Px(TW, TH + DEPTH)
        val s = px.s
        val r = Random(if (wet) 2 else 1)
        for (x in 0 until TW) {
            val t = top(x); val b = bottom(x)
            for (y in t..b) {
                val edge = y == t || y == b || x == 0 || x == 31 || y == t + 1 && (x < 2 || x > 29)
                px.set(x, y, if (edge) wood else soil)
            }
            for (d in 1..DEPTH) px.set(x, b + d, if (d == 1) shade(wood, 0.8f) else if (x < 16) 0xFF6E4A2A.toInt() else 0xFF55381F.toInt())
        }
        // Ridged rows running corner to corner: lit crest, shadowed trough, crumbs of soil.
        for (fy in 0 until (TH + 1) * s) for (fx in 0 until px.fw) {
            val x = fx / s; val y = fy / s
            if (!onTop(x, y) || px.p[fy * px.fw + fx] == wood || px.p[fy * px.fw + fx] != soil) continue
            val ph = ((fy * 2 - fx) % 16 + 16) % 16
            var c = when {
                ph < 3 -> shade(soil, 0.68f)
                ph < 5 -> shade(soil, 0.85f)
                ph in 9..10 -> shade(soil, 1.18f)
                else -> soil
            }
            val n = r.nextInt(100)
            if (n < 8) c = shade(c, 1.12f) else if (n < 14) c = shade(c, 0.88f)
            if (wet && n == 99) c = 0xFFB8D8E8.toInt()
            px.dot(fx, fy, c)
        }
        // Wood grain on the frame.
        for (fx in 0 until px.fw) {
            val x = fx / s
            px.dot(fx, top(x) * s + (if (x < 16) s - 1 - fx % s else fx % s), shade(wood, 1.3f))
            if (fx % 7 == 0) px.dot(fx, bottom(x) * s, shade(wood, 0.8f))
        }
        px.bitmap()
    }

    private val stairCache = HashMap<Int, Bitmap>()
    /**
     * Half of a bunker stairwell sunk into the ground: [part] 0 is the top square, 1 the bottom one with the door.
     * [way] is the view direction the steps go down: 0 toward +u, 1 toward -u, 2 toward +v, 3 toward -v.
     */
    fun stairs(way: Int, part: Int): Bitmap = stairCache.getOrPut(way * 2 + part) {
        val px = Px(TW, TH + DEPTH)
        val s = px.s
        val rim = 0xFF9C978C.toInt(); val step = 0xFF86817A.toInt()
        for (x in 0 until TW) for (d in 1..DEPTH) {
            val b = bottom(x)
            px.set(x, b + d, if (d == 1) shade(rim, 0.8f) else if (x < 16) 0xFF8A8478.toInt() else 0xFF6A645A.toInt())
        }
        for (fy in 0 until (TH + 1) * s) for (fx in 0 until px.fw) {
            if (!onTop(fx / s, fy / s)) continue
            val x = (fx + 0.5f) / s; val y = (fy + 0.5f) / s
            // Where on the square this is: a runs along view u, b along view v.
            val a = (((x - 16) / 16 + y / 8) / 2).coerceIn(0f, 1f); val b = ((y / 8 - (x - 16) / 16) / 2).coerceIn(0f, 1f)
            val along = when (way) { 0 -> a; 1 -> 1 - a; 2 -> b; else -> 1 - b }
            val cross = if (way < 2) b else a
            val deep = (part + along) / 2
            val c = when {
                part == 0 && along < 0.14f -> if (((cross * 8).toInt()) % 2 == 0) 0xFFF2C440.toInt() else 0xFF2A2620.toInt()
                cross < 0.13f || cross > 0.87f -> if (cross < 0.06f || cross > 0.94f) shade(rim, 1.12f) else rim
                cross < 0.2f -> shade(step, 0.45f - deep * 0.2f)
                deep > 0.84f -> if (cross < 0.26f || cross > 0.8f || deep < 0.88f) 0xFF4A4038.toInt() else if (deep > 0.97f) 0xFFFFD27A.toInt() else 0xFF15110E.toInt()
                else -> {
                    val k = deep / 0.84f * 7
                    shade(step, (1f - deep * 0.6f) * if (k % 1f < 0.22f) 1.25f else 1f)
                }
            }
            px.dot(fx, fy, c)
        }
        px.bitmap()
    }

    /** A diamond outline used to show which square a tap will hit. */
    private val cursorCache = HashMap<Int, Bitmap>()
    fun cursor(c: Int): Bitmap = cursorCache.getOrPut(c) {
        val px = Px(TW, TH)
        for (x in 0 until TW) { px.set(x, top(x), c); px.set(x, bottom(x), c) }
        px.bitmap()
    }

    // ---- Plants ----

    private val plantCache = HashMap<String, Bitmap>()

    /**
     * The plant at growth [stage] (0 sprout, 1 seedling, 2 young, 3 leafy, 4 flowering, 5 ready), its top leaning
     * [sway] fine pixels for the breeze. A rare pheno [colour] tints the buds. [dirt] is the farm's own soil colour, if it has one.
     */
    fun plant(s: Strain, stage: Int, sway: Int, colour: Int = 0, dirt: Int? = null): Bitmap = plantCache.getOrPut("${s.id}/$stage/$sway/$colour/$dirt") {
        val budTint = if (colour > 0) mix(s.bud, Pheno.TINTS[colour], 0.6f) else s.bud
        val px = Px(PLANT_W, PLANT_H)
        val f = px.s.toFloat()
        val r = Random(s.id.hashCode() * 31 + stage)
        val bx = PLANT_AX + 0.5f; val by = PLANT_AY.toFloat()
        val leaf = s.leaf
        val back = shade(leaf, 0.7f)
        val light = shade(leaf, 1.28f)
        val stem = shade(leaf, 0.62f)
        // The little mound of soil it grows from.
        px.poly(dirt?.let { shade(it, 0.48f) } ?: 0xFF4A2E1A.toInt(), PLANT_AX - 3, PLANT_AY, PLANT_AX - 2, PLANT_AY - 1, PLANT_AX + 3, PLANT_AY - 1, PLANT_AX + 4, PLANT_AY)
        if (stage == 0) {
            // A sprout: a curl of stem and two seed leaves.
            for (k in 0 until 5) px.dot((bx * f).toInt(), (by * f).toInt() - 1 - k, stem)
            leaflet(px, bx, by - 2.2f, PI + 0.8, 2.4f, 0.8f, leaf, light, leaf, serrate = false)
            leaflet(px, bx, by - 2.2f, -0.8, 2.4f, 0.8f, leaf, light, leaf, serrate = false)
            px.light()
            px.outline(shade(leaf, 0.3f))
            return@getOrPut px.bitmap()
        }
        val height = floatArrayOf(0f, 6f, 12f, 18f, 24f, 28f)[stage]
        fun xAt(h: Float) = bx + sway / f * (h / height).let { it * it }
        val nodes = intArrayOf(0, 1, 2, 3, 4, 4)[stage]
        val nodeH = (1..nodes).map { height * (0.22f + 0.62f * (it - 1) / maxOf(1, nodes - 1).toFloat()) }
        val maxSize = floatArrayOf(0f, 3.4f, 5.5f, 7.5f, 8.5f, 9f)[stage]
        // Back leaves, stem, front leaves.
        nodeH.forEachIndexed { i, h ->
            val size = maxOf(2.6f, maxSize - i * 1.6f)
            val dir = if (i % 2 == 0) PI + 0.5 else -0.5
            fanLeaf(px, xAt(h), by - h, dir, size, back, shade(back, 1.15f))
        }
        for (fy in 0..(height * f).toInt()) {
            val h = fy / f
            val fx = (xAt(h) * f).toInt()
            px.dot(fx, (by * f).toInt() - fy, stem)
            if (h < height * 0.6f) px.dot(fx + 1, (by * f).toInt() - fy, shade(stem, 0.8f))
        }
        nodeH.forEachIndexed { i, h ->
            val size = maxOf(2.4f, maxSize - 1f - i * 1.6f)
            val dir = if (i % 2 == 0) -0.4 else PI + 0.4
            fanLeaf(px, xAt(h), by - h + 0.5f, dir, size, leaf, light)
        }
        val tx = xAt(height); val ty = by - height
        if (stage < 4) {
            fanLeaf(px, tx, ty + 1, -PI / 2, if (stage == 1) 2.8f else 4f, leaf, light)
        } else {
            val ripe = stage == 5
            val bud = budTint
            // Colas: stacked calyxes on the main top and every upper node.
            cola(px, r, tx, ty, if (ripe) 8f else 4.5f, if (ripe) 2.4f else 1.6f, s, budTint, ripe)
            nodeH.drop(1).forEachIndexed { i, h ->
                val side = if (i % 2 == 0) -1 else 1
                cola(px, r, xAt(h) + side * 2.6f, by - h - 1.5f, if (ripe) 3.5f else 2f, if (ripe) 1.6f else 1.1f, s, budTint, ripe)
                cola(px, r, xAt(h) - side * 2.2f, by - h - 0.5f, if (ripe) 2.5f else 1.5f, if (ripe) 1.3f else 0.9f, s, budTint, ripe)
            }
            if (ripe) px.dot((tx * f).toInt(), ((ty - 0.6f) * f).toInt(), shade(bud, 1.4f))
        }
        px.light()
        px.outline(shade(leaf, 0.3f))
        px.bitmap()
    }

    /** A bud: overlapping calyxes down a [len]-pixel spear, sugar leaves poking out, pistils, and frost when [ripe]. */
    private fun cola(px: Px, r: Random, x: Float, y: Float, len: Float, wid: Float, s: Strain, bud: Int, ripe: Boolean) {
        val f = px.s.toFloat()
        val steps = (len * 2).toInt()
        for (k in 0..steps) {
            val t = k / steps.toFloat()
            val cy = y + t * len
            val w = wid * (0.55f + 0.45f * sin(PI * (0.25 + t * 0.75)).toFloat())
            val cx = x + (r.nextFloat() - 0.5f) * 0.6f
            fdisc(px, cx * f, cy * f, w * f, if (k % 2 == 0) bud else shade(bud, 0.86f))
            px.dot(((cx - w * 0.4f) * f).toInt(), ((cy - w * 0.4f) * f).toInt(), shade(bud, 1.22f))
        }
        // Sugar leaves.
        repeat(if (ripe) 3 else 2) {
            val t = 0.3f + r.nextFloat() * 0.6f
            val side = if (it % 2 == 0) -1 else 1
            leaflet(px, x, y + t * len, if (side < 0) PI + 0.7 else -0.7, wid + 1.4f, 0.55f, shade(s.leaf, 1.1f), shade(s.leaf, 1.35f), s.leaf, serrate = false)
        }
        // Pistils curling out, then trichome frost.
        repeat((len * if (ripe) 2.2f else 1.6f).toInt()) {
            val fx = ((x + (r.nextFloat() - 0.5f) * wid * 2) * f).toInt()
            val fy = ((y + r.nextFloat() * len) * f).toInt()
            val dx = if (r.nextBoolean()) 1 else -1
            px.dot(fx, fy, s.hair); px.dot(fx + dx, fy - 1, s.hair)
            if (ripe) px.dot(fx + dx * 2, fy - 1, shade(s.hair, 0.8f))
        }
        if (ripe) repeat((len * 3).toInt()) {
            val fx = ((x + (r.nextFloat() - 0.5f) * wid * 1.8f) * f).toInt()
            val fy = ((y + r.nextFloat() * len) * f).toInt()
            if (px.p.getOrNull(fy * px.fw + fx)?.let { it != 0 } == true) px.dot(fx, fy, if (r.nextInt(3) == 0) 0xFFFFFFFF.toInt() else 0xFFE8F0E0.toInt())
        }
    }

    /** A disc in fine pixels (centre and radius in fine pixels, fractional allowed). */
    internal fun fdisc(px: Px, cx: Float, cy: Float, rad: Float, c: Int) {
        for (fy in (cy - rad - 1).toInt()..(cy + rad + 1).toInt()) for (fx in (cx - rad - 1).toInt()..(cx + rad + 1).toInt()) {
            val dx = fx + 0.5f - cx; val dy = fy + 0.5f - cy
            if (dx * dx + dy * dy <= rad * rad) px.dot(fx, fy, c)
        }
    }

    /**
     * One pointed leaflet from art point x,y along angle [a]: [len] long, [wid] half-width at its widest, a toothed edge
     * when [serrate], the side facing up-left lit with [hi] and a [vein] down the middle.
     */
    internal fun leaflet(px: Px, x: Float, y: Float, a: Double, len: Float, wid: Float, c: Int, hi: Int, vein: Int, serrate: Boolean = true) {
        val f = px.s.toFloat()
        val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
        val ex = x + ca * len; val ey = y + sa * len
        val pad = wid + 1
        // Which side of the leaflet faces the light (up and left).
        val litSide = if (-sa * -0.6f + ca * -0.8f > 0) 1f else -1f
        for (fy in ((minOf(y, ey) - pad) * f).toInt()..((maxOf(y, ey) + pad) * f).toInt())
            for (fx in ((minOf(x, ex) - pad) * f).toInt()..((maxOf(x, ex) + pad) * f).toInt()) {
                val qx = (fx + 0.5f) / f - x; val qy = (fy + 0.5f) / f - y
                val u = qx * ca + qy * sa
                if (u < 0 || u > len) continue
                val v = -qx * sa + qy * ca
                val t = u / len
                var w = wid * sin(PI * Math.pow(t.toDouble(), 0.7)).toFloat()
                if (serrate && len > 3) w *= 0.8f + 0.2f * ((u * 1.6f) % 1f)
                w = maxOf(w, if (t < 0.9f) 0.35f else 0f)
                if (kotlin.math.abs(v) > w) continue
                val col = when {
                    kotlin.math.abs(v) > w - 0.3f && v * litSide < 0 && w > 0.6f -> shade(c, 0.72f)
                    kotlin.math.abs(v) < 0.3f && t < 0.85f && len > 3 -> vein
                    v * litSide > 0 -> hi
                    else -> c
                }
                px.dot(fx, fy, col)
            }
    }

    /** A palmate fan leaf: up to seven serrated leaflets fanning out from one point (art pixels, fractional allowed). */
    internal fun fanLeaf(px: Px, cx: Float, cy: Float, dir: Double, size: Float, c: Int, hi: Int = c) {
        val n = if (size >= 6) 3 else if (size >= 3.5f) 2 else 1
        val vein = shade(c, 0.78f)
        // Outer leaflets first and a shade darker, so each one reads against the next.
        for (k in intArrayOf(-3, 3, -2, 2, -1, 1, 0)) {
            if (kotlin.math.abs(k) > n) continue
            val a = dir + k * 0.55
            val len = size * (1 - kotlin.math.abs(k) * 0.2f)
            val dim = 1f - kotlin.math.abs(k) * 0.07f
            leaflet(px, cx, cy, a, len, maxOf(0.5f, size * 0.105f), shade(c, dim), shade(hi, dim), vein, serrate = size >= 4)
        }
    }

    internal fun fanLeaf(px: Px, cx: Int, cy: Int, dir: Double, size: Int, c: Int, hi: Int = c) =
        fanLeaf(px, cx + 0.5f, cy + 0.5f, dir, size.toFloat(), c, hi)

    /**
     * A ball of foliage lit from the top left, in four tones with leafy clumps, so trees and bushes look round.
     * Centre and radius in art pixels.
     */
    internal fun foliage(px: Px, cx: Float, cy: Float, rad: Float, g: Int, seed: Int) {
        val f = px.s.toFloat()
        val r = Random(seed)
        val tones = intArrayOf(shade(g, 0.62f), shade(g, 0.8f), g, shade(g, 1.2f), shade(g, 1.42f))
        for (fy in ((cy - rad - 1) * f).toInt()..((cy + rad + 1) * f).toInt()) for (fx in ((cx - rad - 1) * f).toInt()..((cx + rad + 1) * f).toInt()) {
            val dx = ((fx + 0.5f) / f - cx) / rad; val dy = ((fy + 0.5f) / f - cy) / rad
            // A lumpy edge: the radius wobbles with the angle.
            val ang = kotlin.math.atan2(dy, dx)
            val edge = 1f + 0.08f * sin(ang * 7 + seed).toFloat() + 0.05f * sin(ang * 13 + seed * 2).toFloat()
            val d2 = dx * dx + dy * dy
            if (d2 > edge * edge) continue
            val nz = kotlin.math.sqrt(maxOf(0f, 1 - d2))
            val light = -0.55f * dx - 0.65f * dy + 0.5f * nz
            // Clumps: a cheap pattern of overlapping little blobs.
            val clump = sin(fx * 0.9 + seed).toFloat() * sin(fy * 1.1 + seed * 0.7).toFloat() * 0.18f
            val lv = light + clump + (r.nextFloat() - 0.5f) * 0.12f
            val i = when { lv > 0.62f -> 4; lv > 0.32f -> 3; lv > 0.02f -> 2; lv > -0.3f -> 1; else -> 0 }
            px.dot(fx, fy, tones[i])
        }
    }

    // ---- Things on the land ----

    private val treeCache = HashMap<Int, Bitmap>()
    fun tree(v: Int): Bitmap = treeCache.getOrPut(v) {
        val px = Px(26, 40)
        val f = px.s
        val trunk = 0xFF6A4325.toInt()
        px.rect(12, 29, 3, 9, trunk); px.set(12, 37, shade(trunk, 0.8f))
        for (fy in 29 * f until 38 * f) { px.dot(12 * f, fy, shade(trunk, 1.25f)); px.dot(15 * f - 1, fy, shade(trunk, 0.7f)); if (fy % 5 == 0) px.dot(13 * f + 1, fy, shade(trunk, 0.8f)) }
        if (v % 3 == 2) {
            // A pine: layered skirts of needles, lit on the left.
            val g = 0xFF2F6B44.toInt()
            for (layer in 0..3) {
                val cy = 8 + layer * 6; val hw = 4 + layer * 2
                for (fj in 0 until 8 * f) {
                    val j = fj / f.toFloat()
                    val half = hw * j / 7f
                    for (fi in (-(half * f)).toInt()..(half * f).toInt()) {
                        val i = fi / f.toFloat()
                        val tone = when {
                            fj >= 8 * f - 2 -> shade(g, 0.6f)
                            i < -half * 0.4f -> shade(g, 1.25f)
                            i < 0 -> shade(g, 1.08f)
                            i > half * 0.6f -> shade(g, 0.78f)
                            else -> g
                        }
                        val needle = ((fi + fj * 3) % 5 == 0)
                        px.dot(13 * f + f / 2 + fi, cy * f + fj, if (needle) shade(tone, 0.85f) else tone)
                    }
                }
            }
        } else {
            val g = if (v % 3 == 0) 0xFF3F8A3A.toInt() else 0xFF4E9A3C.toInt()
            foliage(px, 13f, 22f, 9f, shade(g, 0.85f), v * 3)
            foliage(px, 8f, 21f, 6f, shade(g, 0.92f), v * 3 + 1)
            foliage(px, 18.5f, 21.5f, 6f, shade(g, 0.88f), v * 3 + 2)
            foliage(px, 13f, 14f, 8.5f, g, v * 3 + 5)
            if (v % 4 == 1) repeat(5) { k ->
                val ax = 8 + (k * 7 + v) % 11; val ay = 12 + (k * 5) % 12
                fdisc(px, ax * f + 1f, ay * f + 1f, 1.6f, 0xFFE0503A.toInt()); px.dot(ax * f, ay * f, 0xFFFFB0A0.toInt())
            }
        }
        px.light()
        px.outline(OUT)
        px.bitmap()
    }

    private val bushCache = HashMap<Int, Bitmap>()
    fun bush(v: Int): Bitmap = bushCache.getOrPut(v) {
        val px = Px(14, 10)
        val f = px.s
        val g = if (v % 2 == 0) 0xFF3A7436.toInt() else 0xFF4A8A3A.toInt()
        foliage(px, 7f, 6.5f, 3.5f, shade(g, 0.9f), v + 40)
        foliage(px, 4f, 6.8f, 2.6f, g, v + 41)
        foliage(px, 9.5f, 5.8f, 2.6f, shade(g, 1.05f), v + 42)
        if (v % 3 == 0) for ((ax, ay) in listOf(5 to 4, 9 to 7, 7 to 5)) { px.dot(ax * f, ay * f, 0xFFE05A5A.toInt()); px.dot(ax * f + 1, ay * f, 0xFFE05A5A.toInt()); px.dot(ax * f, ay * f - 1, 0xFFFFB0B0.toInt()) }
        px.light()
        px.outline(OUT)
        px.bitmap()
    }

    val sprinkler: Bitmap by lazy {
        val px = Px(14, 20)
        px.rect(4, 16, 6, 2, 0xFF7C8088.toInt()); px.rect(3, 17, 8, 1, 0xFF5C6068.toInt())
        px.rect(6, 6, 2, 11, 0xFF9AA0A8.toInt()); px.rect(7, 6, 1, 11, 0xFF6C7078.toInt())
        px.rect(4, 3, 6, 3, 0xFFC9A04A.toInt()); px.rect(4, 3, 6, 1, 0xFFE8C66A.toInt())
        px.set(3, 4, 0xFF4AA8E0.toInt()); px.set(10, 4, 0xFF4AA8E0.toInt())
        px.light()
        px.outline(OUT)
        px.bitmap()
    }

    val lamp: Bitmap by lazy {
        val px = Px(18, 36)
        px.rect(5, 32, 6, 2, 0xFF505560.toInt())
        px.rect(7, 8, 2, 25, 0xFF3C4048.toInt()); px.rect(7, 8, 1, 25, 0xFF5A5F6A.toInt())
        px.rect(7, 6, 9, 2, 0xFF3C4048.toInt())
        px.rect(10, 8, 7, 3, 0xFF2C3038.toInt())
        px.rect(11, 11, 5, 1, 0xFFFFE27A.toInt())
        px.light()
        px.outline(OUT)
        px.bitmap()
    }

    // ---- Icons ----

    internal val P = mapOf(
        'k' to OUT, 'y' to 0xFFF2C440.toInt(), 'Y' to 0xFFFFE89A.toInt(), 'o' to 0xFFB07A1C.toInt(),
        'w' to 0xFFF4F4F0.toInt(), 'b' to 0xFF3E8EDC.toInt(), 'B' to 0xFF9AD4FF.toInt(), 'g' to 0xFF4FA03C.toInt(),
        'G' to 0xFF9AD86A.toInt(), 'r' to 0xFFD8484A.toInt(), 'n' to 0xFF7A4E2A.toInt(), 'N' to 0xFFC89A62.toInt(),
        's' to 0xFF6C7078.toInt(), 'S' to 0xFFC8CCD4.toInt(), 'P' to 0xFFF0C090.toInt(),
    )

    internal fun icon(rows: Array<String>): Bitmap = Px(12, 12).apply { map(0, 0, rows, P) }.bitmap()

    val coin by lazy { icon(arrayOf("...kkkkkk...", "..kYYYYYyk..", ".kYYyyyyyok.", "kYYyyoyyyyok", "kYyyoooyyyok", "kYyyoyyyyyok", "kYyyyoooyyok", "kYyyyyyoyyok", "kyyyoooyyyok", ".kyyyyoyyok.", "..koooooook.", "...kkkkkk...")) }
    val star by lazy { icon(arrayOf(".....kk.....", "....kYYk....", "....kYyk....", "kkkkYyyykkkk", "kYYYyyyyyyok", ".kYyyyyyyok.", "..kyyyyyyk..", "..kyyyyyyk..", ".kyyyookyyk.", ".kyok..koyk.", "kkk......kkk", "............")) }
    val drop by lazy { icon(arrayOf(".....kk.....", "....kBBk....", "....kBbk....", "...kBbbbk...", "...kBbbbk...", "..kBwbbbbk..", "..kBwbbbbk..", ".kBbbbbbbbk.", ".kBbbbbbbbk.", ".kbbbbbbbbk.", "..kbbbbbbk..", "...kkkkkk...")) }
    val jar by lazy { icon(arrayOf("..kkkkkkkk..", "..knnnnnnk..", "..kkkkkkkk..", ".kSwwwwwwSk.", ".kwgGgggwSk.", ".kwgGGgggSk.", ".kwggggGgSk.", ".kwgGgggggk.", ".kwggggGgSk.", ".kwgggggggk.", ".kSSSSSSSSk.", "..kkkkkkkk..")) }
    val bag by lazy { icon(arrayOf("....kkkk....", "...knnnnk...", "....kNNk....", "...kNNNNk...", "..kNNNNNNk..", ".kNNNgGNNNk.", ".kNNgGGgNNk.", ".kNNNgGNNNk.", ".kNNNNgNNNk.", ".kNNNNNNNNk.", "..knnnnnnk..", "...kkkkkk...")) }
    /** Bud Drop's falling things: a nug, a golden nug and a bug. */
    val nug by lazy { icon(arrayOf("....kkkk....", "...kGGgGk...", "..kGgogGgk..", ".kGgGgGogGk.", ".kgogGgGggk.", ".kGgGogGgGk.", ".kgGgGgoggk.", "..kgGogGgk..", "..kgGgGggk..", "...kgogk....", "....kggk....", ".....kk.....")) }
    val goldNug by lazy { icon(arrayOf("....kkkk....", "...kYYyYk...", "..kYywyYyk..", ".kYyYyYwyYk.", ".kywyYyYyyk.", ".kYyYwyYyYk.", ".kyYyYywyyk.", "..kyYwyYyk..", "..kyYyYyyk..", "...kywyk....", "....kyyk....", ".....kk.....")) }
    val bug by lazy { icon(arrayOf("..k......k..", "...k....k...", "....kkkk....", "...kssssk...", "..krrkkrrk..", ".krrrkkrrrk.", "kkrkrkkrkrkk", ".krrrkkrrrk.", "kkrrrkkrrrkk", ".krkrkkrkrk.", "..krrkkrrk..", "...kkkkkk...")) }
    val hammer by lazy { icon(arrayOf("..kkkkkk....", ".ksSSSSsk...", ".ksSSSSssk..", "..kkknnkkk..", "....knnk....", "....knnk....", "....knnk....", "....knnk....", "....knnk....", "....knnk....", "....kNNk....", "....kkkk....")) }
    val stall by lazy { icon(arrayOf("kkkkkkkkkkkk", "krrwwrrwwrrk", "krrwwrrwwrrk", "kkkkkkkkkkkk", ".kn......nk.", ".kn......nk.", ".kn.gG.yY.k.", ".kkkkkkkkkk.", ".knNNNNNNnk.", ".knNNNNNNnk.", ".knnnnnnnnk.", ".kkkkkkkkkk.")) }
    val arrow by lazy { icon(arrayOf(".....kk.....", "....kGGk....", "...kGggGk...", "..kGggggGk..", ".kGggggggGk.", "kkkkggggkkkk", "...kggggk...", "...kggggk...", "...kggggk...", "...kggggk...", "...kggggk...", "...kkkkkk...")) }
    val hat by lazy { icon(arrayOf("....kkkk....", "...kNNNNk...", "...kNNNNk...", ".kkkrrrrkkk.", "kNNNNNNNNNNk", ".kkkkkkkkkk.", "..kPPPPPPk..", "..kPkPPkPk..", "..kPPPPPPk..", "..kPPrrPPk..", "...kPPPPk...", "....kkkk....")) }
    val clock by lazy { icon(arrayOf("...kkkkkk...", "..kwwwwwwk..", ".kwwwkwwwwk.", "kwwwwkwwwwwk", "kwwwwkwwwwwk", "kwwwwkkkkwwk", "kwwwwwwwwwwk", "kwwwwwwwwwwk", ".kwwwwwwwwk.", "..kwwwwwwk..", "...kkkkkk...", "............")) }
    val lock by lazy { icon(arrayOf("...kkkkkk...", "..ksk..ksk..", "..ks....sk..", "..ks....sk..", ".kkkkkkkkkk.", ".kyyyyyyyyk.", ".kyyykkyyyk.", ".kyyykkyyyk.", ".kyyyykyyyk.", ".kyyyyyyyyk.", ".kooooooook.", ".kkkkkkkkkk.")) }
    val map by lazy { icon(arrayOf("............", "kkkkkkkkkkkk", "kGGgkNNkGGgk", "kGgGkNnkGgGk", "kgGGkNNkgGGk", "kGGgkNnkGGgk", "kGgGkNNkGgGk", "kgGGkNnkgGGk", "kGGgkNNkGGgk", "kkkkkkkkkkkk", "............", "............")) }
    val trash by lazy { icon(arrayOf("....kkkk....", "kkkkkkkkkkkk", "kSSSSSSSSSSk", "kkkkkkkkkkkk", ".kSsSsSsSsk.", ".kSsSsSsSsk.", ".kSsSsSsSsk.", ".kSsSsSsSsk.", ".kSsSsSsSsk.", ".kSsSsSsSsk.", ".kSSSSSSSSk.", "..kkkkkkkk..")) }
    val scissors by lazy { icon(arrayOf("kSk......kSk", "kwSk....kSsk", ".kwSk..kSsk.", "..kwSkkSsk..", "...kwSSsk...", "....kSsk....", "...kkkkkk...", "..krrk.krrk.", ".kr..rkr..rk", ".kr..rkr..rk", "..krrk.krrk.", "...kk...kk..")) }
    val sun by lazy { icon(arrayOf(".....y......", "..y..y..y...", "...kkkkk....", "..kYYYYyk...", "ykYYyyyyoky.", ".kYyyyyyok..", ".kYyyyyyok..", "ykyyyyyyoky.", "..kyyyyok...", "...kkkkk....", "..y..y..y...", ".....y......")) }
    val moon by lazy { icon(arrayOf("....kkkk....", "..kkwwSk....", ".kwwwSk.....", ".kwwSk......", "kwwwSk......", "kwwwSk......", "kwwwwSk.....", "kwwwwwSkkkk.", ".kwwwwwwwSk.", ".kSwwwwwSSk.", "..kkSSSSkk..", "....kkkk....")) }
    val cloud by lazy { icon(arrayOf("............", "............", "....kkkk....", "...kwwwwk...", ".kkwwwwwSkk.", "kwwwwwwwwwSk", "kwwwwwwwwwSk", "kSwwwwwwwSSk", ".kSSSSSSSSk.", "..kkkkkkkk..", "............", "............")) }
    val rain by lazy { icon(arrayOf("....kkkk....", "...kwwwwk...", ".kkwwwwwSkk.", "kwwwwwwwwwSk", "kSwwwwwwwSSk", ".kSSSSSSSSk.", "..kkkkkkkk..", "..b...b...b.", ".b...b...b..", "...b...b....", "..b...b.....", "............")) }
    val trophy by lazy { icon(arrayOf("kkkkkkkkkkkk", "kYkYYYYyykYk", "kYkYyyyyokYk", ".kkYyyyyokk.", "...kyyyyok..", "....kyyok...", ".....kok....", ".....kok....", "....kyyok...", "...kkkkkkk..", "...knnnnnk..", "...kkkkkkk..")) }
    val gift by lazy { icon(arrayOf("...kk..kk...", "..kYYkkYYk..", "...kkYYkk...", "kkkkkYYkkkkk", "krrrrYYrrrrk", "kkkkkYYkkkkk", ".krrrYYrrrk.", ".krrrYYrrrk.", ".krrrYYrrrk.", ".krrrYYrrrk.", ".krrrYYrrrk.", ".kkkkkkkkkk.")) }
    val flask by lazy { icon(arrayOf("...kkkkkk...", "....kwwk....", "....kwwk....", "....kwwk....", "...kwwwwk...", "..kwwwwwwk..", "..kgGggGgk..", ".kgGgggggGk.", ".kggGggGggk.", ".kgggggggGk.", "..kgggggk...", "...kkkkkk...")) }
    val scroll by lazy { icon(arrayOf("..kkkkkkkk..", ".kNwwwwwwNk.", ".kNkkkkkwNk.", "..kwwwwwwk..", "..kwkkkkwk..", "..kwwwwwwk..", "..kwkkkwwk..", "..kwwwwwwk..", "..kwkkkkwk..", ".kNwwwwwwNk.", ".kNkkkkkkNk.", "..kkkkkkkk..")) }
    val chart by lazy { icon(arrayOf("k...........", "k.........Y.", "k........kY.", "k.......kk..", "k..kk..k....", "k.k..kk.....", "kk..........", "k...gg...gg.", "k.gggg.gggg.", "kggggggggggg", "kkkkkkkkkkkk", "............")) }
    val snow by lazy { icon(arrayOf(".....w......", "...w.w.w....", "....www.....", ".w..www..w..", "..wwwwwwww..", "wwwwwSwwwwww", "..wwwwwwww..", ".w..www..w..", "....www.....", "...w.w.w....", ".....w......", "............")) }
    val fog by lazy { icon(arrayOf("............", "..SSSSSSS...", "............", ".SSSSSSSSSS.", "............", "SSSSSSSSS...", "............", "..SSSSSSSSSS", "............", ".SSSSSSSS...", "............", "............")) }

    /** A strain's own leaf-and-bud icon for menus. */
    private val strainIcons = HashMap<String, Bitmap>()
    fun strainIcon(s: Strain): Bitmap = strainIcons.getOrPut(s.id) {
        val px = Px(16, 16)
        fanLeaf(px, 8, 12, -PI / 2, 7, s.leaf, shade(s.leaf, 1.3f))
        px.line(8, 12, 8, 15, shade(s.leaf, 0.6f))
        px.disc(8, 11, 2, s.bud); px.set(7, 10, s.hair); px.set(9, 12, s.hair); px.set(8, 9, shade(s.bud, 1.2f))
        px.outline(shade(s.leaf, 0.3f))
        px.bitmap()
    }
}
