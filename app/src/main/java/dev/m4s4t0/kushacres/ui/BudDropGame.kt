package dev.m4s4t0.kushacres.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.model.BudDrop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Seconds the end of a round stays on screen before the overlay closes. */
private const val OUTRO = 3.5f

/**
 * Bud Drop over the farm while [UiState.drop] is set: drag (or move the mouse) across the field to slide the jar under the
 * falling buds and away from the bugs. Ends when the lives run out or the player stops, and banks the round with [BudDrop.reward].
 */
@Composable
fun BudDropOverlay(ui: UiState) {
    if (!ui.drop) return
    val f = Store.farm
    val game = remember { BudDrop(Random) }
    var clock by remember { mutableFloatStateOf(0f) }
    var endAt by remember { mutableFloatStateOf(-1f) }
    // The last catch, for the jar's flash: when, and whether it was a bug.
    var hitAt by remember { mutableFloatStateOf(-1f) }
    var hitBug by remember { mutableIntStateOf(0) }
    var paid by remember { mutableLongStateOf(-1L) }
    // What's landed in the jar (for the pile), the "+1" popups, and the run of catches without a bug.
    val pile = remember { ArrayList<BudDrop.Kind>() }
    val pops = remember { ArrayList<Pop>() }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    val tm = rememberTextMeasurer()
    val best = remember { f.dropBest }
    val left = remember { BudDrop.left(f, Store.now()) }

    fun bank() {
        if (paid < 0) paid = BudDrop.reward(f, game.score, Store.now())
    }
    fun finish() {
        if (!ui.drop) return
        bank()
        ui.drop = false
        Store.toast(when {
            game.score > best && game.score > 0 -> "New Bud Drop best: ${game.score}!" + if (paid > 0) " +${money(paid)}" else ""
            paid > 0 -> "Bud Drop: ${game.score} points, +${money(paid)}"
            else -> "Bud Drop: ${game.score} points"
        })
        Store.changed()
    }
    LaunchedEffect(Unit) {
        var last = 0L
        while (ui.drop) withFrameNanos { n ->
            if (last != 0L) {
                // Capped, so a stall doesn't drop everything through the jar at once.
                val dt = ((n - last) / 1e9f).coerceAtMost(0.05f)
                clock += dt
                if (!game.done) for (k in game.step(dt)) {
                    hitAt = clock; hitBug = if (k == BudDrop.Kind.BUG) 1 else 0
                    if (k == BudDrop.Kind.BUG) combo = 0 else { pile += k; combo++; bestCombo = maxOf(bestCombo, combo) }
                    pops += Pop(game.jar, clock, k)
                    when (k) {
                        BudDrop.Kind.BUD -> Audio.vary(Sfx.TAP)
                        BudDrop.Kind.GOLD -> Audio.play(Sfx.GOLDEN)
                        BudDrop.Kind.BUG -> Audio.play(Sfx.ERROR)
                    }
                }
                else if (endAt < 0) { endAt = clock; bank() }
                pops.removeAll { clock - it.at > 1.2f }
                if (endAt >= 0 && clock - endAt > OUTRO) finish()
            }
            last = n
        }
    }
    BackHandler { finish() }

    Box(
        Modifier.fillMaxSize().background(Color(0xB0000000)).clickable(remember { MutableInteractionSource() }, null) {}
            .statusBarsPadding().navigationBarsPadding().padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 440.dp).fillMaxWidth().heightIn(max = 720.dp).fillMaxHeight().woodBox().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            T("Bud Drop", 20.sp, Gold, align = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            T("Drag the jar under the buds. Golden ones are worth ${BudDrop.GOLD_PTS}. Dodge the bugs!", 14.sp, Cream, align = TextAlign.Center, bold = false)
            T(if (left > 0) "Paid rounds left today: $left of ${BudDrop.PAID}" else "Just for fun now · pays again tomorrow", 13.sp, if (left > 0) Leaf else Dim, align = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                T("Score ${game.score}", 16.sp, Gold, Modifier.weight(1f))
                T(if (combo >= 3) "Combo ×$combo" else "Best ${maxOf(best, game.score)}", 13.sp, if (combo >= 3) Leaf else Dim, Modifier.weight(1f), align = TextAlign.Center)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    repeat(BudDrop.LIVES) { i -> T("♥", 18.sp, if (i < game.lives) Bad else Dim.copy(alpha = 0.4f)) }
                }
            }
            Spacer(Modifier.height(6.dp))
            clock // redraw every frame
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier.fillMaxSize().background(Color(0xFF0B1230)).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent()
                                e.changes.firstOrNull()?.let { c ->
                                    if (!game.done) game.jar = c.position.x / size.width
                                    if (c.pressed) c.consume()
                                }
                            }
                        }
                    },
                ) { drawField(game, clock, hitAt, hitBug == 1, pile, pops, if (combo >= 3) combo else 0, tm) }
                // The end of the round: a card over the field with how it went.
                if (game.done) {
                    val since = if (endAt >= 0) clock - endAt else 0f
                    Column(
                        Modifier.padding(24.dp).woodBox().clickable { finish() }.padding(horizontal = 22.dp, vertical = 14.dp)
                            .graphicsLayer { val k = (since / 0.3f).coerceIn(0f, 1f); scaleX = 0.7f + 0.3f * k; scaleY = scaleX; alpha = k },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val newBest = game.score > best && game.score > 0
                        T(if (newBest) "New best!" else "Out of lives", 18.sp, if (newBest) Gold else Cream, align = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        T("${game.score}", 34.sp, Gold, align = TextAlign.Center)
                        T("points · ${game.caught} buds", 12.sp, Cream, align = TextAlign.Center, bold = false)
                        if (bestCombo >= 3) T("Longest combo ×$bestCombo", 12.sp, Leaf, align = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        T(if (paid > 0) "+${money(paid)}" else if (left > 0) "No pay for zero" else "Just for fun today", 14.sp, if (paid > 0) Gold else Dim, align = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        T("Tap to close", 10.sp, Dim, align = TextAlign.Center, bold = false)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (game.done) {
                Spacer(Modifier.height(48.dp))
            } else Btn("Stop", Modifier.fillMaxWidth().height(48.dp), color = WoodEdge, size = 16.sp) { finish() }
        }
    }
}

/** A still of the field for screenshots: [seconds] of a seeded round played by a jar that chases the nearest bud. */
@Composable
internal fun BudDropStill(seed: Int, seconds: Float, after: Float = 0.15f) {
    val game = BudDrop(Random(seed))
    val pile = ArrayList<BudDrop.Kind>(); val pops = ArrayList<Pop>()
    var hitAt = -1f; var bug = false; var combo = 0
    while (game.clock < seconds && !game.done) {
        game.items.filter { it.kind != BudDrop.Kind.BUG }.maxByOrNull { it.y }?.let { game.jar += (it.x - game.jar).coerceIn(-0.02f, 0.02f) }
        for (k in game.step(1 / 60f)) {
            hitAt = game.clock; bug = k == BudDrop.Kind.BUG
            if (bug) combo = 0 else { pile += k; combo++ }
            pops += Pop(game.jar, game.clock, k)
        }
    }
    pops.removeAll { game.clock - it.at > 1.0f }
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) { drawField(game, game.clock + after, hitAt, bug, pile, pops, if (combo >= 3) combo else 0, tm) }
}

/** A catch, for its "+1" and sparkles: where across the jar was then, when, and what it was. */
private class Pop(val x: Float, val at: Float, val kind: BudDrop.Kind)

private fun hash(i: Int, k: Int): Float = (((i * 73856093) xor (k * 19349663)) and 0xFFFF) / 65535f

private fun DrawScope.drawField(game: BudDrop, now: Float, hitAt: Float, bug: Boolean, pile: List<BudDrop.Kind>, pops: List<Pop>, combo: Int, tm: TextMeasurer) {
    val w = size.width; val h = size.height
    val unit = minOf(w, h)
    // The game's field ends a little above the bottom, so the jar has room to stand on the counter below its rim.
    val fh = h * 0.9f
    val counter = h * 0.955f
    // Night sky, moon and stars.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1230), Color(0xFF1D2352), Color(0xFF32264A)), 0f, fh * 0.62f))
    for (i in 0 until 34) {
        val tw = 0.5f + 0.5f * sin(now * (1.5f + hash(i, 1) * 2) + i * 1.7f)
        val sz = if (i % 7 == 0) 3f else 2f
        drawRect(Color.White.copy(alpha = 0.15f + 0.6f * tw * hash(i, 2)), Offset(w * hash(i, 3), fh * 0.55f * hash(i, 4)), Size(sz, sz))
    }
    val moon = Offset(w * 0.8f, fh * 0.13f); val mr = unit * 0.065f
    drawCircle(Brush.radialGradient(listOf(Color(0x40FFF0B8), Color.Transparent), moon, mr * 3.5f), mr * 3.5f, moon)
    drawCircle(Color(0xFFF6EDC6), mr, moon)
    drawCircle(Color(0xFFE2D7A8), mr * 0.24f, moon + Offset(-mr * 0.3f, -mr * 0.2f))
    drawCircle(Color(0xFFE2D7A8), mr * 0.15f, moon + Offset(mr * 0.35f, mr * 0.3f))
    drawCircle(Color(0xFFE2D7A8), mr * 0.1f, moon + Offset(-mr * 0.1f, mr * 0.5f))
    // The grow room wall below: shelves of plants under purple lights.
    val wallTop = fh * 0.6f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1E2E), Color(0xFF1A1418)), wallTop, counter), Offset(0f, wallTop), Size(w, counter - wallTop))
    drawRect(Color(0xFF3E2C2A), Offset(0f, wallTop - 4f), Size(w, 6f))
    for (row in 0 until 2) {
        val shelf = wallTop + (counter - wallTop) * (0.42f + 0.46f * row)
        val lamp = shelf - (counter - wallTop) * 0.36f
        val glow = 0.18f + 0.05f * sin(now * 1.3f + row)
        drawRect(Brush.verticalGradient(listOf(Color(0xFFD060FF).copy(alpha = glow), Color.Transparent), lamp, shelf), Offset(0f, lamp), Size(w, shelf - lamp))
        drawRect(Color(0xFFE6A0FF), Offset(0f, lamp - 2f), Size(w, 3f))
        val n = 7
        for (j in 0 until n) {
            val px = w * (j + 0.5f + (if (row == 1) 0.3f else 0f)) / n
            val ph = (shelf - lamp) * (0.55f + 0.25f * hash(j, row + 5))
            val pw = w / n * 0.32f
            val sway = sin(now * 0.8f + j + row) * pw * 0.08f
            for (l in 0 until 5) {
                val a = (-90f + (l - 2) * 28f) * PI.toFloat() / 180f
                val len = ph * (if (l == 2) 0.95f else 0.7f)
                drawLine(Color(0xFF2E6A30), Offset(px, shelf - pw * 0.5f), Offset(px + cos(a) * len * 0.5f + sway, shelf - pw * 0.5f + sin(a) * len), pw * 0.32f)
            }
            drawRect(Color(0xFF7A4A2A), Offset(px - pw / 2, shelf - pw * 0.55f), Size(pw, pw * 0.55f))
        }
        drawRect(Color(0xFF5A3E28), Offset(0f, shelf), Size(w, 5f))
    }
    // The wood counter the jar stands on.
    drawRect(Color(0xFF7A5232), Offset(0f, counter), Size(w, h - counter))
    drawRect(Color(0xFFA8784A), Offset(0f, counter), Size(w, 3f))
    for (i in 0 until 5) drawRect(Color(0xFF5E3E24), Offset(w * hash(i, 9), counter + (h - counter) * (0.35f + 0.4f * hash(i, 10))), Size(w * 0.18f, 1.5f))

    val px = (w * BudDrop.JAR * 2 / 12f).coerceAtMost(h / 14f)
    fun art(b: Bitmap, cx: Float, cy: Float, scale: Float, turn: Float = 0f, alpha: Float = 1f) {
        val s = (12 * scale).toInt()
        rotate(turn, Offset(cx, cy)) {
            drawImage(b.img(), IntOffset.Zero, IntSize(b.width, b.height), IntOffset((cx - s / 2f).toInt(), (cy - s / 2f).toInt()),
                IntSize(s, s), alpha = alpha, filterQuality = FilterQuality.None)
        }
    }
    // What's falling, with a fading trail behind it; golden ones glint.
    for ((n, i) in game.items.withIndex()) {
        val b = when (i.kind) { BudDrop.Kind.BUD -> Sprites.nug; BudDrop.Kind.GOLD -> Sprites.goldNug; BudDrop.Kind.BUG -> Sprites.bug }
        val turn = if (i.kind == BudDrop.Kind.BUG) sin(now * 9 + i.spin * 6) * 12 else (now * 60 + i.spin * 360) % 360
        val x = i.x * w + (if (i.kind == BudDrop.Kind.BUG) sin(now * 5 + i.spin * 9) * px * 1.2f else 0f)
        val y = i.y * fh
        val tail = if (i.kind == BudDrop.Kind.GOLD) Color(0xFFFFD84A) else if (i.kind == BudDrop.Kind.BUG) Color(0xFFE05A4A) else Color(0xFF8CE06A)
        for (k in 1..4) drawCircle(tail.copy(alpha = 0.16f * (5 - k) / 4), px * (2.4f - k * 0.35f), Offset(x, y - k * game.speed() * fh * 0.035f))
        if (i.kind == BudDrop.Kind.GOLD) {
            drawCircle(Brush.radialGradient(listOf(Color(0x66FFE07A), Color.Transparent), Offset(x, y), px * 6), px * 6, Offset(x, y))
            val tw = (now * 3 + n) % 1f
            drawStar(Offset(x + px * 3.5f, y - px * 3f), px * 2.2f * (1 - tw), Color.White.copy(alpha = 1 - tw))
        }
        art(b, x, y, px * 0.8f, turn)
    }

    // The jar: a big glass jar on the counter, its mouth on the rim line, the catch piling up inside.
    val since = now - hitAt
    val hit = hitAt >= 0 && since < 0.45f
    val shake = if (hit && bug) sin(since * 70) * px * 1.6f * (1 - since / 0.45f) else 0f
    val cx = game.jar * w + shake
    val mw = BudDrop.JAR * w
    val mouth = BudDrop.RIM * fh
    val neck = mouth + px * 2.6f
    val bw = mw * 1.18f
    val body = Rect(cx - bw, neck, cx + bw, counter)
    val glass = Color(0xFFCFEFFF)
    drawRoundRect(Color(0x33000000), Offset(body.left + px, counter - px * 0.6f), Size(body.width, px * 1.4f), CornerRadius(px))
    drawRoundRect(glass.copy(alpha = 0.1f), body.topLeft, body.size, CornerRadius(mw * 0.35f))
    // The pile, row by row from the bottom.
    val bud = px * 0.62f * 12
    val perRow = maxOf(1, ((body.width - px * 2) / (bud * 0.72f)).toInt())
    val rows = maxOf(1, ((body.height - px * 2) / (bud * 0.55f)).toInt())
    for ((n, k) in pile.take(perRow * rows).withIndex()) {
        val r = n / perRow; val c = n % perRow
        val x = body.left + px + bud * 0.5f + c * (body.width - px * 2 - bud) / maxOf(1, perRow - 1) + (if (r % 2 == 1) bud * 0.2f else 0f)
        val y = body.bottom - px * 1.2f - bud * 0.45f - r * bud * 0.55f
        art(if (k == BudDrop.Kind.GOLD) Sprites.goldNug else Sprites.nug, x.coerceAtMost(body.right - bud * 0.5f), y, px * 0.62f, hash(n, 11) * 80 - 40)
    }
    drawRoundRect(glass.copy(alpha = 0.08f), body.topLeft, body.size, CornerRadius(mw * 0.35f))
    if (hit) drawRoundRect((if (bug) Color(0xFFE05A4A) else Color(0xFFFFD84A)).copy(alpha = 0.4f * (1 - since / 0.45f)), body.topLeft, body.size, CornerRadius(mw * 0.35f))
    drawRoundRect(glass.copy(alpha = 0.75f), body.topLeft, body.size, CornerRadius(mw * 0.35f), style = Stroke(px * 0.6f))
    // Shine down the left and a glint up top.
    drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(body.left + bw * 0.16f, body.top + px * 2), Size(px * 1.1f, body.height * 0.62f), CornerRadius(px))
    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(body.left + bw * 0.34f, body.top + px * 2), Size(px * 0.5f, body.height * 0.3f), CornerRadius(px))
    // The threaded neck.
    drawRect(Color(0xFFB8C4CC), Offset(cx - mw, mouth), Size(mw * 2, neck - mouth))
    for (l in 1..2) drawRect(Color(0xFF8A96A0), Offset(cx - mw, mouth + (neck - mouth) * l / 3f), Size(mw * 2, px * 0.4f))
    drawRect(Color(0xFFE6EEF2), Offset(cx - mw, mouth), Size(mw * 2, px * 0.5f))
    // A glow off the mouth when something good lands.
    if (hit && !bug) drawCircle(Brush.radialGradient(listOf(Color(0x88FFE07A), Color.Transparent), Offset(cx, mouth), mw * 1.6f), mw * 1.6f, Offset(cx, mouth))
    // A red throb round the edges when a bug gets in.
    if (hit && bug) drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFFE05A4A).copy(alpha = 0.45f * (1 - since / 0.45f))), Offset(w / 2, h / 2), maxOf(w, h) * 0.75f))

    // Sparkles and "+1"s rising off each catch.
    for ((n, p) in pops.withIndex()) {
        val q = ((now - p.at) / 1.0f).coerceIn(0f, 1f)
        val at = Offset(p.x * w, mouth)
        val col = when (p.kind) { BudDrop.Kind.GOLD -> Color(0xFFFFD84A); BudDrop.Kind.BUG -> Color(0xFFFF6A5A); BudDrop.Kind.BUD -> Color(0xFFB6F07A) }
        if (p.kind != BudDrop.Kind.BUG) for (i in 0 until (if (p.kind == BudDrop.Kind.GOLD) 12 else 7)) {
            val a = -PI.toFloat() * (0.1f + 0.8f * hash(n * 31 + i, 12))
            val d = mw * (0.4f + 1.2f * hash(i, n + 13)) * ease(q * 1.6f)
            drawStar(at + Offset(cos(a) * d, sin(a) * d - q * px * 4), px * 1.4f * (1 - q), col.copy(alpha = 1 - q))
        }
        val label = when (p.kind) { BudDrop.Kind.BUD -> "+1"; BudDrop.Kind.GOLD -> "+${BudDrop.GOLD_PTS}"; BudDrop.Kind.BUG -> "-♥" }
        val style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (if (p.kind == BudDrop.Kind.GOLD) 22 else 17).sp, color = col.copy(alpha = 1 - q * q))
        val l = tm.measure(label, style)
        val o = Offset(at.x - l.size.width / 2f, at.y - px * 4 - q * fh * 0.14f - l.size.height)
        drawText(tm.measure(label, style.copy(color = Color.Black.copy(alpha = 0.6f * (1 - q * q)))), topLeft = o + Offset(2f, 2f))
        drawText(l, topLeft = o)
    }
    // The combo, big in the corner and bumping with each catch.
    if (combo > 0) {
        val bump = if (hitAt >= 0 && since < 0.2f && !bug) 1 + 0.25f * (1 - since / 0.2f) else 1f
        val style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = (18 * bump).sp,
            color = if (combo >= 10) Color(0xFFFFD84A) else Color(0xFFB6F07A))
        drawText(tm.measure("×$combo", style.copy(color = Color.Black.copy(alpha = 0.6f))), topLeft = Offset(12f, 10f))
        drawText(tm.measure("×$combo", style), topLeft = Offset(10f, 8f))
    }
    if (game.done) drawRect(Color(0x99000000), size = size)
}

private fun ease(x: Float) = x.coerceIn(0f, 1f).let { 1 - (1 - it) * (1 - it) }

/** A four-point twinkle [r] across from the middle. */
private fun DrawScope.drawStar(c: Offset, r: Float, col: Color) {
    if (r <= 0.5f) return
    val p = Path().apply {
        moveTo(c.x, c.y - r); lineTo(c.x + r * 0.25f, c.y - r * 0.25f); lineTo(c.x + r, c.y); lineTo(c.x + r * 0.25f, c.y + r * 0.25f)
        lineTo(c.x, c.y + r); lineTo(c.x - r * 0.25f, c.y + r * 0.25f); lineTo(c.x - r, c.y); lineTo(c.x - r * 0.25f, c.y - r * 0.25f); close()
    }
    drawPath(p, col)
}
