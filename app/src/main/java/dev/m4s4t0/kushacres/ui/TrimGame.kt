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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.TrimArt
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Pheno
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.Trim
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/** Seconds the end of a round stays on screen before the overlay closes. */
private const val OUTRO = 1.1f

/**
 * The trimming game, over the middle of the farm: a big pixel cola with sugar leaves poking out, and scissors sweeping up
 * and down it. Snip (or tap the bud) while the scissors line up with a leaf. Ends when every leaf is off, the misses run
 * out, or the player stops; the clean snips are banked on the plant at [UiState.trim].
 */
@Composable
fun TrimOverlay(ui: UiState) {
    val at = ui.trim ?: return
    val f = Store.farm
    val strain = f.tile(at.first, at.second).strain?.let { Strains[it] } ?: run { ui.trim = null; return }
    val colour = Pheno.colour(f.tile(at.first, at.second).pheno)
    val game = remember(at) { Trim(Random) }
    var sweeps by remember(at) { mutableFloatStateOf(0f) }
    var clock by remember(at) { mutableFloatStateOf(0f) }
    // When each leaf came off (for its fall), and the last snip's time and whether it missed.
    val cutAt = remember(at) { FloatArray(Farm.TRIM_MAX) { -1f } }
    var snipAt by remember(at) { mutableFloatStateOf(-1f) }
    var missed by remember(at) { mutableIntStateOf(0) }
    var endAt by remember(at) { mutableFloatStateOf(-1f) }

    fun finish(n: Int) {
        if (ui.trim == null) return
        ui.trim = null
        if (f.trim(at.first, at.second, n) == Result.OK)
            Store.toast(if (n > 0) "Trimmed: +${Math.round(Farm.TRIM_STEP * 100 * f.tile(at.first, at.second).trim)}% bud at harvest" else "No clean leaves that time")
        Store.changed()
    }
    LaunchedEffect(at) {
        var last = 0L
        while (ui.trim == at) withFrameNanos { n ->
            if (last != 0L) {
                val dt = (n - last) / 1e9f
                clock += dt
                if (!game.done) sweeps += dt * Trim.speed(game.clean)
                else if (endAt < 0) endAt = clock
                if (endAt >= 0 && clock - endAt > OUTRO) finish(game.clean)
            }
            last = n
        }
    }
    fun snip() {
        if (game.done) return
        val before = game.clean
        snipAt = clock
        if (game.snip(Trim.shears(sweeps))) {
            Audio.vary(Sfx.TAP)
            game.cut.indices.firstOrNull { game.cut[it] && cutAt[it] < 0 }?.let { cutAt[it] = clock }
        } else { Audio.play(Sfx.ERROR); missed++ }
        check(game.clean >= before)
    }
    BackHandler { finish(game.clean) }

    Box(
        Modifier.fillMaxSize().background(Color(0xB0000000)).clickable(remember { MutableInteractionSource() }, null) {}
            .statusBarsPadding().navigationBarsPadding().padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 440.dp).fillMaxWidth().heightIn(max = 720.dp).fillMaxHeight().woodBox().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            T("Trim the ${strain.name}", 20.sp, Gold, align = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            T("Snip when the scissors line up with a glowing sugar leaf.", 14.sp, Cream, align = TextAlign.Center, bold = false)
            T("Each clean leaf: +${Math.round(Farm.TRIM_STEP * 100)}% bud at harvest", 13.sp, Leaf, align = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            clock // redraw every frame
            Canvas(
                Modifier.weight(1f).fillMaxWidth().background(Color(0xFF15110D))
                    .clickable(remember { MutableInteractionSource() }, null) { snip() },
            ) { drawScene(strain, colour, game, Trim.shears(sweeps), clock, cutAt, snipAt, missed) }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T("Clean", 12.sp, Dim)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(game.leaves.size) { i ->
                            Box(Modifier.size(18.dp).frame(if (i < game.clean) Leaf else Color(0xFF231B14), if (i < game.clean) Cream else Dim))
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    T("Misses", 12.sp, Dim)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(Trim.MISSES) { i -> T("✕", 18.sp, if (i < game.misses) Bad else Dim.copy(alpha = 0.5f)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (game.done) {
                val all = game.clean == game.leaves.size
                T(if (all) "All clean! +${Math.round(Farm.TRIM_STEP * 100 * game.clean)}% bud" else if (game.clean > 0) "Out of misses: ${game.clean} clean" else "Out of misses",
                    18.sp, if (all) Gold else Cream, align = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Snip ✂", Modifier.weight(2f).height(56.dp), color = Leaf, size = 20.sp) { snip() }
                Btn("Stop", Modifier.weight(1f).height(56.dp), color = WoodEdge, size = 16.sp) { finish(game.clean) }
            }
        }
    }
}

private fun DrawScope.drawScene(s: dev.m4s4t0.kushacres.model.Strain, colour: Int, game: Trim, pos: Float, now: Float, cutAt: FloatArray, snipAt: Float, missed: Int) {
    val sc = min(size.width / TrimArt.W, size.height / TrimArt.H)
    val ox = (size.width - TrimArt.W * sc) / 2; val oy = (size.height - TrimArt.H * sc) / 2
    fun art(b: Bitmap, x: Float, y: Float, alpha: Float = 1f) = drawImage(
        b.img(), IntOffset.Zero, IntSize(b.width, b.height), IntOffset((ox + x * sc).toInt(), (oy + y * sc).toInt()),
        IntSize((b.width / 2f * sc).toInt(), (b.height / 2f * sc).toInt()), alpha = alpha, filterQuality = FilterQuality.None,
    )
    fun leafAt(i: Int): Pair<Float, Float> {
        val side = if (i % 2 == 0) 1 else -1
        val (rx, ry) = TrimArt.leafRoot(game.leaves[i], side)
        val x = if (side > 0) rx - TrimArt.LEAF_BASE else rx - (TrimArt.LEAF_W - TrimArt.LEAF_BASE)
        return x to ry - TrimArt.LEAF_H / 2f - 1
    }
    val guideY = TrimArt.TOP + pos * TrimArt.LEN
    // The reach band behind everything, so it's plain which height counts.
    val band = Trim.HALF * TrimArt.LEN
    drawRect(Color(0x22FFD84A), Offset(ox, oy + (guideY - band) * sc), Size(TrimArt.W * sc, band * 2 * sc))
    // Leaves sit behind the bud, rooted in it.
    for (i in game.leaves.indices) if (!game.cut[i]) {
        val (x, y) = leafAt(i)
        val hot = abs(game.leaves[i] - pos) <= Trim.HALF && !game.done
        art(TrimArt.leaf(s, if (i % 2 == 0) 1 else -1, hot), x, y)
    }
    art(TrimArt.bud(s, colour), 0f, 0f)
    // Snipped leaves tumble off and fade.
    for (i in game.leaves.indices) if (cutAt[i] >= 0) {
        val t = now - cutAt[i]
        if (t > 0.9f) continue
        val side = if (i % 2 == 0) 1 else -1
        val (x, y) = leafAt(i)
        art(TrimArt.leaf(s, side, false), x + side * t * 14, y + t * t * 70, (1 - t / 0.9f).coerceIn(0f, 1f))
    }
    // The guide across the bud, then the scissors on the right edge.
    val miss = missed > 0 && snipAt >= 0 && now - snipAt < 0.3f && !game.cut.indices.any { cutAt[it] == snipAt }
    val lineC = if (miss) Color(0xFFE05A4A) else Color(0xAAFFD84A)
    var gx = 2f
    while (gx < TrimArt.W - TrimArt.SHEARS_W + 4) { drawRect(lineC, Offset(ox + gx * sc, oy + (guideY - 0.5f) * sc), Size(2f * sc, 1f * sc)); gx += 4f }
    val shut = snipAt >= 0 && now - snipAt < 0.15f
    val tip = TrimArt.CX + TrimArt.half(pos) + 1f
    art(TrimArt.shears(!shut), tip.coerceAtMost(TrimArt.W - TrimArt.SHEARS_W.toFloat()), guideY - TrimArt.SHEARS_H / 2f)
}
