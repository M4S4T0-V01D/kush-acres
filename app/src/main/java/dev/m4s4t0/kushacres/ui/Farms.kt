package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.SiteDef
import dev.m4s4t0.kushacres.model.Sites

/** The menus take their wood from the farm you're on, so you can tell farms apart at a glance. */
object Theme {
    var site by mutableStateOf(Sites.HOME)
}

private fun go(ui: UiState, def: SiteDef) {
    ui.selected = null; ui.mode = Mode.Farm; ui.shop = null; ui.panel = Panel.NONE; ui.peek = null
    ui.recenter++
    Theme.site = def
}

/** One card per farm: switch to the ones you own, buy the rest from Mona once you're Lv 15. */
internal fun LazyListScope.farmCards(ui: UiState) {
    items(Sites.all + (Sites.holidays + Sites.premium).filter { Store.farm.owns(it.id) }) { def ->
        Store.rev
        val f = Store.farm
        val owned = f.owns(def.id)
        val here = f.site.def === def
        val locked = !owned && f.level < def.level
        Row(Modifier.fillMaxWidth().frame(Color(def.woodIn), Color(def.edge)).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            // A little patch of the farm's grass, ringed in its wood.
            Box(Modifier.size(30.dp).frame(Color(def.wild), Color(def.edge)).padding(5.dp)) {
                Box(Modifier.size(20.dp).background(Color(def.ground)))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                T(def.name, 13.sp, if (locked) Dim else Cream)
                T(def.blurb, 10.sp, Dim, bold = false)
                if (owned) f.sites.first { it.def === def }.let { T("${it.land}×${it.land}" + if (here) " · you're here" else "", 10.sp, Sky) }
            }
            Spacer(Modifier.width(6.dp))
            when {
                here -> T("HERE", 12.sp, Gold)
                owned -> Btn("Go", Modifier.widthIn(min = 64.dp), color = Color(def.edge)) {
                    if (f.switchTo(def.id)) { Store.changed(); go(ui, def); Store.toast("Welcome back to ${def.name}.") }
                }
                locked -> T("Lv ${def.level}", 12.sp, Bad)
                else -> Btn(money(def.price), Modifier.widthIn(min = 76.dp), enabled = f.cash >= def.price, color = Gold) {
                    val r = f.buySite(def); report(r)
                    if (r == Result.OK) { Store.changed(); go(ui, def); Store.toast("${def.name} is yours! Your shack and four plots are waiting.") }
                }
            }
        }
    }
}

@Composable
internal fun FarmsPanel(ui: UiState) {
    val f = Store.farm
    Store.rev
    BarColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            T(if (f.level < SiteDef.REALTOR_LEVEL) "Mona starts selling farms in other places at Lv ${SiteDef.REALTOR_LEVEL}."
                else "Cash, stash and helpers go with you. Every farm keeps growing while you're away.", 11.sp, Dim, bold = false)
        }
        item { FarmActions(ui) }
        farmCards(ui)
    }
}

/** What you can do with the farm you're on: throw a hippie festival, or move it somewhere else on its map. */
@Composable
private fun FarmActions(ui: UiState) {
    val f = Store.farm
    Store.rev
    val now = Store.now()
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card(People.hippie(0, 0), "Hippie festival", "Tents and hippies take over ${f.site.def.name} for ${Farm.FESTIVAL_HOURS} hours. Prices ×1.5 everywhere.",
            when {
                f.festivalOn(now) -> "On now · ${dur((f.festivalUntil - now) / 1000)} left"
                !f.canFestival() -> "Needs a ${Farm.FESTIVAL_LAND}×${Farm.FESTIVAL_LAND} farm"
                else -> null
            },
            money(f.festivalCost()), enabled = f.canFestival() && !f.festivalOn(now) && f.cash >= f.festivalCost()) {
            val r = f.festival(now); report(r)
            if (r == Result.OK) { go(ui, f.site.def); Store.toast("The hippies have arrived! Peace, love and higher prices.") }
        }
        Card(Sprites.hat, "Move this farm", "Sell the land under ${f.site.def.name} and buy the same size somewhere else on its map. Everything comes along.", null,
            money(f.relocateCost()), enabled = f.cash >= f.relocateCost()) { ui.mover = true; ui.panel = Panel.NONE }
    }
}
