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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.sin
import kotlin.random.Random

/** Seconds the end of a round stays on screen before the overlay closes. */
private const val OUTRO = 1.6f

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
                    when (k) {
                        BudDrop.Kind.BUD -> Audio.vary(Sfx.TAP)
                        BudDrop.Kind.GOLD -> Audio.play(Sfx.GOLDEN)
                        BudDrop.Kind.BUG -> Audio.play(Sfx.ERROR)
                    }
                }
                else if (endAt < 0) { endAt = clock; bank() }
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
                T("Best ${maxOf(best, game.score)}", 13.sp, Dim, Modifier.weight(1f), align = TextAlign.Center)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    repeat(BudDrop.LIVES) { i -> T("♥", 18.sp, if (i < game.lives) Bad else Dim.copy(alpha = 0.4f)) }
                }
            }
            Spacer(Modifier.height(6.dp))
            clock // redraw every frame
            Canvas(
                Modifier.weight(1f).fillMaxWidth().background(Color(0xFF15110D)).pointerInput(Unit) {
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
            ) { drawField(game, clock, hitAt, hitBug == 1) }
            Spacer(Modifier.height(8.dp))
            if (game.done) {
                T(when {
                    game.score > best && game.score > 0 -> "New best! ${game.score} points"
                    else -> "Out of lives: ${game.score} points"
                } + if (paid > 0) " · +${money(paid)}" else "", 18.sp, if (game.score > best) Gold else Cream, align = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
            } else Btn("Stop", Modifier.fillMaxWidth().height(48.dp), color = WoodEdge, size = 16.sp) { finish() }
        }
    }
}

private fun DrawScope.drawField(game: BudDrop, now: Float, hitAt: Float, bug: Boolean) {
    val w = size.width; val h = size.height
    // A night sky fading to a grassy strip under the jar.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF14203A), Color(0xFF243A2A)), 0f, h * BudDrop.RIM))
    drawRect(Color(0xFF2E4A22), Offset(0f, h * 0.95f), Size(w, h * 0.05f))
    for (i in 0 until 14) {
        val tw = 0.5f + 0.5f * sin(now * 2 + i * 1.7f)
        drawRect(Color.White.copy(alpha = 0.15f + 0.35f * tw), Offset(w * ((i * 0.381f) % 1f), h * 0.6f * ((i * 0.618f) % 1f)), Size(3f, 3f))
    }
    val px = (w * BudDrop.JAR * 2 / 12f).coerceAtMost(h / 14f)
    fun art(b: Bitmap, cx: Float, cy: Float, scale: Float, turn: Float = 0f, alpha: Float = 1f) {
        val s = (12 * scale).toInt()
        rotate(turn, Offset(cx, cy)) {
            drawImage(b.img(), IntOffset.Zero, IntSize(b.width, b.height), IntOffset((cx - s / 2f).toInt(), (cy - s / 2f).toInt()),
                IntSize(s, s), alpha = alpha, filterQuality = FilterQuality.None)
        }
    }
    for (i in game.items) {
        val b = when (i.kind) { BudDrop.Kind.BUD -> Sprites.nug; BudDrop.Kind.GOLD -> Sprites.goldNug; BudDrop.Kind.BUG -> Sprites.bug }
        val turn = if (i.kind == BudDrop.Kind.BUG) sin(now * 9 + i.spin * 6) * 12 else (now * 60 + i.spin * 360) % 360
        art(b, i.x * w, i.y * h, px * 0.8f, turn)
    }
    // The jar, its mouth on the rim line, flashing on a catch.
    val since = now - hitAt
    val flash = hitAt >= 0 && since < 0.25f
    val cx = game.jar * w; val top = h * BudDrop.RIM
    val s = 12 * px
    if (flash) drawRect((if (bug) Color(0xFFE05A4A) else Color(0xFFFFD84A)).copy(alpha = 0.35f * (1 - since / 0.25f)),
        Offset(cx - s / 2, top - px * 2), Size(s, s + px * 2))
    art(Sprites.jar, cx, top + s / 2 - px, px, if (flash && bug) sin(since * 60) * 8 else 0f)
    if (game.done) drawRect(Color(0x88000000), size = size)
}
