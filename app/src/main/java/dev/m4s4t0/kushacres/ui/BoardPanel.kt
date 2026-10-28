package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.model.Board
import dev.m4s4t0.kushacres.model.Contracts
import dev.m4s4t0.kushacres.model.Market
import dev.m4s4t0.kushacres.model.Need
import dev.m4s4t0.kushacres.model.Restore
import dev.m4s4t0.kushacres.model.Strains

private val Cork = Color(0xFFB98A55)
private val CorkEdge = Color(0xFF6E4A26)
private val Paper = Color(0xFFF5ECD2)
private val Ink = Color(0xFF2B2118)

/** Draws a cork board with a few pinned notes, for the Main Street card. */
private fun Modifier.corkboard() = drawBehind {
    drawRect(CorkEdge)
    val e = 5.dp.toPx()
    drawRect(Cork, Offset(e, e), Size(size.width - 2 * e, size.height - 2 * e))
    // Cork speckle.
    var seed = 17
    repeat(90) {
        seed = seed * 1103515245 + 12345
        val x = ((seed ushr 8) and 0xFFFF) / 65535f * (size.width - 2 * e) + e
        seed = seed * 1103515245 + 12345
        val y = ((seed ushr 8) and 0xFFFF) / 65535f * (size.height - 2 * e) + e
        drawCircle(Color(0x55603A18), 1.2.dp.toPx(), Offset(x, y))
    }
    val notes = listOf(Triple(0.08f, 0.12f, -6f), Triple(0.55f, 0.08f, 5f), Triple(0.3f, 0.48f, -2f))
    val tints = listOf(Paper, Color(0xFFFFF2A8), Color(0xFFD8F0C8))
    for ((i, n) in notes.withIndex()) {
        val w = size.width * 0.36f; val h = size.height * 0.38f
        val x = size.width * n.first; val y = size.height * n.second
        rotate(n.third, Offset(x + w / 2, y + h / 2)) {
            drawRect(Color(0x44000000), Offset(x + 3, y + 3), Size(w, h))
            drawRect(tints[i], Offset(x, y), Size(w, h))
            for (l in 1..3) drawRect(Color(0x55402A18), Offset(x + w * 0.12f, y + h * (0.18f + l * 0.18f)), Size(w * (if (l == 3) 0.5f else 0.76f), 1.5.dp.toPx()))
            drawCircle(listOf(Color(0xFFE8664A), Color(0xFF4A8AE8), Color(0xFF7BD34B))[i], 3.dp.toPx(), Offset(x + w / 2, y + 4.dp.toPx()))
        }
    }
}

/** The town board's card on Main Street. */
@Composable
internal fun BoardCard(ui: UiState) {
    val f = Store.farm
    Store.rev
    val open = f.level >= Board.LEVEL
    val left = if (open) Board.notes(f, Store.now()).count { !it.done } else 0
    Column(Modifier.frame(Color(0xFF6A8AA8), WoodEdge).clickable(enabled = open) { ui.shop = null; ui.panel = Panel.BOARD },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(140.dp).padding(10.dp).corkboard(), contentAlignment = Alignment.Center) {
            if (!open) Px(Sprites.lock, 36.dp)
        }
        Column(Modifier.fillMaxWidth().background(WoodIn).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            T("Town Board", 12.sp, if (open) Cream else Dim, align = TextAlign.Center)
            T(when { !open -> "Opens at Lv ${Board.LEVEL}"; left > 0 -> "$left request" + (if (left == 1) "" else "s") + " today"; else -> "All done. New notes tomorrow" },
                10.sp, if (!open) Bad else if (left > 0) Leaf else Dim)
        }
    }
}

private fun Modifier.background(c: Color) = drawBehind { drawRect(c) }

@Composable
private fun NeedLine(n: Need, have: Int, showGot: Boolean = false) {
    val s = Strains[n.what]
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (s != null) Px(Sprites.strainIcon(s), 18.dp) else Px(Sprites.jar, 18.dp)
        Spacer(Modifier.width(5.dp))
        val u = Contracts.unit(n)
        T(Contracts.label(n), 11.sp, Ink, Modifier.weight(1f), bold = false)
        T(if (showGot) "${n.got}/${n.amount}$u" else "${n.amount}$u", 11.sp, Ink)
        Spacer(Modifier.width(6.dp))
        T("have $have$u", 10.sp, if (have >= n.left) Color(0xFF3E7A1E) else Color(0xFFA8402A))
    }
}

/** The town board: today's notes from the townsfolk. */
@Composable
internal fun BoardPanel() {
    val f = Store.farm
    Store.rev
    val notes = Board.notes(f, Store.now())
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        T("Small favours for the townsfolk, paid from your stash in one go. They pay well and come with a little extra. " +
            "Some of the radio's best tracks only turn up here.", 10.sp, Dim, bold = false)
        if (notes.isEmpty()) T("Nobody's asking for anything you grow yet.", 12.sp, Dim)
        for ((i, note) in notes.withIndex()) {
            val tint = listOf(Paper, Color(0xFFFFF2A8), Color(0xFFD8F0C8))[i % 3]
            val have = Board.have(f, note.need)
            Column(Modifier.fillMaxWidth().frame(if (note.done) Color(0xFFCFC6B0) else tint, CorkEdge).padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).drawBehind { drawCircle(listOf(Color(0xFFE8664A), Color(0xFF4A8AE8), Color(0xFF7BD34B))[i % 3]) })
                    Spacer(Modifier.width(6.dp))
                    T("${note.who}, ${note.why}", 12.sp, Ink, Modifier.weight(1f))
                }
                Spacer(Modifier.height(3.dp))
                NeedLine(note.need, have)
                Spacer(Modifier.height(3.dp))
                T("Reward: ${Board.prizeLabel(note.prize, note.amount)} + cash", 11.sp, if (note.prize.startsWith("track:")) Color(0xFF7A3EB8) else Color(0xFF8A5A10))
                Spacer(Modifier.height(4.dp))
                if (note.done) T("Thanks! ✓", 12.sp, Color(0xFF3E7A1E))
                else Btn("Hand it over", Modifier.fillMaxWidth(), enabled = Board.canFill(f, note), color = Leaf, icon = Sprites.bag) {
                    Board.fill(f, note)?.let { pay ->
                        Audio.play(Sfx.UNLOCK)
                        Store.toast("${note.who} paid ${money(pay.cash)}, +${pay.xp} XP and ${Board.prizeLabel(pay.prize, note.amount)}")
                    }
                    Store.changed()
                }
            }
        }
        T("A fresh board goes up every day · ${f.boardDone} notes filled so far", 10.sp, Dim, bold = false)
    }
}

/** The market news ticker over Dex's shop. */
@Composable
internal fun NewsBar() {
    val f = Store.farm
    Store.rev
    val now = Store.now()
    if (f.level < Market.LEVEL) return
    val n = Market.news(f, now)
    Row(Modifier.fillMaxWidth().frame(Color(0xFF1E1A14), if (n == null) WoodEdge else if (n.boom) Leaf else Bad).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        T("NEWS", 10.sp, Gold)
        Spacer(Modifier.width(8.dp))
        if (n == null) T("A quiet day at the market.", 11.sp, Dim, Modifier.weight(1f), bold = false)
        else {
            Column(Modifier.weight(1f)) {
                T(n.headline, 11.sp, Cream, bold = false)
                T("${Market.subject(n.what).replaceFirstChar { it.uppercase() }} ${if (n.boom) "+" else ""}${n.percent}% for ${dur(((n.until - now) / 1000).coerceAtLeast(0))}",
                    10.sp, if (n.boom) Leaf else Bad)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

/** A boom or glut tag for a row in the selling lists, or nothing. */
@Composable
internal fun NewsTag(mult: Double) {
    if (mult == 1.0) return
    val p = ((mult - 1) * 100).toInt()
    T(if (p > 0) "▲ +$p%" else "▼ $p%", 10.sp, if (p > 0) Leaf else Bad)
}

/** The landmark info card's line about restoring it. */
@Composable
internal fun RestoreLine(ui: UiState) {
    val f = Store.farm
    Store.rev
    if (f.level < Restore.LEVEL) { T("Locals talk about doing it up one day (Lv ${Restore.LEVEL}).", 11.sp, Dim, bold = false); return }
    val site = f.site.def.id
    if (Restore.done(f, site)) T("Restored. Plants here yield +${(Restore.YIELD * 100).toInt()}%.", 11.sp, Leaf)
    else {
        Spacer(Modifier.height(4.dp))
        Btn("Restore it · step ${Restore.step(f, site) + 1}/${Restore.STEPS}", Modifier.fillMaxWidth(), color = Gold) { ui.panel = Panel.LANDMARK }
    }
}

/** Doing up this farm's landmark, a step at a time. */
@Composable
internal fun LandmarkPanel() {
    val f = Store.farm
    Store.rev
    val site = f.site.def.id
    val mark = f.site.def.landmark
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mark == null) { T("There's nothing to restore on this farm.", 12.sp, Dim); return@Column }
        T(mark.blurb, 11.sp, Dim, bold = false)
        T("Do it up in ${Restore.STEPS} steps. Each one pays well, and once it's finished it lights up and plants on " +
            "${f.site.def.name} yield +${(Restore.YIELD * 100).toInt()}% for good.", 10.sp, Dim, bold = false)
        if (f.level < Restore.LEVEL) { T("Opens at Lv ${Restore.LEVEL}", 12.sp, Bad); return@Column }
        val step = Restore.step(f, site)
        for (i in 0 until Restore.STEPS) {
            val done = i < step; val now = i == step
            Column(Modifier.fillMaxWidth().frame(if (now) WoodIn else Color(0xFF2A2219), if (done) Leaf else if (now) Gold else WoodEdge).padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(if (done) "✓" else "${i + 1}", 13.sp, if (done) Leaf else if (now) Gold else Dim)
                    Spacer(Modifier.width(8.dp))
                    T(Restore.title(i), 13.sp, if (now) Cream else Dim, Modifier.weight(1f))
                }
                if (now) {
                    T(Restore.line(i, mark.name), 11.sp, Dim, bold = false)
                    val n = Restore.need(f, site)
                    if (n == null) T("Grow something first.", 11.sp, Bad)
                    else {
                        Spacer(Modifier.height(4.dp))
                        val have = Restore.have(f, site)
                        val s = Strains[n.what]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (s != null) Px(Sprites.strainIcon(s), 18.dp) else Px(Sprites.jar, 18.dp)
                            Spacer(Modifier.width(5.dp))
                            val u = Contracts.unit(n)
                            T(Contracts.label(n), 11.sp, Cream, Modifier.weight(1f), bold = false)
                            T("${n.got}/${n.amount}$u", 11.sp, Cream)
                            Spacer(Modifier.width(6.dp))
                            T("have $have$u", 10.sp, if (have > 0) Leaf else Bad)
                        }
                        Spacer(Modifier.height(3.dp))
                        Bar(n.got.toFloat() / n.amount, Leaf)
                        Spacer(Modifier.height(4.dp))
                        Btn("Send it", Modifier.fillMaxWidth(), enabled = have > 0, color = Leaf, icon = Sprites.bag) {
                            val pay = Restore.ship(f, site)
                            if (pay != null) {
                                Audio.play(Sfx.UNLOCK)
                                Store.toast(if (pay.finished) "The ${mark.name} is restored! ${money(pay.cash)}, +${pay.xp} XP, 10 Pot Bucks"
                                    else "${Restore.title(i)} done: ${money(pay.cash)}, +${pay.xp} XP")
                            } else Store.toast("Sent. Keep it coming")
                            Store.changed()
                        }
                    }
                }
            }
        }
        if (step >= Restore.STEPS) T("Restored! Plants here yield +${(Restore.YIELD * 100).toInt()}%.", 13.sp, Leaf)
    }
}
