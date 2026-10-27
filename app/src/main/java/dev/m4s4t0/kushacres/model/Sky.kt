package dev.m4s4t0.kushacres.model

import kotlin.math.cos
import kotlin.math.PI

enum class Weather(val label: String) { CLEAR("Clear"), CLOUDY("Cloudy"), RAIN("Rain"), FOG("Fog"), STORM("Storm") }

/** The sky over the farm: real-time day and night, and weather that changes every couple of hours. */
object Sky {
    const val SLOT = 2 * 3_600_000L

    /** Test overrides (debug builds only, from the launch intent). */
    var forceWeather: Weather? = null
    var forceHour: Float? = null

    /** The local time of day as a fractional hour, 0..24. */
    fun hour(now: Long): Float = forceHour ?: (((now + java.util.TimeZone.getDefault().getOffset(now)) % 86_400_000L) / 3_600_000f)

    /** The weather for the two-hour slot [now] falls in; the same for everyone at that moment. */
    fun weather(now: Long): Weather = forceWeather ?: rolled(now)

    /** How strongly [w] is happening: eases in and out over a few minutes when the weather changes. */
    fun amount(now: Long, w: Weather): Float {
        if (weather(now) != w) return 0f
        if (forceWeather != null) return 1f
        val f = slotFrac(now); val edge = 0.05f
        val a = if (rolled(now - SLOT) == w) 1f else (f / edge).coerceAtMost(1f)
        val b = if (rolled(now + SLOT) == w) 1f else ((1 - f) / edge).coerceAtMost(1f)
        return minOf(a, b)
    }

    private fun rolled(now: Long): Weather {
        var h = (now / SLOT) * 0x9E3779B97F4A7C15uL.toLong()
        h = h xor (h ushr 31)
        val n = ((h ushr 8) and 0xFFFF).toInt() % 100
        return when {
            n < 56 -> Weather.CLEAR
            n < 76 -> Weather.CLOUDY
            n < 87 -> Weather.RAIN
            n < 90 -> Weather.STORM
            else -> Weather.FOG
        }
    }

    /** How the weather is changing: 0 just started .. 1 about to change, for fading in and out. */
    fun slotFrac(now: Long) = (now % SLOT) / SLOT.toFloat()

    /**
     * Daylight for a local hour of day (fractional, 0..24): 1 at noon-ish, 0 deep at night,
     * with a soft dawn around 6 and dusk around 19.
     */
    fun daylight(hour: Float): Float {
        fun ramp(a: Float, b: Float, x: Float) = ((x - a) / (b - a)).coerceIn(0f, 1f)
        val up = ramp(5f, 7.5f, hour)
        val down = 1f - ramp(18f, 20.5f, hour)
        val d = minOf(up, down)
        return (0.5f - 0.5f * cos(PI * d).toFloat())
    }

    /** How golden the light is: strongest just after dawn and just before dark. */
    fun golden(hour: Float): Float {
        fun bump(c: Float, w: Float) = (1f - kotlin.math.abs(hour - c) / w).coerceIn(0f, 1f)
        return maxOf(bump(6.8f, 1.6f), bump(18.8f, 1.8f))
    }
}
