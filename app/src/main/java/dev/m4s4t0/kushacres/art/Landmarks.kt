package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A drawn landmark: its picture, the point that sits on the footprint's front corner, and the lights and chimney
 * smoke on it. Each light is x, y, rgb and a [Landmarks] light mode; each smoke source is x, y.
 */
class Mark(val bitmap: Bitmap, val w: Int, val ax: Int, val ay: Int, val lights: List<IntArray>, val smoke: List<IntArray>)

/** The one big fixed building on each map (see Feature.Landmark): a barn, a lighthouse, a windmill and so on. */
object Landmarks {
    /** Light modes: a warm flicker, a slow beacon pulse, party lights blinking in turn, and a steady glow. */
    const val FLICKER = 0
    const val BEACON = 1
    const val PARTY = 2
    const val STEADY = 3

    private val cache = HashMap<String, Mark>()

    /** The art for landmark [id], [n] squares across. */
    fun sprite(id: String, n: Int): Mark = cache.getOrPut(id) {
        val k = Kit(n * 32 + 40, 250, n)
        when (id) {
            "barn" -> barn(k); "lookout" -> lookout(k); "mission" -> mission(k); "stiltshack" -> stiltShack(k)
            "lighthouse" -> lighthouse(k); "observatory" -> observatory(k); "growdome" -> growDome(k); "moai" -> moai(k)
            "diner" -> diner(k); "pagoda" -> pagoda(k); "ufo" -> ufo(k); "crypt" -> crypt(k)
            "xmastree" -> xmasTree(k); "balltower" -> ballTower(k); "gazebo" -> gazebo(k); "roundtower" -> roundTower(k)
            "bong" -> bong(k); "egg" -> egg(k); "watertower" -> waterTower(k); "windmill" -> windmill(k)
            "factory" -> factory(k); "watertank" -> waterTank(k); "fountain" -> fountain(k); "potofgold" -> potOfGold(k)
            "balloon" -> balloon(k); "burrow" -> burrow(k); "saloon" -> saloon(k); "ciderbarn" -> ciderBarn(k)
            "stage" -> stage(k)
            else -> k.box(0.5f, 0.5f, n - 1f, n - 1f).run { walls(30, STONE); top(30) }
        }
        k.px.light(bands = false)
        k.px.outline(Sprites.OUT)
        Mark(k.px.bitmap(), k.w, k.ax, k.ay, k.lights, k.smoke)
    }

    private fun c(v: Long) = v.toInt()
    private val STONE = c(0xFFA89E8A)
    private val SLATE = c(0xFF3E4656)
    private val WOOD = c(0xFF8A6238)
    private val DARK = c(0xFF2A2420)
    private val GLASS = c(0xFF9FD8E8)
    private val LIT = c(0xFFF2C25A)
    private val GOLD = c(0xFFF2D04A)
    private val WHITE = c(0xFFEEEAE0)
    private val RED = c(0xFFB83A2E)
    private val GRASS = c(0xFF5E9A40)
    private val SNOW = c(0xFFF4F8FC)

    // Home Acres: grandpa's red barn and its silo.
    private fun barn(k: Kit) {
        val red = c(0xFFA8362A)
        val b = k.box(0.3f, 1.1f, 2.2f, 1.6f)
        b.walls(30, red, planks)
        b.gableL(30, 22, c(0xFF5A4A44), red, planks)
        b.onR(6, 0, 14, 20) { i, j ->
            if (i == 0 || i == 13 || j == 19 || j == 0 || abs(i - j * 13 / 19) < 1 || abs(13 - i - j * 13 / 19) < 1) WHITE else c(0xFF6E241C)
        }
        b.onR(10, 27, 6, 6) { i, j -> if (i == 0 || j == 0 || i == 5 || j == 5) WHITE else DARK }
        k.litL(b, 8, 12, 6, 6); k.litL(b, 22, 12, 6, 6)
        val sx = k.cx(2.5f, 0.35f); val sy = k.cy(2.5f, 0.35f)
        k.cyl(sx, sy, 9f, 56f, c(0xFFB8BCC0), null, rings = 6)
        k.dome(sx, sy - 56, 9f, 8f) { u, _ -> k.round(c(0xFF8E959C), u) }
        k.px.rect(sx.toInt() - 8, sy.toInt() - 4, 16, 2, RED)
    }

    // Redwood Hollow: a fire lookout cabin on tall legs.
    private fun lookout(k: Kit) {
        val leg = c(0xFF6A4A30)
        for ((p, q) in listOf(2.4f to 2.4f, 2.4f to 0.6f, 0.6f to 2.4f, 0.6f to 0.6f)) k.leg(p, q, 72, leg, p2 = (p + 1.5f) / 2 + 0.3f * (p - 1.5f) / 0.9f, q2 = (q + 1.5f) / 2 + 0.3f * (q - 1.5f) / 0.9f)
        for (z in listOf(0, 24, 48)) {
            k.px.line(k.x(0.6f, 0.6f), k.y(0.6f, 0.6f, z), k.x(2.4f, 0.6f), k.y(2.4f, 0.6f, z + 24), leg)
            k.px.line(k.x(0.6f, 0.6f), k.y(0.6f, 0.6f, z), k.x(0.6f, 2.4f), k.y(0.6f, 2.4f, z + 24), shade(leg, 0.8f))
        }
        // A zigzag stair up the side.
        for (j in 0 until 6) k.px.line(k.x(1.2f + (j % 2) * 0.8f, 0.4f), k.y(1.2f + (j % 2) * 0.8f, 0.4f, j * 12), k.x(2f - (j % 2) * 0.8f, 0.4f), k.y(2f - (j % 2) * 0.8f, 0.4f, j * 12 + 12), WOOD)
        k.box(0.5f, 0.5f, 2f, 2f, 72).top(0, WOOD)
        val b = k.box(0.75f, 0.75f, 1.5f, 1.5f, 72)
        b.walls(20, c(0xFF8A6A48), siding)
        b.onL(2, 8, b.a - 4, 9) { i, _ -> if (i % 6 == 0) WHITE else GLASS }
        b.onR(2, 8, b.b - 4, 9) { i, _ -> if (i % 6 == 0) shade(WHITE, 0.8f) else shade(GLASS, 0.8f) }
        b.pyramid(20, 14, c(0xFF4A4A40), 4)
        k.px.line(b.midX(), b.midY(20) - 14, b.midX(), b.midY(20) - 24, DARK)
        k.light(b.cx, b.by - 70 - 12, 0xFFD080)
        k.px.rect(k.x(0.5f, 0.5f) - 1, k.y(0.5f, 0.5f, 72) - 6, 1, 6, WOOD)
    }

    // Dusty Mesa: an old adobe mission with a bell tower, and a cactus.
    private fun mission(k: Kit) {
        val adobe = c(0xFFD8B080); val tile = c(0xFFB0583A)
        val b = k.box(0.6f, 1.1f, 2.1f, 1.5f)
        b.walls(26, adobe)
        b.gableL(26, 12, tile, adobe)
        b.onR(8, 0, 8, 14) { i, j -> if (j >= 11 && (i == 0 || i == 7) || j >= 13 && (i == 1 || i == 6)) 0 else if (i == 0 || i == 7 || j == 13) c(0xFF8A5A2A) else c(0xFF5A3A20) }
        b.onR(10, 20, 4, 4) { _, _ -> c(0xFF6A3A9A) }
        k.light(b.cx + 12, b.yr(b.cx + 12) - 22, 0xC080FF, Landmarks.STEADY)
        val t = k.box(0.3f, 0.3f, 0.9f, 0.9f)
        t.walls(54, shade(adobe, 1.04f))
        t.top(54, shade(adobe, 0.9f))
        t.onL(4, 40, 6, 9) { i, j -> if (j >= 7 && (i == 0 || i == 5)) 0 else DARK }
        k.ell(t.cx - t.a + 7f, t.yl(t.cx - t.a + 7) - 45f, 2.5f, 3f) { u, _ -> k.round(GOLD, u) }
        val mx = t.midX(); val my = t.midY(54)
        k.px.rect(mx, my - 14, 1, 14, DARK); k.px.rect(mx - 3, my - 11, 7, 1, DARK)
        val cx = k.x(2.6f, 0.2f); val cy = k.y(2.6f, 0.2f)
        val cac = c(0xFF4E8A3A)
        k.px.rect(cx - 2, cy - 20, 4, 20, cac); k.px.rect(cx - 7, cy - 14, 3, 6, cac); k.px.rect(cx - 7, cy - 10, 5, 2, cac)
        k.px.rect(cx + 3, cy - 17, 3, 7, cac); k.px.rect(cx + 2, cy - 12, 4, 2, cac)
    }

    // Bayou Bend: a tin-roofed shack on stilts over the swamp, with a little dock.
    private fun stiltShack(k: Kit) {
        k.pad(0.1f, 0.1f, 2.8f, 2.8f, c(0xFF3E5A44)) { cc, x, y -> if ((x * 3 + y * 7) % 23 == 0) shade(cc, 1.3f) else cc }
        val pole = c(0xFF5A4630)
        for ((p, q) in listOf(2.5f to 2.3f, 2.5f to 0.7f, 0.7f to 2.3f, 0.7f to 0.7f, 0.2f to 1f, 0.2f to 2f)) k.leg(p, q, 18, pole)
        k.box(0.1f, 0.9f, 0.6f, 1.2f, 16).top(0, WOOD)
        val b = k.box(0.7f, 0.7f, 1.8f, 1.6f, 18)
        b.walls(22, c(0xFF7A6A52), planks)
        b.gableR(22, 10, c(0xFF9A5A34), c(0xFF7A6A52), planks)
        b.onR(6, 0, 6, 13) { i, j -> if (i == 0 || i == 5 || j == 12) c(0xFF4A3A2A) else c(0xFF2E2620) }
        k.litL(b, 10, 9, 7, 6, frame = c(0xFF4A3A2A))
        k.litR(b, 16, 9, 6, 5, frame = c(0xFF4A3A2A))
        val lx = b.cx + 14; k.px.rect(lx, b.yr(lx) - 18, 2, 3, LIT); k.light(lx, b.yr(lx) - 17, 0xFFB050)
        for (j in 0 until 4) k.px.rect(k.x(0.2f, 1.5f) - 2, k.y(0.2f, 1.5f, j * 4 + 2), 5, 1, WOOD)
    }

    // Sunset Coast: a red and white lighthouse on the rocks, and the keeper's cottage.
    private fun lighthouse(k: Kit) {
        val bx = k.cx(1.5f, 1.7f); val by = k.cy(1.5f, 1.7f)
        k.dome(bx, by + 4, 26f, 12f) { u, v -> k.round(if (((u * 9).toInt() + (v * 5).toInt()) % 3 == 0) c(0xFF7A7068) else c(0xFF8E847A), u) }
        val h = 100f
        k.tower(bx, by - 6, 13f, 8f, h) { z, d -> k.round(if ((z / 17).toInt() % 2 == 0) WHITE else c(0xFFC8302A), d) }
        val gy = by - 6 - h
        k.ell(bx, gy, 12f, 5f) { u, _ -> k.round(DARK, u) }
        k.cyl(bx, gy - 1, 7f, 12f, c(0xFFFFE890), null)
        for (i in -6..6 step 4) k.px.rect(bx.toInt() + i, (gy - 12).toInt(), 1, 11, DARK)
        k.cone(bx, gy - 13, 9f, 11f) { _, d -> k.round(c(0xFFC8302A), d) }
        k.px.rect(bx.toInt(), (gy - 28).toInt(), 1, 5, DARK)
        k.light(bx.toInt(), (gy - 7).toInt(), 0xFFF0A0, BEACON)
        val b = k.box(2f, 0.2f, 0.9f, 1.1f)
        b.walls(16, WHITE, siding)
        b.gableL(16, 9, c(0xFFC8302A), WHITE, siding)
        k.litL(b, 4, 5, 5, 5)
        b.onR(6, 0, 5, 10) { _, _ -> c(0xFF2A4A7A) }
    }

    // Frostpeak: a stone observatory with a silver dome and its telescope out.
    private fun observatory(k: Kit) {
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f) + 4
        k.tower(bx, by, 27f, 27f, 30f, null) { z, d -> k.round(stone(STONE, ((d + 1) * 30).toInt(), z.toInt()), d) }
        k.px.rect(bx.toInt() - 4, by.toInt() - 1, 8, 13, shade(WOOD, 0.75f))
        k.litLight(bx.toInt() - 18, by.toInt() - 14, 4, 5); k.litLight(bx.toInt() + 14, by.toInt() - 14, 4, 5)
        val dy = by - 30
        k.dome(bx, dy, 28f, 25f) { u, v ->
            when {
                v > 0.86f -> SNOW
                u in -0.16f..0.04f && v > 0.12f -> c(0xFF1A2030)
                ((u + 1) * 7).toInt() != ((u + 1.04f) * 7).toInt() -> c(0xFF8A94A0)
                else -> k.round(c(0xFFC8D0DA), u)
            }
        }
        for (i in 0 until 3) k.px.line(bx.toInt() - 2 + i, (dy - 12).toInt(), bx.toInt() - 13 + i, (dy - 33).toInt(), if (i == 2) c(0xFF5A6070) else c(0xFF9AA2B0))
        k.ell(bx - 13f, dy - 34f, 2.5f, 2.5f) { _, _ -> c(0xFF2A3040) }
        k.light(bx.toInt() - 4, (dy - 8).toInt(), 0x90B0FF, STEADY)
    }

    // Emerald Triangle: a geodesic grow dome full of purple light.
    private fun growDome(k: Kit) {
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f) + 6
        for (i in 0 until 7) { val a = i * 0.9f; k.ell(bx + 26 * cos(a), by - 8 + 6 * sin(a), 7f, 9f) { u, _ -> k.round(c(0xFF3E8A34), u) } }
        k.dome(bx, by, 42f, 40f) { u, v ->
            val a = u * 6; val b = v * 6
            val grid = (a + b + 20) % 1 < 0.1f || (a - b + 20) % 1 < 0.1f || (v * 5) % 1 < 0.07f
            if (grid) k.round(c(0xFFD8E4E8), u) else argb(0x80, 0xB0, 0x90, 0xE8)
        }
        k.px.rect(bx.toInt() - 5, by.toInt() + 4, 10, 14, c(0xFF6A5A8A))
        k.px.rect(bx.toInt() - 4, by.toInt() + 5, 8, 13, c(0xFFC8A0F0))
        for ((dx, dz) in listOf(-18 to 14, 14 to 18, -2 to 30, 22 to 6, -26 to 4)) k.light(bx.toInt() + dx, by.toInt() - dz, 0xC060FF, STEADY)
    }

    // Volcano Isle: three old stone heads on a platform, between tiki torches.
    private fun moai(k: Kit) {
        val rock = c(0xFF4A4440)
        k.box(0.6f, 0.2f, 1.4f, 2.6f).run { walls(8, rock, stone); top(8, shade(rock, 1.1f)) }
        val grey = c(0xFF7A6E62)
        // In a row across the face of the platform, so each head's face stays in view.
        for (q in listOf(1.95f, 1.15f, 0.35f)) {
            val h = k.box(1f, q, 0.7f, 0.7f, 8)
            h.walls(40, grey, { cc, x, z -> if ((x * 7 + z * 3) % 17 == 0) shade(cc, 0.86f) else cc })
            h.top(40, shade(grey, 0.9f))
            h.onR(0, 30, h.b, 2) { _, _ -> shade(grey, 0.55f) }
            h.onR(2, 24, 3, 4) { _, _ -> c(0xFF1C1A18) }; h.onR(7, 24, 3, 4) { _, _ -> c(0xFF1C1A18) }
            h.onR(4, 13, 3, 11) { _, j -> shade(grey, if (j == 0) 0.6f else 0.95f) }
            h.onR(2, 6, 8, 1) { _, _ -> shade(grey, 0.5f) }
            k.cyl(h.midX().toFloat(), h.midY(40).toFloat() + 2, 6f, 6f, c(0xFF9A4A34))
        }
        for ((p, q) in listOf(2.7f to 2.6f, 0.2f to 0.3f)) {
            val x = k.x(p, q); val y = k.y(p, q)
            k.px.rect(x, y - 26, 2, 26, WOOD)
            k.ell(x + 1f, y - 29f, 2.5f, 4f) { _, v -> if (v < -0.2f) c(0xFFFFD040) else c(0xFFFF7020) }
            k.light(x + 1, y - 29, 0xFF9030)
        }
    }

    // Neon Valley: an all-night chrome diner with an EAT sign.
    private fun diner(k: Kit) {
        val chrome = c(0xFFC8CCD0)
        val b = k.box(0.5f, 0.6f, 2.2f, 1.6f)
        b.walls(26, chrome, siding)
        b.onL(0, 3, b.a, 4) { _, _ -> c(0xFFD03050) }
        b.onR(0, 3, b.b, 4) { _, _ -> c(0xFFA02040) }
        b.onL(2, 9, b.a - 4, 10) { i, j -> if (i % 7 == 0 || j == 0 || j == 9) c(0xFF8A8E94) else c(0xFFFFE8A0) }
        for (u in listOf(8, 19, 30)) { val x = b.cx - b.a + u; k.light(x, b.yl(x) - 14, 0xFFD890) }
        b.onR(9, 0, 7, 17) { i, j -> if (i == 0 || i == 6 || j == 16) c(0xFF8A8E94) else c(0xFFB0E0F0) }
        b.top(26, c(0xFF7A7E84))
        k.px.rect(b.midX() - 1, b.midY(26) - 14, 2, 14, DARK)
        val sx = b.midX() - 9; val sy = b.midY(26) - 27
        k.px.rect(sx, sy, 19, 11, c(0xFF20203A))
        k.text(sx + 4, sy + 3, "EAT", c(0xFFFF60C0))
        k.light(sx + 9, sy + 5, 0xFF60C0, PARTY)
        k.light(sx + 9, sy + 5, 0x60E0FF, PARTY)
    }

    // Highland Terraces: a red pagoda, four tiers and a gold spire.
    private fun pagoda(k: Kit) {
        k.box(0.3f, 0.3f, 2.4f, 2.4f).run { walls(6, STONE, stone); top(6, shade(STONE, 0.9f)) }
        val red = c(0xFFB8322A); val roof = c(0xFF2E3A3A)
        for (i in 0 until 4) {
            val inset = 0.6f + i * 0.2f
            val b = k.box(inset, inset, 3 - 2 * inset, 3 - 2 * inset, 6 + i * 24)
            b.walls(16, red)
            b.onL(b.a / 2 - 3, 3, 6, 9) { _, _ -> c(0xFF3A1A14) }
            if (i < 3) k.litR(b, b.b / 2 - 2, 5, 4, 6, 0xFFA040, frame = GOLD) else b.onR(b.b / 2 - 2, 5, 4, 6) { _, _ -> c(0xFF3A1A14) }
            b.pyramid(16, if (i == 3) 16 else 9, roof, 7)
            val ey = b.by - 16 + 7
            k.px.rect(b.cx, ey, 1, 3, GOLD); k.light(b.cx, ey + 3, 0xFF6030)
        }
        val tx = k.x(1.5f, 1.5f); val ty = k.y(1.5f, 1.5f, 6 + 3 * 24 + 16 + 14)
        k.px.rect(tx, ty - 16, 1, 16, GOLD)
        for (j in 0 until 3) k.px.disc(tx, ty - 4 - j * 4, 1, GOLD)
    }

    // Cosmic Crater: the flying saucer that came down here, nose first into a mound.
    private fun ufo(k: Kit) {
        val bx = k.cx(2f, 2f); val by = k.cy(2f, 2f) + 6
        k.dome(bx, by, 60f, 14f) { u, v -> k.round(if (v > 0.6f) c(0xFF3A3040) else c(0xFF5A4A50), u) }
        val th = -0.22f; val cs = cos(th); val sn = sin(th)
        val hx = bx; val hy = by - 22
        k.ell(hx + 4, hy - 9, 18f, 15f) { u, v -> if (v > 0.2f) 0 else if (u < -0.4f && v < -0.4f) c(0xFFE0FFFF) else argb(0xD0, 0x60, 0xE0, 0xF0) }
        k.ell(hx + 6, hy - 10, 3f, 4f) { _, _ -> c(0xFF70D060) }
        k.fill((hx - 52).toInt(), (hy - 26).toInt(), (hx + 52).toInt(), (hy + 26).toInt()) { x, y ->
            val dx = x - hx; val dy = y - hy
            val u = (dx * cs + dy * sn) / 50f; val v = (-dx * sn + dy * cs) / 11f
            if (u * u + v * v > 1) 0
            else if (abs(v) < 0.14f) c(0xFF5A6070)
            else k.round(if (v < 0) c(0xFFC8D0DC) else c(0xFF8A92A0), u)
        }
        for (i in 0 until 7) {
            val u = -0.85f + i * 0.28f
            val lx = (hx + u * 50 * cs).toInt(); val ly = (hy + u * 50 * sn).toInt()
            k.px.rect(lx, ly, 2, 2, if (i % 2 == 0) c(0xFFFF60A0) else c(0xFF60FF90))
            k.light(lx + 1, ly + 1, if (i % 3 == 0) 0xFF60A0 else if (i % 3 == 1) 0x60FF90 else 0x60C0FF, PARTY)
        }
        k.light(bx.toInt(), by.toInt() - 4, 0x60FF80, STEADY)
    }

    // Spooky Hollow: a crooked stone chapel, a purple window and a few graves.
    private fun crypt(k: Kit) {
        val st = c(0xFF6A6670)
        for ((i, pq) in listOf(2.6f to 0.5f, 2.6f to 1.6f, 1.9f to 0.3f).withIndex()) {
            val x = k.x(pq.first, pq.second); val y = k.y(pq.first, pq.second)
            k.px.rect(x - 3, y - 8, 6, 8, shade(st, 1.2f - i * 0.1f)); k.px.disc(x, y - 8, 3, shade(st, 1.2f - i * 0.1f))
            k.px.rect(x - 1, y - 7, 2, 1, shade(st, 0.7f))
        }
        val b = k.box(0.6f, 1.1f, 1.9f, 1.4f)
        b.walls(30, st, stone)
        b.gableL(30, 28, c(0xFF2A2A36), st, stone, 3)
        b.onR(b.b / 2 - 3, 9, 7, 15) { i, j ->
            if (j >= 12 && (i == 0 || i == 6) || j >= 14 && (i == 1 || i == 5)) 0
            else if (i == 0 || i == 6) c(0xFF3A3640) else if ((i + j) % 3 == 0) c(0xFF40C0B0) else c(0xFF9A40E0)
        }
        k.light(b.cx + b.b / 2, b.yr(b.cx + b.b / 2) - 18, 0xA050FF, STEADY)
        b.onR(b.b / 2 - 2, 0, 5, 8) { _, _ -> c(0xFF1A1418) }
        k.litL(b, 8, 12, 4, 7, 0xFFB060, frame = c(0xFF3A3640)); k.litL(b, 20, 12, 4, 7, 0xFFB060, frame = c(0xFF3A3640))
        val rx = b.cx + b.b / 2; val ry = b.by - b.b / 4 - 30 - 28
        k.px.rect(rx, ry - 10, 1, 10, c(0xFF4A4650)); k.px.rect(rx - 2, ry - 8, 5, 1, c(0xFF4A4650))
        k.light(k.x(2.2f, 0.9f), k.y(2.2f, 0.9f, 10), 0x80FFB0)
    }

    // North Pole Acres: the great tree, lit every night.
    private fun xmasTree(k: Kit) {
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f) + 4
        k.px.rect(bx.toInt() - 4, by.toInt() - 12, 8, 12, c(0xFF5A3A22))
        val green = c(0xFF1E5A30)
        val tiers = listOf(Triple(0f, 36f, 40f), Triple(24f, 29f, 36f), Triple(48f, 22f, 30f), Triple(70f, 14f, 24f))
        for ((z, r, h) in tiers) k.cone(bx, by - 8 - z, r, h) { zz, d -> if (zz == 0f) shade(green, 0.75f) else if ((zz.toInt() * 7 + ((d + 1) * 19).toInt() * 5) % 23 == 0) SNOW else k.round(if ((zz.toInt() + (d * 6).toInt()) % 7 == 0) shade(green, 1.25f) else green, d) }
        val cols = intArrayOf(0xFFE03030.toInt(), GOLD, 0xFF4080F0.toInt(), 0xFFF060C0.toInt())
        var i = 0
        for ((z, r, _) in tiers) for (d in listOf(-0.7f, -0.25f, 0.2f, 0.65f)) {
            val ox = (bx + d * r * 0.9f).toInt(); val oy = (by - 8 - z - 4 + abs(d) * 3).toInt()
            k.px.disc(ox, oy, 1, cols[i % 4]); k.light(ox, oy, cols[i % 4], PARTY); i++
        }
        val sy = (by - 8 - 70 - 24).toInt(); val sx = bx.toInt()
        k.px.line(sx - 4, sy, sx + 4, sy, GOLD); k.px.line(sx, sy - 4, sx, sy + 4, GOLD); k.px.disc(sx, sy, 1, c(0xFFFFF0A0))
        k.light(sx, sy, 0xFFE070, BEACON)
        for ((pq, col) in listOf((0.3f to 0.7f) to c(0xFFC83030), (0.6f to 0.25f) to c(0xFF2E8A40))) {
            val g = k.box(pq.first, pq.second, 0.4f, 0.4f)
            g.walls(7, col); g.top(7, shade(col, 1.15f))
            g.onL(g.a / 2, 0, 1, 7) { _, _ -> GOLD }; g.onR(g.b / 2, 0, 1, 7) { _, _ -> GOLD }
        }
    }

    // Midnight Square: a glass tower with the ball on top, ready to drop.
    private fun ballTower(k: Kit) {
        val glass = c(0xFF2A3448)
        val b = k.box(0.75f, 0.75f, 1.5f, 1.5f)
        val win: (Int, Int, Int) -> Int = { cc, x, z ->
            if (z % 6 in 2..4 && (x + 400) % 4 != 0) { if (((x + 400) / 4 * 7 + z / 6 * 13) % 5 < 3) mix(cc, c(0xFFF0D890), if (cc == glass) 0.9f else 0.75f) else shade(cc, 1.3f) } else cc
        }
        b.walls(124, glass, win)
        b.top(124, c(0xFF3A4458))
        val mx = b.midX(); val my = b.midY(124)
        k.px.rect(mx - 1, my - 24, 2, 24, c(0xFF8A90A0))
        k.ell(mx.toFloat(), my - 30f, 7f, 7f) { u, v -> if (((u * 5 + 10).toInt() + (v * 5 + 10).toInt()) % 2 == 0) k.round(c(0xFFE8F0FF), u) else k.round(c(0xFF80A0E0), u) }
        k.light(mx, my - 30, 0xFFFFFF, BEACON); k.light(mx, my - 30, 0x80C0FF, PARTY)
        val sx = b.cx - b.a + 2; val sy = b.yl(sx) - 70
        k.px.rect(sx - 4, sy - 12, 17, 9, c(0xFF101018))
        k.text(sx - 2, sy - 10, "HNY", c(0xFFFF50B0))
        k.light(sx + 4, sy - 8, 0xFF50B0, PARTY)
        b.onL(0, 40, b.a, 3) { i, _ -> if ((i / 2) % 3 == 0) c(0xFF60FFE0) else c(0xFF101018) }
        b.onR(0, 40, b.b, 3) { i, _ -> if ((i / 2) % 3 == 1) c(0xFFFF60C0) else c(0xFF101018) }
    }

    // Lovers' Lane: a pink-roofed gazebo strung with fairy lights.
    private fun gazebo(k: Kit) {
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f)
        for ((p, q) in listOf(2.7f to 0.3f, 0.3f to 2.7f)) k.heart(k.cx(p, q), k.cy(p, q) - 8, 7f, c(0xFF3E8A3A))
        k.cyl(bx, by, 30f, 5f, WHITE, c(0xFFC8A880))
        val posts = (0 until 6).map { (it * 60 + 30) * Math.PI.toFloat() / 180 }
        for (a in posts.sortedBy { sin(it) }) k.px.rect((bx + 25 * cos(a)).toInt(), (by - 5 + 12 * sin(a) - 30).toInt(), 2, 30, WHITE)
        k.px.rect(bx.toInt() - 8, by.toInt() - 13, 16, 3, c(0xFFE07090))
        k.cone(bx, by - 34, 34f, 22f) { z, d -> k.round(if (z < 2f && ((d * 10 + 20).toInt() % 2 == 0)) c(0xFFE07898) else c(0xFFF0A0C0), d) }
        k.heart(bx, by - 59, 4f, c(0xFFE03040))
        for (i in 0 until 9) {
            val a = (i / 8f) * Math.PI.toFloat()
            val x = (bx - 32 * cos(a)).toInt(); val y = (by - 33 + 15 * sin(a)).toInt()
            k.px.rect(x, y, 1, 1, c(0xFFFFF0B0)); k.light(x, y, if (i % 2 == 0) 0xFFE8A0 else 0xFF90C0, PARTY)
        }
    }

    // Clover Glen: a thousand-year-old round tower and a Celtic cross.
    private fun roundTower(k: Kit) {
        val bx = k.cx(1f, 1.1f); val by = k.cy(1f, 1.1f)
        k.tower(bx, by, 13f, 11f, 104f, null) { z, d -> k.round(stone(c(0xFF9A9488), ((d + 1) * 20).toInt(), z.toInt()), d) }
        k.cone(bx, by - 104, 12f, 22f) { z, d -> k.round(if (z.toInt() % 4 == 0) c(0xFF5A5650) else c(0xFF6E6A62), d) }
        k.px.rect(bx.toInt() - 2, by.toInt() - 40, 5, 9, c(0xFF2A2420))
        k.px.rect(bx.toInt() - 1, by.toInt() - 41, 3, 1, c(0xFF2A2420))
        for (z in listOf(64, 88)) k.px.rect(bx.toInt() + 3, by.toInt() - z, 2, 4, c(0xFF2A2420))
        k.litLight(bx.toInt() - 1, by.toInt() - 98, 2, 3)
        val cx = k.x(1.7f, 0.2f); val cy = k.y(1.7f, 0.2f); val g = c(0xFF8A8A82)
        k.px.rect(cx - 1, cy - 24, 3, 24, g); k.px.rect(cx - 6, cy - 19, 13, 3, g)
        for (a in 0 until 24) { val t = a * 0.2618f; k.px.set(cx + 1 + (4 * cos(t)).roundToInt() - 1 + 1, cy - 18 + (4 * sin(t)).roundToInt(), g) }
        k.px.rect(cx - 3, cy - 3, 7, 3, shade(g, 0.85f))
    }

    // 420 Hill: the county's big bong monument, still smoking.
    private fun bong(k: Kit) {
        val b = k.box(0.25f, 0.25f, 1.5f, 1.5f)
        b.walls(14, STONE, stone); b.top(14, shade(STONE, 0.92f))
        k.textL(b, 6, 4, "420", c(0xFF2E7A30))
        val bx = k.cx(1f, 1f); val by = k.cy(1f, 1f) - 14
        val glass = argb(0xB0, 0xA8, 0xE0, 0xD8)
        k.tower(bx, by - 18, 6f, 6f, 56f, argb(0xC0, 0xD0, 0xF0, 0xF0)) { z, d -> if (z.toInt() in 20..23) c(0xFF3AB04A) else if (z.toInt() in 30..33) GOLD else if (z.toInt() in 40..43) c(0xFFD03030) else if (d < -0.5f) c(0xFFE8FFFF) else glass }
        k.px.line(bx.toInt() - 8, by.toInt() - 10, bx.toInt() - 20, by.toInt() - 24, c(0xFF90C8C0))
        k.px.line(bx.toInt() - 8, by.toInt() - 9, bx.toInt() - 20, by.toInt() - 23, c(0xFF70A8A0))
        k.ell(bx - 21, by - 26, 5f, 3f) { u, v -> if (v < -0.2f) c(0xFFFF7020) else k.round(c(0xFF6A9890), u) }
        k.light(bx.toInt() - 21, by.toInt() - 27, 0xFF8030)
        k.smoke(bx.toInt() - 21, by.toInt() - 28)
        k.ell(bx, by - 10, 13f, 12f) { u, v -> if (v > 0.15f) mix(c(0xFF3AA070), c(0xFF206A4A), (v + 1) / 2) else if (u < -0.4f && v < -0.3f) c(0xFFF0FFFF) else glass }
    }

    // Easter Meadow: a giant painted egg in a straw nest.
    private fun egg(k: Kit) {
        val bx = k.cx(1f, 1f); val by = k.cy(1f, 1f)
        val straw: (Float, Float) -> Int = { u, v -> if (((u * 13 + v * 7) * 3).toInt() % 3 == 0) c(0xFFB08A3A) else c(0xFFD8B860) }
        k.ell(bx, by - 4, 30f, 12f) { u, v -> if (v < 0.2f) straw(u, v) else 0 }
        val bands = intArrayOf(0xFFF0A8C8.toInt(), 0xFFA8E8C8.toInt(), 0xFFC8B0F0.toInt(), 0xFFF8E890.toInt(), 0xFF98D0F0.toInt())
        k.ell(bx, by - 30, 20f, 28f) { u, v ->
            val zig = abs(((u * 6 + 20) % 2) - 1) * 0.18f
            val band = ((v + 1) * 3.2f + zig).toInt()
            val col = bands[band.coerceIn(0, 4)]
            k.round(if (band == 2 && ((u * 10 + 20).toInt() % 3 == 0) && ((v * 14 + 20).toInt() % 3 == 0)) WHITE else col, u)
        }
        k.ell(bx, by - 2, 31f, 12f) { u, v -> if (v >= 0.2f) shade(straw(u, v), 0.9f) else 0 }
    }

    // Liberty Fields: a stars-and-stripes water tower with a red beacon.
    private fun waterTower(k: Kit) {
        val steel = c(0xFF7A8088)
        for ((p, q) in listOf(0.6f to 0.6f, 2.4f to 0.6f, 0.6f to 2.4f, 2.4f to 2.4f).sortedBy { -(it.first + it.second) }) k.leg(p, q, 70, steel, p2 = 1.5f + (p - 1.5f) * 0.6f, q2 = 1.5f + (q - 1.5f) * 0.6f)
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f) - 70
        k.px.line(k.x(0.8f, 0.8f), k.y(0.8f, 0.8f, 35), k.x(2.2f, 2.2f), k.y(2.2f, 2.2f, 35), steel)
        k.tower(bx, by, 26f, 26f, 28f, null) { z, d ->
            val col = if (d < -0.1f && z > 14) { if ((z.toInt() + (d * 20).toInt()) % 5 == 0) WHITE else c(0xFF2A4A9A) } else if ((z.toInt() / 4) % 2 == 0) c(0xFFC0302A) else WHITE
            k.round(col, d)
        }
        k.cone(bx, by - 28, 28f, 16f) { _, d -> k.round(c(0xFF8A9098), d) }
        k.px.rect(bx.toInt(), (by - 50).toInt(), 1, 6, steel)
        k.px.rect(bx.toInt() - 1, (by - 51).toInt(), 3, 2, c(0xFFFF3020))
        k.light(bx.toInt(), (by - 50).toInt(), 0xFF2020, BEACON)
    }

    // Prairie Ridge: an old Dutch windmill, sails turning with the seasons.
    private fun windmill(k: Kit) {
        for ((p, q) in listOf(2.6f to 0.4f, 2.6f to 0.9f, 0.4f to 2.6f)) k.ell(k.cx(p, q), k.cy(p, q) - 4, 6f, 5f) { u, v -> k.round(if (((u + v) * 8).toInt() % 3 == 0) c(0xFFB89838) else c(0xFFE0C058), u) }
        val bx = k.cx(1.4f, 1.5f); val by = k.cy(1.4f, 1.5f)
        k.tower(bx, by, 22f, 14f, 78f, null) { z, d -> k.round(siding(c(0xFFE8E0D0), 0, z.toInt()), d) }
        k.px.rect(bx.toInt() - 4, by.toInt() - 14, 8, 14, c(0xFF5A3A22))
        k.litLight(bx.toInt() - 2, by.toInt() - 44, 4, 5)
        k.dome(bx, by - 78, 17f, 14f) { u, _ -> k.round(c(0xFF6A3A2A), u) }
        val hx = bx.toInt() - 6; val hy = by.toInt() - 82
        val wood = c(0xFF7A5A3A); val cloth = c(0xFFE8DCC0)
        for (a in listOf(0.35f, 1.92f, 3.49f, 5.06f)) {
            val ex = (hx + 52 * cos(a) * 0.55f).roundToInt(); val ey = (hy + 52 * sin(a)).roundToInt()
            k.px.line(hx, hy, ex, ey, wood)
            for (t in 3..10) {
                val f = t / 10f
                val sx = (hx + (ex - hx) * f).roundToInt(); val sy = (hy + (ey - hy) * f).roundToInt()
                for (w in 1..10) { val nx = (-sin(a) * 0.55f * w).roundToInt(); val ny = (cos(a) * w).roundToInt(); k.px.set(sx + nx, sy + ny, if (t % 3 == 0 || w == 10) wood else cloth) }
            }
        }
        k.px.disc(hx, hy, 2, DARK)
    }

    // Frosty Peaks: Santa's toy factory, chimney going all winter.
    private fun factory(k: Kit) {
        val brick0 = c(0xFF9A3A2E)
        val b = k.box(0.5f, 0.8f, 3f, 2.6f)
        b.walls(40, brick0, brick)
        b.gableL(40, 26, SNOW, brick0, brick, 3)
        for (u in listOf(10, 30, 50, 70)) k.litL(b, u, 16, 7, 10, 0xFFD070, frame = c(0xFF2E6A3A))
        b.onR(b.b / 2 - 6, 0, 12, 20) { i, j -> if (i == 0 || i == 11 || j == 19) (if ((i + j) / 2 % 2 == 0) RED else WHITE) else c(0xFF2E6A3A) }
        k.light(b.cx + b.b / 2, b.yr(b.cx + b.b / 2) - 10, 0xFFD070)
        k.textL(b, 30, 30, "TOYS", GOLD)
        val sx = b.cx - b.a + 18; val sy = b.yl(sx) - 40 - 20
        k.px.rect(sx, sy - 24, 8, 30, brick0); k.px.rect(sx - 1, sy - 26, 10, 3, shade(brick0, 0.8f)); k.px.rect(sx - 1, sy - 27, 10, 1, SNOW)
        k.smoke(sx + 4, sy - 28)
        for ((p, q) in listOf(3.7f to 0.3f, 0.3f to 3.7f)) {
            val g = k.box(p - 0.2f, q - 0.2f, 0.4f, 0.4f)
            g.walls(6, if (p > q) c(0xFF3070C0) else c(0xFFC8A030)); g.top(6, WHITE)
        }
    }

    // Hydro Haven: a cedar water tank on stilts with a neon GROW sign.
    private fun waterTank(k: Kit) {
        val leg = c(0xFF5A4A3A)
        for ((p, q) in listOf(0.7f to 0.7f, 2.3f to 0.7f, 0.7f to 2.3f, 2.3f to 2.3f).sortedBy { -(it.first + it.second) }) k.leg(p, q, 50, leg)
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f) - 50
        k.cyl(bx, by + 2, 34f, 3f, c(0xFF6A5A48), null)
        k.tower(bx, by, 28f, 26f, 40f, null) { z, d -> k.round(if (z.toInt() % 10 == 4) c(0xFF3A3A40) else planks(c(0xFF9A6A40), (d * 30).toInt(), 0), d) }
        k.cone(bx, by - 40, 29f, 12f) { _, d -> k.round(c(0xFF4A5A4A), d) }
        val sx = bx.toInt() - 10; val sy = by.toInt() - 22
        k.px.rect(sx - 2, sy - 2, 20, 9, c(0xFF101418))
        k.text(sx, sy, "GROW", c(0xFF60FF70))
        k.light(sx + 8, sy + 2, 0x60FF70, PARTY)
        k.px.line(bx.toInt() + 22, by.toInt() + 6, bx.toInt() + 22, k.cy(1.5f, 1.5f).toInt(), c(0xFF3A3A40))
    }

    // Sunset Valley: a three-tier fountain, lit blue at night.
    private fun fountain(k: Kit) {
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f)
        for ((p, q) in listOf(2.6f to 0.4f, 0.4f to 2.6f, 2.7f to 1.4f)) { val x = k.x(p, q); val y = k.y(p, q); k.px.rect(x, y - 6, 1, 6, c(0xFF3A7A30)); k.px.disc(x, y - 7, 2, c(0xFFD02848)) }
        k.cyl(bx, by, 40f, 9f, c(0xFFC8C0B0), c(0xFF4A90D0))
        k.ell(bx, by - 9, 34f, 13f) { u, v -> if (((u * 9 + 20).toInt() + (v * 5 + 20).toInt()) % 9 == 0) GOLD else if (v < -0.4f) c(0xFF80C0F0) else c(0xFF4A90D0) }
        k.cyl(bx, by - 9, 5f, 26f, c(0xFFC8C0B0), null)
        k.cyl(bx, by - 30, 20f, 5f, c(0xFFC8C0B0), c(0xFF5AA0E0))
        k.cyl(bx, by - 35, 3f, 18f, c(0xFFC8C0B0), null)
        k.cyl(bx, by - 49, 10f, 4f, c(0xFFC8C0B0), c(0xFF5AA0E0))
        val jet = c(0xFFB8E0FF)
        k.px.rect(bx.toInt(), (by - 62).toInt(), 1, 10, jet)
        for (d in listOf(-1, 1)) {
            for (t in 0..10) { val x = bx.toInt() + d * (10 + t); val y = (by - 52 + t * t * 0.18f).toInt(); k.px.set(x, y, jet) }
            for (t in 0..12) { val x = bx.toInt() + d * (20 + t); val y = (by - 32 + t * t * 0.14f).toInt(); k.px.set(x, y, jet) }
        }
        k.light(bx.toInt(), (by - 20).toInt(), 0x60A0FF, STEADY)
    }

    // Rainbow Ridge: the end of the rainbow, and the pot that waits there.
    private fun potOfGold(k: Kit) {
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f)
        val bands = intArrayOf(0xFFE03030.toInt(), 0xFFF08830.toInt(), 0xFFF0D840.toInt(), 0xFF50B848.toInt(), 0xFF3870E0.toInt(), 0xFF5A40B0.toInt(), 0xFF9A50D0.toInt())
        val ox = bx - 16; val oy = by - 6
        k.fill((ox - 56).toInt(), (oy - 82).toInt(), (ox + 56).toInt(), oy.toInt()) { x, y ->
            val d = sqrt((x - ox) * (x - ox) + (y - oy) * (y - oy) * 1.6f)
            val i = ((56 - d) / 4).toInt()
            if (d <= 56 && i in 0..6 && y <= oy) bands[i] else 0
        }
        val px = bx + 20; val py = by + 2
        k.ell(px, py - 12, 15f, 13f) { u, _ -> k.round(c(0xFF2A2A2E), u) }
        k.ell(px, py - 22, 14f, 4f) { u, v -> if (((u * 7 + 20).toInt() + (v * 3 + 20).toInt()) % 3 == 0) c(0xFFFFF0A0) else k.round(GOLD, u) }
        for ((dx, dy) in listOf(-6 to -26, 2 to -27, 7 to -25, -1 to -29)) k.px.disc(px.toInt() + dx, py.toInt() + dy, 2, GOLD)
        k.light(px.toInt(), py.toInt() - 24, 0xFFD040, STEADY)
        for (dx in listOf(-22, 18)) k.px.disc(px.toInt() + dx, py.toInt() - 1, 2, GOLD)
    }

    // Harvest Hills: a hot-air balloon tethered for the festival.
    private fun balloon(k: Kit) {
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f)
        val rope = c(0xFF6A5A48)
        for ((p, q) in listOf(0.4f to 1.5f, 2.6f to 1.5f)) k.px.line(k.x(p, q), k.y(p, q), bx.toInt() + if (p < 1) 6 else -6, by.toInt() - 30, rope)
        val bk = k.box(1.2f, 1.2f, 0.6f, 0.6f, 22)
        bk.walls(12, c(0xFFA87A40), planks); bk.top(12, c(0xFF5A3A22))
        val gores = intArrayOf(0xFFD8402A.toInt(), 0xFFF0B030.toInt(), 0xFFD8402A.toInt(), 0xFF3A7AC8.toInt(), 0xFFF0B030.toInt(), 0xFF3A7AC8.toInt())
        val ey = by - 92
        for (d in listOf(-1, 1)) k.px.line(bx.toInt() + d * 6, by.toInt() - 36, bx.toInt() + d * 12, (ey + 40).toInt(), rope)
        k.fill((bx - 44).toInt(), (ey - 48).toInt(), (bx + 44).toInt(), (ey + 46).toInt()) { x, y ->
            val v = (y - ey) / 46f
            val wid = if (v < 0.35f) sqrt((1 - (v + 0.05f) * (v + 0.05f) / 1.05f).coerceAtLeast(0f)) else (1 - (v - 0.35f) / 0.65f) * 0.89f + 0.12f * (v - 0.35f) / 0.65f
            val u = (x - bx) / (42f * wid)
            if (abs(u) > 1 || v < -1 || v > 1) 0 else {
                val g = ((Math.asin(u.toDouble()) / Math.PI + 0.5) * 6).toInt().coerceIn(0, 5)
                k.round(if (v in 0.5f..0.6f) GOLD else gores[g], u)
            }
        }
        k.px.rect(bx.toInt() - 3, by.toInt() - 44, 6, 4, c(0xFF3A3A40))
        k.px.rect(bx.toInt() - 1, by.toInt() - 47, 2, 3, c(0xFFFFA030))
        k.light(bx.toInt(), by.toInt() - 46, 0xFF9030)
    }

    // Shire Hollow: a round green door in a grassy hill, pipe smoke from the chimney.
    private fun burrow(k: Kit) {
        val bx = k.cx(1.5f, 1.5f); val by = k.cy(1.5f, 1.5f) + 4
        k.dome(bx, by, 52f, 36f) { u, v -> k.round(if ((((u + 2) * 23).toInt() * 7 + ((v + 2) * 11).toInt()) % 11 == 0) c(0xFF4A8A34) else GRASS, u) }
        val dx = bx.toInt() - 20; val dy = by.toInt() - 4
        k.px.rect(dx - 5, dy - 4, 26, 4, c(0xFFB0A890))
        k.ell(dx.toFloat(), dy - 14f, 11f, 12f) { u, v -> if (u * u + v * v > 0.7f) c(0xFF7A5A38) else if (abs(u) < 0.1f && abs(v) < 0.1f) GOLD else c(0xFF2E7A3A) }
        k.ell(dx + 30f, dy - 18f, 6f, 6f) { u, v -> if (u * u + v * v > 0.55f) c(0xFF7A5A38) else if (abs(u) < 0.08f || abs(v) < 0.08f) c(0xFF7A5A38) else LIT }
        k.light(dx + 30, dy - 18, 0xFFC060)
        val sx = bx.toInt() + 14; val sy = by.toInt() - 34
        k.px.rect(sx, sy - 10, 4, 10, c(0xFF8A6A50)); k.px.rect(sx - 1, sy - 11, 6, 2, c(0xFF6A5040))
        k.smoke(sx + 2, sy - 12)
        for ((p, q, col) in listOf(Triple(2.7f, 0.3f, c(0xFFE04040)), Triple(2.8f, 0.6f, c(0xFFF0D040)), Triple(0.3f, 2.7f, c(0xFFF080C0)))) k.px.disc(k.x(p, q), k.y(p, q) - 2, 2, col)
    }

    // Dusty Gulch: a false-fronted saloon with swinging doors.
    private fun saloon(k: Kit) {
        val wood = c(0xFFA8784A)
        val b = k.box(0.9f, 0.5f, 1.8f, 2.2f)
        b.walls(38, wood, siding)
        b.top(38, shade(wood, 0.7f))
        b.onR(-2, 38, b.b + 2, 14) { i, j -> if (j > 10 && (i < 6 || i > b.b - 6)) 0 else if (j > 12 && (i < 10 || i > b.b - 10)) 0 else shade(wood, if (j % 4 == 0) 0.66f else 0.74f) }
        val sx = b.cx + 6; val sy = b.yr(sx) - 46
        for ((n, ch) in "SALOON".withIndex()) {
            val g = FONT[ch] ?: continue
            for (j in 0 until 5) for (i in 0 until 3) if (g[j * 3 + i] == '#') k.px.set(sx + n * 4 + i, sy + j + (n * 4 + i) / 2, c(0xFFF0E0B0))
        }
        b.onR(b.b / 2 - 5, 4, 10, 10) { i, j -> if (j % 3 == 0 || i == 0 || i == 9) DARK else if (i == 4 || i == 5) 0 else c(0xFF8A5A30) }
        k.litR(b, 6, 12, 6, 8, 0xFFC060); k.litR(b, b.b - 12, 12, 6, 8, 0xFFC060)
        k.litL(b, 12, 16, 6, 8, 0xFFC060)
        k.px.rect(k.x(2.8f, 0.2f), k.y(2.8f, 0.2f) - 7, 14, 2, WOOD)
        for (d in listOf(1, 12)) k.px.rect(k.x(2.8f, 0.2f) + d, k.y(2.8f, 0.2f) - 7, 1, 7, DARK)
    }

    // Orchard Grove: the cider barn, press out front and barrels stacked.
    private fun ciderBarn(k: Kit) {
        val wall = c(0xFFB88A58)
        val b = k.box(0.5f, 0.5f, 1.8f, 2.3f)
        b.walls(28, wall, planks)
        b.gableR(28, 24, c(0xFF3A6A3A), wall, planks, 3)
        b.onL(b.a / 2 - 7, 0, 14, 20) { i, j -> if (i == 0 || i == 13 || j == 19 || i == j * 13 / 19 || i == 13 - j * 13 / 19) c(0xFFEEE0C8) else c(0xFF7A3A2A) }
        k.litR(b, 12, 12, 6, 8); k.litR(b, b.b - 18, 12, 6, 8)
        for ((p, q) in listOf(2.6f to 1.1f, 2.6f to 1.7f, 2.6f to 2.3f)) {
            k.tower(k.cx(p, q), k.cy(p, q), 6f, 6f, 11f, c(0xFF7A5030)) { z, d -> k.round(if (z.toInt() == 2 || z.toInt() == 8) c(0xFF3A3A3A) else c(0xFF9A6A3A), d) }
        }
        k.tower(k.cx(2.7f, 0.4f) + 3, k.cy(2.7f, 0.4f) - 9, 6f, 6f, 12f, c(0xFF8A3020)) { z, d -> k.round(if (z.toInt() == 1 || z.toInt() == 10) c(0xFF3A3A3A) else c(0xFFA87038), d) }
        for ((dx, dy) in listOf(0 to 0, 6 to -2, -5 to -1)) k.px.disc(k.x(2.6f, 0.2f) + dx, k.y(2.6f, 0.2f) + dy - 2, 2, if (dx == 0) RED else c(0xFF70B030))
    }

    // Zion Gardens: an open-air stage in red, gold and green, sound system up.
    private fun stage(k: Kit) {
        val b = k.box(0.4f, 0.4f, 2.4f, 2.2f)
        b.walls(10, c(0xFF5A4030), planks); b.top(10, c(0xFF7A5A3A))
        val back = k.box(0.4f, 2.4f, 2.4f, 0.2f, 10)
        back.walls(46, c(0xFF2A2A2A))
        back.onL(0, 0, back.a, 46) { i, j -> when (j * 3 / 46) { 0 -> c(0xFF2E8A3A); 1 -> c(0xFFF0C030); else -> c(0xFFC8302A) } }
        k.ell(back.cx - back.a / 2f, back.yl(back.cx - back.a / 2) - 33f, 6f, 6f) { u, v -> if (abs(u) < 0.2f && v > -0.7f || v in -0.1f..0.2f && abs(u) < 0.6f) c(0xFF2E8A3A) else 0 }
        for (u in listOf(4, b.a - 14)) {
            val s = k.box(0.6f + u / 32f * 1.4f - if (u > 10) 0.2f else 0f, 2.0f, 0.4f, 0.35f, 10)
            s.walls(22, c(0xFF202024)); s.top(22, c(0xFF303034))
            s.onR(1, 4, 4, 4) { i, j -> if ((i - 1.5f) * (i - 1.5f) + (j - 1.5f) * (j - 1.5f) < 3) c(0xFF505058) else c(0xFF202024) }
            s.onR(1, 13, 4, 4) { i, j -> if ((i - 1.5f) * (i - 1.5f) + (j - 1.5f) * (j - 1.5f) < 3) c(0xFF505058) else c(0xFF202024) }
        }
        val top = back.yl(back.cx - back.a) - 56
        k.px.line(back.cx - back.a, top, back.cx, top + back.a / 2, c(0xFF404048))
        val cols = intArrayOf(0xFFFF3030.toInt(), 0xFFFFD030.toInt(), 0xFF30FF60.toInt())
        for (i in 0 until 6) {
            val x = back.cx - back.a + 6 + i * (back.a - 12) / 5; val y = top + (x - back.cx + back.a) / 2 + 2
            k.px.rect(x - 1, y, 3, 2, cols[i % 3]); k.light(x, y + 1, cols[i % 3], PARTY)
        }
    }

    // Wall textures: given the face colour, the x across and the height up the wall.
    private val plain: (Int, Int, Int) -> Int = { c, _, _ -> c }
    private val planks: (Int, Int, Int) -> Int = { c, x, _ -> if ((x + 400) % 4 == 0) shade(c, 0.8f) else c }
    private val siding: (Int, Int, Int) -> Int = { c, _, z -> if (z % 4 == 0) shade(c, 0.82f) else c }
    private val brick: (Int, Int, Int) -> Int = { c, x, z -> if (z % 3 == 0 || (x + 400 + (z / 3) % 2 * 3) % 6 == 0) shade(c, 0.78f) else c }
    private val stone: (Int, Int, Int) -> Int = { c, x, z ->
        val row = z / 5
        if (z % 5 == 0 || (x + row * 5 + 400) % 9 == 0) shade(c, 0.8f) else if ((x / 9 * 7 + row * 13) % 11 == 0) shade(c, 0.9f) else c
    }

    /** One landmark's drawing: the pixels, the footprint's front corner at [ax],[ay], and the lights and smoke. */
    private class Kit(val w: Int, val h: Int, val n: Int) {
        val px = Px(w, h)
        val ax = w / 2
        val ay = h - 4
        val lights = ArrayList<IntArray>()
        val smoke = ArrayList<IntArray>()
        fun light(x: Int, y: Int, rgb: Int, mode: Int = FLICKER) { lights += intArrayOf(x, y, rgb and 0xFFFFFF, mode) }
        fun smoke(x: Int, y: Int) { smoke += intArrayOf(x, y) }

        /** The screen x and y of footprint point [p] squares along the left edge and [q] along the right, [z] up. */
        fun x(p: Float, q: Float) = (ax + (q - p) * 16).roundToInt()
        fun y(p: Float, q: Float, z: Int = 0) = (ay - (p + q) * 8).roundToInt() - z

        /** A box on the footprint from [p],[q], [dp] squares deep to the left and [dq] to the right, standing [z] up. */
        fun box(p: Float, q: Float, dp: Float, dq: Float, z: Int = 0) =
            Box(px, x(p, q), y(p, q, z), (dp * 16).roundToInt(), (dq * 16).roundToInt())

        /** Paints every fine pixel in the art-pixel rectangle [l],[t]..[r],[b] that [f] gives a colour for (0 skips). */
        fun fill(l: Int, t: Int, r: Int, b: Int, f: (Float, Float) -> Int) {
            for (fy in t * S until (b + 1) * S) for (fx in l * S until (r + 1) * S) {
                val c = f((fx + 0.5f) / S, (fy + 0.5f) / S)
                if (c != 0) px.dot(fx, fy, c)
            }
        }

        /** Rounded shading: lit on the left, dark on the right, for [d] from -1 (left edge) to 1. */
        fun round(c: Int, d: Float) = shade(c, 1.18f - 0.42f * (d + 1) / 2 - if (d > 0.75f) 0.1f else 0f)

        /**
         * An upright round tower, [r0] wide at the foot ([bx],[by]) narrowing to [r1] at [h] up. [col] colours it by
         * height and left-right (-1..1); [cap] fills the top, if it shows.
         */
        fun tower(bx: Float, by: Float, r0: Float, r1: Float, h: Float, cap: Int? = null, col: (Float, Float) -> Int) {
            val r = maxOf(r0, r1)
            fill((bx - r - 1).toInt(), (by - h - r).toInt(), (bx + r + 1).toInt(), (by + r).toInt()) { x, y ->
                val z = by - y
                val rz = r0 + (r1 - r0) * (z.coerceIn(0f, h) / h)
                val dx = x - bx
                if (abs(dx) > rz) return@fill 0
                val e = rz / 2 * sqrt(1 - (dx / rz) * (dx / rz))
                when {
                    z < -e || z > h + e -> 0
                    cap != null && z > h - e -> cap
                    else -> col(z, dx / rz)
                }
            }
        }

        fun cyl(bx: Float, by: Float, r: Float, h: Float, c: Int, cap: Int? = shade(c, 1.1f), rings: Int = 0) =
            tower(bx, by, r, r, h, cap) { z, d -> round(if (rings > 0 && z.toInt() % rings == 0) shade(c, 0.82f) else c, d) }

        /** A round cone [h] tall on a base [r] wide centred on [bx],[by]. */
        fun cone(bx: Float, by: Float, r: Float, h: Float, col: (Float, Float) -> Int) =
            fill((bx - r - 1).toInt(), (by - h - 1).toInt(), (bx + r + 1).toInt(), (by + r / 2 + 1).toInt()) { x, y ->
                val z = by - y; val dx = x - bx
                if (z >= 0) { val rz = r * (1 - z / h); if (z < h && abs(dx) <= rz) col(z, dx / rz) else 0 }
                else if (abs(dx) <= r && -z <= r / 2 * sqrt(1 - (dx / r) * (dx / r))) col(0f, dx / r) else 0
            }

        /** An ellipse [rx] by [ry] round [ex],[ey]; [col] gets the offset across and down, each -1..1. */
        fun ell(ex: Float, ey: Float, rx: Float, ry: Float, col: (Float, Float) -> Int) =
            fill((ex - rx - 1).toInt(), (ey - ry - 1).toInt(), (ex + rx + 1).toInt(), (ey + ry + 1).toInt()) { x, y ->
                val u = (x - ex) / rx; val v = (y - ey) / ry
                if (u * u + v * v <= 1) col(u, v) else 0
            }

        /** A dome [h] tall over a round base [r] wide. */
        fun dome(bx: Float, by: Float, r: Float, h: Float, col: (Float, Float) -> Int) =
            fill((bx - r - 1).toInt(), (by - h - 1).toInt(), (bx + r + 1).toInt(), (by + r / 2 + 1).toInt()) { x, y ->
                val u = (x - bx) / r; val z = by - y
                if (abs(u) > 1) 0
                else if (z >= 0) { if ((z / h) * (z / h) + u * u <= 1) col(u, z / h) else 0 }
                else if (-z <= r / 2 * sqrt(1 - u * u)) col(u, 0f) else 0
            }

        /** A flat diamond of ground over the footprint area given. */
        fun pad(p: Float, q: Float, dp: Float, dq: Float, c: Int, tex: (Int, Int, Int) -> Int = { k, _, _ -> k }) {
            val pts = intArrayOf(x(p, q), y(p, q), x(p + dp, q), y(p + dp, q), x(p + dp, q + dq), y(p + dp, q + dq), x(p, q + dq), y(p, q + dq))
            px.poly(pts) { xx, yy -> tex(c, xx, yy) }
        }
        fun cx(p: Float, q: Float) = x(p, q).toFloat()
        fun cy(p: Float, q: Float) = y(p, q).toFloat()

        fun post(x: Int, yb: Int, h: Int, c: Int, wide: Int = 1) = px.rect(x, yb - h, wide, h, c)

        /** A little flag: pole from [x],[yb] up [h], cloth [cw] by [ch] in [c]. */
        fun flag(x: Int, yb: Int, h: Int, c: Int, cw: Int = 6, ch: Int = 4) {
            px.line(x, yb, x, yb - h, DARK)
            px.rect(x + 1, yb - h, cw, ch, c)
            px.rect(x + 1, yb - h + ch - 1, cw, 1, shade(c, 0.8f))
        }

        /** A lit window painted on a face of [b], with its glow. */
        fun litL(b: Box, u: Int, z: Int, w: Int, hh: Int, rgb: Int = LIT, mode: Int = FLICKER, frame: Int = WHITE) {
            b.winL(u, z, w, hh, rgb or 0xFF000000.toInt(), frame)
            val x = b.cx - b.a + u + w / 2
            light(x, b.yl(x) - z - hh / 2, rgb, mode)
        }
        fun litR(b: Box, u: Int, z: Int, w: Int, hh: Int, rgb: Int = LIT, mode: Int = FLICKER, frame: Int = WHITE) {
            b.winR(u, z, w, hh, rgb or 0xFF000000.toInt(), frame)
            val x = b.cx + u + w / 2
            light(x, b.yr(x) - z - hh / 2, rgb, mode)
        }
        /** A small lit pane straight on the art, with its glow. */
        fun litLight(x: Int, y: Int, w: Int, hh: Int, rgb: Int = LIT, mode: Int = FLICKER) {
            px.rect(x, y, w, hh, rgb or 0xFF000000.toInt()); light(x + w / 2, y + hh / 2, rgb, mode)
        }
        /** A leg or stilt from footprint point [p],[q] on the ground up to [z]. */
        fun leg(p: Float, q: Float, z: Int, c: Int, lean: Float = 0f, p2: Float = p, q2: Float = q) {
            val x0 = x(p, q); val y0 = y(p, q)
            val x1 = x(p2, q2); val y1 = y(p2, q2, z)
            px.line(x0, y0, x1, y1, c); px.line(x0 + 1, y0, x1 + 1, y1, shade(c, 0.75f))
        }

        /** A heart [r] across centred on [hx],[hy]. */
        fun heart(hx: Float, hy: Float, r: Float, col: Int) =
            fill((hx - r - 1).toInt(), (hy - r).toInt(), (hx + r + 1).toInt(), (hy + r + 1).toInt()) { x, y ->
                val u = (x - hx) / r; val v = -(y - hy) / r * 1.1f + 0.1f
                val q = u * u + v * v - 0.5f
                if (q * q * q - u * u * v * v * v <= 0) round(col, u) else 0
            }

        /** [s] in the pixel font, painted onto [b]'s left face [u] in and [z] up. */
        fun textL(b: Box, u: Int, z: Int, s: String, col: Int) {
            for ((n, ch) in s.withIndex()) {
                val g = FONT[ch] ?: continue
                for (j in 0 until 5) for (i in 0 until 3) if (g[j * 3 + i] == '#') b.onL(u + n * 4 + i, z + 4 - j, 1, 1) { _, _ -> col }
            }
        }

        /** [s] in a 3×5 pixel font, its top-left at [x],[y]. */
        fun text(x: Int, y: Int, s: String, c: Int) {
            var cx = x
            for (ch in s) {
                val g = FONT[ch]
                if (g == null) { cx += 4; continue }
                for (j in 0 until 5) for (i in 0 until 3) if (g[j * 3 + i] == '#') px.set(cx + i, y + j, c)
                cx += 4
            }
        }
    }

    private val FONT: Map<Char, String> = mapOf(
        'A' to ".#. #.# ### #.# #.#", 'C' to "### #.. #.. #.. ###", 'D' to "##. #.# #.# #.# ##.", 'E' to "### #.. ### #.. ###",
        'G' to "### #.. #.# #.# ###", 'H' to "#.# #.# ### #.# #.#", 'I' to "### .#. .#. .#. ###", 'L' to "#.. #.. #.. #.. ###",
        'N' to "##. #.# #.# #.# #.#", 'O' to "### #.# #.# #.# ###", 'R' to "##. #.# ##. #.# #.#", 'S' to "### #.. ### ..# ###",
        'T' to "### .#. .#. .#. .#.", 'W' to "#.# #.# #.# ### #.#", 'Y' to "#.# #.# .#. .#. .#.", '0' to "### #.# #.# #.# ###",
        '2' to "### ..# ### #.. ###", '4' to "#.# #.# ### ..# ..#", '!' to ".#. .#. .#. ... .#.",
    ).mapValues { (_, v) -> v.replace(" ", "") }

    /**
     * A box with its front corner at [cx],[by], running [a] pixels out to the left and [b] to the right, after
     * Castle's: faces, a flat top, pitched roofs and pictures painted onto either face.
     */
    private class Box(val px: Px, val cx: Int, val by: Int, val a: Int, val b: Int) {
        fun yl(x: Int) = by - (cx - x) / 2
        fun yr(x: Int) = by - (x - cx) / 2
        /** The middle of the top at height [h]. */
        fun midX() = cx - a / 2 + b / 2
        fun midY(h: Int) = by - a / 4 - b / 4 - h

        fun walls(h: Int, c: Int, tex: (Int, Int, Int) -> Int = { k, _, _ -> k }, dark: Float = 0.74f) {
            px.poly(intArrayOf(cx - a, by - a / 2, cx, by, cx, by - h, cx - a, by - a / 2 - h)) { x, y -> tex(c, x, yl(x) - y) }
            px.poly(intArrayOf(cx, by, cx + b, by - b / 2, cx + b, by - b / 2 - h, cx, by - h)) { x, y -> tex(shade(c, dark), x, yr(x) - y) }
        }

        fun top(h: Int, c: Int = shade(STONE, 0.88f)) =
            px.poly(c, cx, by - h, cx - a, by - a / 2 - h, cx - a + b, by - a / 2 - b / 2 - h, cx + b, by - b / 2 - h)

        /** Paints [w] by [hh] pixels onto the left face, [u] in from its left end and [z] up. */
        fun onL(u: Int, z: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = cx - a + u + i; px.set(x, yl(x) - z - j, c(i, j)) }
        }
        fun onR(u: Int, z: Int, w: Int, hh: Int, c: (Int, Int) -> Int) {
            for (i in 0 until w) for (j in 0 until hh) { val x = cx + u + i; px.set(x, yr(x) - z - j, c(i, j)) }
        }
        /** A window or door: frame, glass (or wood) and a cross bar. */
        fun winL(u: Int, z: Int, w: Int, hh: Int, glass: Int, frame: Int = WHITE) = onL(u, z, w, hh) { i, j ->
            if (i == 0 || j == 0 || i == w - 1 || j == hh - 1) frame else if (w > 4 && i == w / 2) frame else glass
        }
        fun winR(u: Int, z: Int, w: Int, hh: Int, glass: Int, frame: Int = WHITE) = onR(u, z, w, hh) { i, j ->
            if (i == 0 || j == 0 || i == w - 1 || j == hh - 1) frame else if (w > 4 && i == w / 2) frame else shade(glass, 0.85f)
        }

        /** A pitched roof whose ridge runs along the left face, so the gable end shows on the right face. */
        fun gableL(h: Int, rise: Int, roof: Int, wall: Int, tex: (Int, Int, Int) -> Int = { k, _, _ -> k }, o: Int = 2) {
            val r1x = cx + b / 2; val r1y = by - b / 4 - h - rise
            val r0x = r1x - a; val r0yy = r1y - a / 2
            // The far slope, peeking over the ridge.
            px.poly(shade(roof, 0.7f), r0x, r0yy, r1x, r1y, cx + b - a, by - b / 2 - a / 2 - h)
            px.poly(intArrayOf(cx, by - h, cx + b, by - b / 2 - h, r1x, r1y)) { x, y -> tex(shade(wall, 0.74f), x, yr(x) - y) }
            px.poly(intArrayOf(cx - a - o, by - a / 2 - h + o, cx - o / 2, by - h + o, r1x, r1y, r0x, r0yy)) { x, y ->
                if ((y + (x - cx) / 2 + 400) % 4 == 0) shade(roof, 0.82f) else roof
            }
        }

        /** A pitched roof whose ridge runs along the right face, with the gable end on the left face. */
        fun gableR(h: Int, rise: Int, roof: Int, wall: Int, tex: (Int, Int, Int) -> Int = { k, _, _ -> k }, o: Int = 2) {
            val r1x = cx - a / 2; val r1y = by - a / 4 - h - rise
            val r0x = r1x + b; val r0y = r1y - b / 2
            px.poly(shade(roof, 0.95f), r1x, r1y, r0x, r0y, cx - a + b, by - a / 2 - b / 2 - h)
            px.poly(intArrayOf(cx - a, by - a / 2 - h, cx, by - h, r1x, r1y)) { x, y -> tex(wall, x, yl(x) - y) }
            px.poly(intArrayOf(cx + o / 2, by - h + o, cx + b + o, by - b / 2 - h + o, r0x, r0y, r1x, r1y)) { x, y ->
                if ((y - (x - cx) / 2 + 400) % 4 == 0) shade(roof, 0.6f) else shade(roof, 0.74f)
            }
        }

        /** A four-sided pointed roof [rise] high over walls [h] high, hanging [o] over. */
        fun pyramid(h: Int, rise: Int, roof: Int, o: Int = 2) {
            val tx = midX(); val ty = midY(h) - rise
            px.poly(intArrayOf(cx - a - o, by - a / 2 - h + o / 2, cx, by - h + o, tx, ty)) { x, y ->
                if ((y - (x - cx + a) / 2 + 400) % 3 == 0) shade(roof, 0.88f) else roof
            }
            px.poly(intArrayOf(cx, by - h + o, cx + b + o, by - b / 2 - h + o / 2, tx, ty)) { x, y ->
                if ((y + (x - cx) / 2 + 400) % 3 == 0) shade(roof, 0.6f) else shade(roof, 0.72f)
            }
        }
    }
}
