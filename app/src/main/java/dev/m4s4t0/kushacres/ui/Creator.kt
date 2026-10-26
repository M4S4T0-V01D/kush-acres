package dev.m4s4t0.kushacres.ui

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.Coins
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Farmer
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.Result
import kotlinx.coroutines.delay

/** A brand-new game: no farmer yet, and nothing done on the starting land, so it can still move for free. */
private fun fresh(f: Farm) = f.farmer == null && f.level == 1 && f.totalHarvests == 0L && f.sites.size == 1

/**
 * The farmer creator: shown for a new game (and once for farms from before farmers), or from Settings to restyle.
 * A new game then picks where on the map its farm starts.
 */
@Composable
internal fun Creator(ui: UiState) {
    val f = Store.farm
    Store.rev
    val first = f.farmer == null
    if (!first && !ui.creator) return
    var look by remember(first) { mutableStateOf(f.farmer ?: f.restartLook ?: Farmer.random()) }
    var step by remember(first) { mutableIntStateOf(0) }
    val newGame = remember(first) { fresh(f) }
    Overlay {
        if (step == 1) {
            T("Where's your farm?", 18.sp, Gold)
            T("Tap the map to pick a spot. You can always move later for a fee.", 11.sp, Dim, bold = false)
            Spacer(Modifier.height(8.dp))
            // The farmer only moves in once the spot is picked, so this stays up until then.
            MapPicker(Modifier.fillMaxWidth(), cost = null) { done ->
                if (!done) step = 0
                else { f.restyle(look.copy(name = look.name.trim().ifEmpty { "Farmer" }), false); f.restartLook = null; Store.changed(); ui.recenter++; Store.toast("Welcome to Kush Acres, ${f.farmer?.name}!") }
            }
            return@Overlay
        }
        T(if (first) "Make your farmer" else "Restyle your farmer", 18.sp, Gold)
        Spacer(Modifier.height(6.dp))
        var frame by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { while (true) { delay(420); frame = 1 - frame } }
        Box(Modifier.size(132.dp).frame(Color(0xFF3E6A30), Color(0xFF6A9A4A)), contentAlignment = Alignment.Center) {
            Px(People.farmer(look, frame), 124.dp)
        }
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            look.name, { look = look.copy(name = it.replace("\n", "").take(Farmer.NAME_MAX)) },
            Modifier.widthIn(min = 160.dp).frame(Color(0xFF14100B), WoodEdge).padding(horizontal = 8.dp, vertical = 5.dp),
            singleLine = true, textStyle = TextStyle(color = Cream, fontSize = 15.sp), cursorBrush = SolidColor(Gold),
        )
        Spacer(Modifier.height(6.dp))
        Pick("Skin", look.skin, Farmer.SKINS.size, swatch = Farmer.SKINS[look.skin]) { look = look.copy(skin = it) }
        Pick("Hair", look.hair, Farmer.HAIRS.size, label = Farmer.HAIRS[look.hair]) { look = look.copy(hair = it) }
        Pick("Hair colour", look.hairColor, Farmer.HAIR_COLORS.size, swatch = Farmer.HAIR_COLORS[look.hairColor]) { look = look.copy(hairColor = it) }
        Pick("Outfit", look.outfit, Farmer.OUTFITS.size, label = Farmer.OUTFITS[look.outfit]) { look = look.copy(outfit = it) }
        Pick("Top", look.shirt, Farmer.CLOTH.size, swatch = Farmer.CLOTH[look.shirt]) { look = look.copy(shirt = it) }
        Pick("Bottoms", look.pants, Farmer.CLOTH.size, swatch = Farmer.CLOTH[look.pants]) { look = look.copy(pants = it) }
        Pick("Hat", look.hat, Farmer.HATS.size, label = Farmer.HATS[look.hat]) { look = look.copy(hat = it) }
        Pick("Face", look.face, Farmer.FACES.size, label = Farmer.FACES[look.face]) { look = look.copy(face = it) }
        Spacer(Modifier.height(8.dp))
        Btn("🎲 Random", Modifier.fillMaxWidth(), color = WoodEdge) { look = Farmer.random().copy(name = look.name) }
        Spacer(Modifier.height(6.dp))
        val named = look.copy(name = look.name.trim().ifEmpty { "Farmer" })
        if (first) {
            Btn(if (newGame) "Next: pick your spot" else "Let's farm", Modifier.fillMaxWidth(), color = Gold) {
                if (newGame) step = 1 else { f.restyle(named, false); Store.changed(); Store.toast("Welcome, ${named.name}!") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn(money(Farmer.restyleCost(f.level)), Modifier.weight(1f), enabled = f.cash >= Farmer.restyleCost(f.level), color = Gold) {
                    val r = f.restyle(named, false); report(r); if (r == Result.OK) ui.creator = false
                }
                Btn("${Farmer.RESTYLE_BUCKS}", Modifier.weight(1f), enabled = f.potBucks >= Farmer.RESTYLE_BUCKS, color = Leaf, icon = Coins.bucks) {
                    val r = f.restyle(named, true); report(r); if (r == Result.OK) ui.creator = false
                }
            }
            Spacer(Modifier.height(6.dp))
            Btn("Cancel", Modifier.fillMaxWidth(), color = WoodEdge) { ui.creator = false }
        }
    }
}

/** Paid moving: sell the land under this farm and buy the same size elsewhere on its map. */
@Composable
internal fun Mover(ui: UiState) {
    if (!ui.mover) return
    val f = Store.farm
    Store.rev
    Overlay(onBack = { ui.mover = false }) {
        T("Move ${f.site.def.name}", 18.sp, Gold)
        T("Everything on your land moves with it. Tap the map to pick the new spot.", 11.sp, Dim, bold = false)
        Spacer(Modifier.height(8.dp))
        MapPicker(Modifier.fillMaxWidth(), cost = f.relocateCost()) { done -> ui.mover = false; if (done) { Store.changed(); ui.recenter++ } }
    }
}

@Composable
private fun Overlay(onBack: (() -> Unit)? = null, content: @Composable () -> Unit) {
    if (onBack != null) BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(10.dp).woodBox(edge = Gold, w = 3.dp).verticalScroll(rememberScrollState()).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

/** One option of the look: ‹ value › with a colour swatch or a name. */
@Composable
private fun Pick(name: String, v: Int, n: Int, swatch: Int? = null, label: String? = null, set: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        T(name, 12.sp, Dim, Modifier.weight(1f))
        Btn("‹", color = WoodEdge) { set((v - 1).mod(n)) }
        Box(Modifier.width(96.dp), contentAlignment = Alignment.Center) {
            if (swatch != null) Box(Modifier.size(width = 60.dp, height = 18.dp).frame(Color(swatch), Color(0xFF14100B)))
            else T(label ?: "", 12.sp, Cream)
        }
        Btn("›", color = WoodEdge) { set((v + 1).mod(n)) }
    }
}

/**
 * The farm's map from above: tap where the middle of the farm should go. Green when it fits, red when water's in
 * the way. [cost] null means free (a new game). [onDone] gets true once the farm has moved, false to go back.
 */
@Composable
private fun MapPicker(modifier: Modifier, cost: Long?, onDone: (Boolean) -> Unit) {
    val f = Store.farm
    val def = f.site.def
    val n = f.land
    val tiles = remember(def.id) { f.mapOf(def) }
    val M = Farm.MAX
    var pick by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val ok = pick?.let { (x, y) -> f.canPlace(def, x, y, n) } ?: false
    val wild = Color(def.wild); val ground = Color(def.ground)
    Canvas(modifier.aspectRatio(1f).frame(Color(0xFF14100B), WoodEdge).padding(3.dp).pointerInput(def.id) {
        detectTapGestures { o -> pick = (o.x / size.width * M).toInt().coerceIn(0, M - 1) to (o.y / size.height * M).toInt().coerceIn(0, M - 1) }
    }) {
        val c = size.width / M
        for (y in 0 until M) for (x in 0 until M) {
            val col = when (tiles[y * M + x].kind) {
                Kind.WATER -> Color(0xFF3A7AC8)
                Kind.ROCK -> Color(0xFF8A8A8A)
                Kind.HOUSE -> Color(0xFFA0583A)
                Kind.PATH -> Color(0xFFC8A870)
                else -> if ((x * 7 + y * 13) % 5 == 0) wild else ground.copy(alpha = 0.85f)
            }
            drawRect(col, Offset(x * c, y * c), Size(c + 0.5f, c + 0.5f))
        }
        // Where the farm is now, then where it would go.
        drawRect(Color(0xFFF2C440), Offset(f.ox * c, f.oy * c), Size(n * c, n * c), style = Stroke(2f))
        pick?.let { (px, py) ->
            val x0 = (px - n / 2).coerceIn(0, M - n); val y0 = (py - n / 2).coerceIn(0, M - n)
            val col = if (ok) Color(0xFF6AE04A) else Color(0xFFE04A4A)
            drawRect(col.copy(alpha = 0.35f), Offset(x0 * c, y0 * c), Size(n * c, n * c))
            drawRect(col, Offset(x0 * c, y0 * c), Size(n * c, n * c), style = Stroke(3f))
        }
    }
    Spacer(Modifier.height(4.dp))
    T("Gold: where it is now" + if (pick != null) (if (ok) " · green: fits" else " · red: too much water") else "", 10.sp, Dim, bold = false)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Btn(if (cost == null) "Back" else "Cancel", Modifier.weight(1f), color = WoodEdge) { onDone(false) }
        val label = when { cost == null && pick == null -> "Keep this spot"; cost == null -> "Farm here"; else -> money(cost) }
        Btn(label, Modifier.weight(1f), enabled = (pick == null && cost == null) || (ok && (cost == null || f.cash >= cost)), color = Gold) {
            val p = pick
            if (p == null) { onDone(true); return@Btn }
            val r = f.relocate(p.first, p.second, free = cost == null)
            // Picking the spot it already sits on is fine for a new game.
            if (r == Result.OK || (cost == null && r == Result.BLOCKED && ok)) { report(Result.OK); onDone(true) } else report(r)
        }
    }
}
