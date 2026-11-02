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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
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
    private const val SMOKE = 5_000L
    var start by mutableLongStateOf(0L)
        private set
    var length by mutableLongStateOf(0L)
        private set
    var theme by mutableStateOf(TripTheme.CLASSIC)
        private set
    /** The strain's own bud colour tints the smoke in the classic theme. */
    var tint by mutableStateOf<Color?>(null)
        private set
    var size by mutableStateOf(SeshSize.JOINT)
        private set

    fun minutes(size: SeshSize) = when (size) { SeshSize.JOINT -> 3; SeshSize.BLUNT -> 5; SeshSize.HOTBOX -> 8 }

    fun begin(s: Strain, size: SeshSize) {
        theme = TripTheme.of(s)
        this.size = size
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
    TripLayer(Stoned.theme, now - start, Stoned.length, Stoned.tint, Stoned.size)
}

/** One frame of a trip, [e] ms in; split out so the desktop dump can render any moment of it. */
@Composable
fun TripLayer(theme: TripTheme, e: Long, length: Long, tint: Color? = null, size: SeshSize = SeshSize.JOINT) {
    Canvas(Modifier.fillMaxSize()) { drawTrip(theme, e, length, tint, size) }
}

private fun ease(x: Float) = x.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
private fun rnd(i: Int, k: Int): Float = (((i * 73856093) xor (k * 19349663)) and 0xFFFF) / 65535f

/** How hard each size hits on screen: a joint is a light buzz, a hotbox fills the room. */
private fun punch(size: SeshSize) = when (size) { SeshSize.JOINT -> 0.65f; SeshSize.BLUNT -> 1f; SeshSize.HOTBOX -> 1.4f }

internal fun DrawScope.drawTrip(theme: TripTheme, e: Long, length: Long, tint: Color? = null, sesh: SeshSize = SeshSize.JOINT) {
    val t = e / 1000f
    val w = size.width; val h = size.height
    val unit = min(w, h)
    val cols = theme.colors
    val k = punch(sesh)
    val smoke = tint ?: theme.smoke
    // The beat everything breathes to once it kicks in; the theme's tempo sets it.
    val beat = t * 76f * theme.rate / 60f
    val pulse = exp(-(beat - floor(beat)) * 3.5f)

    // 1. Waves: colour bands drifting across everything, swelling on the beat until the trip wears off.
    val fadeIn = ease((t - 3.6f) / 3f)
    val fadeOut = ease((length - e) / 20_000f)
    val wa = (0.08f + 0.05f * pulse) * min(1.2f, k) * fadeIn * fadeOut
    if (wa > 0.003f) {
        val bands = 9
        val bh = h / (bands - 1)
        val shift = t / 2.5f
        for (b in -1 until bands) {
            val ci = Math.floorMod(b + shift.toInt(), cols.size)
            val c = lerp(cols[ci], cols[(ci + 1) % cols.size], shift - shift.toInt())
            val p = Path()
            val n = 28
            val amp = 0.45f + 0.12f * pulse
            fun y(x: Float, row: Int) = row * bh + sin(x / w * 2.6f * PI.toFloat() + t * 0.9f + row * 0.8f) * bh * amp +
                sin(x / w * 5.3f * PI.toFloat() - t * 0.6f + row) * bh * 0.12f
            p.moveTo(0f, y(0f, b))
            for (i in 1..n) { val x = w * i / n; p.lineTo(x, y(x, b)) }
            for (i in n downTo 0) { val x = w * i / n; p.lineTo(x, y(x, b + 1)) }
            p.close()
            drawPath(p, c.copy(alpha = wa))
        }
        // A soft glow round the edges that throbs with the beat.
        val edge = lerp(cols[Math.floorMod(beat.toInt(), cols.size)], cols[Math.floorMod(beat.toInt() + 1, cols.size)], beat - floor(beat))
        val ga = (0.1f + 0.14f * pulse) * min(1.2f, k) * fadeIn * fadeOut
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, edge.copy(alpha = ga)), Offset(w / 2, h / 2), hypot(w, h) / 2))
        // A few of the theme's shapes drifting about.
        val ba = 0.32f * fadeIn * fadeOut
        for (i in 0 until (6 + 6 * k).toInt()) {
            val sp = 0.02f + rnd(i, 1) * 0.03f
            val x = ((rnd(i, 2) + t * sp * (if (i % 2 == 0) 1 else -1)) % 1f + 1f) % 1f * w
            val y = ((rnd(i, 3) - t * sp * 0.6f) % 1f + 1f) % 1f * h
            val sz = unit * (0.035f + rnd(i, 4) * 0.03f) * (1 + 0.15f * pulse)
            drawBit(theme.bit, Offset(x + sin(t + i) * unit * 0.03f, y), sz, cols[i % cols.size].copy(alpha = ba), sin(t * 0.7f + i) * 25f)
        }
    }

    // 2. The tie-dye burst: a spiral of the theme's colours that blooms, spins and fades as the exhale clears.
    val bt = t - 3.0f
    if (bt > 0f && bt < 4f) {
        val a = min(0.8f, 0.6f * k) * ease(bt / 0.8f) * ease((4f - bt) / 1.6f)
        val cx = w / 2; val cy = h / 2
        val maxR = hypot(w, h) / 2 * (0.3f + 0.7f * ease(bt / 1.4f)) * min(1f, 0.55f + 0.45f * k)
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
        for (i in 0 until (10 + 8 * k).toInt()) {
            val ang = i / 18f * 2 * PI.toFloat() + rnd(i, 5)
            val d = unit * (0.1f + bt * (0.18f + rnd(i, 6) * 0.15f))
            drawBit(theme.bit, Offset(cx + cos(ang) * d, cy + sin(ang) * d), unit * 0.05f, Color.White.copy(alpha = a * 0.9f), bt * 90f + i * 20f)
        }
    }

    // 3. The sesh itself: light up, draw on it, blow it out.
    if (t < 5.2f) {
        // The haze: a hotbox fills the room before it clears.
        val haze = (0.18f + 0.5f * (k - 0.65f)) * ease((t - 1.8f) / 1.2f) * ease((5.2f - t) / 1.8f)
        if (haze > 0f) drawRect(smoke.copy(alpha = haze.coerceAtMost(0.85f)))
        // The ember: lit by the lighter, flaring while you draw on it.
        val shown = ease(t / 0.5f) * ease((4.4f - t) / 0.5f)
        val lit = ease((t - 0.55f) / 0.3f) * shown
        val inhale = ease((t - 1.1f) / 0.4f) * ease((2.3f - t) / 0.3f)
        val glow = (lit * (0.45f + 0.55f * inhale) + 0.05f * sin(t * 13f) * lit).coerceIn(0f, 1f)
        val tip = if (shown > 0f) drawPiece(sesh, t, shown, inhale, smoke) else Offset(w / 2, h)
        if (shown > 0f) {
            // A thin curl of smoke off the ember the whole time it's lit.
            val sa = 0.5f * lit
            if (sa > 0.01f) for (i in 0 until 36) {
                val s = i / 36f
                val x = tip.x + sin(s * 7f - t * 3f) * unit * 0.025f * (1 + s * 3f)
                val y = tip.y - s * h * 0.45f
                drawCircle(smoke.copy(alpha = sa * (1 - s) * 0.6f), unit * (0.006f + 0.03f * s), Offset(x, y))
            }
            if (glow > 0.01f) {
                val er = unit * 0.016f
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFF7A1A).copy(alpha = 0.7f * glow), Color.Transparent), tip, er * (3 + 4 * glow)), er * (3 + 4 * glow), tip)
                drawCircle(lerp(Color(0xFFB0301A), Color(0xFFFFE07A), glow), er, tip)
                drawCircle(Color(0xFFFFF4C0).copy(alpha = glow * inhale), er * 0.5f, tip)
                // Ash flakes and sparks flicking off it.
                for (i in 0 until 5) {
                    val life = ((t * 0.9f + rnd(i, 11)) % 1f)
                    drawCircle(Color(0xFFFFB040).copy(alpha = glow * (1 - life) * 0.9f), er * 0.22f,
                        tip + Offset((rnd(i, 12) - 0.5f) * unit * 0.06f * life, -unit * 0.08f * life))
                }
            }
            drawLighter(tip, t, unit)
        }
        // The exhale: big soft puffs rolling up from the bottom…
        val puffs = (10 + 14 * k).toInt()
        for (i in 0 until puffs) {
            val born = 2.2f + rnd(i, 7) * 1.1f
            val life = t - born
            if (life <= 0f || life > 2.6f) continue
            val q = life / 2.6f
            val x = w * (0.5f + (rnd(i, 8) - 0.5f) * (0.3f + 0.6f * q)) + sin(life * 2 + i) * unit * 0.06f
            val y = h * (0.75f - q * (0.55f + 0.4f * rnd(i, 9)))
            val r = unit * (0.06f + 0.24f * q) * (0.7f + 0.6f * rnd(i, 10)) * (0.8f + 0.2f * k)
            val al = 0.55f * ease(q * 4f) * (1 - q)
            drawCircle(Brush.radialGradient(listOf(smoke.copy(alpha = al), smoke.copy(alpha = al * 0.5f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
        // …and a few rings floating up out of them.
        val rings = when (sesh) { SeshSize.JOINT -> 2; SeshSize.BLUNT -> 3; SeshSize.HOTBOX -> 4 }
        for (j in 0 until rings) {
            val life = t - (2.5f + j * 0.45f)
            if (life <= 0f || life > 2.2f) continue
            val q = life / 2.2f
            val c = Offset(w * (0.5f + (rnd(j, 13) - 0.5f) * 0.2f) + sin(life * 1.7f + j) * unit * 0.03f, h * (0.58f - 0.4f * q))
            val rr = unit * (0.04f + 0.14f * ease(q * 1.3f))
            val al = 0.6f * ease(q * 5f) * (1 - q)
            val ring = Size(rr * 2, rr * 1.1f)
            drawOval(smoke.copy(alpha = al * 0.35f), c - Offset(rr, rr * 0.55f), ring, style = Stroke(rr * 0.5f))
            drawOval(smoke.copy(alpha = al), c - Offset(rr, rr * 0.55f), ring, style = Stroke(rr * 0.22f))
        }
    }
}

/** The joint, blunt or bong sliding up from the bottom of the screen; returns where its ember sits. */
private fun DrawScope.drawPiece(sesh: SeshSize, t: Float, shown: Float, inhale: Float, smoke: Color): Offset {
    val w = size.width; val h = size.height
    val unit = min(w, h)
    val line = unit * 0.005f
    val ink = Color(0xFF2A1C12)
    val drop = (1 - shown) * unit * 0.6f
    if (sesh != SeshSize.HOTBOX) {
        val blunt = sesh == SeshSize.BLUNT
        val len = unit * (if (blunt) 0.46f else 0.42f)
        val c = Offset(w * 0.47f, h * 0.84f + drop)
        val deg = -16f
        val r1 = len * (if (blunt) 0.07f else 0.03f)
        val r2 = len * (if (blunt) 0.075f else 0.058f)
        rotate(deg, c) {
            val x0 = c.x - len / 2; val x1 = c.x + len / 2
            val body = Path().apply { moveTo(x0, c.y - r1); lineTo(x1, c.y - r2); lineTo(x1, c.y + r2); lineTo(x0, c.y + r1); close() }
            if (blunt) {
                drawPath(body, Color(0xFF7A4E2A))
                // The leaf wrap spiralling round it.
                var x = x0 + len * 0.04f
                while (x < x1 - len * 0.06f) { drawLine(Color(0xFF55341A), Offset(x, c.y - r1), Offset(x + r1 * 1.2f, c.y + r1), line * 1.2f); x += len * 0.07f }
                drawPath(Path().apply { moveTo(x0, c.y - r1 * 0.5f); lineTo(x1, c.y - r2 * 0.5f); lineTo(x1, c.y - r2 * 0.75f); lineTo(x0, c.y - r1 * 0.75f); close() }, Color(0xFFA8784A))
            } else {
                drawPath(body, Color(0xFFF4EFE2))
                drawPath(Path().apply { moveTo(x0, c.y + r1 * 0.3f); lineTo(x1, c.y + r2 * 0.3f); lineTo(x1, c.y + r2); lineTo(x0, c.y + r1); close() }, Color(0xFFD9D0BB))
                drawLine(Color(0xFFCFC6B0), Offset(x0 + len * 0.17f, c.y - r1 * 0.2f), Offset(x1, c.y - r2 * 0.2f), line * 0.8f)
                // The crutch.
                drawRect(Color(0xFFD9B98A), Offset(x0, c.y - r1 * 1.05f), Size(len * 0.15f, r1 * 2.1f))
                drawLine(Color(0xFFB08A5A), Offset(x0 + len * 0.05f, c.y - r1), Offset(x0 + len * 0.05f, c.y + r1), line)
                drawLine(Color(0xFFB08A5A), Offset(x0 + len * 0.1f, c.y - r1), Offset(x0 + len * 0.1f, c.y + r1), line)
            }
            // The burnt end.
            drawRect(Color(0xFF8A8580), Offset(x1 - len * 0.035f, c.y - r2), Size(len * 0.035f, r2 * 2))
            drawPath(body, ink, style = Stroke(line))
        }
        val rad = deg * PI.toFloat() / 180f
        return c + Offset(cos(rad) * len / 2, sin(rad) * len / 2)
    }
    // The bong: a glass tube on a round base with water in it, and a bowl on the downstem.
    val b = unit * 0.4f
    val base = Offset(w * 0.5f, h * 0.92f + drop)
    val glass = Color(0xFF9EE6D0)
    val tubeW = b * 0.13f
    val bulb = Offset(base.x, base.y - b * 0.16f)
    val br = b * 0.17f
    val tube = Rect(base.x - tubeW / 2, base.y - b, base.x + tubeW / 2, base.y - b * 0.28f)
    drawRect(glass.copy(alpha = 0.22f), tube.topLeft, tube.size)
    drawCircle(glass.copy(alpha = 0.22f), br, bulb)
    // Milky smoke filling the tube as you pull, and the water.
    if (inhale > 0f) {
        val top = tube.bottom - (tube.height + br) * inhale
        drawRect(smoke.copy(alpha = 0.7f), Offset(tube.left, top), Size(tubeW, (tube.bottom - top).coerceAtLeast(0f)))
        drawCircle(smoke.copy(alpha = 0.55f * inhale), br * 0.95f, bulb)
    }
    drawArc(Color(0xFF4FA8C8).copy(alpha = 0.55f), 10f, 160f, false, bulb - Offset(br, br), Size(br * 2, br * 2))
    if (inhale > 0f) for (i in 0 until 7) {
        val q = (t * 2.2f + rnd(i, 14)) % 1f
        drawCircle(Color.White.copy(alpha = 0.7f * inhale * (1 - q)), b * 0.012f * (1 + rnd(i, 15)),
            Offset(bulb.x + (rnd(i, 16) - 0.5f) * br, bulb.y + br * 0.75f - q * br * 0.8f))
    }
    // The mouthpiece flare, the rim lines and a shine down one side.
    val flare = Rect(tube.left - tubeW * 0.12f, tube.top, tube.right + tubeW * 0.12f, tube.top + b * 0.05f)
    drawRect(glass.copy(alpha = 0.3f), flare.topLeft, flare.size)
    drawRect(glass, flare.topLeft, flare.size, style = Stroke(line))
    drawRect(glass, tube.topLeft, tube.size, style = Stroke(line))
    drawCircle(glass, br, bulb, style = Stroke(line))
    drawLine(Color.White.copy(alpha = 0.7f), Offset(tube.left + tubeW * 0.22f, tube.top + b * 0.07f), Offset(tube.left + tubeW * 0.22f, tube.bottom - b * 0.02f), line * 1.5f)
    drawArc(Color.White.copy(alpha = 0.6f), 200f, 60f, false, bulb - Offset(br * 0.75f, br * 0.75f), Size(br * 1.5f, br * 1.5f), style = Stroke(line * 1.5f))
    // The downstem and bowl, with the ember in it.
    val stem0 = Offset(bulb.x - br * 0.4f, bulb.y - br * 0.3f)
    val bowl = Offset(bulb.x - b * 0.3f, bulb.y - b * 0.25f)
    drawLine(glass.copy(alpha = 0.8f), stem0, bowl, b * 0.035f)
    drawPath(Path().apply { moveTo(bowl.x - b * 0.06f, bowl.y - b * 0.03f); lineTo(bowl.x + b * 0.06f, bowl.y - b * 0.03f); lineTo(bowl.x + b * 0.02f, bowl.y + b * 0.03f); lineTo(bowl.x - b * 0.02f, bowl.y + b * 0.03f); close() }, glass)
    return Offset(bowl.x, bowl.y - b * 0.035f)
}

/** A lighter that comes up under the ember, flicks a few sparks and holds a flame to it. */
private fun DrawScope.drawLighter(ember: Offset, t: Float, unit: Float) {
    val vis = ease((t - 0.1f) / 0.25f) * ease((1.2f - t) / 0.25f)
    if (vis <= 0f) return
    val fl = unit * (0.05f + 0.008f * sin(t * 37f) + 0.005f * sin(t * 59f))
    val bw = unit * 0.05f; val bh = unit * 0.09f
    val top = Offset(ember.x + unit * 0.012f, ember.y + fl * 0.85f + (1 - vis) * unit * 0.3f)
    val cap = unit * 0.02f
    drawRoundRect(Color(0xFFE0453A), Offset(top.x - bw / 2, top.y + cap), Size(bw, bh), CornerRadius(unit * 0.008f))
    drawRect(Color(0xFFFF7A6A), Offset(top.x - bw / 2 + bw * 0.12f, top.y + cap + bh * 0.08f), Size(bw * 0.14f, bh * 0.8f))
    drawRect(Color(0xFFB8BCC4), Offset(top.x - bw / 2, top.y), Size(bw, cap))
    drawCircle(Color(0xFF5A5E66), cap * 0.45f, Offset(top.x + bw * 0.3f, top.y + cap * 0.3f))
    // Sparks off the wheel, then the flame.
    if (t in 0.25f..0.5f) for (i in 0 until 8) {
        val q = (t - 0.25f) / 0.25f
        val a = -PI.toFloat() / 2 + (rnd(i, 17) - 0.5f) * 2.4f
        drawCircle(Color(0xFFFFE07A).copy(alpha = 1 - q), unit * 0.004f, top + Offset(cos(a), sin(a)) * unit * 0.07f * q)
    }
    if (t > 0.32f && t < 1.15f) {
        val fa = ease((t - 0.32f) / 0.08f) * vis
        drawOval(Color(0xFFFF8A1E).copy(alpha = 0.85f * fa), Offset(top.x - fl * 0.28f, top.y - fl), Size(fl * 0.56f, fl))
        drawOval(Color(0xFFFFE07A).copy(alpha = fa), Offset(top.x - fl * 0.16f, top.y - fl * 0.7f), Size(fl * 0.32f, fl * 0.66f))
        drawOval(Color(0xFF6AA8FF).copy(alpha = 0.8f * fa), Offset(top.x - fl * 0.1f, top.y - fl * 0.2f), Size(fl * 0.2f, fl * 0.2f))
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
