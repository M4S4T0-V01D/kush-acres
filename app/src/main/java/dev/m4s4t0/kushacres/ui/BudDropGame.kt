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
import dev.m4s4t0.kushacres.model.JarSkin
import dev.m4s4t0.kushacres.model.Result
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
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
    val daily = remember { BudDrop.challenge(Store.now()) }
    val dailyDone = remember { f.dropWon == daily.day }
    var beat by remember { mutableStateOf(false) }
    // The round waits on the start card, where the jar can be picked, until Start.
    var started by remember { mutableStateOf(false) }
    var skin by remember { mutableStateOf(f.jarSkin) }

    fun bank() {
        if (paid < 0) {
            paid = BudDrop.reward(f, game.score, Store.now())
            beat = BudDrop.beat(f, game, Store.now())
        }
    }
    fun finish() {
        if (!ui.drop) return
        if (!started) { ui.drop = false; return }
        bank()
        ui.drop = false
        Store.toast(when {
            game.score > best && game.score > 0 -> "New Bud Drop best: ${game.score}!" + if (paid > 0) " +${money(paid)}" else ""
            paid > 0 -> "Bud Drop: ${game.score} points, +${money(paid)}"
            else -> "Bud Drop: ${game.score} points"
        } + if (beat) " · Daily beaten! +${BudDrop.DAILY_BUCKS} Pot Bucks" else "")
        Store.changed()
    }
    LaunchedEffect(Unit) {
        var last = 0L
        while (ui.drop) withFrameNanos { n ->
            if (last != 0L) {
                // Capped, so a stall doesn't drop everything through the jar at once.
                val dt = ((n - last) / 1e9f).coerceAtMost(0.05f)
                clock += dt
                if (!started) Unit
                else if (!game.done) {
                    val blocked0 = game.blocked
                    for (k in game.step(dt)) {
                        // A bug the shield stopped doesn't hurt or break the combo.
                        val saved = k == BudDrop.Kind.BUG && game.blocked > blocked0
                        hitAt = clock; hitBug = if (k == BudDrop.Kind.BUG && !saved) 1 else 0
                        when {
                            k == BudDrop.Kind.BUG -> if (!saved) combo = 0
                            k.power -> Unit
                            else -> { pile += k; combo++; bestCombo = maxOf(bestCombo, combo) }
                        }
                        pops += Pop(game.jar, clock, k, saved)
                        when (k) {
                            BudDrop.Kind.BUD -> Audio.vary(Sfx.TAP)
                            BudDrop.Kind.GOLD -> Audio.play(Sfx.GOLDEN)
                            BudDrop.Kind.BUG -> Audio.play(if (saved) Sfx.SHOO else Sfx.ERROR)
                            else -> Audio.play(Sfx.UNLOCK)
                        }
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
            T(if (dailyDone) "Daily ✓ ${daily.text}" else "Daily: ${daily.text} (${daily.progress(game).coerceAtMost(daily.target)}/${daily.target})",
                12.sp, if (dailyDone || daily.met(game)) Leaf else Sky, align = TextAlign.Center)
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
                                    if (started && !game.done) game.jar = c.position.x / size.width
                                    if (c.pressed) c.consume()
                                }
                            }
                        }
                    },
                ) { drawField(game, clock, hitAt, hitBug == 1, pile, pops, if (combo >= 3) combo else 0, tm, skin) }
                if (!started) StartCard(daily.text, dailyDone, skin, { skin = it }) { started = true }
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
                        if (game.powers > 0 || game.blocked > 0) T("Power-ups ${game.powers}" + if (game.blocked > 0) " · bugs blocked ${game.blocked}" else "", 12.sp, Sky, align = TextAlign.Center, bold = false)
                        if (beat) T("Daily beaten! +${BudDrop.DAILY_BUCKS} Pot Bucks", 13.sp, Leaf, align = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        T(if (paid > 0) "+${money(paid)}" else if (left > 0) "No pay for zero" else "Just for fun today", 14.sp, if (paid > 0) Gold else Dim, align = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        T("Tap to close", 10.sp, Dim, align = TextAlign.Center, bold = false)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (game.done || !started) {
                Spacer(Modifier.height(48.dp))
            } else Btn("Stop", Modifier.fillMaxWidth().height(48.dp), color = WoodEdge, size = 16.sp) { finish() }
        }
    }
}

/** A still of the field for screenshots: [seconds] of a seeded round played by a jar that chases the nearest bud. */
@Composable
internal fun BudDropStill(seed: Int, seconds: Float, after: Float = 0.15f, skin: JarSkin = JarSkin.MASON) {
    val game = BudDrop(Random(seed))
    val pile = ArrayList<BudDrop.Kind>(); val pops = ArrayList<Pop>()
    var hitAt = -1f; var bug = false; var combo = 0
    while (game.clock < seconds && !game.done) {
        game.items.filter { it.kind != BudDrop.Kind.BUG }.maxByOrNull { it.y }?.let { game.jar += (it.x - game.jar).coerceIn(-0.02f, 0.02f) }
        for (k in game.step(1 / 60f)) {
            hitAt = game.clock; bug = k == BudDrop.Kind.BUG
            if (bug) combo = 0 else if (!k.power) { pile += k; combo++ }
            pops += Pop(game.jar, game.clock, k)
        }
    }
    pops.removeAll { game.clock - it.at > 1.0f }
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) { drawField(game, game.clock + after, hitAt, bug, pile, pops, if (combo >= 3) combo else 0, tm, skin) }
}

/** Before a round: today's challenge, the jar to play with (owned, unlockable or for sale) and Start. */
@Composable
private fun StartCard(daily: String, done: Boolean, skin: JarSkin, pick: (JarSkin) -> Unit, start: () -> Unit) {
    val f = Store.farm
    Store.rev
    Column(Modifier.padding(16.dp).woodBox().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        T("Today's challenge", 13.sp, Dim, align = TextAlign.Center)
        T(daily, 16.sp, if (done) Leaf else Gold, align = TextAlign.Center)
        T(if (done) "Already beaten today" else "Beat it for +${BudDrop.DAILY_BUCKS} Pot Bucks", 12.sp, if (done) Dim else Cream, align = TextAlign.Center, bold = false)
        Spacer(Modifier.height(10.dp))
        T("Jar", 13.sp, Dim, align = TextAlign.Center)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (j in JarSkin.entries) {
                val own = JarSkin.owns(f, j)
                Column(
                    Modifier.width(74.dp).frame(if (j == skin) WoodEdge else WoodIn, if (j == skin) Gold else WoodEdge).clickable {
                        if (own) { JarSkin.use(f, j); pick(j); Store.changed() }
                        else if (j.bucks > 0) report(JarSkin.buy(f, j).also { if (it == Result.OK) { pick(j); Store.changed() } })
                        else Store.toast("Score ${j.need} in one round to unlock")
                    }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Canvas(Modifier.size(30.dp)) { jarIcon(j, own) }
                    T(j.title, 10.sp, if (own) Cream else Dim, align = TextAlign.Center, maxLines = 2)
                    T(when { own -> if (j == skin) "In use" else "Use"; j.bucks > 0 -> "${j.bucks} PB"; else -> "Best ${j.need}" },
                        10.sp, if (own) Leaf else if (j.bucks > 0) Gold else Dim, align = TextAlign.Center, bold = false)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        T("Power-ups: magnet, slow-mo, wide mouth, shield", 11.sp, Sky, align = TextAlign.Center, bold = false)
        Spacer(Modifier.height(8.dp))
        Btn("Start", Modifier.fillMaxWidth().height(46.dp), color = Leaf.copy(alpha = 0.5f), size = 16.sp) { start() }
    }
}

private fun DrawScope.jarIcon(j: JarSkin, own: Boolean) {
    val w = size.width; val h = size.height
    val a = if (own) 1f else 0.4f
    val glass = Color(j.glass)
    drawRoundRect(glass.copy(alpha = 0.3f * a), Offset(w * 0.18f, h * 0.3f), Size(w * 0.64f, h * 0.66f), CornerRadius(w * 0.14f))
    drawRoundRect(glass.copy(alpha = 0.9f * a), Offset(w * 0.18f, h * 0.3f), Size(w * 0.64f, h * 0.66f), CornerRadius(w * 0.14f), style = Stroke(w * 0.06f))
    drawRect(Color(j.neck).copy(alpha = a), Offset(w * 0.26f, h * 0.12f), Size(w * 0.48f, h * 0.18f))
    if (j == JarSkin.GALAXY) for (i in 0 until 5) drawCircle(Color.White.copy(alpha = a), w * 0.03f, Offset(w * (0.3f + 0.4f * hash(i, 3)), h * (0.4f + 0.5f * hash(i, 4))))
}

/** How each power-up looks: its bubble colour and letter. */
private fun powerLook(k: BudDrop.Kind): Pair<Color, String> = when (k) {
    BudDrop.Kind.MAGNET -> Color(0xFFFF5A7A) to "M"
    BudDrop.Kind.SLOW -> Color(0xFF6AC8FF) to "S"
    BudDrop.Kind.WIDE -> Color(0xFFFFA040) to "W"
    else -> Color(0xFF8CF0C0) to "+"
}

/** A catch, for its "+1" and sparkles: where across the jar was then, when, and what it was. */
private class Pop(val x: Float, val at: Float, val kind: BudDrop.Kind, val saved: Boolean = false)

private fun hash(i: Int, k: Int): Float = (((i * 73856093) xor (k * 19349663)) and 0xFFFF) / 65535f

private fun DrawScope.drawField(game: BudDrop, now: Float, hitAt: Float, bug: Boolean, pile: List<BudDrop.Kind>, pops: List<Pop>, combo: Int, tm: TextMeasurer, skin: JarSkin = JarSkin.MASON) {
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
        if (i.kind.power) {
            // A power-up: a glowing bubble with its letter, bobbing as it falls.
            val (col, ch) = powerLook(i.kind)
            val c = Offset(i.x * w + sin(now * 3 + i.spin * 7) * px * 0.8f, i.y * fh)
            val r = px * 5.5f * (1 + 0.08f * sin(now * 8 + n))
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.55f), Color.Transparent), c, r * 2.2f), r * 2.2f, c)
            drawCircle(col.copy(alpha = 0.35f), r, c)
            drawCircle(col, r, c, style = Stroke(px * 0.7f))
            drawCircle(Color.White.copy(alpha = 0.5f), r * 0.25f, c + Offset(-r * 0.4f, -r * 0.4f))
            val l = tm.measure(ch, TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White))
            drawText(l, topLeft = c - Offset(l.size.width / 2f, l.size.height / 2f))
            continue
        }
        val b = when (i.kind) { BudDrop.Kind.BUD -> Sprites.nug; BudDrop.Kind.GOLD -> Sprites.goldNug; else -> Sprites.bug }
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
    val mw = game.half() * w
    val mouth = BudDrop.RIM * fh
    val neck = mouth + px * 2.6f
    val bw = mw * 1.18f
    val body = Rect(cx - bw, neck, cx + bw, counter)
    val glass = Color(skin.glass)
    val metal = Color(skin.neck)
    drawRoundRect(Color(0x33000000), Offset(body.left + px, counter - px * 0.6f), Size(body.width, px * 1.4f), CornerRadius(px))
    drawRoundRect(glass.copy(alpha = if (skin == JarSkin.MASON) 0.1f else 0.22f), body.topLeft, body.size, CornerRadius(mw * 0.35f))
    if (skin == JarSkin.GALAXY) for (i in 0 until 14) {
        val tw = 0.5f + 0.5f * sin(now * 2 + i * 1.3f)
        drawCircle(Color.White.copy(alpha = 0.3f + 0.6f * tw), px * 0.35f, Offset(body.left + body.width * hash(i, 21), body.top + body.height * hash(i, 22)))
    }
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
    drawRect(metal, Offset(cx - mw, mouth), Size(mw * 2, neck - mouth))
    for (l in 1..2) drawRect(lerpColor(metal, Color.Black, 0.25f), Offset(cx - mw, mouth + (neck - mouth) * l / 3f), Size(mw * 2, px * 0.4f))
    drawRect(lerpColor(metal, Color.White, 0.5f), Offset(cx - mw, mouth), Size(mw * 2, px * 0.5f))
    // A shield: a shimmering bubble over the mouth that pops on the next bug.
    if (game.shield) {
        val r = mw * 1.5f
        drawArc(Color(0xFF8CF0C0).copy(alpha = 0.5f + 0.3f * sin(now * 6)), 180f, 180f, false, Offset(cx - r, mouth - r * 0.8f), Size(r * 2, r * 1.6f), style = Stroke(px * 0.8f))
    }
    // The magnet's pull: faint lines reaching up from the jar.
    if (game.magnet > 0) for (i in 0 until 3) {
        val q = (now * 1.5f + i / 3f) % 1f
        drawArc(Color(0xFFFF5A7A).copy(alpha = 0.5f * (1 - q)), 200f, 140f, false, Offset(cx - mw * (1 + q * 2), mouth - mw * (0.6f + q * 2.4f)), Size(mw * (2 + q * 4), mw * (1.2f + q * 3)), style = Stroke(px * 0.5f))
    }
    // A glow off the mouth when something good lands.
    if (hit && !bug) drawCircle(Brush.radialGradient(listOf(Color(0x88FFE07A), Color.Transparent), Offset(cx, mouth), mw * 1.6f), mw * 1.6f, Offset(cx, mouth))
    // A red throb round the edges when a bug gets in.
    if (hit && bug) drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xFFE05A4A).copy(alpha = 0.45f * (1 - since / 0.45f))), Offset(w / 2, h / 2), maxOf(w, h) * 0.75f))

    // Sparkles and "+1"s rising off each catch.
    for ((n, p) in pops.withIndex()) {
        val q = ((now - p.at) / 1.0f).coerceIn(0f, 1f)
        val at = Offset(p.x * w, mouth)
        val col = when (p.kind) {
            BudDrop.Kind.GOLD -> Color(0xFFFFD84A); BudDrop.Kind.BUG -> if (p.saved) Color(0xFF8CF0C0) else Color(0xFFFF6A5A)
            BudDrop.Kind.BUD -> Color(0xFFB6F07A); else -> powerLook(p.kind).first
        }
        if (p.kind != BudDrop.Kind.BUG) for (i in 0 until (if (p.kind == BudDrop.Kind.GOLD) 12 else 7)) {
            val a = -PI.toFloat() * (0.1f + 0.8f * hash(n * 31 + i, 12))
            val d = mw * (0.4f + 1.2f * hash(i, n + 13)) * ease(q * 1.6f)
            drawStar(at + Offset(cos(a) * d, sin(a) * d - q * px * 4), px * 1.4f * (1 - q), col.copy(alpha = 1 - q))
        }
        val label = when (p.kind) {
            BudDrop.Kind.BUD -> "+1"; BudDrop.Kind.GOLD -> "+${BudDrop.GOLD_PTS}"; BudDrop.Kind.BUG -> if (p.saved) "Blocked!" else "-♥"
            BudDrop.Kind.MAGNET -> "Magnet!"; BudDrop.Kind.SLOW -> "Slow-mo!"; BudDrop.Kind.WIDE -> "Wide jar!"; BudDrop.Kind.SHIELD -> "Shield!"
        }
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
    // Timed power-ups still running, as shrinking bars down the right.
    var row = 0
    for ((k, t) in listOf(BudDrop.Kind.MAGNET to game.magnet, BudDrop.Kind.SLOW to game.slow, BudDrop.Kind.WIDE to game.wide)) {
        if (t <= 0) continue
        val (col, ch) = powerLook(k)
        val y = 10f + row++ * px * 7f
        val bw = w * 0.22f
        drawRoundRect(Color(0x88000000), Offset(w - bw - 10f, y), Size(bw, px * 5f), CornerRadius(px))
        drawRoundRect(col, Offset(w - bw - 10f, y), Size(bw * t / BudDrop.POWER_SECS, px * 5f), CornerRadius(px))
        val l = tm.measure(ch, TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White))
        drawText(l, topLeft = Offset(w - bw - 4f, y + (px * 5f - l.size.height) / 2))
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

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)
