package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.SiteDef
import dev.m4s4t0.kushacres.model.Sites

/**
 * Every farm in the order the arrows walk them: all the farms you own first (realtor's list, then holiday and
 * Pot Bucks farms), so hopping between them is one tap, then the ones still for sale.
 */
internal fun farmOrder(): List<SiteDef> {
    val (owned, forSale) = Sites.all.partition { Store.farm.owns(it.id) }
    return owned + (Sites.holidays + Sites.premium).filter { Store.farm.owns(it.id) } + forSale
}

/**
 * Slim ‹ › tabs down the sides of the farm, below the rail of buttons. They walk your farms in order; one you don't own yet
 * shows its map with a button to buy it (or the level it needs) instead of switching.
 */
@Composable
fun BoxScope.FarmArrows(ui: UiState) {
    val f = Store.farm
    Store.rev
    if (ui.mode != Mode.Farm) return
    val order = farmOrder()
    val at = order.indexOf(ui.peek ?: f.site.def).coerceAtLeast(0)
    fun step(d: Int) {
        val def = order.getOrNull(at + d) ?: return
        ui.selected = null; ui.menu = null
        if (f.owns(def.id)) {
            ui.peek = null
            if (f.site.def !== def && f.switchTo(def.id)) { Store.changed(); ui.recenter++; Theme.site = def }
        } else ui.peek = def
    }
    ui.peek?.let { PeekCard(ui, it) }
    // Centred in the space under the Lab / Strains / Hunts column, so the left one never covers it.
    Box(Modifier.matchParentSize().padding(top = RAIL_ROOM)) {
        Arrow("‹", at > 0, Modifier.align(Alignment.CenterStart)) { step(-1) }
        Arrow("›", at < order.size - 1, Modifier.align(Alignment.CenterEnd)) { step(1) }
    }
}

/** Roughly how tall the rail of buttons in the top left is. */
private val RAIL_ROOM = 172.dp

@Composable
private fun Arrow(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier.size(width = 32.dp, height = 84.dp).pressable(src)
            .frame(if (enabled) WoodIn.copy(alpha = 0.85f) else Color(0x99231B14), if (enabled) WoodEdge else Color(0x994A3E30))
            .clickable(src, null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, 22.sp, if (enabled) Cream else Dim.copy(alpha = 0.4f)) }
}

/** A farm you don't own, from above, with what it takes to get it. */
@Composable
private fun BoxScope.PeekCard(ui: UiState, def: SiteDef) {
    val f = Store.farm
    val tiles = remember(def.id) { f.mapOf(def) }
    val M = Farm.MAX
    Column(
        Modifier.align(Alignment.Center).widthIn(max = 260.dp).fillMaxWidth().padding(horizontal = 30.dp)
            .frame(Color(def.woodIn), Color(def.edge), 3.dp).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        T(def.name, 15.sp, Gold, align = TextAlign.Center)
        T(def.blurb, 10.sp, Dim, align = TextAlign.Center, bold = false)
        Spacer(Modifier.height(6.dp))
        val wild = Color(def.wild); val ground = Color(def.ground)
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f).frame(Color(0xFF14100B), Color(def.edge)).padding(2.dp)) {
            val c = size.width / M
            for (y in 0 until M) for (x in 0 until M) {
                val col = when (tiles[y * M + x].kind) {
                    Kind.WATER -> Color(0xFF3A7AC8)
                    Kind.ROCK -> Color(0xFF8A8A8A)
                    Kind.HOUSE, Kind.WING -> Color(0xFFA0583A)
                    Kind.PATH -> Color(0xFFC8A870)
                    else -> if ((x * 7 + y * 13) % 5 == 0) wild else ground
                }
                drawRect(col, Offset(x * c, y * c), Size(c + 0.5f, c + 0.5f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Btn("Back", color = WoodEdge) { ui.peek = null }
            when {
                f.level < def.level -> Btn("Locked · Lv ${def.level}", enabled = false) {}
                else -> Btn("Buy ${money(def.price)}", enabled = f.cash >= def.price, color = Gold) {
                    val r = f.buySite(def); report(r)
                    if (r == Result.OK) {
                        ui.peek = null; ui.selected = null; ui.recenter++; Theme.site = def
                        Store.changed(); Store.toast("${def.name} is yours! Your shack and four plots are waiting.")
                    }
                }
            }
        }
    }
}
