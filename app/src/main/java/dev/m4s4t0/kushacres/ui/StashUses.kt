package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.layout.Arrangement
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        T("Smoke a sesh", 14.sp, Gold)
        T("Each strain's trait gives its own buff; better strains and a comfier home make it last. Piling on more fades out at ${Sesh.MAX_MINUTES / 60}h.", 10.sp, Dim, bold = false)
        val highs = listOf(Buff.HIGH_GROW, Buff.HIGH_YIELD, Buff.HIGH_GOLD, Buff.HIGH_XP, Buff.HIGH_PRICE).filter { f.on(it) }
        highs.forEach { b -> T("${b.title} · ${dur(((f.buffs[b] ?: now) - now) / 1000 + 1)}", 11.sp, Leaf) }
        val list = stashed(SeshSize.JOINT.grams)
        if (list.isEmpty()) T("Nothing to roll. Harvest something first.", 12.sp, Dim)
        list.forEach { (s, g) ->
            val buffs = Sesh.buffs(s).joinToString { it.title.substringBefore(':') }
            StrainRow(s, g, "$buffs · ${Sesh.minutes(f, s, SeshSize.JOINT)}m a joint") {
                SeshSize.entries.forEach { z ->
                    Btn("${z.title} ${z.grams}g", Modifier.weight(1f), enabled = g >= z.grams, color = if (z == SeshSize.HOTBOX) Gold else WoodEdge, size = 11.sp) {
                        val r = Sesh.smoke(f, s.id, z, Store.now())
                        if (r == Result.OK) {
                            Stoned.begin(s, z)
                            Store.changed()
                            Store.toast(Stoned.theme.line)
                            ui.panel = Panel.NONE
                        } else if (r == Result.LIMIT) Store.toast("You're as high as you can get. Give it a while")
                        else report(r)
                    }
                }
            }
        }
    }
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
