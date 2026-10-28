package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Trim
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * The trimming game in the tile menu: tap Snip (or the bud) while the shears are over a leaf. [onDone] gets the clean
 * snips once every leaf is off, the misses run out, or the player stops early.
 */
@Composable
internal fun TrimGame(onDone: (Int) -> Unit) {
    val game = remember { Trim(Random) }
    var sweeps by remember { mutableFloatStateOf(0f) }
    // Bumped on each snip so the bar redraws.
    var snips by remember { mutableIntStateOf(0) }
    LaunchedEffect(game) {
        var last = 0L
        while (!game.done) withFrameNanos { n ->
            if (last != 0L) sweeps += (n - last) / 1e9f * Trim.speed(game.clean)
            last = n
        }
        delay(450)
        onDone(game.clean)
    }
    fun snip() {
        if (game.done) return
        if (game.snip(Trim.shears(sweeps))) Audio.vary(Sfx.TAP) else Audio.play(Sfx.ERROR)
        snips++
    }
    T("Trim it: snip each leaf", 12.sp, Gold)
    T("Tap while the shears are over a leaf. Every clean leaf is +${Math.round(Farm.TRIM_STEP * 100)}% bud at harvest.", 9.sp, Dim, bold = false)
    Spacer(Modifier.height(6.dp))
    snips
    val pos = Trim.shears(sweeps)
    Canvas(Modifier.fillMaxWidth().height(36.dp).clickable(remember { MutableInteractionSource() }, null) { snip() }) {
        val w = size.width; val h = size.height
        // The bud: a fat green cola along the bar.
        drawRect(Color(0xFF1C1A14), Offset(0f, h * 0.3f), Size(w, h * 0.5f))
        drawRect(Color(0xFF5E8F2E), Offset(2f, h * 0.3f + 2f), Size(w - 4f, h * 0.5f - 4f))
        for (i in game.leaves.indices) {
            val lx = game.leaves[i] * w; val lw = Trim.HALF * 2 * w
            if (game.cut[i]) {
                drawRect(Color(0xFFE8F0C0).copy(alpha = 0.5f), Offset(lx - lw / 2, h * 0.38f), Size(lw, h * 0.34f))
            } else {
                // A sugar leaf sticking up out of the bud.
                drawRect(Color(0xFF2F5A1A), Offset(lx - lw / 2, h * 0.08f), Size(lw, h * 0.5f))
                drawRect(Color(0xFF7BD34B), Offset(lx - lw / 2 + 2f, h * 0.08f + 2f), Size(lw - 4f, h * 0.5f - 4f))
            }
        }
        // The shears.
        val sx = pos * w
        drawRect(Color(0xFF1C1A14), Offset(sx - 3f, 0f), Size(6f, h))
        drawRect(Color(0xFFF2E6C8), Offset(sx - 1.5f, 1f), Size(3f, h - 2f))
        drawRect(Color(0xFFE05A4A), Offset(sx - 4f, h - 7f), Size(8f, 6f))
    }
    Spacer(Modifier.height(4.dp))
    T("Clean ${game.clean}/${game.leaves.size}  ·  misses ${game.misses}/${Trim.MISSES}", 10.sp, if (game.misses > 0) Bad else Leaf, bold = false)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Btn("Snip ✂", Modifier.weight(1f), enabled = !game.done, color = Leaf) { snip() }
        Btn("Done", Modifier.weight(1f), enabled = !game.done, color = WoodEdge) { onDone(game.clean) }
    }
}
