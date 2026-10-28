package dev.m4s4t0.kushacres.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import dev.m4s4t0.kushacres.model.Holiday
import dev.m4s4t0.kushacres.model.Season
import dev.m4s4t0.kushacres.model.Sky
import dev.m4s4t0.kushacres.model.Weather
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** How the farm is lit right now: time of day and weather, folded into one colour to multiply everything by. */
/** [season] tints the light a little and turns rain to snow in winter. */
class Light(val night: Float, val tint: Int, val rain: Float, val fog: Float, val cloud: Float, val season: Season = Season.SPRING) {
    companion object {
        fun at(now: Long): Light {
            val h = Sky.hour(now)
            val d = Sky.daylight(h)
            val g = Sky.golden(h)
            val storm = Sky.amount(now, Weather.STORM)
            val rain = maxOf(Sky.amount(now, Weather.RAIN), storm)
            val fog = Sky.amount(now, Weather.FOG)
            val cloud = Sky.amount(now, Weather.CLOUDY)
            fun mix(a: Float, b: Float, k: Float) = a + (b - a) * k
            var r = mix(0.30f, 1f, d); var gg = mix(0.36f, 1f, d); var b = mix(0.68f, 1f, d)
            val gk = g * 0.55f
            r *= mix(1f, 1.0f, gk); gg *= mix(1f, 0.84f, gk); b *= mix(1f, 0.64f, gk)
            r *= mix(1f, 0.84f, cloud); gg *= mix(1f, 0.86f, cloud); b *= mix(1f, 0.9f, cloud)
            r *= mix(1f, 0.68f, rain); gg *= mix(1f, 0.72f, rain); b *= mix(1f, 0.84f, rain)
            r *= mix(1f, 0.9f, fog); gg *= mix(1f, 0.91f, fog); b *= mix(1f, 0.93f, fog)
            if (storm > 0f) {
                r *= mix(1f, 0.85f, storm); gg *= mix(1f, 0.85f, storm); b *= mix(1f, 0.9f, storm)
                // Lightning: a quick double flash somewhere in most 9-second windows.
                val slot = now / 9000; val at = Random(slot).nextLong(9000); val t = now % 9000 - at
                val flash = storm * when (t) { in 0L..80L -> 1f; in 160L..260L -> 0.6f; else -> 0f } * (if (slot % 3 == 0L) 0f else 1f)
                r = mix(r, 1.2f, flash); gg = mix(gg, 1.2f, flash); b = mix(b, 1.3f, flash)
            }
            val season = Season.at(now)
            when (season) {
                Season.WINTER -> { r *= 0.93f; gg *= 0.97f; b *= 1.04f }
                Season.AUTUMN -> { r *= 1.03f; gg *= 0.97f; b *= 0.9f }
                Season.SUMMER -> { r *= 1.02f; b *= 0.96f }
                Season.SPRING -> {}
            }
            fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255).toInt()
            return Light(1f - d, (0xFF shl 24) or (ch(r) shl 16) or (ch(gg) shl 8) or ch(b), rain, fog, cloud, season)
        }
    }
}

private val BAT_UP = arrayOf("#.....#", "##.#.##", ".##o##.", "...#...")
private val BAT_DOWN = arrayOf("...#...", ".##o##.", "##...##", "#.....#")
private val HEART = arrayOf(".#.#.", "#####", ".###.", "..#..")
private val CLOVER = arrayOf(".#.", "#o#", ".#.", "..o")
private val SPARK = arrayOf(".#.", "###", ".#.")
private val FLY_UP = arrayOf("#.o.#", "##o##", "#.o.#")
private val FLY_DOWN = arrayOf("..o..", ".#o#.", "..o..")
private val GOOSE_UP = arrayOf("#...", ".#oo", "..##")
private val GOOSE_DOWN = arrayOf("..oo", "####", "#...")
private val LEAF = arrayOf(".#.", "###", ".#.", "#..")

/** What kind of puff to throw. */
enum class Burst { WATER, DIRT, HARVEST, GOLDEN, DUST, COINS, SPLASH }

private class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var life: Float, val max: Float, val color: Int, val size: Float, val grav: Float, val leaf: Boolean,
)

/** Little bits that fly off when you do things, and the soft glows of the night. All in world (art) pixels. */
class Fx {
    private val ps = ArrayList<Particle>()
    private val r = Random(7)
    private val fill = Paint().apply { isAntiAlias = false }
    private val add = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD); isFilterBitmap = true }
    private val soft = Paint().apply { isFilterBitmap = true }
    private val rect = RectF()
    private val glows = HashMap<Int, Bitmap>()

    fun burst(kind: Burst, x: Float, y: Float) {
        fun p(n: Int, colors: IntArray, speed: Float, up: Float, grav: Float, life: Float, size: Float = 1f, leaf: Boolean = false) {
            repeat(n) {
                val a = r.nextFloat() * 6.283f
                val s = speed * (0.4f + r.nextFloat() * 0.6f)
                val l = life * (0.7f + r.nextFloat() * 0.5f)
                ps += Particle(x + (r.nextFloat() - 0.5f) * 6, y + (r.nextFloat() - 0.5f) * 3,
                    kotlin.math.cos(a) * s, sin(a) * s * 0.5f - up * (0.6f + r.nextFloat() * 0.6f),
                    l, l, colors[r.nextInt(colors.size)], size, grav, leaf)
            }
        }
        when (kind) {
            Burst.WATER -> p(12, intArrayOf(0xFF8FD4FF.toInt(), 0xFFCFEFFF.toInt(), 0xFF4FA3E0.toInt()), 18f, 34f, 120f, 0.6f, 0.5f)
            Burst.DIRT -> p(9, intArrayOf(0xFF6B4A2E.toInt(), 0xFF8A6440.toInt(), 0xFF4A3220.toInt()), 16f, 22f, 110f, 0.5f, 0.5f)
            Burst.HARVEST -> {
                p(10, intArrayOf(0xFF5FB040.toInt(), 0xFF8BD45A.toInt(), 0xFF3E8A30.toInt()), 22f, 30f, 40f, 1.1f, 1f, leaf = true)
                p(5, intArrayOf(0xFFF2C440.toInt(), 0xFFFFE27A.toInt()), 14f, 44f, 90f, 0.9f)
            }
            Burst.GOLDEN -> p(22, intArrayOf(0xFFFFD84A.toInt(), 0xFFFFF6C0.toInt(), 0xFFF2A030.toInt()), 30f, 40f, 50f, 1.4f)
            Burst.DUST -> p(16, intArrayOf(0xFFB8A888.toInt(), 0xFF9A8C70.toInt(), 0xFFD8CCB0.toInt()), 26f, 10f, -6f, 0.9f, 1.5f)
            Burst.COINS -> p(8, intArrayOf(0xFFF2C440.toInt(), 0xFFFFE27A.toInt()), 12f, 42f, 100f, 0.9f)
            Burst.SPLASH -> p(3, intArrayOf(0xCCCFEFFF.toInt()), 10f, 14f, 90f, 0.25f, 0.5f)
        }
        while (ps.size > 400) ps.removeAt(0)
    }

    fun step(dt: Float) {
        val it = ps.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0) { it.remove(); continue }
            p.vy += p.grav * dt
            if (p.leaf) { p.vx *= 0.96f; p.vy = minOf(p.vy, 12f); p.x += sin(p.life * 9f) * 8f * dt }
            p.x += p.vx * dt; p.y += p.vy * dt
        }
    }

    fun draw(c: Canvas) {
        for (p in ps) {
            val k = p.life / p.max
            fill.color = p.color
            fill.alpha = ((p.color ushr 24) * minOf(1f, k * 3f)).toInt()
            val s = p.size
            val x = floor(p.x * 2) / 2; val y = floor(p.y * 2) / 2
            c.drawRect(x, y, x + s, y + s, fill)
            if (p.leaf) c.drawRect(x + s, y + (if (sin(p.life * 9f) > 0) 0f else s), x + 2 * s, y + s + (if (sin(p.life * 9f) > 0) 0f else s), fill)
        }
    }

    /** A round soft light of [color], [radius] art pixels, [strength] 0..1, added on top of what's there. */
    fun glow(c: Canvas, x: Float, y: Float, radius: Float, color: Int, strength: Float, flat: Float = 1f) {
        if (strength <= 0.01f) return
        val b = glows.getOrPut(color) { glowBitmap(color) }
        add.alpha = (strength.coerceIn(0f, 1f) * 255).toInt()
        rect.set(x - radius, y - radius * flat, x + radius, y + radius * flat)
        c.drawBitmap(b, null, rect, add)
    }

    private fun glowBitmap(color: Int): Bitmap {
        val n = 48
        val px = IntArray(n * n)
        for (j in 0 until n) for (i in 0 until n) {
            val dx = (i + 0.5f) / n * 2 - 1; val dy = (j + 0.5f) / n * 2 - 1
            val d = sqrt(dx * dx + dy * dy)
            val a = ((1 - d).coerceIn(0f, 1f)).let { it * it }
            px[j * n + i] = ((a * 255).toInt() shl 24) or (color and 0xFFFFFF)
        }
        return Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888)
    }

    private fun hf(i: Int, k: Int): Float {
        var h = i * 374761393 + k * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return ((h xor (h ushr 16)) and 0xFFFF) / 65536f
    }

    /** A little pixel picture: '#' in [a], 'o' in [b], at [s] screen pixels per dot, centred on x,y. */
    private fun dots(c: Canvas, x: Float, y: Float, s: Float, rows: Array<String>, a: Int, b: Int = a, alpha: Int = 255) {
        val x0 = x - rows[0].length * s / 2; val y0 = y - rows.size * s / 2
        for ((j, row) in rows.withIndex()) for ((i, ch) in row.withIndex()) {
            if (ch == '.' || ch == ' ') continue
            fill.color = if (ch == '#') a else b; fill.alpha = alpha
            c.drawRect(x0 + i * s, y0 + j * s, x0 + (i + 1) * s, y0 + (j + 1) * s, fill)
        }
        fill.alpha = 255
    }

    /** Something in the air for each holiday, in screen pixels: bats at Halloween, hearts at Valentine's, and so on. */
    fun drawHoliday(c: Canvas, w: Float, h: Float, scale: Float, t: Float, light: Light, holiday: Holiday?) {
        holiday ?: return
        val s = maxOf(2f, scale * 0.5f)
        val wet = 1f - light.rain * 0.6f
        when (holiday) {
            Holiday.HALLOWEEN -> {
                // Bats loop about the screen, more of them after dark, wings beating.
                val n = 5 + (light.night * 7).toInt()
                for (i in 0 until n) {
                    val sp = 0.05f + hf(i, 31) * 0.05f
                    val x = (0.5f + 0.45f * sin(t * sp * 2.1f + i * 2.3f) + 0.08f * sin(t * 0.9f + i)) * w
                    val y = (0.35f + 0.3f * sin(t * sp * 3.3f + i * 1.3f) + 0.05f * sin(t * 2.3f + i * 0.7f)) * h
                    val up = sin(t * (14f + hf(i, 32) * 6f) + i) > 0
                    dots(c, x, y, s, if (up) BAT_UP else BAT_DOWN, 0xFF1E1826.toInt(), 0xFFFF5A3A.toInt())
                }
            }
            Holiday.VALENTINE -> for (i in 0 until 9) {
                // Hearts float up and fade.
                val ph = t * (0.04f + hf(i, 41) * 0.03f) + hf(i, 42); val k = floor(ph).toInt(); val fr = ph - k
                val x = hf(i, k + 43) * w + sin(t * 1.3f + i) * s * 4
                dots(c, x, h * (1.05f - fr * 1.1f), s, HEART, if (i % 2 == 0) 0xFFFF4A7A.toInt() else 0xFFFF9ABA.toInt(), alpha = (255 * (1f - fr) * wet).toInt())
            }
            Holiday.STPATRICK -> for (i in 0 until 10) {
                // Clovers tumble down, and a gold glint here and there.
                val ph = t * (0.05f + hf(i, 51) * 0.04f) + hf(i, 52); val k = floor(ph).toInt(); val fr = ph - k
                val x = hf(i, k + 53) * w + sin(t * 1.1f + i * 2f) * s * 6
                if (i % 4 == 3) dots(c, x, fr * h, s, SPARK, 0xFFFFD23A.toInt(), alpha = (255 * sin(t * 6f + i).coerceAtLeast(0f)).toInt())
                else dots(c, x, -s * 4 + fr * (h + s * 8), s, CLOVER, 0xFF3AC04A.toInt(), 0xFF1E7A2A.toInt())
            }
            Holiday.FOURTWENTY -> {
                // Lazy smoke drifting up and across.
                val b = glows.getOrPut(0xC8D8C0) { glowBitmap(0xC8D8C0) }
                for (i in 0 until 8) {
                    val ph = t * (0.025f + hf(i, 61) * 0.02f) + hf(i, 62); val k = floor(ph).toInt(); val fr = ph - k
                    val r = s * (6f + fr * 18f)
                    val x = hf(i, k + 63) * w + fr * w * 0.2f + sin(t * 0.7f + i) * s * 6
                    val y = h * (1.1f - fr * 1.2f)
                    soft.alpha = (90 * (1f - fr) * wet).toInt()
                    rect.set(x - r, y - r * 0.7f, x + r, y + r * 0.7f)
                    c.drawBitmap(b, null, rect, soft)
                }
                for (i in 0 until 6) {
                    val x = (0.5f + 0.45f * sin(t * 0.07f + i * 2.1f)) * w; val y = (0.5f + 0.4f * sin(t * 0.11f + i * 1.7f)) * h
                    dots(c, x, y, s, if (sin(t * 9f + i) > 0) FLY_UP else FLY_DOWN, 0xFF7AE03A.toInt(), 0xFF8A4AE0.toInt())
                }
            }
            Holiday.EASTER -> for (i in 0 until 8) {
                // Pastel butterflies flitting about.
                val cols = intArrayOf(0xFFC0A0FF.toInt(), 0xFFFFE07A.toInt(), 0xFFA0E0FF.toInt(), 0xFFFFB0D0.toInt())
                val x = (0.5f + 0.45f * sin(t * (0.06f + hf(i, 71) * 0.04f) + i * 2.4f) + 0.03f * sin(t * 3f + i)) * w
                val y = (0.5f + 0.4f * sin(t * (0.08f + hf(i, 72) * 0.05f) + i * 1.1f) + 0.03f * sin(t * 4f + i)) * h
                dots(c, x, y, s, if (sin(t * 11f + i * 3) > 0) FLY_UP else FLY_DOWN, cols[i % 4], 0xFF3A2A1E.toInt())
            }
            Holiday.JULY4 -> for (i in 0 until 3) {
                // Fireworks: a burst every few seconds high up, brightest at night.
                val period = 3.2f + i
                val ph = (t + i * 1.3f) / period; val k = floor(ph).toInt(); val fr = ph - k
                if (fr > 0.6f) continue
                val cx = (0.15f + hf(i * 7 + 1, k) * 0.7f) * w; val cy = (0.1f + hf(i * 7 + 2, k) * 0.3f) * h
                val col = intArrayOf(0xFFE03A3A.toInt(), 0xFFF4F4FF.toInt(), 0xFF3A6AE0.toInt(), 0xFFFFD23A.toInt())[(k + i) % 4]
                val r = fr / 0.6f; val reach = s * 22 * kotlin.math.sqrt(r)
                val a = ((1f - r) * 255 * (0.5f + light.night * 0.5f)).toInt()
                for (j in 0 until 14) {
                    val ang = j / 14f * 6.283f
                    dots(c, cx + kotlin.math.cos(ang) * reach, cy + sin(ang) * reach + r * r * s * 8, s, SPARK, col, alpha = a)
                }
            }
            Holiday.THANKSGIVING -> {
                // A skein of geese crosses now and then, under drifting leaves.
                val ph = t / 40f; val k = floor(ph).toInt(); val fr = ph - k
                val gy = (0.1f + hf(k, 81) * 0.3f) * h
                for (g in 0 until 7) {
                    val off = (g + 1) / 2 * (if (g % 2 == 0) 1 else -1)
                    val x = -w * 0.2f + fr * w * 1.4f - kotlin.math.abs(off) * s * 7
                    dots(c, x, gy + off * s * 5, s, if (sin(t * 8f + g) > 0) GOOSE_UP else GOOSE_DOWN, 0xFF4A3A2A.toInt(), 0xFFE8E0D0.toInt())
                }
                for (i in 0 until 8) {
                    val p2 = t * (0.04f + hf(i, 82) * 0.03f) + hf(i, 83); val k2 = floor(p2).toInt(); val f2 = p2 - k2
                    dots(c, hf(i, k2 + 84) * w + sin(t * 1.4f + i) * s * 6, -s * 4 + f2 * (h + s * 8), s, LEAF,
                        if (i % 2 == 0) 0xFFE0903A.toInt() else 0xFFA04A20.toInt())
                }
            }
            Holiday.CHRISTMAS -> for (i in 0 until 16) {
                // Twinkling red, green and gold sparkles hanging in the air.
                val a = sin(t * (1.5f + hf(i, 91)) + i * 2.2f)
                if (a <= 0) continue
                val col = intArrayOf(0xFFE03A3A.toInt(), 0xFF3AA04A.toInt(), 0xFFFFD23A.toInt())[i % 3]
                dots(c, hf(i, 92 + floor(t / 9f + hf(i, 93)).toInt()) * w, hf(i, 94 + floor(t / 9f + hf(i, 93)).toInt()) * h, s, SPARK, col, alpha = (255 * a).toInt())
            }
            Holiday.NEWYEAR -> for (i in 0 until 24) {
                // Confetti tumbling down.
                val ph = t * (0.06f + hf(i, 101) * 0.05f) + hf(i, 102); val k = floor(ph).toInt(); val fr = ph - k
                val x = hf(i, k + 103) * w + sin(t * 2f + i) * s * 5
                val y = -s * 2 + fr * (h + s * 4)
                fill.color = intArrayOf(0xFFFFD23A.toInt(), 0xFF3A3AA0.toInt(), 0xFFFF4A7A.toInt(), 0xFF3AC0E0.toInt())[i % 4]
                val tw = kotlin.math.abs(sin(t * 5f + i)) * s * 1.5f + s * 0.3f
                c.drawRect(x - tw / 2, y, x + tw / 2, y + s, fill)
            }
        }
    }

    /** Rain and fog over the whole screen, in screen pixels. */
    fun drawSky(c: Canvas, w: Float, h: Float, scale: Float, t: Float, light: Light) {
        if (light.fog > 0.01f) {
            soft.color = 0xFFDCE4E6.toInt(); soft.alpha = (light.fog * 46).toInt()
            c.drawRect(0f, 0f, w, h, soft)
            val b = glows.getOrPut(0xDCE4E6) { glowBitmap(0xDCE4E6) }
            soft.alpha = (light.fog * 120).toInt()
            for (i in 0 until 7) {
                val bw = w * (0.7f + hf(i, 1) * 0.6f); val bh = h * (0.12f + hf(i, 2) * 0.1f)
                val x = ((hf(i, 3) * (w + bw) + t * (6f + hf(i, 4) * 10f) * scale * 0.3f) % (w + bw)) - bw
                val y = hf(i, 5) * h
                rect.set(x, y - bh / 2, x + bw, y + bh / 2)
                c.drawBitmap(b, null, rect, soft)
            }
        }
        // Snow drifts down in winter instead of rain.
        if (light.rain > 0.01f && light.season == Season.WINTER) {
            fill.color = 0xE8F4F8FF.toInt()
            val n = (140 * light.rain).toInt()
            for (i in 0 until n) {
                val sp = 0.12f + hf(i, 9) * 0.1f
                val ph = t * sp + hf(i, 8)
                val k = floor(ph).toInt(); val fr = ph - k
                val s = maxOf(3f, scale * (0.6f + hf(i, 7) * 0.8f))
                val x = hf(i, k) * w + sin(t * (0.8f + hf(i, 6)) + i) * scale * 4f
                val y = -s + fr * (h + s * 2)
                c.drawRect(x, y, x + s, y + s, fill)
            }
            return
        }
        // A few petals in spring and leaves in autumn, whatever the weather.
        if (light.season == Season.SPRING || light.season == Season.AUTUMN) {
            // Petals are plainly pink, and few, so they never pass for snow.
            val cols = if (light.season == Season.SPRING) intArrayOf(0xFFF090B0.toInt(), 0xFFE878A0.toInt(), 0xFFF4A8C4.toInt())
                else intArrayOf(0xFFE08A3A.toInt(), 0xFFC8582E.toInt(), 0xFFE8B840.toInt())
            for (i in 0 until if (light.season == Season.SPRING) 8 else 14) {
                val sp = 0.05f + hf(i, 19) * 0.05f
                val ph = t * sp + hf(i, 18)
                val k = floor(ph).toInt(); val fr = ph - k
                val s = maxOf(3f, scale * 0.9f)
                val sw = sin(t * (1.2f + hf(i, 16)) + i * 1.7f)
                val x = hf(i, k + 40) * w + fr * w * 0.25f + sw * scale * 6f
                val y = -s + fr * (h + s * 2)
                fill.color = cols[i % cols.size]
                c.drawRect(x, y, x + s, y + s * (0.5f + 0.5f * kotlin.math.abs(sw)), fill)
            }
        }
        if (light.rain > 0.01f) {
            fill.color = 0x80C8DCFF.toInt()
            fill.strokeWidth = maxOf(2f, scale * 0.5f)
            val len = maxOf(18f, scale * 7f)
            val n = (160 * light.rain).toInt()
            for (i in 0 until n) {
                val sp = 0.8f + hf(i, 9) * 0.5f
                val ph = t * sp * 1.6f + hf(i, 8)
                val k = floor(ph).toInt(); val fr = ph - k
                val x0 = hf(i, k) * (w + h * 0.3f)
                val y = -len + fr * (h + len * 2)
                val x = x0 - fr * h * 0.3f
                c.drawLine(x, y, x - len * 0.3f, y + len, fill)
            }
        }
    }
}
