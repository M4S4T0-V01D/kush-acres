package dev.m4s4t0.kushacres.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
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
            val rain = Sky.amount(now, Weather.RAIN)
            val fog = Sky.amount(now, Weather.FOG)
            val cloud = Sky.amount(now, Weather.CLOUDY)
            fun mix(a: Float, b: Float, k: Float) = a + (b - a) * k
            var r = mix(0.30f, 1f, d); var gg = mix(0.36f, 1f, d); var b = mix(0.68f, 1f, d)
            val gk = g * 0.55f
            r *= mix(1f, 1.0f, gk); gg *= mix(1f, 0.84f, gk); b *= mix(1f, 0.64f, gk)
            r *= mix(1f, 0.84f, cloud); gg *= mix(1f, 0.86f, cloud); b *= mix(1f, 0.9f, cloud)
            r *= mix(1f, 0.68f, rain); gg *= mix(1f, 0.72f, rain); b *= mix(1f, 0.84f, rain)
            r *= mix(1f, 0.9f, fog); gg *= mix(1f, 0.91f, fog); b *= mix(1f, 0.93f, fog)
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
            val cols = if (light.season == Season.SPRING) intArrayOf(0xFFF8C8D8.toInt(), 0xFFFFE8F0.toInt(), 0xFFF0A8C0.toInt())
                else intArrayOf(0xFFE08A3A.toInt(), 0xFFC8582E.toInt(), 0xFFE8B840.toInt())
            for (i in 0 until 14) {
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
