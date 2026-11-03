package dev.m4s4t0.kushacres.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.style.TextAlign
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.model.Gear
import dev.m4s4t0.kushacres.model.Rolling
import dev.m4s4t0.kushacres.model.TasteMark
import dev.m4s4t0.kushacres.model.Tasting
import dev.m4s4t0.kushacres.model.nice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.model.Buff
import dev.m4s4t0.kushacres.model.Contracts
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.Sesh
import dev.m4s4t0.kushacres.model.SeshSize
import dev.m4s4t0.kushacres.model.Strain
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.Towns

private fun stashed(min: Int): List<Pair<Strain, Int>> =
    Store.farm.stash.entries.filter { it.value >= min }.mapNotNull { e -> Strains[e.key]?.let { it to e.value } }.sortedByDescending { it.second }

@Composable
private fun StrainRow(s: Strain, grams: Int, line: String, buttons: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Px(Sprites.strainIcon(s), 28.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                T("${s.name} · ${grams}g", 12.sp)
                T(line, 10.sp, Sky, bold = false)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { buttons() }
    }
}

/** Shack: smoke your own stash for a buff, and a trip on screen to go with it. */
@Composable
internal fun SeshSection(ui: UiState) {
    val f = Store.farm
    Store.rev
    val now = Store.now()
    var circle by remember { mutableStateOf(false) }
    val crew = Sesh.crew(f)
    if (crew == 0) circle = false
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        T("Smoke a sesh", 14.sp, Gold)
        T("Each strain's trait gives its own buff; better strains, better gear and a comfier home make it last. Piling on more fades out at ${Sesh.MAX_MINUTES / 60}h.", 10.sp, Dim, bold = false)
        val highs = listOf(Buff.HIGH_GROW, Buff.HIGH_YIELD, Buff.HIGH_GOLD, Buff.HIGH_XP, Buff.HIGH_PRICE, Buff.CIRCLE).filter { f.on(it) }
        highs.forEach { b -> T("${b.title} · ${dur(((f.buffs[b] ?: now) - now) / 1000 + 1)}", 11.sp, Leaf) }
        GearRow()
        // The smoke circle: pass it round the crew for a work-speed buff.
        Row(Modifier.fillMaxWidth().frame(if (circle) Leaf.copy(alpha = 0.25f) else WoodIn, if (circle) Leaf else WoodEdge)
            .clickable(enabled = crew > 0) { circle = !circle }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            T(if (circle) "☑" else "☐", 16.sp, if (crew > 0) Leaf else Dim)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                T("Smoke circle" + if (crew > 0) " · $crew crew" else "", 12.sp, if (crew > 0) Cream else Dim)
                T(if (crew > 0) "Pass it round: each takes ${(Sesh.CIRCLE_SHARE * 100).toInt()}% of your share, and the crew works ×${nice(Sesh.CIRCLE_SPEED)} faster for the buff"
                    else "Hire some help to pass it round", 10.sp, Dim, bold = false)
            }
        }
        val list = stashed(1).filter { (s, g) -> g >= Sesh.total(f, s, SeshSize.JOINT, circle) }
        if (list.isEmpty()) T(if (circle) "Not enough for the whole circle. Harvest more first." else "Nothing to roll. Harvest something first.", 12.sp, Dim)
        list.forEach { (s, g) ->
            val buffs = Sesh.buffs(s).joinToString { it.title.substringBefore(':') }
            val less = 100 - 100 * Sesh.grams(s, SeshSize.HOTBOX) / SeshSize.HOTBOX.grams
            val tried = f.tasted[s.id] ?: 0
            StrainRow(s, g, "$buffs · ${Sesh.minutes(f, s, SeshSize.JOINT, f.gearOn)}m a joint" + (if (less >= 5) " · strong stuff, $less% less" else "") +
                if (tried == 0) " · new to the journal!" else "") {
                SeshSize.entries.forEach { z ->
                    val need = Sesh.total(f, s, z, circle)
                    Btn("${z.title} ${need}g", Modifier.weight(1f), enabled = g >= need, color = if (z == SeshSize.HOTBOX) Gold else WoodEdge, size = 11.sp) {
                        ui.roll = RollReq(s.id, z, circle)
                    }
                }
            }
        }
        JournalSection()
    }
}

/** What there is to smoke out of: tap one you own to use it, or one you've reached to buy it. */
@Composable
private fun GearRow() {
    val f = Store.farm
    T("Gear", 12.sp, Cream)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Gear.entries.forEach { g ->
            val own = Gear.owns(f, g)
            val on = f.gearOn == g
            Column(
                Modifier.width(92.dp).frame(if (on) Color(0xFF2F4A1E) else WoodIn, if (on) Leaf else WoodEdge).clickable {
                    val r = if (own) Gear.use(f, g) else Gear.buy(f, g)
                    if (r == Result.OK) { Store.changed(); if (!own) Store.toast("Got the ${g.title}!") } else if (r == Result.LOCKED) Store.toast("${g.title} unlocks at level ${g.level}") else report(r)
                }.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.size(26.dp)) { gearIcon(g, own || f.level >= g.level) }
                T(g.title, 11.sp, if (own) Cream else Dim, align = TextAlign.Center)
                T(when { on -> "In use"; own -> "Use"; f.level < g.level -> "Lv ${g.level}"; else -> money(g.cost) }, 10.sp,
                    when { on -> Gold; own -> Cream; f.level < g.level -> Dim; f.cash >= g.cost -> Gold; else -> Bad }, align = TextAlign.Center)
                if (g != Gear.PAPERS) T("−${(100 - g.grams * 100).toInt()}%g +${(g.time * 100 - 100).toInt()}%", 9.sp, Sky, align = TextAlign.Center, bold = false)
            }
        }
    }
    T(f.gearOn.blurb, 10.sp, Dim, bold = false)
}

/** Little drawings of each piece, for the gear row and the rolling game. */
internal fun DrawScope.gearIcon(g: Gear, lit: Boolean) {
    val w = size.width; val h = size.height
    val a = if (lit) 1f else 0.35f
    val glass = Color(0xFF7AD0E0).copy(alpha = a)
    when (g) {
        Gear.PAPERS -> {
            rotate(-25f) {
                drawRoundRect(Color(0xFFF2ECDC).copy(alpha = a), Offset(w * 0.12f, h * 0.42f), Size(w * 0.76f, h * 0.16f), CornerRadius(h * 0.08f))
                drawRect(Color(0xFFC89A60).copy(alpha = a), Offset(w * 0.12f, h * 0.42f), Size(w * 0.14f, h * 0.16f))
                drawCircle(Color(0xFFFF7A3A).copy(alpha = a), h * 0.07f, Offset(w * 0.9f, h * 0.5f))
            }
        }
        Gear.PIPE -> {
            drawCircle(glass, w * 0.2f, Offset(w * 0.3f, h * 0.55f))
            drawCircle(Color(0xFF2A4A2A).copy(alpha = a), w * 0.09f, Offset(w * 0.3f, h * 0.52f))
            drawLine(glass, Offset(w * 0.45f, h * 0.55f), Offset(w * 0.92f, h * 0.42f), h * 0.12f)
        }
        Gear.BONG -> {
            drawRect(glass.copy(alpha = 0.7f * a), Offset(w * 0.38f, h * 0.06f), Size(w * 0.22f, h * 0.6f))
            drawCircle(glass, w * 0.27f, Offset(w * 0.49f, h * 0.7f))
            drawCircle(Color(0xFF3A8AC0).copy(alpha = 0.6f * a), w * 0.18f, Offset(w * 0.49f, h * 0.76f))
            drawLine(Color(0xFFB0B0B0).copy(alpha = a), Offset(w * 0.6f, h * 0.7f), Offset(w * 0.86f, h * 0.48f), h * 0.07f)
        }
        Gear.VAPE -> {
            drawRoundRect(Color(0xFF40404C).copy(alpha = a), Offset(w * 0.3f, h * 0.25f), Size(w * 0.4f, h * 0.7f), CornerRadius(w * 0.08f))
            drawRoundRect(Color(0xFF202028).copy(alpha = a), Offset(w * 0.4f, h * 0.05f), Size(w * 0.2f, h * 0.22f), CornerRadius(w * 0.05f))
            drawCircle(Color(0xFF6AE0FF).copy(alpha = a), w * 0.06f, Offset(w * 0.5f, h * 0.5f))
        }
        Gear.DAB -> {
            drawRoundRect(glass.copy(alpha = 0.7f * a), Offset(w * 0.22f, h * 0.45f), Size(w * 0.4f, h * 0.5f), CornerRadius(w * 0.12f))
            drawRect(glass, Offset(w * 0.34f, h * 0.05f), Size(w * 0.14f, h * 0.42f))
            drawRect(Color(0xFFC0C0C8).copy(alpha = a), Offset(w * 0.62f, h * 0.58f), Size(w * 0.3f, h * 0.12f))
            drawCircle(Color(0xFFFF8A2A).copy(alpha = a), w * 0.07f, Offset(w * 0.86f, h * 0.64f))
        }
    }
}

/** The tasting journal: trophies for variety, and every strain you've smoked with a few words on it. */
@Composable
private fun JournalSection() {
    val f = Store.farm
    var open by remember { mutableStateOf(false) }
    val next = Tasting.next(f)
    Column(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).clickable { open = !open }.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("📖 Tasting journal · ${Tasting.kinds(f)} strains", 12.sp, Gold, Modifier.weight(1f))
            T(if (open) "▲" else "▼", 12.sp, Dim)
        }
        T(next?.let { "Next trophy: ${it.title} at ${it.count} strains (+${it.bucks} Pot Bucks)" } ?: "Every trophy earned!", 10.sp, Sky, bold = false)
        if (open) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Tasting.MARKS.forEachIndexed { i, m ->
                    val got = i < f.tasteMarks
                    Column(Modifier.width(70.dp).frame(if (got) Gold.copy(alpha = 0.2f) else WoodIn, if (got) Gold else WoodEdge).padding(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        T(if (got) "🏆" else "🔒", 14.sp, if (got) Gold else Dim, align = TextAlign.Center)
                        T(m.title, 9.sp, if (got) Gold else Dim, align = TextAlign.Center, maxLines = 2)
                        T("${m.count} strains", 9.sp, Dim, align = TextAlign.Center, bold = false)
                    }
                }
            }
            if (f.tasted.isEmpty()) T("Nothing written yet. Smoke something!", 11.sp, Dim)
            f.tasted.entries.sortedByDescending { it.value }.forEach { (id, n) ->
                val s = Strains[id] ?: return@forEach
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Px(Sprites.strainIcon(s), 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        T("${s.name} ${"★".repeat(Tasting.stars(s))}${"☆".repeat(5 - Tasting.stars(s))}", 11.sp, Cream)
                        T(Tasting.notes(s), 9.sp, Dim, bold = false)
                    }
                    T("×$n", 11.sp, Leaf)
                }
            }
        }
    }
}

/** A sesh picked in [SeshSection], waiting on its roll. */
class RollReq(val strain: String, val size: SeshSize, val circle: Boolean)

/**
 * The rolling game: a needle sweeps a bar and you stop it [Rolling.TAPS] times near the middle. A clean roll stretches the
 * buff (see [Rolling.bonus]); Skip smokes it as it is.
 */
@Composable
internal fun RollOverlay(ui: UiState) {
    val req = ui.roll ?: return
    val f = Store.farm
    val s = Strains[req.strain] ?: run { ui.roll = null; return }
    val gear = f.gearOn
    val offs = remember(req) { mutableStateListOf<Double>() }
    var clock by remember(req) { mutableFloatStateOf(0f) }
    var tapAt by remember(req) { mutableFloatStateOf(0f) }
    var doneAt by remember(req) { mutableFloatStateOf(-1f) }

    fun smoke(bonus: Double) {
        if (ui.roll !== req) return
        ui.roll = null
        val marks = ArrayList<TasteMark>()
        val r = Sesh.smoke(f, s.id, req.size, Store.now(), req.circle, bonus, marks)
        when (r) {
            Result.OK -> {
                Stoned.begin(s, req.size)
                Store.changed()
                Store.toast(marks.lastOrNull()?.let { "Journal trophy: ${it.title}! +${it.bucks} Pot Bucks" }
                    ?: ((if (bonus > 0) "${Rolling.word(bonus)} +${(bonus * 100).toInt()}% · " else "") + Stoned.theme.line))
                ui.panel = Panel.NONE
            }
            Result.LIMIT -> Store.toast("You're as high as you can get. Give it a while")
            Result.BLOCKED -> Store.toast("No crew to pass it to")
            else -> report(r)
        }
    }
    LaunchedEffect(req) {
        var last = 0L
        while (ui.roll === req) withFrameNanos { n ->
            if (last != 0L) clock += ((n - last) / 1e9f).coerceAtMost(0.05f)
            last = n
            if (doneAt >= 0 && clock - doneAt > 1.1f) smoke(Rolling.bonus(offs))
        }
    }
    fun tap() {
        if (offs.size >= Rolling.TAPS) return
        offs += Rolling.needle((clock - tapAt).toDouble(), offs.size)
        tapAt = clock
        Audio.vary(Sfx.TAP)
        if (offs.size == Rolling.TAPS) { doneAt = clock; Audio.play(if (Rolling.bonus(offs) >= Rolling.MAX * 0.7) Sfx.GOLDEN else Sfx.COLLECT) }
    }
    BackHandler { ui.roll = null }
    Box(
        Modifier.fillMaxSize().background(Color(0xB0000000)).clickable(remember { MutableInteractionSource() }, null) { tap() }
            .statusBarsPadding().navigationBarsPadding().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth().woodBox().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            T("${gear.prep} the ${if (gear == Gear.PAPERS) req.size.title.lowercase() else gear.bowl}", 18.sp, Gold, align = TextAlign.Center)
            T("${s.name} · ${gear.title} · ${req.size.title}" + if (req.circle) " · smoke circle" else "", 12.sp, Cream, align = TextAlign.Center, bold = false)
            Spacer(Modifier.height(4.dp))
            T("Tap when the needle's in the green, ${Rolling.TAPS} times. A clean ${gear.prep.lowercase()} lasts up to ${(Rolling.MAX * 100).toInt()}% longer.", 11.sp, Dim, align = TextAlign.Center, bold = false)
            Spacer(Modifier.height(10.dp))
            clock // redraw every frame
            Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                val t = (clock - tapAt).toDouble()
                val needle = if (offs.size < Rolling.TAPS) Rolling.needle(t, offs.size) else offs.last()
                drawRollArt(gear, offs, if (doneAt >= 0) (clock - doneAt) / 1.1f else 0f)
                drawRollBar(needle, offs)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0 until Rolling.TAPS) {
                    val g = offs.getOrNull(i)?.let { Rolling.grade(it) }
                    T(when { g == null -> "○"; g >= 1.0 -> "★"; g >= 0.5 -> "●"; g > 0 -> "◐"; else -> "✗" }, 20.sp,
                        when { g == null -> Dim; g >= 1.0 -> Gold; g >= 0.5 -> Leaf; g > 0 -> Cream; else -> Bad })
                }
            }
            if (doneAt >= 0) {
                val b = Rolling.bonus(offs)
                T("${Rolling.word(b)}  +${(b * 100).toInt()}% buff time", 15.sp, if (b >= Rolling.MAX * 0.7) Gold else Cream, align = TextAlign.Center)
            } else {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("Skip", Modifier.weight(1f).height(40.dp), color = WoodEdge, size = 13.sp) { smoke(0.0) }
                    Btn("Cancel", Modifier.weight(1f).height(40.dp), color = WoodEdge, size = 13.sp) { ui.roll = null }
                }
            }
        }
    }
}

/** The bar: red ends, green middle, the gold sweet spot, marks where earlier taps landed, and the needle. */
private fun DrawScope.drawRollBar(needle: Double, offs: List<Double>) {
    val w = size.width
    val top = size.height * 0.72f; val bh = size.height * 0.2f
    val pad = w * 0.06f; val bw = w - pad * 2
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFB04A3A), Color(0xFFE0B04A), Color(0xFF6AC04A), Color(0xFFE0B04A), Color(0xFFB04A3A)), pad, pad + bw),
        Offset(pad, top), Size(bw, bh), CornerRadius(bh / 2))
    val sweet = (Rolling.SWEET * bw / 2).toFloat()
    drawRect(Color(0xFFFFE07A), Offset(pad + bw / 2 - sweet, top), Size(sweet * 2, bh))
    drawRoundRect(Color(0xFF2A1E14), Offset(pad, top), Size(bw, bh), CornerRadius(bh / 2), style = Stroke(3f))
    fun at(o: Double) = pad + bw * ((o + 1) / 2).toFloat()
    for (o in offs) drawLine(Color.White.copy(alpha = 0.7f), Offset(at(o), top - 4f), Offset(at(o), top + bh + 4f), 3f)
    val x = at(needle)
    drawPath(Path().apply { moveTo(x, top - 2f); lineTo(x - 9f, top - 16f); lineTo(x + 9f, top - 16f); close() }, Color.White)
    drawLine(Color.White, Offset(x, top), Offset(x, top + bh), 4f)
}

/** The piece being got ready, coming together with each tap: [k] runs 0..1 once the last tap's in, for a puff of smoke. */
private fun DrawScope.drawRollArt(g: Gear, offs: List<Double>, k: Float) {
    val w = size.width; val h = size.height * 0.62f
    val n = offs.size
    val q = offs.map { Rolling.grade(it) }.average().takeIf { !it.isNaN() } ?: 1.0
    val c = Offset(w / 2, h * 0.55f)
    if (g == Gear.PAPERS) {
        // The paper rolls up tighter with each tap; a bad tap leaves it lumpy.
        val len = w * 0.5f
        val thick = h * (0.5f - 0.1f * n).coerceAtLeast(0.2f)
        val lump = ((1 - q) * thick * 0.4f).toFloat()
        val path = Path().apply {
            moveTo(c.x - len / 2, c.y - thick * 0.4f)
            quadraticBezierTo(c.x, c.y - thick * 0.5f - lump, c.x + len / 2, c.y - thick * 0.55f)
            lineTo(c.x + len / 2, c.y + thick * 0.55f)
            quadraticBezierTo(c.x, c.y + thick * 0.5f + lump, c.x - len / 2, c.y + thick * 0.4f)
            close()
        }
        drawPath(path, Color(0xFFF2ECDC))
        if (n < Rolling.TAPS) drawRect(Color(0xFF5A8A3A), Offset(c.x - len * 0.42f, c.y - thick * 0.15f), Size(len * 0.84f, thick * 0.3f))
        drawRect(Color(0xFFC89A60), Offset(c.x - len / 2, c.y - thick * 0.4f), Size(len * 0.12f, thick * 0.8f))
        if (n >= 1) drawLine(Color(0xFFD8D0BC), Offset(c.x - len * 0.35f, c.y - thick * 0.2f), Offset(c.x + len * 0.4f, c.y - thick * 0.25f), 2f)
        if (k > 0) drawCircle(Color(0xFFFF7A3A), thick * 0.3f, Offset(c.x + len / 2, c.y))
    } else {
        val s = h * 0.95f
        val left = Offset(c.x - s / 2, c.y - s / 2)
        // Each tap fills the bowl a bit more.
        translate(left.x, left.y) {
            scaleIcon(Size(s, s)) { gearIcon(g, true) }
        }
        val fill = n / Rolling.TAPS.toFloat()
        drawRoundRect(Color(0xFF5A8A3A), Offset(c.x + s * 0.6f, c.y + s * 0.4f - s * 0.8f * fill), Size(s * 0.12f, s * 0.8f * fill), CornerRadius(4f))
        drawRoundRect(Color(0xFF2A1E14), Offset(c.x + s * 0.6f, c.y - s * 0.4f), Size(s * 0.12f, s * 0.8f), CornerRadius(4f), style = Stroke(2f))
    }
    // A puff once it's ready.
    if (k > 0) for (i in 0 until 4) {
        val r = h * (0.12f + 0.1f * i) * k
        drawCircle(Color.White.copy(alpha = 0.35f * (1 - k)), r, Offset(c.x + w * 0.22f + i * 6f, c.y - h * 0.3f * k - i * 10f))
    }
}

/** Draws [block] as if the canvas were [box]. */
private inline fun DrawScope.scaleIcon(box: Size, block: DrawScope.() -> Unit) {
    val sx = box.width / size.width; val sy = box.height / size.height
    scale(sx, sy, Offset.Zero) { block() }
}

/** Dex: crate a pile off to the out-of-town distributor. */
@Composable
internal fun ShipTab() {
    val f = Store.farm
    Store.rev
    val now = Store.now()
    BarColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Store.rev
            T("A distributor two counties over takes big lots at ${(Sesh.SHIP_RATE * 100).toInt()}% of the going rate. " +
                "It doesn't touch demand here. One truck at a time, ${Sesh.SHIP_HOURS}h on the road, ${Sesh.SHIP_MIN}g minimum.", 11.sp, Cream, bold = false)
            Spacer(Modifier.height(6.dp))
            val sh = f.shipment
            if (sh != null) {
                val s = Strains[sh.strain]
                T("On the road: ${sh.grams}g ${s?.name ?: sh.strain} · ${money(sh.pay)} + ${sh.xp} XP · arrives in ${dur(maxOf(0L, sh.at - now) / 1000 + 1)}", 12.sp, Gold)
            }
        }
        val list = stashed(Sesh.SHIP_MIN)
        if (list.isEmpty()) item { T("Nothing big enough to crate up. You need at least ${Sesh.SHIP_MIN}g of one strain.", 12.sp, Dim) }
        items(list, key = { it.first.id }) { (s, g) ->
            Store.rev
            val free = f.shipment == null
            StrainRow(s, g, "All of it: ${money(Sesh.shipPay(f, s, g))} + ${Sesh.shipXp(s, g)} XP") {
                listOf(Sesh.SHIP_MIN to "${Sesh.SHIP_MIN}g", (g / 2) to "Half", g to "All").distinctBy { it.first }.forEach { (n, label) ->
                    Btn(label, Modifier.weight(1f), enabled = free && n >= Sesh.SHIP_MIN, color = if (n == g) Gold else WoodEdge, size = 11.sp) {
                        val r = Sesh.ship(f, s.id, n, Store.now())
                        if (r == Result.OK) { Store.changed(); Store.toast("${n}g of ${s.name} is on the truck") }
                        else if (r == Result.BUSY) Store.toast("The truck's already out") else report(r)
                    }
                }
            }
        }
    }
}

/** Dex: give bud to the compassion clinic for reputation, XP and a few Pot Bucks. */
@Composable
internal fun ClinicTab() {
    val f = Store.farm
    Store.rev
    val town = Sesh.clinicTown(f)
    BarColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Store.rev
            T("The ${Towns[town].town} compassion clinic hands it out to patients who can't pay. No cash, but folks remember.", 11.sp, Cream, bold = false)
            T("Reputation here: ${Contracts.rep(f, town)}", 12.sp, Gold)
        }
        val list = stashed(Sesh.DONATE_MIN)
        if (list.isEmpty()) item { T("Nothing to give yet.", 12.sp, Dim) }
        items(list, key = { it.first.id }) { (s, g) ->
            Store.rev
            StrainRow(s, g, "All of it: +${Sesh.donateRep(s, g)} rep, +${Sesh.donateXp(s, g)} XP, +${Sesh.donateBucks(g)} Pot Bucks") {
                listOf(Sesh.DONATE_MIN to "${Sesh.DONATE_MIN}g", (g / 2) to "Half", g to "All").distinctBy { it.first }.forEach { (n, label) ->
                    Btn(label, Modifier.weight(1f), enabled = n >= Sesh.DONATE_MIN, color = if (n == g) Gold else WoodEdge, size = 11.sp) {
                        val gift = Sesh.donate(f, s.id, n)
                        if (gift != null) { Store.changed(); Store.toast("Thank you! +${gift.rep} rep, +${gift.bucks} Pot Bucks") } else report(Result.NO_BUD)
                    }
                }
            }
        }
    }
}
