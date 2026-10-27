package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.model.Decor
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.Generator
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.Special
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.WorkHouse
import kotlin.math.roundToInt

/** The right-click (desktop) or long-hold (phone) menu for one square: move it, destroy it, and so on. */
@Composable
fun TileMenu(ui: UiState, at: Pair<Int, Int>) {
    val f = Store.farm
    Store.rev
    val (x, y) = at
    val tile = f.tile(x, y)
    // Asking before a move that would stunt a growing plant, and before destroying one.
    var confirm by remember(at) { mutableStateOf<String?>(null) }
    fun close() { ui.menu = null }
    fun moveIt() { ui.mode = Mode.Move(x to y); close(); Store.toast("Tap where it should go") }
    BoxWithConstraints(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { close() }) {
        val d = LocalDensity.current
        val w = 210.dp
        val wPx = with(d) { w.toPx() }; val maxW = constraints.maxWidth.toFloat(); val maxH = constraints.maxHeight.toFloat()
        val px = (ui.menuAt.x + 8).coerceIn(4f, (maxW - wPx - 4).coerceAtLeast(4f))
        val py = (ui.menuAt.y + 8).coerceIn(4f, (maxH - with(d) { 260.dp.toPx() }).coerceAtLeast(4f))
        Column(
            Modifier.offset { IntOffset(px.roundToInt(), py.roundToInt()) }.width(w).woodBox()
                .clickable(remember { MutableInteractionSource() }, null) {}.padding(8.dp),
        ) {
            val strain = tile.strain?.let { Strains[it] }
            val title = when (tile.kind) {
                Kind.PLOT -> strain?.name ?: "Empty plot"
                Kind.SPRINKLER -> "Sprinkler"
                Kind.LAMP -> "Grow lamp"
                Kind.ROCK -> "Boulder"
                Kind.WATER -> "Water"
                Kind.SHACK, Kind.PART -> "Shack"
                Kind.FACILITY -> Facility.of(tile.item)?.title ?: "Facility"
                Kind.HOUSE -> WorkHouse.of(tile.item)?.title ?: "Building"
                Kind.POWER -> Generator.of(tile.item)?.title ?: "Generator"
                Kind.SPECIAL -> Special.of(tile.item)?.title ?: "Special"
                Kind.DECOR, Kind.PATH -> Decor.of(tile.item)?.title ?: "Decor"
                Kind.GRASS -> "Grass"
            }
            T(title, 14.sp, Gold)
            Spacer(Modifier.height(6.dp))
            val c = confirm
            if (c != null) {
                T(c, 12.sp, Cream, bold = false)
                Spacer(Modifier.height(6.dp))
                val stunt = c.startsWith("Moving")
                Btn(if (stunt) "Move anyway" else "Destroy it", Modifier.fillMaxWidth(), color = Bad) {
                    if (stunt) moveIt()
                    else {
                        val r = f.uproot(x, y)
                        if (r == Result.OK) { Audio.play(Sfx.DEMOLISH); Store.changed(); Store.toast("Plant pulled up") } else report(r)
                        close()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Btn("Cancel", Modifier.fillMaxWidth(), color = WoodEdge) { confirm = null }
                return@Column
            }
            @Composable fun item(text: String, color: Color = Leaf, onClick: () -> Unit) {
                Btn(text, Modifier.fillMaxWidth(), color = color, onClick = onClick)
                Spacer(Modifier.height(4.dp))
            }
            when (tile.kind) {
                Kind.PLOT -> {
                    val stunt = f.stuntSeconds(x, y)
                    if (tile.ready()) item("Harvest", Gold) {
                        report(f.harvest(x, y, Store.now())); Store.changed(); close()
                    }
                    if (tile.pest) item("Shoo the bugs", Gold) { f.clearPest(x, y); Store.changed(); close() }
                    if (strain != null && !tile.ready() && !f.wet(x, y, Store.now())) item("Water", Sky) {
                        report(f.water(x, y, Store.now())); Audio.vary(Sfx.WATER); Store.changed(); close()
                    }
                    item(if (stunt > 0) "Move (stunts it)" else "Move") {
                        if (stunt > 0) confirm = "Moving a growing ${strain?.name ?: "plant"} stunts it: it'll take ${dur(stunt.toLong())} longer to grow."
                        else moveIt()
                    }
                    if (strain != null) item("Destroy plant", Bad) {
                        confirm = "Pull up the ${strain.name}? It's gone for good; the plot stays."
                    } else item("Remove plot", Bad) {
                        val r = f.demolish(x, y)
                        if (r == Result.OK) { Audio.play(Sfx.DEMOLISH); Store.changed() }
                        else if (r == Result.BLOCKED) Store.toast("You need at least one plot") else report(r)
                        close()
                    }
                }
                Kind.ROCK -> item("Break it up · ${money(f.rockCost())}", Gold) {
                    val r = f.clearRock(x, y); report(r); if (r == Result.OK) Store.changed(); close()
                }
                Kind.WATER -> T("That stays where nature put it.", 12.sp, Dim)
                Kind.SHACK, Kind.PART -> {
                    item("Open", Gold) { ui.panel = Panel.SHACK; close() }
                    item("Move") { moveIt() }
                }
                Kind.FACILITY -> {
                    item("Open", Gold) { ui.fac = x to y; ui.panel = Panel.FACILITY; close() }
                    item("Move") { moveIt() }
                }
                Kind.HOUSE, Kind.POWER, Kind.SPECIAL -> {
                    item("Details", Gold) { ui.selected = x to y; close() }
                    item("Move") { moveIt() }
                }
                Kind.DECOR, Kind.PATH -> {
                    item("Move") { moveIt() }
                    item("Put away", Bad) {
                        val r = f.demolish(x, y)
                        if (r == Result.OK) { Audio.play(Sfx.DEMOLISH); Store.changed(); Store.toast("Back in your decor box") } else report(r)
                        close()
                    }
                }
                Kind.SPRINKLER, Kind.LAMP -> {
                    item("Move") { moveIt() }
                    item("Remove", Bad) {
                        val r = f.demolish(x, y); if (r == Result.OK) { Audio.play(Sfx.DEMOLISH); Store.changed() } else report(r); close()
                    }
                }
                Kind.GRASS -> {}
            }
            T("Close", 11.sp, Dim, Modifier.fillMaxWidth().clickable { close() }.padding(top = 2.dp), align = TextAlign.Center)
        }
    }
}
