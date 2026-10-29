package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zion Gardens' own wilds: flame trees, golden showers, jacarandas and coconut palms, flower beds striped red, gold and
 * green, hibiscus, heliconia, sunflowers and stone lanterns, lilies on the water and a meadow of flowers underfoot.
 */
object Garden {
    private const val RED = 0xFFE0302A.toInt()
    private const val GOLD = 0xFFFFC82E.toInt()
    private const val GREEN = 0xFF3FAE3A.toInt()
    private const val STONE = 0xFF9A968A.toInt()

    private fun petals(px: Px, fx: Float, fy: Float, r: Float, c: Int, eye: Int) {
        val f = px.s
        for (k in 0 until 5) {
            val a = k * 2 * PI / 5 - PI / 2
            Sprites.fdisc(px, fx + (cos(a) * r * 0.75f).toFloat(), fy + (sin(a) * r * 0.75f).toFloat(), r * 0.62f, if (k < 2) shade(c, 1.15f) else c)
        }
        Sprites.fdisc(px, fx, fy, maxOf(0.8f, r * 0.3f), eye)
        if (f > 1) px.dot(fx.toInt(), fy.toInt() - 1, shade(eye, 1.3f))
    }

    private val treeCache = HashMap<Int, Bitmap>()
    /**
     * A garden tree, 26×40 like any other, foot at (13, 38): 0-1 a flame tree in scarlet bloom, 2 a golden shower
     * dripping with yellow, 3-4 a coconut palm, 5 a jacaranda in violet.
     */
    fun tree(v: Int): Bitmap = treeCache.getOrPut(v) {
        val px = Px(26, 40)
        val f = px.s
        val r = Random(v * 97 + 11)
        val bark = if (v == 5) 0xFF5A4038.toInt() else 0xFF6E4A2C.toInt()
        when (v) {
            3, 4 -> {
                // A palm: a ringed trunk leaning into a curve, fronds drooping from the crown, coconuts under them.
                val lean = if (v == 3) 1f else -1f
                var x = 13f; var y = 38f
                var top = 0f to 0f
                for (i in 0 until 23) {
                    val t = i / 22f
                    x = 13f + lean * (t * t * 3.5f); y = 38f - i
                    val w = 1.7f - t * 0.5f
                    for (fx in ((x - w) * f).toInt()..((x + w) * f).toInt()) {
                        val ring = i % 3 == 0
                        val side = (fx / f.toFloat() - x) / w
                        px.dot(fx, (y * f).toInt(), if (ring) shade(bark, 0.72f) else if (side < -0.4f) shade(bark, 1.25f) else if (side > 0.4f) shade(bark, 0.8f) else bark)
                        px.dot(fx, (y * f).toInt() + 1, if (ring) shade(bark, 0.85f) else bark)
                    }
                    top = x to y
                }
                val (cx, cy) = top
                val frond = 0xFF3E9A3A.toInt()
                for (k in 0 until 8) {
                    val a = -PI / 2 + (k - 3.5) * 0.48 + r.nextDouble() * 0.15
                    // Each frond arcs out and droops at the tip: two leaflets along its line.
                    val len = 6f + r.nextFloat() * 2.5f
                    Sprites.leaflet(px, cx, cy + 0.5f, a + (if (cos(a) > 0) 0.25 else -0.25) + PI * 0.0, len, 1.8f, shade(frond, 0.9f + (k % 3) * 0.08f), shade(frond, 1.3f), shade(frond, 0.7f))
                    val ex = cx + cos(a).toFloat() * len * 0.8f; val ey = cy + sin(a).toFloat() * len * 0.8f + 0.5f
                    Sprites.leaflet(px, ex, ey, a + (if (cos(a) >= 0) 0.9 else -0.9), 4.5f, 1.4f, shade(frond, 0.85f), shade(frond, 1.2f), shade(frond, 0.65f))
                }
                for ((dx, dy) in listOf(-1.2f to 1.6f, 1f to 1.8f, 0f to 2.6f)) Sprites.fdisc(px, (cx + dx) * f, (cy + dy) * f, 1.25f * f / 2f + 0.6f, 0xFF7A5A2A.toInt())
            }
            else -> {
                px.rect(12, 28, 3, 10, bark); px.set(11, 37, bark); px.set(15, 37, bark)
                for (fy in 28 * f until 38 * f) { px.dot(12 * f, fy, shade(bark, 1.25f)); px.dot(15 * f - 1, fy, shade(bark, 0.7f)) }
                // Two limbs forking up into the crown.
                px.line(13, 29, 8, 22, bark); px.line(14, 29, 19, 22, bark); px.line(13, 28, 13, 20, bark)
                when (v) {
                    0, 1 -> {
                        // The flame tree: a broad, flat umbrella of fine leaves, crowned with scarlet.
                        val g = 0xFF3C8E36.toInt()
                        Sprites.foliage(px, 6f, 20f, 5.5f, shade(g, 0.85f), v * 5)
                        Sprites.foliage(px, 20f, 20f, 5.5f, shade(g, 0.85f), v * 5 + 1)
                        Sprites.foliage(px, 13f, 18f, 7.5f, g, v * 5 + 2)
                        Sprites.foliage(px, 9f, 15f, 5f, shade(g, 1.05f), v * 5 + 3)
                        Sprites.foliage(px, 17.5f, 15.5f, 5f, shade(g, 1.05f), v * 5 + 4)
                        val n = if (v == 0) 26 else 38
                        repeat(n) {
                            val a = r.nextDouble() * PI; val d = 0.35f + r.nextFloat() * 0.65f
                            val bx = 13f + (cos(a) * 11.5f * d).toFloat(); val by = 19f - (sin(a) * 8.5f * d).toFloat() + r.nextFloat() * 2f
                            val c = when (r.nextInt(6)) { 0 -> 0xFFFF7A2A.toInt(); 1 -> 0xFFC81E1E.toInt(); else -> RED }
                            Sprites.fdisc(px, bx * f, by * f, 1.3f + r.nextFloat() * 0.9f, c)
                            if (r.nextInt(3) == 0) px.dot((bx * f).toInt() - 1, (by * f).toInt() - 1, 0xFFFFD0A0.toInt())
                        }
                    }
                    2 -> {
                        // A golden shower: a round crown with chains of yellow blossom hanging under it.
                        val g = 0xFF4A9A3A.toInt()
                        Sprites.foliage(px, 13f, 18f, 8.5f, shade(g, 0.88f), 31)
                        Sprites.foliage(px, 9f, 14f, 5.5f, g, 32)
                        Sprites.foliage(px, 17f, 14.5f, 5.5f, g, 33)
                        repeat(11) {
                            val hx = 6f + r.nextFloat() * 14f; val hy = 14f + r.nextFloat() * 8f
                            val len = 3 + r.nextInt(5)
                            for (k in 0 until len * f) {
                                val c = if (k % 3 == 0) 0xFFFFF09A.toInt() else if (k % 3 == 1) GOLD else 0xFFE0A41A.toInt()
                                px.dot((hx * f).toInt() + (if (k % 4 < 2) 0 else 1), (hy * f).toInt() + k, c)
                                px.dot((hx * f).toInt() + (if (k % 4 < 2) 1 else 0), (hy * f).toInt() + k, shade(c, 0.9f))
                            }
                        }
                        repeat(14) { Sprites.fdisc(px, (7f + r.nextFloat() * 12f) * f, (9f + r.nextFloat() * 10f) * f, 1.1f, GOLD) }
                    }
                    else -> {
                        // A jacaranda: a cloud of violet over a little green.
                        val violet = 0xFF9A6ADE.toInt()
                        Sprites.foliage(px, 13f, 19.5f, 7f, shade(violet, 0.72f), 51)
                        Sprites.foliage(px, 7f, 17f, 5.5f, shade(violet, 0.92f), 52)
                        Sprites.foliage(px, 19f, 17.5f, 5.5f, shade(violet, 0.9f), 53)
                        Sprites.foliage(px, 13f, 13f, 7f, violet, 54)
                        repeat(16) { Sprites.fdisc(px, (6f + r.nextFloat() * 14f) * f, (8f + r.nextFloat() * 12f) * f, 1f, 0xFFC8A8FF.toInt()) }
                    }
                }
            }
        }
        px.light()
        px.outline(Sprites.OUT)
        px.bitmap()
    }

    /** The props below stand with their foot on (PROP_AX, PROP_AY). */
    const val PROP_AX = 8
    const val PROP_AY = 12
    const val PROPS = 5
    /** Where a stone lantern's flame sits above its foot, for its glow. */
    const val LANTERN_UP = 7

    private val propCache = HashMap<Int, Bitmap>()
    /**
     * Something growing in the garden, 16×14: 0 a bed in rows of red, gold and green, 1 a hibiscus in flower,
     * 2 heliconia and bird of paradise, 3 sunflowers, 4 a stone lantern.
     */
    fun prop(k: Int): Bitmap = propCache.getOrPut(k) {
        val px = Px(16, 14)
        val f = px.s
        val r = Random(k * 13 + 3)
        when (k) {
            0 -> {
                // A raised bed edged in stone, planted in the colours.
                for (dx in -7..7) for (dy in -3..3) if (dx * dx * 9 + dy * dy * 49 <= 441) px.set(8 + dx, 10 + dy, if (dx * dx * 9 + dy * dy * 49 > 300) STONE else 0xFF4A2E1C.toInt())
                for ((row, c) in listOf(8 to RED, 10 to GOLD, 12 to GREEN)) for (i in 0 until 5) {
                    val bx = (2.5f + i * 2.6f + (row % 4) * 0.4f) * f; val by = (row - 1.2f) * f
                    Sprites.fdisc(px, bx, by + f, 1.4f, 0xFF2E6A2A.toInt())
                    if (c == GREEN) Sprites.leaflet(px, bx / f, by / f + 1, -PI / 2 + (i % 2 - 0.5) * 0.6, 2.6f, 0.9f, 0xFF5ACE4A.toInt(), 0xFFA8F07A.toInt(), 0xFF2E7A2A.toInt(), false)
                    else petals(px, bx, by, 2f, c, if (c == RED) 0xFF3A1A10.toInt() else 0xFFB0601A.toInt())
                }
            }
            1 -> {
                // Hibiscus: a glossy bush with big red trumpets, gold stamens poking out.
                Sprites.foliage(px, 8f, 8.5f, 5.2f, 0xFF2E7A30.toInt(), 61)
                Sprites.foliage(px, 4.5f, 9.5f, 3.2f, 0xFF388A34.toInt(), 62)
                Sprites.foliage(px, 11.5f, 9f, 3.2f, 0xFF388A34.toInt(), 63)
                for ((x, y) in listOf(5 to 6, 10 to 5, 8 to 9, 12 to 9, 4 to 10)) {
                    petals(px, x * f + 1f, y * f + 1f, 2.6f, if ((x + y) % 3 == 0) 0xFFFF4A6A.toInt() else RED, 0xFF7A0A14.toInt())
                    px.dot(x * f + 2, y * f - 1, GOLD); px.dot(x * f + 3, y * f - 2, GOLD)
                }
            }
            2 -> {
                // Heliconia: tall paddle leaves with lobster-claw bracts, and a bird of paradise in front.
                for ((x, a) in listOf(4f to -PI / 2 - 0.35, 8f to -PI / 2 + 0.05, 12f to -PI / 2 + 0.4))
                    Sprites.leaflet(px, x, 13f, a, 11f, 1.8f, 0xFF3A9238.toInt(), 0xFF6AC85A.toInt(), 0xFF2A6A28.toInt(), false)
                for (i in 0 until 4) {
                    val y = 4 + i * 2
                    px.poly(if (i % 2 == 0) RED else 0xFFFF6A2A.toInt(), 6 + i % 2, y, 10 + i % 2, y - 1, 8 + i % 2, y + 1)
                    px.set(9 + i % 2, y - 1, GOLD)
                }
                px.line(3, 13, 3, 8, 0xFF3A7A3A.toInt())
                px.poly(0xFFFF8A1A.toInt(), 3, 8, 1, 4, 4, 6, 2, 3, 5, 7)
                px.poly(0xFF3A6AE0.toInt(), 3, 8, 6, 7, 5, 9)
            }
            3 -> {
                // Sunflowers, three of them, faces to the sun.
                for ((x, h, rad) in listOf(Triple(4, 9, 2.4f), Triple(9, 12, 2.9f), Triple(13, 8, 2.2f))) {
                    px.line(x, 13, x, 13 - h + 2, 0xFF3E8A30.toInt())
                    Sprites.leaflet(px, x + 0.5f, 13f - h / 2f, if (x % 2 == 0) -0.4 else PI + 0.4, 3f, 1.1f, 0xFF3E8A30.toInt(), 0xFF7AC85A.toInt(), 0xFF2A6A28.toInt(), false)
                    val fx = (x + 0.5f) * f; val fy = (13 - h + 1.5f) * f
                    for (k in 0 until 12) {
                        val a = k * PI / 6
                        Sprites.fdisc(px, fx + (cos(a) * rad * f * 0.75f).toFloat(), fy + (sin(a) * rad * f * 0.75f).toFloat(), rad * 0.55f, if (k % 2 == 0) GOLD else 0xFFFFA81A.toInt())
                    }
                    Sprites.fdisc(px, fx, fy, rad * 0.9f, 0xFF5A3418.toInt())
                    px.dot(fx.toInt() - 1, fy.toInt() - 1, 0xFF8A5A2A.toInt())
                }
            }
            else -> {
                // A stone lantern: a footing, a post, a lamp box with a light in it, a curved cap.
                px.rect(5, 12, 6, 1, shade(STONE, 0.8f)); px.rect(6, 11, 4, 1, STONE)
                px.rect(7, 7, 2, 4, STONE)
                px.rect(5, 4, 6, 3, STONE); px.rect(6, 5, 4, 1, 0xFFFFD86A.toInt()); px.set(7, 5, 0xFFFFF4C0.toInt())
                px.poly(shade(STONE, 0.9f), 3, 4, 13, 4, 11, 2, 5, 2)
                px.set(8, 1, STONE)
                // Moss on the cap and the foot.
                for ((x, y) in listOf(4 to 3, 5 to 3, 11 to 3, 6 to 11, 9 to 11)) px.set(x, y, 0xFF5A8A3A.toInt())
            }
        }
        px.light()
        px.outline(Sprites.OUT)
        px.bitmap()
    }

    private fun diamond(x: Float, y: Float) = abs(x - Sprites.TW / 2f) / (Sprites.TW / 2f) + abs(y - Sprites.TH / 2f) / (Sprites.TH / 2f) <= 0.86f

    private val meadowCache = HashMap<Int, Bitmap>()
    /** Flowers scattered over a square of wild grass, laid over the ground tile at its top-left. */
    fun meadow(v: Int): Bitmap = meadowCache.getOrPut(v) {
        val px = Px(Sprites.TW, Sprites.TH)
        val f = px.s
        val r = Random(v * 41 + 9)
        val cols = intArrayOf(RED, GOLD, 0xFFFFF4E0.toInt(), RED, GOLD, 0xFFFF7AB0.toInt(), 0xFFB08AFF.toInt())
        repeat(13 + v % 3 * 3) {
            val x = 3f + r.nextFloat() * (Sprites.TW - 6); val y = 2f + r.nextFloat() * (Sprites.TH - 4)
            if (!diamond(x, y)) return@repeat
            val c = cols[r.nextInt(cols.size)]
            val fx = (x * f).toInt(); val fy = (y * f).toInt()
            // A stem, then a tiny four-petal flower with a gold or dark eye.
            px.dot(fx, fy + 1, 0xFF2A6A28.toInt()); px.dot(fx, fy + 2, 0xFF2A6A28.toInt())
            px.dot(fx - 1, fy, c); px.dot(fx + 1, fy, c); px.dot(fx, fy - 1, shade(c, 1.15f)); px.dot(fx, fy + 1, shade(c, 0.85f))
            px.dot(fx, fy, if (c == GOLD) 0xFF6A3A12.toInt() else GOLD)
        }
        px.bitmap()
    }

    private val lilyCache = HashMap<Int, Bitmap>()
    /** Lily pads, laid over a square of water at its top-left; some carry a pink or white water lily. */
    fun lilies(v: Int): Bitmap = lilyCache.getOrPut(v) {
        val px = Px(Sprites.TW, Sprites.TH)
        val f = px.s
        val r = Random(v * 29 + 1)
        val pad = 0xFF3E9A3A.toInt()
        repeat(2 + v % 2) { i ->
            val x = 8f + r.nextFloat() * 16f; val y = 4.5f + r.nextFloat() * 7f
            if (!diamond(x, y)) return@repeat
            val rad = 2.2f + r.nextFloat() * 1.3f
            val notch = r.nextDouble() * 2 * PI
            for (fy in ((y - rad) * f).toInt()..((y + rad) * f).toInt()) for (fx in ((x - rad * 1.6f) * f).toInt()..((x + rad * 1.6f) * f).toInt()) {
                val dx = (fx + 0.5f) / f - x; val dy = ((fy + 0.5f) / f - y) * 1.6f
                if (dx * dx + dy * dy > rad * rad * 1.6f) continue
                val a = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                if (abs(((a - notch + 3 * PI) % (2 * PI)) - PI) < 0.3 && dx * dx + dy * dy > 0.5f) continue
                px.dot(fx, fy, if (dy < -rad * 0.4f) shade(pad, 1.25f) else if (dx * dx + dy * dy > rad * rad * 1.2f) shade(pad, 0.75f) else pad)
            }
            if ((i + v) % 2 == 0) {
                val c = if (v % 3 == 0) 0xFFFFF4F4.toInt() else 0xFFFF8AC0.toInt()
                val bx = x * f; val by = (y - 0.8f) * f
                for (k in -2..2) Sprites.fdisc(px, bx + k * 1.4f, by - (2 - abs(k)) * 1.2f, 1.3f, if (abs(k) == 2) shade(c, 0.85f) else c)
                px.dot(bx.toInt(), (by - 1).toInt(), GOLD)
            }
        }
        px.light(bands = false)
        px.bitmap()
    }
}
