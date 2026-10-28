package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.model.SeshSize
import dev.m4s4t0.kushacres.model.Strain
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The little shapes that float around while you're high. */
enum class Bit { LEAF, HEART, SNOW, BAT, CLOVER, STAR, EGG, NOTE, SPARK }

/** How a sesh looks and sounds: the strain's holiday picks it, everything else gets the classic tie-dye. */
enum class TripTheme(val colors: List<Color>, val smoke: Color, val bit: Bit, val rate: Float, val line: String) {
    CLASSIC(listOf(Color(0xFFE53935), Color(0xFFFF9800), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA)),
        Color(0xFFE8E8E0), Bit.LEAF, 0.88f, "Whoa… the colours, man"),
    COSMIC(listOf(Color(0xFF5E35B1), Color(0xFF00BCD4), Color(0xFFE040FB), Color(0xFF1A237E), Color(0xFF64FFDA)),
        Color(0xFFB39DDB), Bit.STAR, 0.8f, "You can see the whole universe from here"),
    HALLOWEEN(listOf(Color(0xFFFF8A1E), Color(0xFF7A3CC8), Color(0xFF5BE36A), Color(0xFF2A1A3A)),
        Color(0xFF9CE89C), Bit.BAT, 0.78f, "Did that pumpkin just wink at you?"),
    CHRISTMAS(listOf(Color(0xFFD83A3A), Color(0xFF2E9E4A), Color(0xFFF4F4F4), Color(0xFFF2C94C)),
        Color(0xFFF8F8FF), Bit.SNOW, 1.12f, "Jingle all the waaay"),
    VALENTINE(listOf(Color(0xFFFF5A8A), Color(0xFFFF9AC1), Color(0xFFE0245E), Color(0xFFC79BFF)),
        Color(0xFFFFC0D8), Bit.HEART, 0.92f, "You love everyone right now"),
    STPATRICK(listOf(Color(0xFF2EA043), Color(0xFF7BE07B), Color(0xFFFFD23A), Color(0xFF0E6B2E)),
        Color(0xFFC8F5C8), Bit.CLOVER, 1.05f, "There's gold at the end of this one"),
    FOURTWENTY(listOf(Color(0xFF7AE03A), Color(0xFF8A4AE0), Color(0xFFFFD23A), Color(0xFF2E7D32)),
        Color(0xFFEFF5E8), Bit.LEAF, 0.82f, "Happy high holiday"),
    EASTER(listOf(Color(0xFFC0A0FF), Color(0xFFFFE07A), Color(0xFF9EE6C8), Color(0xFFFFB3C7), Color(0xFF9CC8FF)),
        Color(0xFFFFF4FA), Bit.EGG, 1.06f, "Everything's so soft and pastel"),
    JULY4(listOf(Color(0xFFE03A3A), Color(0xFFF4F4F4), Color(0xFF3A6AE0)),
        Color(0xFFDDE4F5), Bit.STAR, 1.08f, "Freedom tastes like this"),
    NEWYEAR(listOf(Color(0xFFFFD23A), Color(0xFFC0C0D0), Color(0xFF3A3AA0), Color(0xFFE040FB)),
        Color(0xFFFFF3C4), Bit.SPARK, 1.1f, "New year, new you. Same couch"),
    THANKSGIVING(listOf(Color(0xFFE0903A), Color(0xFF8A4A20), Color(0xFFF2C94C), Color(0xFFC0392B)),
        Color(0xFFF0DCC0), Bit.LEAF, 0.9f, "Is there more pie?"),
    BUCKS(listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047)),
        Color(0xFFE8F0D8), Bit.NOTE, 0.85f, "One love, one heart");

    companion object {
        fun of(s: Strain): TripTheme = when (s.holiday) {
            "halloween" -> HALLOWEEN; "christmas" -> CHRISTMAS; "valentine" -> VALENTINE; "stpatrick" -> STPATRICK
            "fourtwenty" -> FOURTWENTY; "easter" -> EASTER; "july4" -> JULY4; "newyear" -> NEWYEAR
            "thanksgiving" -> THANKSGIVING; "bucks" -> BUCKS
            else -> if (s.exotic) COSMIC else CLASSIC
        }
    }
}

/** The sesh going on right now, if any. Only on screen: a restart sobers you up, the buffs stay. */
object Stoned {
    private const val SMOKE = 3_500L
    var start by mutableLongStateOf(0L)
        private set
    var length by mutableLongStateOf(0L)
        private set
    var theme by mutableStateOf(TripTheme.CLASSIC)
        private set
    /** The strain's own bud colour tints the smoke in the classic theme. */
    var tint by mutableStateOf<Color?>(null)
        private set

    fun minutes(size: SeshSize) = when (size) { SeshSize.JOINT -> 3; SeshSize.BLUNT -> 5; SeshSize.HOTBOX -> 8 }

    fun begin(s: Strain, size: SeshSize) {
        theme = TripTheme.of(s)
        tint = if (theme == TripTheme.CLASSIC) lerp(Color(s.bud), Color.White, 0.55f) else null
        length = SMOKE + minutes(size) * 60_000L
        start = System.currentTimeMillis()
        Audio.trip(theme.rate)
    }

    fun end() { start = 0L; Audio.trip(1f) }
}

/** Full-screen overlay that never takes a touch; runs its own frame clock while a trip lasts. */
@Composable
fun StonedOverlay() {
    val start = Stoned.start
    if (start == 0L) return
    var now by androidx.compose.runtime.remember(start) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) {
        while (true) {
            withFrameMillis { }
            now = System.currentTimeMillis()
            if (now - start >= Stoned.length) { Stoned.end(); break }
        }
    }
    TripLayer(Stoned.theme, now - start, Stoned.length, Stoned.tint)
}

/** One frame of a trip, [e] ms in; split out so the desktop dump can render any moment of it. */
@Composable
fun TripLayer(theme: TripTheme, e: Long, length: Long, tint: Color? = null) {
    Canvas(Modifier.fillMaxSize()) { drawTrip(theme, e, length, tint) }
}

private fun ease(x: Float) = x.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
private fun rnd(i: Int, k: Int): Float = (((i * 73856093) xor (k * 19349663)) and 0xFFFF) / 65535f

internal fun DrawScope.drawTrip(theme: TripTheme, e: Long, length: Long, tint: Color? = null) {
    val t = e / 1000f
    val w = size.width; val h = size.height
    val unit = min(w, h)
    val cols = theme.colors

    // 1. Waves: faint colour bands drifting across everything until the trip wears off.
    val fadeIn = ease((t - 2.5f) / 3f)
    val fadeOut = ease((length - e) / 20_000f)
    val wa = 0.11f * fadeIn * fadeOut
    if (wa > 0.003f) {
        val bands = 9
        val bh = h / (bands - 1)
        val shift = t / 2.5f
        for (k in -1 until bands) {
            val ci = Math.floorMod(k + shift.toInt(), cols.size)
            val c = lerp(cols[ci], cols[(ci + 1) % cols.size], shift - shift.toInt())
            val p = Path()
            val n = 28
            fun y(x: Float, row: Int) = row * bh + sin(x / w * 2.6f * PI.toFloat() + t * 0.9f + row * 0.8f) * bh * 0.45f +
                sin(x / w * 5.3f * PI.toFloat() - t * 0.6f + row) * bh * 0.12f
            p.moveTo(0f, y(0f, k))
            for (i in 1..n) { val x = w * i / n; p.lineTo(x, y(x, k)) }
            for (i in n downTo 0) { val x = w * i / n; p.lineTo(x, y(x, k + 1)) }
            p.close()
            drawPath(p, c.copy(alpha = wa))
        }
        // A few of the theme's shapes drifting about.
        val ba = 0.32f * fadeIn * fadeOut
        for (i in 0 until 10) {
            val sp = 0.02f + rnd(i, 1) * 0.03f
            val x = ((rnd(i, 2) + t * sp * (if (i % 2 == 0) 1 else -1)) % 1f + 1f) % 1f * w
            val y = ((rnd(i, 3) - t * sp * 0.6f) % 1f + 1f) % 1f * h
            val sz = unit * (0.035f + rnd(i, 4) * 0.03f)
            drawBit(theme.bit, Offset(x + sin(t + i) * unit * 0.03f, y), sz, cols[i % cols.size].copy(alpha = ba), sin(t * 0.7f + i) * 25f)
        }
    }

    // 2. The tie-dye burst: a spiral of the theme's colours that blooms, spins and fades.
    val bt = t - 1.6f
    if (bt > 0f && bt < 4f) {
        val a = 0.7f * ease(bt / 0.8f) * ease((4f - bt) / 1.6f)
        val cx = w / 2; val cy = h / 2
        val maxR = kotlin.math.hypot(w, h) / 2 * (0.3f + 0.7f * ease(bt / 1.4f))
        val step = unit * 0.045f
        val arms = cols.size * 2
        val sweep = 360f / arms
        var r = step / 2
        while (r < maxR) {
            for (j in 0 until arms) {
                val start = j * sweep + bt * 70f + r / unit * 260f
                drawArc(cols[j % cols.size].copy(alpha = a * (1 - 0.35f * r / maxR)), start, sweep + 1f, false,
                    Offset(cx - r, cy - r), Size(r * 2, r * 2), style = Stroke(step + 1f))
            }
            r += step
        }
        // Shapes thrown out from the middle.
        for (i in 0 until 18) {
            val ang = i / 18f * 2 * PI.toFloat() + rnd(i, 5)
            val d = unit * (0.1f + bt * (0.18f + rnd(i, 6) * 0.15f))
            drawBit(theme.bit, Offset(cx + cos(ang) * d, cy + sin(ang) * d), unit * 0.05f, Color.White.copy(alpha = a * 0.9f), bt * 90f + i * 20f)
        }
    }

    // 3. Smoke: a haze and big soft puffs rolling up and out from the bottom.
    if (t < 3.5f) {
        val smoke = tint ?: theme.smoke
        val haze = 0.5f * ease(t / 0.8f) * ease((3.5f - t) / 1.6f)
        drawRect(smoke.copy(alpha = haze * 0.6f))
        for (i in 0 until 26) {
            val born = rnd(i, 7) * 1.2f
            val life = t - born
            if (life <= 0f || life > 2.6f) continue
            val k = life / 2.6f
            val x = w * (0.1f + 0.8f * rnd(i, 8)) + sin(life * 2 + i) * unit * 0.06f
            val y = h * (1.05f - k * (0.7f + 0.4f * rnd(i, 9)))
            val r = unit * (0.08f + 0.22f * k) * (0.7f + 0.6f * rnd(i, 10))
            val al = 0.55f * ease(k * 4f) * (1 - k)
            drawCircle(Brush.radialGradient(listOf(smoke.copy(alpha = al), smoke.copy(alpha = al * 0.5f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
    }
}

private fun DrawScope.drawBit(b: Bit, at: Offset, s: Float, c: Color, deg: Float) {
    translate(at.x, at.y) { rotate(deg, Offset.Zero) { bit(b, s, c) } }
}

/** Draws [b] centred on the origin, about [s] across. */
private fun DrawScope.bit(b: Bit, s: Float, c: Color) {
    val r = s / 2
    when (b) {
        Bit.LEAF -> {
            // Seven blades fanning up from the stem.
            val ang = floatArrayOf(-90f, -55f, -125f, -20f, -160f, 15f, 165f)
            val len = floatArrayOf(1f, 0.85f, 0.85f, 0.65f, 0.65f, 0.4f, 0.4f)
            for (i in ang.indices) rotate(ang[i] + 90f, Offset(0f, r * 0.3f)) {
                drawOval(c, Offset(-r * 0.13f, r * 0.3f - r * 1.1f * len[i]), Size(r * 0.26f, r * 1.1f * len[i]))
            }
            drawLine(c, Offset(0f, r * 0.3f), Offset(0f, r * 0.8f), r * 0.08f)
        }
        Bit.HEART -> {
            drawCircle(c, r * 0.5f, Offset(-r * 0.42f, -r * 0.2f)); drawCircle(c, r * 0.5f, Offset(r * 0.42f, -r * 0.2f))
            drawPath(Path().apply { moveTo(-r * 0.9f, 0f); lineTo(r * 0.9f, 0f); lineTo(0f, r * 0.85f); close() }, c)
        }
        Bit.SNOW -> for (i in 0 until 3) rotate(i * 60f, Offset.Zero) {
            drawLine(c, Offset(0f, -r), Offset(0f, r), r * 0.14f)
            drawLine(c, Offset(0f, -r * 0.6f), Offset(-r * 0.25f, -r * 0.85f), r * 0.1f)
            drawLine(c, Offset(0f, -r * 0.6f), Offset(r * 0.25f, -r * 0.85f), r * 0.1f)
            drawLine(c, Offset(0f, r * 0.6f), Offset(-r * 0.25f, r * 0.85f), r * 0.1f)
            drawLine(c, Offset(0f, r * 0.6f), Offset(r * 0.25f, r * 0.85f), r * 0.1f)
        }
        Bit.BAT -> {
            val p = Path().apply {
                moveTo(0f, -r * 0.2f)
                lineTo(-r * 0.4f, -r * 0.45f); lineTo(-r, -r * 0.2f); lineTo(-r * 0.75f, r * 0.05f); lineTo(-r * 0.55f, -r * 0.05f)
                lineTo(-r * 0.4f, r * 0.2f); lineTo(-r * 0.15f, r * 0.05f); lineTo(0f, r * 0.3f)
                lineTo(r * 0.15f, r * 0.05f); lineTo(r * 0.4f, r * 0.2f); lineTo(r * 0.55f, -r * 0.05f); lineTo(r * 0.75f, r * 0.05f)
                lineTo(r, -r * 0.2f); lineTo(r * 0.4f, -r * 0.45f); close()
            }
            drawPath(p, c)
            drawCircle(c, r * 0.2f, Offset(0f, -r * 0.25f))
        }
        Bit.CLOVER -> {
            for (i in 0 until 4) rotate(i * 90f + 45f, Offset.Zero) {
                drawCircle(c, r * 0.3f, Offset(-r * 0.2f, -r * 0.42f)); drawCircle(c, r * 0.3f, Offset(r * 0.2f, -r * 0.42f))
            }
            drawLine(c, Offset.Zero, Offset(r * 0.3f, r), r * 0.1f)
        }
        Bit.STAR, Bit.SPARK -> {
            val pts = if (b == Bit.STAR) 5 else 4
            val inner = if (b == Bit.STAR) 0.42f else 0.25f
            val p = Path()
            for (i in 0 until pts * 2) {
                val a = (i * PI / pts - PI / 2).toFloat()
                val rr = if (i % 2 == 0) r else r * inner
                if (i == 0) p.moveTo(cos(a) * rr, sin(a) * rr) else p.lineTo(cos(a) * rr, sin(a) * rr)
            }
            p.close()
            drawPath(p, c)
        }
        Bit.EGG -> {
            drawOval(c, Offset(-r * 0.7f, -r), Size(r * 1.4f, r * 1.9f))
            drawLine(Color.White.copy(alpha = c.alpha * 0.8f), Offset(-r * 0.65f, 0f), Offset(r * 0.65f, 0f), r * 0.16f)
        }
        Bit.NOTE -> {
            drawOval(c, Offset(-r * 0.7f, r * 0.35f), Size(r * 0.7f, r * 0.5f))
            drawLine(c, Offset(-r * 0.04f, r * 0.6f), Offset(-r * 0.04f, -r * 0.8f), r * 0.12f)
            drawLine(c, Offset(-r * 0.04f, -r * 0.8f), Offset(r * 0.55f, -r * 0.45f), r * 0.16f)
        }
    }
}
