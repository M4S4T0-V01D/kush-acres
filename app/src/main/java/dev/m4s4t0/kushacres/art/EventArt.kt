package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.art.People.SHOULDERS
import dev.m4s4t0.kushacres.art.People.SKINS
import dev.m4s4t0.kushacres.art.People.clothes
import dev.m4s4t0.kushacres.art.People.eyes
import dev.m4s4t0.kushacres.art.People.face
import dev.m4s4t0.kushacres.art.People.mirrored
import dev.m4s4t0.kushacres.art.People.tint
import dev.m4s4t0.kushacres.art.People.trace
import dev.m4s4t0.kushacres.model.EventKind
import dev.m4s4t0.kushacres.model.Holiday
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A big picture for each random event, the same size and style as the shopkeepers' portraits: the people who come by
 * (Officer Doyle, the stranger, Grandma...) as faces, and the weather and luck events as little scenes.
 */
object EventArt {
    private const val N = People.BIG
    private val OUT = Sprites.OUT
    private val cache = HashMap<String, Bitmap>()

    fun portrait(kind: EventKind, holiday: Holiday? = null): Bitmap =
        cache.getOrPut("${kind.name}/${holiday?.id}") { portraitPx(kind, holiday).bitmap() }

    internal fun portraitPx(kind: EventKind, holiday: Holiday? = null): Px = when (kind) {
        EventKind.RAID -> person(0xFF1A2440.toInt() to 0xFF0A0E1A.toInt(), ::sirens, ::cop)
        EventKind.STRANGER -> person(0xFF2E2C34.toInt() to 0xFF0E0E12.toInt(), ::alley, ::stranger)
        EventKind.TRADER -> person(0xFFF0B870.toInt() to 0xFF8A5030.toInt(), ::road, ::trader)
        EventKind.GRANDMA -> person(0xFFE8C8B8.toInt() to 0xFFA88070.toInt(), ::wallpaper, ::grandma)
        EventKind.CELEB -> person(0xFFB040A0.toInt() to 0xFF3A0E3A.toInt(), ::flashes, ::celeb)
        EventKind.HIPPIES -> person(0xFFF4C860.toInt() to 0xFFD05A40.toInt(), ::rays, ::hippie)
        EventKind.TOURISTS -> person(0xFF8CD0F4.toInt() to 0xFF3A88C8.toInt(), ::clouds, ::tourist)
        EventKind.HOLIDAY -> {
            val main = holiday?.main ?: 0xFFE07A30.toInt(); val accent = holiday?.accent ?: 0xFF6A3A8A.toInt()
            person(shade(main, 0.9f) to shade(main, 0.3f), { px -> confetti(px, main, accent) }, { px -> visitor(px, main, accent) })
        }
        EventKind.PESTS -> scene(0xFF2E522E.toInt(), 0xFF0C180C.toInt(), ::pests)
        EventKind.RAIN -> scene(0xFF8494AA.toInt(), 0xFF3A4658.toInt()) { rain(it, false) }
        EventKind.STORM -> scene(0xFF3A3A5C.toInt(), 0xFF121220.toInt()) { rain(it, true) }
        EventKind.RUSH -> scene(0xFFF0A050.toInt(), 0xFF7A3030.toInt(), ::rush)
        EventKind.LUCKY -> scene(0xFF6ACC6A.toInt(), 0xFF164A24.toInt(), ::lucky)
        EventKind.VIBES -> scene(0xFFC8A0F0.toInt(), 0xFFF08AB0.toInt(), ::vibes)
        EventKind.METEOR -> scene(0xFF1C1644.toInt(), 0xFF05040C.toInt(), ::meteor)
        EventKind.HEATWAVE -> scene(0xFFFFD868.toInt(), 0xFFE05A20.toInt(), ::heat)
        EventKind.MOON -> scene(0xFF1C2C5C.toInt(), 0xFF080C1C.toInt(), ::moon)
        EventKind.POTBUCKS -> scene(0xFF9A5444.toInt(), 0xFF40201A.toInt(), ::couch)
    }

    // ---- Building blocks ----

    /** A gradient ground with the soft light behind the head, like the keepers' portraits. */
    private fun ground(top: Int, bottom: Int): Px {
        val px = Px(N, N)
        val r = N * 0.42f
        for (y in 0 until N) for (x in 0 until N) {
            val dx = x - N / 2f; val dy = y - N * 0.38f
            val d = (dx * dx + dy * dy) / (r * r)
            val c = mix(top, bottom, y / N.toFloat())
            px.set(x, y, if (d < 1f) mix(c, 0xFFFFFFFF.toInt(), (1f - d) * 0.14f) else c)
        }
        return px
    }

    /** Draws onto a clear layer, outlines it and lays it over [px]. */
    private fun layer(px: Px, draw: (Px) -> Unit) {
        val l = Px(N, N)
        draw(l)
        l.outline(OUT)
        for (i in l.p.indices) if (l.p[i] != 0) px.p[i] = l.p[i]
    }

    private fun person(bg: Pair<Int, Int>, back: (Px) -> Unit, fig: (Px) -> Unit): Px {
        val px = ground(bg.first, bg.second)
        back(px)
        layer(px, fig)
        return px
    }

    private fun scene(top: Int, bottom: Int, draw: (Px) -> Unit): Px = ground(top, bottom).also(draw)

    /** Light spilling over what's there, fading out to [r]. */
    private fun glow(px: Px, cx: Int, cy: Int, r: Int, c: Int, a: Float) {
        for (y in cy - r..cy + r) for (x in cx - r..cx + r) {
            if (x !in 0 until N || y !in 0 until N) continue
            val d = sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toFloat()) / r
            if (d < 1f) px.set(x, y, mix(px.get(x, y), c, a * (1 - d) * (1 - d)))
        }
    }

    private fun ring(px: Px, cx: Int, cy: Int, r: Float, c: Int) {
        val e = r.toInt() + 1
        for (y in cy - e..cy + e) for (x in cx - e..cx + e) {
            val d = sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toFloat())
            if (abs(d - r) < 0.7f) px.set(x, y, c)
        }
    }

    private fun sparkle(px: Px, x: Int, y: Int, c: Int, r: Int = 3) {
        px.line(x - r, y, x + r, y, c); px.line(x, y - r, x, y + r, c); px.set(x, y, 0xFFFFFFFF.toInt())
    }

    /**
     * A tapering blade from ([x0], [y0]) pointing [deg] degrees clockwise from straight up: leaf fingers, lightning,
     * meteor trails. [col] gets how far along it is (0..1) and which side (-1..1).
     */
    private fun blade(px: Px, x0: Float, y0: Float, deg: Float, len: Float, wid: Float, teeth: Boolean = false, col: (Float, Float) -> Int) {
        val a = deg * PI.toFloat() / 180f
        val ux = sin(a); val uy = -cos(a)
        val reach = len.toInt() + wid.toInt() + 2
        for (y in (y0 - reach).toInt()..(y0 + reach).toInt()) for (x in (x0 - reach).toInt()..(x0 + reach).toInt()) {
            val dx = x - x0; val dy = y - y0
            val u = dx * ux + dy * uy
            if (u < 0 || u > len) continue
            val v = dx * -uy + dy * ux
            var w = wid * sin(PI.toFloat() * (u / len)).let { if (u / len < 0.5f) sqrt(it) else it }
            if (teeth && (u.toInt() / 3) % 2 == 0) w *= 0.8f
            if (abs(v) <= w) px.set(x, y, col(u / len, v / (w + 0.01f)))
        }
    }

    /** Fixed scattered points so the pictures look the same every time. */
    private fun scatter(count: Int, seed: Int, x0: Int, y0: Int, w: Int, h: Int): List<Pair<Int, Int>> {
        var s = seed * 0x2545F491 + 12345
        fun next(m: Int): Int { s = s * 1103515245 + 12345; return ((s ushr 8) and 0xFFFF) % m }
        return List(count) { (x0 + next(w)) to (y0 + next(h)) }
    }

    // ---- Behind the people ----

    private fun sirens(px: Px) {
        glow(px, 6, 14, 34, 0xFFE03030.toInt(), 0.6f)
        glow(px, 90, 14, 34, 0xFF3060F0.toInt(), 0.6f)
        // A light bar along the bottom of the picture, red and blue.
        px.rect(0, 6, 10, 4, 0xFFFF5050.toInt()); px.rect(86, 6, 10, 4, 0xFF60A0FF.toInt())
    }

    private fun alley(px: Px) {
        for (y in 0 until N) for (x in 0 until N) {
            val row = y / 6
            if (y % 6 == 0 || (x + row % 2 * 6) % 12 == 0) px.set(x, y, shade(px.get(x, y), 0.72f))
        }
        glow(px, 84, 6, 40, 0xFFF0C060.toInt(), 0.45f)
        px.rect(80, 0, 8, 3, 0xFFF8E0A0.toInt())
    }

    private fun road(px: Px) {
        px.disc(76, 20, 9, 0xFFFFF0C0.toInt())
        glow(px, 76, 20, 22, 0xFFFFF0C0.toInt(), 0.4f)
        px.poly(0xFF7A5A3A.toInt(), 0, 66, 24, 58, 50, 62, 80, 56, 96, 60, 96, 96, 0, 96)
    }

    private fun wallpaper(px: Px) {
        for (y in 4 until N step 14) for (x in (if (y / 14 % 2 == 0) 6 else 13) until N step 14) {
            val c = shade(px.get(x, y), 0.86f)
            px.set(x, y - 1, c); px.set(x - 1, y, c); px.set(x + 1, y, c); px.set(x, y + 1, c)
            px.set(x, y, 0xFFF4E4A0.toInt())
        }
        // A picture frame on the wall.
        px.rect(70, 8, 18, 14, 0xFF8A5A30.toInt()); px.rect(72, 10, 14, 10, 0xFF9AC0D8.toInt())
        px.poly(0xFF5A8A4A.toInt(), 72, 19, 77, 14, 81, 17, 85, 13, 85, 19)
    }

    private fun flashes(px: Px) {
        for ((x, y) in listOf(10 to 12, 84 to 20, 16 to 52, 82 to 58, 72 to 6)) {
            glow(px, x, y, 12, 0xFFFFFFFF.toInt(), 0.7f)
            sparkle(px, x, y, 0xFFFFF4D0.toInt(), 4)
        }
    }

    private fun rays(px: Px) {
        for (y in 0 until N) for (x in 0 until N) {
            val a = kotlin.math.atan2((y - 40).toFloat(), (x - 48).toFloat())
            if (((a / PI.toFloat() * 8 + 16).toInt()) % 2 == 0) px.set(x, y, mix(px.get(x, y), 0xFFFFF0B0.toInt(), 0.22f))
        }
    }

    private fun clouds(px: Px) {
        for ((x, y) in listOf(14 to 14, 80 to 22, 22 to 50)) {
            oval(px, x, y, 10, 4, 0xFFF4F8FC.toInt()); oval(px, x + 5, y - 3, 6, 4, 0xFFFFFFFF.toInt())
        }
        // Palm fronds poking in from the right.
        for (d in listOf(-70f, -40f, -100f)) blade(px, 96f, 34f, d, 26f, 4f, teeth = true) { _, _ -> 0xFF2E8A3A.toInt() }
    }

    private fun confetti(px: Px, main: Int, accent: Int) {
        for ((i, xy) in scatter(26, 7, 0, 0, N, 60).withIndex()) {
            val c = if (i % 2 == 0) shade(main, 1.4f) else accent
            px.rect(xy.first, xy.second, 2, 1 + i % 2, c)
        }
    }

    // ---- People ----

    /** Officer Doyle: police cap, mirrored aviators, a moustache and a radio on his shoulder. */
    private fun cop(px: Px) {
        val skin = SKINS[1]; val navy = 0xFF24325E.toInt(); val gold = 0xFFF2C440.toInt()
        clothes(px, navy)
        px.poly(0xFF8CB0E0.toInt(), 40, 68, 56, 68, 48, 84)
        px.poly(0xFF141A30.toInt(), 46, 70, 50, 70, 51, 84, 48, 88, 45, 84)
        px.poly(shade(navy, 1.25f), 36, 68, 44, 68, 47, 80, 38, 76)
        px.poly(shade(navy, 0.9f), *mirrored(intArrayOf(36, 68, 44, 68, 47, 80, 38, 76)))
        // Badge, shoulder patch and radio.
        px.poly(gold, 26, 80, 28, 76, 30, 80, 34, 81, 31, 84, 32, 88, 28, 86, 24, 88, 25, 84, 22, 81)
        px.set(28, 82, shade(gold, 0.7f))
        px.rect(8, 82, 6, 7, 0xFFB0342C.toInt()); px.rect(9, 83, 4, 2, gold)
        px.rect(72, 77, 7, 10, 0xFF1A1A1E.toInt()); px.line(77, 77, 77, 70, 0xFF1A1A1E.toInt()); px.set(73, 79, 0xFF60E060.toInt())
        face(px, skin, jaw = 2)
        // Moustache and a flat mouth.
        px.poly(0xFF5A3A24.toInt(), 39, 51, 44, 49, 48, 50, 52, 49, 57, 51, 56, 53, 48, 52, 40, 53)
        px.line(45, 55, 51, 55, shade(skin, 0.62f))
        // Aviators that show the siren lights.
        val lens = intArrayOf(35, 35, 46, 35, 46, 40, 43, 43, 38, 42, 35, 39)
        px.poly(lens) { _, y -> if (y < 38) 0xFF6A2A40.toInt() else 0xFF20283A.toInt() }
        px.poly(mirrored(lens)) { _, y -> if (y < 38) 0xFF2A3A7A.toInt() else 0xFF20283A.toInt() }
        px.line(37, 37, 39, 36, 0xFFFFFFFF.toInt()); px.line(51, 37, 53, 36, 0xFFFFFFFF.toInt())
        px.line(46, 36, 50, 36, gold); px.line(35, 36, 32, 38, gold); px.line(61, 36, 64, 38, gold)
        // The cap.
        px.poly(intArrayOf(29, 26, 26, 12, 36, 6, 48, 4, 60, 6, 70, 12, 67, 26)) { x, _ -> if (x > 60) shade(navy, 0.8f) else navy }
        px.rect(29, 20, 39, 6, 0xFF141A30.toInt())
        for (x in 29 until 68 step 4) px.rect(x, 21, 2, 2, 0xFFE8E8E8.toInt())
        px.poly(gold, 44, 8, 52, 8, 53, 13, 48, 17, 43, 13); px.set(48, 11, shade(gold, 0.7f))
        px.poly(intArrayOf(26, 26, 70, 26, 74, 31, 22, 31)) { x, _ -> if (x < 40) 0xFF2A2A30.toInt() else 0xFF141418.toInt() }
        px.line(30, 28, 44, 28, 0xFF5A5A66.toInt())
    }

    /** The shady stranger: fedora pulled low, collar up, a cigarette. */
    private fun stranger(px: Px) {
        val skin = SKINS[3]; val coat = 0xFFA88A5A.toInt(); val hat = 0xFF3A3430.toInt()
        clothes(px, coat)
        px.poly(0xFF1A1616.toInt(), 42, 68, 54, 68, 48, 86)
        val collar = intArrayOf(22, 74, 34, 58, 44, 70, 40, 92)
        px.poly(collar) { x, _ -> if (x < 32) shade(coat, 1.15f) else coat }
        px.poly(shade(coat, 0.8f), *mirrored(collar))
        px.rect(30, 86, 3, 3, 0xFF4A3A24.toInt()); px.rect(30, 93, 3, 3, 0xFF4A3A24.toInt())
        face(px, skin, jaw = 1)
        eyes(px, 37, skin, 0xFF5A4030.toInt(), lid = 2)
        px.tint(48, 54, 11, 6, 0xFF3A2A20.toInt(), 0.3f)
        px.tint(48, 35, 17, 7, 0xFF000000.toInt(), 0.45f)
        px.line(45, 55, 51, 54, shade(skin, 0.6f))
        // Cigarette with a glowing tip and smoke.
        px.line(52, 55, 61, 57, 0xFFF4F0E8.toInt()); px.set(62, 57, 0xFFFF7030.toInt()); px.set(51, 55, 0xFFD8A060.toInt())
        // Fedora.
        px.poly(intArrayOf(31, 24, 33, 10, 40, 6, 48, 9, 56, 6, 63, 10, 65, 24)) { x, _ -> if (x > 58) shade(hat, 0.8f) else hat }
        px.line(48, 9, 48, 15, shade(hat, 0.6f))
        px.rect(32, 18, 33, 5, 0xFF161212.toInt())
        px.poly(intArrayOf(16, 28, 22, 23, 74, 23, 80, 28, 70, 31, 26, 31)) { x, _ -> if (x > 60) shade(hat, 0.75f) else shade(hat, 0.92f) }
    }

    /** The travelling trader: a straw hat, a beard and a big pack. */
    private fun trader(px: Px) {
        val skin = SKINS[4]; val shirt = 0xFF5E7E42.toInt(); val straw = 0xFFD8B060.toInt(); val beard = 0xFF3A2618.toInt()
        clothes(px, shirt)
        px.poly(shade(shirt, 1.2f), 40, 68, 48, 76, 56, 68, 54, 66, 48, 72, 42, 66)
        for (o in 0..3) { px.line(24 + o, 72, 28 + o, 96, 0xFF6A4424.toInt()); px.line(72 - o, 72, 68 - o, 96, 0xFF5A3A1E.toInt()) }
        px.rect(24, 84, 8, 3, 0xFFB0B0B0.toInt())
        face(px, skin, jaw = 2)
        eyes(px, 37, skin, 0xFF6A4020.toInt())
        px.poly(intArrayOf(34, 46, 39, 51, 44, 49, 52, 49, 57, 51, 62, 46, 62, 54, 57, 62, 48, 66, 39, 62, 34, 54)) { x, y -> if ((x + y) % 4 == 0) shade(beard, 1.3f) else beard }
        px.poly(shade(skin, 0.7f), 44, 53, 52, 53, 50, 56, 46, 56)
        px.poly(beard, 37, 35, 39, 33, 46, 34, 46, 36); px.poly(beard, 59, 35, 57, 33, 50, 34, 50, 36)
        px.disc(32, 47, 1, 0xFFF2C440.toInt())
        px.poly(intArrayOf(33, 24, 35, 12, 48, 8, 61, 12, 63, 24)) { x, y -> if ((x + y) % 3 == 0) shade(straw, 0.85f) else straw }
        px.rect(33, 19, 30, 4, 0xFFB0342C.toInt())
        px.poly(intArrayOf(14, 25, 82, 25, 76, 31, 20, 31)) { x, y -> if ((x - y) % 3 == 0) shade(straw, 0.8f) else shade(straw, 0.95f) }
    }

    /** Grandma: a grey bun, round glasses, pearls and a pink cardigan. */
    private fun grandma(px: Px) {
        val skin = SKINS[0]; val pink = 0xFFD07890.toInt(); val hair = 0xFFD8D4D0.toInt()
        clothes(px, pink)
        px.poly(0xFFF8F4EC.toInt(), 38, 68, 58, 68, 54, 78, 42, 78)
        for (x in 40..56 step 3) px.disc(x, 72 + abs(x - 48) / 4, 1, 0xFFFFFCF4.toInt())
        for (y in 82..94 step 5) px.disc(48, y, 1, shade(pink, 0.7f))
        face(px, skin)
        eyes(px, 37, skin, 0xFF4A80C0.toInt(), lid = 1)
        val crease = shade(skin, 0.8f)
        px.line(34, 38, 35, 41, crease); px.line(62, 38, 61, 41, crease); px.line(40, 50, 42, 53, crease); px.line(56, 50, 54, 53, crease)
        px.tint(39, 47, 3, 2, 0xFFE05A70.toInt(), 0.45f); px.tint(57, 47, 3, 2, 0xFFE05A70.toInt(), 0.45f)
        px.trace(0xFFB04050.toInt(), 44, 54, 46, 56, 50, 56, 52, 54)
        ring(px, 42, 39, 6f, 0xFFB08A40.toInt()); ring(px, 55, 39, 6f, 0xFFB08A40.toInt()); px.line(48, 38, 49, 38, 0xFFB08A40.toInt())
        // Curly hair and a bun.
        val curls: (Int, Int) -> Int = { x, y -> if ((x / 3 + y / 3) % 2 == 0) shade(hair, 0.86f) else hair }
        px.poly(intArrayOf(31, 34, 30, 20, 37, 12, 48, 10, 59, 12, 66, 20, 65, 34, 61, 26, 52, 22, 42, 22, 35, 26), curls)
        for (y in 22..34 step 4) { px.disc(31, y, 2, hair); px.disc(65, y, 2, shade(hair, 0.85f)) }
        px.disc(48, 7, 6, hair); px.disc(46, 5, 2, 0xFFF4F2F0.toInt())
    }

    /** A celebrity: pompadour, white-rimmed shades, velvet jacket and a gold chain. */
    private fun celeb(px: Px) {
        val skin = SKINS[5]; val velvet = 0xFF7A2A9A.toInt(); val gold = 0xFFF2C440.toInt()
        px.poly(SHOULDERS) { x, y -> if ((x + y) % 5 == 0) shade(velvet, 1.15f) else if (x > 70) shade(velvet, 0.8f) else velvet }
        px.poly(0xFF141014.toInt(), 40, 68, 56, 68, 48, 90)
        px.poly(shade(velvet, 0.7f), 34, 68, 42, 68, 47, 92, 36, 82); px.poly(shade(velvet, 0.6f), *mirrored(intArrayOf(34, 68, 42, 68, 47, 92, 36, 82)))
        for (i in 0..8) { val t = i / 8f; px.set((41 + 14 * t).toInt(), (73 + 8 * sin(PI.toFloat() * t)).toInt(), gold) }
        px.disc(48, 82, 2, gold)
        face(px, skin, jaw = 1)
        px.poly(0xFF141014.toInt(), 44, 53, 52, 53, 51, 57, 45, 57); px.rect(45, 53, 7, 2, 0xFFFFFFFF.toInt())
        val lens = intArrayOf(34, 34, 46, 34, 46, 41, 42, 43, 36, 42)
        px.poly(lens) { _, y -> if (y < 38) 0xFFE05A9A.toInt() else 0xFF2A0E2A.toInt() }
        px.poly(mirrored(lens)) { _, y -> if (y < 38) 0xFFE05A9A.toInt() else 0xFF2A0E2A.toInt() }
        px.trace(0xFFFFFFFF.toInt(), 34, 34, 46, 34, 46, 41, 42, 43, 36, 42, 34, 34); px.trace(0xFFFFFFFF.toInt(), *mirrored(intArrayOf(34, 34, 46, 34, 46, 41, 42, 43, 36, 42, 34, 34)))
        px.line(46, 35, 50, 35, 0xFFFFFFFF.toInt())
        val hair = 0xFF16121A.toInt()
        px.poly(intArrayOf(32, 30, 31, 16, 38, 6, 50, 2, 62, 6, 67, 16, 64, 30, 60, 22, 40, 22)) { x, y -> if ((x - y) % 6 == 0) 0xFF4A3A5A.toInt() else hair }
        px.disc(64, 50, 1, 0xFFF4F8FF.toInt())
    }

    /** A hippie from the festival: long hair, a headband, round orange shades and tie-dye. */
    private fun hippie(px: Px) {
        val skin = SKINS[2]; val hair = 0xFF6A4424.toInt()
        val dye = intArrayOf(0xFFE04A4A.toInt(), 0xFFF0A030.toInt(), 0xFFF0E040.toInt(), 0xFF50C060.toInt(), 0xFF4A80E0.toInt(), 0xFF9A5AD0.toInt())
        px.poly(intArrayOf(30, 22, 48, 8, 66, 22, 70, 50, 74, 80, 64, 84, 32, 84, 22, 80, 26, 50)) { x, y -> if ((x + y / 4) % 5 == 0) shade(hair, 0.75f) else shade(hair, 0.88f) }
        px.poly(SHOULDERS) { x, y ->
            val d = sqrt(((x - 48) * (x - 48) + (y - 92) * (y - 92)).toFloat()) + sin(x / 3f) * 2
            val c = dye[(d / 4).toInt() % dye.size]
            if (x > 70) shade(c, 0.82f) else c
        }
        ring(px, 48, 82, 4f, 0xFFFFFFFF.toInt()); px.line(48, 78, 48, 86, 0xFFFFFFFF.toInt()); px.line(48, 82, 45, 85, 0xFFFFFFFF.toInt()); px.line(48, 82, 51, 85, 0xFFFFFFFF.toInt())
        face(px, skin)
        px.trace(shade(skin, 0.6f), 44, 54, 46, 56, 50, 56, 52, 54)
        for ((cx, c) in listOf(42 to 0xFFF09030.toInt(), 54 to 0xFFF09030.toInt())) { px.disc(cx, 39, 5, c); px.set(cx - 2, 37, 0xFFFFE0A0.toInt()); ring(px, cx, 39, 5f, 0xFFE0C060.toInt()) }
        px.line(47, 38, 49, 38, 0xFFE0C060.toInt())
        px.poly(intArrayOf(32, 32, 32, 20, 40, 12, 48, 10, 56, 12, 64, 20, 64, 32, 58, 24, 48, 22, 38, 24)) { x, y -> if ((x + y / 4) % 5 == 0) shade(hair, 0.8f) else hair }
        px.rect(31, 25, 35, 3, 0xFFE04A4A.toInt()); for (x in 32 until 66 step 4) px.set(x, 26, 0xFFF0E040.toInt())
        // A daisy tucked behind the ear.
        for (a in 0 until 5) { val r = a * 2 * PI.toFloat() / 5; px.disc((66 + 3 * cos(r)).toInt(), (34 + 3 * sin(r)).toInt(), 1, 0xFFFFFFFF.toInt()) }
        px.set(66, 34, 0xFFF0C030.toInt())
    }

    /** A tourist off the bus: bucket hat, sunburnt nose, Hawaiian shirt and a camera. */
    private fun tourist(px: Px) {
        val skin = SKINS[0]; val teal = 0xFF2AA8A0.toInt(); val hat = 0xFFE8DCB8.toInt()
        px.poly(SHOULDERS) { x, y ->
            val fx = x % 10; val fy = (y + (x / 10) * 4) % 10
            val c = when { fx in 2..4 && fy in 2..4 -> if ((x / 10) % 2 == 0) 0xFFF06A9A.toInt() else 0xFFF8E060.toInt(); fx == 3 && fy == 5 -> 0xFF1E7A3A.toInt(); else -> teal }
            if (x > 70) shade(c, 0.82f) else c
        }
        px.poly(shade(teal, 1.25f), 38, 68, 48, 80, 42, 82, 34, 72); px.poly(shade(teal, 0.85f), 58, 68, 48, 80, 54, 82, 62, 72)
        px.poly(skin, 42, 68, 54, 68, 48, 78)
        for (o in 0..1) px.line(36 + o, 70, 54 + o, 82, 0xFF2A2A2A.toInt())
        px.rect(52, 80, 16, 11, 0xFF2A2A2E.toInt()); px.disc(60, 86, 3, 0xFF6A7A8A.toInt()); px.set(59, 85, 0xFFE0F0FF.toInt()); px.rect(64, 81, 3, 2, 0xFFE0E0E0.toInt())
        face(px, skin)
        eyes(px, 37, skin, 0xFF4A9A5A.toInt())
        px.tint(48, 44, 4, 3, 0xFFE04A3A.toInt(), 0.5f); px.line(48, 41, 48, 44, 0xFFFFFFFF.toInt())
        px.trace(shade(skin, 0.6f), 43, 53, 45, 56, 51, 56, 53, 53); px.line(46, 54, 50, 54, 0xFFFFFFFF.toInt())
        px.poly(intArrayOf(32, 27, 34, 12, 48, 8, 62, 12, 64, 27)) { x, _ -> if (x > 58) shade(hat, 0.85f) else hat }
        px.rect(33, 21, 31, 3, 0xFF2AA8A0.toInt())
        px.poly(intArrayOf(26, 26, 70, 26, 76, 33, 20, 33)) { x, _ -> if (x > 60) shade(hat, 0.78f) else shade(hat, 0.92f) }
    }

    /** A neighbour in costume for the holiday: a tall hat and scarf in its colours and a little eye mask. */
    private fun visitor(px: Px, main: Int, accent: Int) {
        val skin = SKINS[3]
        clothes(px, shade(accent, 0.9f))
        px.poly(intArrayOf(26, 70, 70, 70, 68, 78, 28, 78)) { x, _ -> if ((x / 4) % 2 == 0) main else shade(main, 0.75f) }
        px.poly(main, 58, 76, 66, 76, 64, 92, 58, 90)
        face(px, skin)
        px.poly(accent, 33, 34, 63, 34, 62, 42, 52, 43, 48, 40, 44, 43, 34, 42)
        eyes(px, 37, skin, 0xFF4A3020.toInt())
        px.trace(shade(skin, 0.6f), 43, 53, 45, 56, 51, 56, 53, 53)
        // A tall hat with a buckle.
        px.poly(intArrayOf(36, 27, 40, 4, 58, 4, 60, 27)) { x, _ -> if (x > 54) shade(main, 0.7f) else main }
        px.rect(38, 20, 21, 4, accent); px.rect(46, 19, 5, 6, 0xFFF2C440.toInt()); px.rect(47, 20, 3, 4, accent)
        px.poly(shade(main, 0.6f), 26, 26, 70, 26, 74, 31, 22, 31)
    }

    // ---- Scenes ----

    private fun leaf(px: Px, x: Float, y: Float, size: Float, deg: Float, c: Int) {
        val fingers = listOf(0f to 1f, -30f to 0.86f, 30f to 0.86f, -60f to 0.64f, 60f to 0.64f, -92f to 0.38f, 92f to 0.38f)
        for ((a, l) in fingers) blade(px, x, y, deg + a, size * l, size * 0.13f, teeth = true) { u, v ->
            when { abs(v) < 0.15f -> shade(c, 1.3f); v < 0 -> shade(c, 1.08f - u * 0.1f); else -> shade(c, 0.88f) }
        }
    }

    private fun pests(px: Px) {
        layer(px) { l ->
            leaf(l, 48f, 84f, 66f, 0f, 0xFF4E9A34.toInt())
            l.rect(47, 84, 3, 12, 0xFF3A7A2A.toInt())
        }
        // Webbing between the fingers.
        for ((a, b) in listOf((30 to 34) to (46 to 24), (62 to 30) to (52 to 22), (20 to 56) to (32 to 40))) px.line(a.first, a.second, b.first, b.second, mix(px.get(a.first, a.second), 0xFFFFFFFF.toInt(), 0.5f))
        for ((x, y) in scatter(16, 3, 22, 18, 52, 56)) {
            glow(px, x, y, 6, 0xFFFF2A2A.toInt(), 0.65f)
            px.set(x, y, 0xFFFF5A4A.toInt()); px.set(x + 1, y, 0xFFE02A2A.toInt()); px.set(x, y - 1, 0xFFFFC0B0.toInt())
        }
    }

    private fun rain(px: Px, storm: Boolean) {
        val grass = if (storm) 0xFF24401E.toInt() else 0xFF4A7A34.toInt()
        px.poly(grass, 0, 76, 30, 72, 66, 74, 96, 70, 96, 96, 0, 96)
        for ((x, y) in listOf(22 to 86, 66 to 84)) { oval(px, x, y, 10, 3, if (storm) 0xFF3A4A6A.toInt() else 0xFF8AA8C8.toInt()); px.line(x - 4, y - 1, x + 2, y - 1, 0xFFC8DCF0.toInt()) }
        // Little plants getting a drink.
        for (x in listOf(10, 44, 82)) layer(px) { l -> leaf(l, x.toFloat(), 76f, 12f, 0f, 0xFF4E9A34.toInt()) }
        val cloud = if (storm) 0xFF2A2A3A.toInt() else 0xFF9AA4B4.toInt()
        for ((x, y, r) in listOf(Triple(16, 14, 14), Triple(38, 8, 16), Triple(62, 12, 15), Triple(84, 16, 14), Triple(50, 20, 14), Triple(26, 22, 11), Triple(74, 24, 11))) {
            px.disc(x, y, r, cloud); px.disc(x - 3, y - 4, r - 5, shade(cloud, 1.15f))
        }
        if (storm) {
            val bolt = intArrayOf(54, 24, 42, 50, 50, 50, 40, 78, 60, 44, 51, 44, 60, 24)
            glow(px, 50, 50, 30, 0xFFF8F0B0.toInt(), 0.5f)
            layer(px) { l -> l.poly(bolt) { x, _ -> if (x < 48) 0xFFFFFFFF.toInt() else 0xFFF8E860.toInt() } }
        }
        val drop = if (storm) 0xFF8AA0D0.toInt() else 0xFFD0E4F8.toInt()
        for ((x, y) in scatter(if (storm) 44 else 30, 11, 0, 26, N, 56)) px.line(x, y, x - 2, y + 6, drop)
    }

    private fun rush(px: Px) {
        for ((i, b) in listOf(0 to 40, 14 to 30, 26 to 48, 42 to 24, 54 to 36, 68 to 44, 82 to 28).withIndex()) {
            val (x, h) = b
            val w = if (i % 2 == 0) 14 else 12
            px.rect(x, 96 - h, w, h, if (i % 2 == 0) 0xFF4A2430.toInt() else 0xFF3A1C28.toInt())
            for (wy in 96 - h + 4 until 92 step 6) for (wx in x + 2 until x + w - 2 step 4) if ((wx + wy + i) % 3 != 0) px.rect(wx, wy, 2, 3, 0xFFF8D870.toInt())
        }
        layer(px) { l ->
            l.poly(intArrayOf(48, 10, 74, 38, 58, 38, 58, 66, 38, 66, 38, 38, 22, 38)) { x, _ -> if (x < 46) 0xFF7AE05A.toInt() else 0xFF4AB040.toInt() }
        }
        for ((x, y) in listOf(18 to 22, 78 to 56, 16 to 60)) layer(px) { l ->
            l.disc(x, y, 6, 0xFFF2C440.toInt()); l.disc(x - 1, y - 1, 4, 0xFFF8DA70.toInt()); l.line(x, y - 3, x, y + 3, 0xFFB88A24.toInt())
        }
    }

    private fun lucky(px: Px) {
        for (y in 0 until N) for (x in 0 until N) {
            val a = kotlin.math.atan2((y - 44).toFloat(), (x - 48).toFloat())
            if (((a / PI.toFloat() * 10 + 20).toInt()) % 2 == 0) px.set(x, y, mix(px.get(x, y), 0xFFF8F0A0.toInt(), 0.16f))
        }
        layer(px) { l ->
            l.line(48, 48, 56, 90, 0xFF2E7A2A.toInt()); l.line(49, 48, 57, 90, 0xFF2E7A2A.toInt())
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 2 + PI.toFloat() / 4
                val cx = 48 + 15 * cos(a); val cy = 46 + 15 * sin(a)
                val px2 = -sin(a) * 6; val py2 = cos(a) * 6
                val c = 0xFF5ACC4A.toInt()
                l.disc((cx + px2).toInt(), (cy + py2).toInt(), 9, c); l.disc((cx - px2).toInt(), (cy - py2).toInt(), 9, c)
                l.poly(c, 48, 46, (cx + px2 * 1.6f).toInt(), (cy + py2 * 1.6f).toInt(), (cx - px2 * 1.6f).toInt(), (cy - py2 * 1.6f).toInt())
                l.line(48, 46, (48 + 20 * cos(a)).toInt(), (46 + 20 * sin(a)).toInt(), 0xFF8AE878.toInt())
            }
        }
        for ((x, y) in listOf(16 to 18, 80 to 14, 84 to 70, 12 to 74, 70 to 34)) sparkle(px, x, y, 0xFFF8DA50.toInt(), 3)
    }

    private fun vibes(px: Px) {
        val colours = intArrayOf(0xFFE04A4A.toInt(), 0xFFF0A030.toInt(), 0xFFF0E040.toInt(), 0xFF50C060.toInt(), 0xFF4A80E0.toInt(), 0xFF9A5AD0.toInt())
        for (y in 0 until N) for (x in 0 until N) {
            val d = sqrt(((x - 48) * (x - 48) + (y - 96) * (y - 96)).toFloat())
            val band = ((86 - d) / 5).toInt()
            if (band in colours.indices && y < 96) px.set(x, y, mix(px.get(x, y), colours[band], 0.75f))
        }
        layer(px) { l ->
            for (k in 0 until 12) {
                val a = k * PI.toFloat() / 6
                l.poly(0xFFF8C830.toInt(), (48 + 18 * cos(a - 0.2f)).toInt(), (42 + 18 * sin(a - 0.2f)).toInt(), (48 + 28 * cos(a)).toInt(), (42 + 28 * sin(a)).toInt(), (48 + 18 * cos(a + 0.2f)).toInt(), (42 + 18 * sin(a + 0.2f)).toInt())
            }
            l.disc(48, 42, 19, 0xFFF8D840.toInt()); l.disc(44, 38, 12, 0xFFFFE878.toInt())
        }
        px.disc(41, 38, 2, 0xFF3A2410.toInt()); px.disc(55, 38, 2, 0xFF3A2410.toInt())
        px.trace(0xFF3A2410.toInt(), 38, 46, 42, 51, 48, 53, 54, 51, 58, 46)
        px.tint(36, 46, 3, 2, 0xFFF06A6A.toInt(), 0.5f); px.tint(60, 46, 3, 2, 0xFFF06A6A.toInt(), 0.5f)
        for ((x, y) in listOf(14 to 14, 82 to 18, 78 to 66)) {
            layer(px) { l -> l.disc(x - 2, y, 2, 0xFFF04A7A.toInt()); l.disc(x + 2, y, 2, 0xFFF04A7A.toInt()); l.poly(0xFFF04A7A.toInt(), x - 4, y + 1, x + 4, y + 1, x, y + 5) }
        }
    }

    private fun meteor(px: Px) {
        for ((i, xy) in scatter(40, 5, 0, 0, N, 70).withIndex()) px.set(xy.first, xy.second, if (i % 5 == 0) 0xFFFFFFFF.toInt() else 0xFF9A9AC8.toInt())
        px.poly(0xFF141A12.toInt(), 0, 80, 20, 76, 44, 80, 70, 76, 96, 78, 96, 96, 0, 96)
        glow(px, 62, 84, 22, 0xFFFF8A30.toInt(), 0.6f)
        blade(px, 58f, 62f, -48f, 70f, 10f) { u, v -> mix(mix(0xFFFFF4C0.toInt(), 0xFFFF6A20.toInt(), u), px.get(0, 0), u * 0.6f + abs(v) * 0.3f) }
        glow(px, 58, 62, 18, 0xFFFFD070.toInt(), 0.6f)
        layer(px) { l ->
            l.disc(60, 64, 9, 0xFF7A6458.toInt()); l.disc(58, 62, 6, 0xFF9A8474.toInt())
            l.disc(63, 67, 2, 0xFF5A4A40.toInt()); l.disc(56, 66, 1, 0xFF5A4A40.toInt()); l.disc(61, 59, 1, 0xFF5A4A40.toInt())
        }
        for (x in 52..72 step 5) px.line(x, 74, x + (x - 62) / 3, 70, 0xFFFFB050.toInt())
    }

    private fun heat(px: Px) {
        glow(px, 48, 22, 40, 0xFFFFFFE0.toInt(), 0.7f)
        px.disc(48, 22, 15, 0xFFFFF8C0.toInt()); px.disc(48, 22, 11, 0xFFFFFFF0.toInt())
        for (y in 44 until 72 step 7) for (x in 0 until N) {
            val yy = y + (sin(x / 4f) * 2).toInt()
            px.set(x, yy, mix(px.get(x, yy), 0xFFFFF0C0.toInt(), 0.35f))
        }
        px.poly(intArrayOf(0, 74, 96, 72, 96, 96, 0, 96)) { x, y -> if ((x * 3 + y * 7) % 11 == 0) 0xFFA06A3A.toInt() else 0xFFC08A50.toInt() }
        for (c in listOf(intArrayOf(10, 80, 16, 86, 14, 92, 20, 96), intArrayOf(50, 76, 46, 84, 52, 90), intArrayOf(80, 78, 86, 84, 84, 92))) px.trace(0xFF6A4224.toInt(), *c)
        // A wilting plant and a thermometer in the red.
        layer(px) { l -> l.line(28, 80, 28, 66, 0xFF6A8A3A.toInt()); leaf(l, 28f, 66f, 12f, 150f, 0xFF8AA048.toInt()) }
        layer(px) { l ->
            l.rect(76, 36, 6, 34, 0xFFF4F0E8.toInt()); l.disc(78, 72, 5, 0xFFE03A30.toInt()); l.rect(78, 40, 2, 30, 0xFFE03A30.toInt())
            for (y in 40..66 step 6) l.line(77, y, 78, y, 0xFF8A8A8A.toInt())
        }
    }

    private fun moon(px: Px) {
        for ((i, xy) in scatter(30, 9, 0, 0, N, 60).withIndex()) px.set(xy.first, xy.second, if (i % 4 == 0) 0xFFFFFFFF.toInt() else 0xFF8A9ACC.toInt())
        glow(px, 48, 42, 46, 0xFFF0C070.toInt(), 0.45f)
        px.disc(48, 42, 28, 0xFFF4D08A.toInt())
        px.poly(intArrayOf(20, 42, 76, 42, 76, 70, 20, 70)) { x, y -> if ((x - 48) * (x - 48) + (y - 42) * (y - 42) <= 28 * 28 && x + y > 100) 0xFFE0B470.toInt() else 0 }
        for ((x, y, r) in listOf(Triple(38, 32, 5), Triple(58, 50, 6), Triple(50, 28, 3), Triple(36, 52, 3))) { px.disc(x, y, r, 0xFFE0B878.toInt()); px.disc(x + 1, y + 1, r - 1, 0xFFD0A468.toInt()) }
        px.poly(0xFF0A1208.toInt(), 0, 82, 30, 78, 60, 82, 96, 78, 96, 96, 0, 96)
        for ((x, s) in listOf(8 to 16f, 26 to 22f, 48 to 18f, 70 to 24f, 90 to 14f)) {
            px.line(x, 84, x, (84 - s * 0.5f).toInt(), 0xFF0A1208.toInt())
            leaf(px, x.toFloat(), 84 - s * 0.4f, s, 0f, 0xFF0C160A.toInt())
        }
    }

    private fun couch(px: Px) {
        val cushion = 0xFFB0604A.toInt()
        for ((x, y, w) in listOf(Triple(-6, 30, 52), Triple(50, 30, 52))) {
            px.poly(intArrayOf(x + 4, y, x + w - 4, y, x + w, y + 4, x + w, y + 40, x + w - 4, y + 44, x + 4, y + 44, x, y + 40, x, y + 4)) { xx, yy -> if (yy < y + 6) shade(cushion, 1.15f) else if ((xx + yy) % 7 == 0) shade(cushion, 0.9f) else cushion }
        }
        px.rect(44, 30, 8, 46, 0xFF2A140E.toInt())
        px.poly(shade(cushion, 0.8f), 0, 74, 96, 74, 96, 96, 0, 96)
        // A Pot Buck wedged in the gap, and a few coins.
        glow(px, 48, 50, 26, 0xFFB0FF90.toInt(), 0.45f)
        layer(px) { l ->
            l.disc(48, 50, 14, 0xFF3A9A3A.toInt()); l.disc(46, 48, 12, 0xFF5AC050.toInt())
            ring(l, 48, 50, 10f, 0xFF2E7A2A.toInt())
            leaf(l, 48f, 58f, 13f, 0f, 0xFF1E5A1E.toInt())
        }
        for ((x, y) in listOf(22 to 80, 72 to 82, 34 to 86)) layer(px) { l -> oval(l, x, y, 6, 3, 0xFFF2C440.toInt()); oval(l, x - 1, y - 1, 4, 1, 0xFFF8DA70.toInt()) }
        for ((x, y) in listOf(24 to 20, 72 to 18, 80 to 50)) sparkle(px, x, y, 0xFFE8FFC0.toInt(), 3)
    }
}
