package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Pheno
import dev.m4s4t0.kushacres.model.Strain
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * The trimming game's pixel art: one big upright cola for the strain, its sugar leaves (as separate sprites so they
 * can glow when in reach and fall when snipped) and a pair of trim scissors.
 */
object TrimArt {
    /** The scene, in art pixels; the bud stands centred, from [TOP] to [TOP] + [LEN]. */
    const val W = 96
    const val H = 124
    const val CX = 48
    const val TOP = 10
    const val LEN = 96
    /** A sugar leaf sprite; its base sits at [LEAF_BASE] (right-pointing leaf) or mirrored (left-pointing). */
    const val LEAF_W = 28
    const val LEAF_H = 14
    const val LEAF_BASE = 2
    const val SHEARS_W = 30
    const val SHEARS_H = 15

    private val cache = HashMap<String, Bitmap>()

    /** Half the bud's width at [t] (0 top .. 1 bottom): a pointed top, fat middle, narrowing to the stem. */
    fun half(t: Float): Float = 15f * (0.16f + 0.84f * max(0f, sin(PI * (0.03 + 0.9 * t)).toFloat()))

    /** Where a leaf at [pos] (0..1 along the bud) roots, on [side] (-1 left, 1 right). */
    fun leafRoot(pos: Float, side: Int): Pair<Float, Float> = (CX + side * (half(pos) - 2.5f)) to (TOP + pos * LEN)

    /** The cola for [s], its buds tinted like the farm sprite's when the plant has a colour phenotype ([Pheno.colour]). */
    fun bud(s: Strain, colour: Int = 0): Bitmap = cache.getOrPut("bud.${s.id}.$colour") {
        val px = Px(W, H)
        val bud = if (colour > 0) mix(s.bud, Pheno.TINTS[colour], 0.6f) else s.bud
        val r = Random(s.id.hashCode())
        val f = px.s.toFloat()
        // The stem and a couple of fan-leaf stubs under the bud.
        val stem = shade(s.leaf, 0.7f)
        px.rect(CX - 2, TOP + LEN - 6, 4, H - (TOP + LEN - 6), stem)
        Sprites.leaflet(px, CX - 1f, TOP + LEN + 8f, PI + 0.5, 14f, 3f, s.leaf, shade(s.leaf, 1.25f), shade(s.leaf, 0.78f))
        Sprites.leaflet(px, CX + 1f, TOP + LEN + 12f, -0.5, 13f, 3f, s.leaf, shade(s.leaf, 1.25f), shade(s.leaf, 0.78f))
        // Calyxes, row by row: overlapping knobs across the width, alternately shaded so the bud reads as clustered.
        var y = TOP.toFloat()
        var row = 0
        while (y <= TOP + LEN) {
            val t = (y - TOP) / LEN
            val hw = half(t)
            val n = max(2, (hw / 3.2f).toInt())
            for (k in 0..n) {
                val cx = CX - hw + 2.4f + (hw * 2 - 4.8f) * k / n + (r.nextFloat() - 0.5f) * 1.4f + if (row % 2 == 0) 0f else 1.2f
                val rad = 2.6f + r.nextFloat() * 1.2f + (if (kotlin.math.abs(cx - CX) < hw * 0.5f) 0.6f else 0f)
                val c = if ((k + row) % 2 == 0) bud else shade(bud, 0.84f)
                Sprites.fdisc(px, cx * f, y * f, rad * f, c)
                px.dot(((cx - rad * 0.4f) * f).toInt(), ((y - rad * 0.4f) * f).toInt(), shade(bud, 1.25f))
            }
            y += 3.2f; row++
        }
        px.light()
        // Pistils curling out of the calyxes, then a dusting of trichome frost on the bud only.
        repeat(70) {
            val t = r.nextFloat()
            val hw = half(t)
            val fx = ((CX + (r.nextFloat() - 0.5f) * hw * 1.9f) * f).toInt()
            val fy = ((TOP + t * LEN) * f).toInt()
            val dx = if (r.nextBoolean()) 1 else -1
            px.dot(fx, fy, s.hair); px.dot(fx + dx, fy - 1, s.hair); px.dot(fx + dx * 2, fy - 1, shade(s.hair, 0.8f))
        }
        repeat(260) {
            val t = r.nextFloat()
            val fx = ((CX + (r.nextFloat() - 0.5f) * half(t) * 1.9f) * f).toInt()
            val fy = ((TOP + t * LEN) * f).toInt()
            if (px.p.getOrNull(fy * px.fw + fx)?.let { it != 0 } == true)
                px.dot(fx, fy, if (r.nextInt(3) == 0) 0xFFFFFFFF.toInt() else 0xFFE4EEDC.toInt())
        }
        px.outline(shade(s.leaf, 0.3f))
        px.bitmap()
    }

    /** A frosty sugar leaf pointing out to [side] (1 right, -1 left); [hot] when the scissors can reach it. */
    fun leaf(s: Strain, side: Int, hot: Boolean): Bitmap = cache.getOrPut("leaf.${s.id}.$side.$hot") {
        val px = Px(LEAF_W, LEAF_H)
        val c = if (hot) shade(s.leaf, 1.3f) else shade(s.leaf, 1.05f)
        val bx = if (side > 0) LEAF_BASE.toFloat() else (LEAF_W - LEAF_BASE).toFloat()
        Sprites.leaflet(px, bx, LEAF_H / 2f + 1, if (side > 0) -0.28 else PI + 0.28, LEAF_W - 4f, 3.6f, c, shade(c, 1.25f), shade(c, 0.75f))
        // Sugar leaves are the frosty ones, which is why they're worth trimming.
        val r = Random(side)
        repeat(22) {
            val fx = r.nextInt(px.fw); val fy = r.nextInt(px.fh)
            if (px.p[fy * px.fw + fx] != 0) px.dot(fx, fy, 0xFFEFF5E8.toInt())
        }
        px.outline(if (hot) 0xFFFFD84A.toInt() else shade(s.leaf, 0.3f))
        px.bitmap()
    }

    /** Trim scissors pointing left, [open] or snapped shut. */
    fun shears(open: Boolean): Bitmap = cache.getOrPut("shears.$open") {
        val px = Px(SHEARS_W, SHEARS_H)
        val steel = 0xFFD8DEE4.toInt(); val edge = 0xFF8A949E.toInt(); val grip = 0xFFE0503C.toInt()
        val py = SHEARS_H / 2
        if (open) {
            px.poly(steel, 14, py - 1, 0, py - 5, 0, py - 4, 14, py + 1)
            px.poly(edge, 14, py - 1, 0, py + 4, 0, py + 5, 14, py + 1)
        } else {
            px.poly(steel, 14, py - 1, 0, py, 14, py)
            px.poly(edge, 14, py, 0, py, 0, py + 1, 14, py + 1)
        }
        // Grips: two red loops behind the pivot.
        val spread = if (open) 4 else 3
        for (d in intArrayOf(-spread, spread)) {
            px.line(14, py, 21, py + d, grip)
            px.disc(24, py + d, 3, grip)
            px.clear(24, py + d)
        }
        px.disc(14, py, 1, 0xFF4A4A52.toInt())
        px.light()
        px.outline(Sprites.OUT)
        px.bitmap()
    }
}
