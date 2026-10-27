package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.Coins
import dev.m4s4t0.kushacres.art.DecorArt
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.model.BucksItem
import dev.m4s4t0.kushacres.model.Holiday
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.Strains

/** Pot Bucks, the holiday's tokens and a running festival, in a row under the weather. Tap one to open its shop. */
@Composable
internal fun Currencies(ui: UiState) {
    val f = Store.farm
    Store.rev
    val now = Store.now()
    val h = Holiday.at(now)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Chip(Modifier.clickable { ui.panel = Panel.BUCKS }) {
            Px(Coins.bucks, 18.dp); Spacer(Modifier.width(4.dp)); T("${f.potBucks}", 13.sp, Color(0xFF8AE07A))
        }
        if (h != null) {
            Spacer(Modifier.width(6.dp))
            Chip(Modifier.clickable { ui.panel = Panel.HOLIDAY }) {
                Px(Coins.token(h), 18.dp); Spacer(Modifier.width(4.dp)); T("${f.tokens(h)} · ${h.title} shop", 13.sp, Color(h.main))
            }
        }
        if (f.festivalOn(now)) {
            Spacer(Modifier.width(6.dp))
            Chip(Modifier.clickable { Store.toast("Festival on! Prices ×1.5 for another ${dur((f.festivalUntil - now) / 1000)}.") }) {
                T("☮ ${dur((f.festivalUntil - now) / 1000)}", 13.sp, Color(0xFFF06AB0))
            }
        }
    }
}

@Composable
private fun Chip(mod: Modifier, content: @Composable () -> Unit) {
    Row(Modifier.woodBox().then(mod).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { content() }
}

/** The holiday shop: its farm, strains, decor and pet, all paid in the holiday's tokens and kept for good. */
@Composable
internal fun HolidayPanel(ui: UiState) {
    val f = Store.farm
    Store.rev
    val h = Holiday.at(Store.now())
    if (h == null) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { T("No holiday on right now. Each one opens its own shop for a few days.", 12.sp, Dim, bold = false) }
            items(Holiday.entries) { x ->
                Row(Modifier.fillMaxWidth().frame(WoodIn, Color(x.main)).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Px(Coins.token(x), 22.dp); Spacer(Modifier.width(8.dp))
                    T(x.title, 13.sp, Cream, Modifier.weight(1f))
                    T("${f.tokens(x)} ${x.tokens.lowercase()}", 11.sp, Dim)
                }
            }
        }
        return
    }
    val tok = Coins.token(h)
    fun buy(r: Result, msg: String) { report(r); if (r == Result.OK) Store.toast(msg) else if (r == Result.NO_CASH) Store.toast("Not enough ${h.tokens.lowercase()}") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(Modifier.fillMaxWidth().frame(Color(0xFF2A1E2E), Color(h.main)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Px(tok, 28.dp); Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    T(h.greeting, 12.sp, Color(h.main))
                    T("Earn ${h.tokens.lowercase()} from every sale, the dailies, challenges and events. Everything here is yours for good.", 10.sp, Dim, bold = false)
                }
                Spacer(Modifier.width(6.dp))
                T("${f.tokens(h)}", 18.sp, Color(h.main))
            }
        }
        item { Section("Farm") }
        item {
            val def = h.site
            val owned = f.owns(def.id)
            Card(DecorArt.sprite(h.decor.first()), def.name, def.blurb, if (owned) "Yours. Switch to it from Your farms." else null,
                "${Holiday.FARM}", enabled = !owned && f.tokens(h) >= Holiday.FARM, actionIcon = tok) {
                buy(f.buyHolidayFarm(h), "${def.name} is yours! Find it under Your farms.")
            }
        }
        item { Section("Strains: unlocked for good") }
        items(h.strains) { s ->
            Store.rev
            val owned = s.id in f.unlockedStrains
            val p = f.tokenPrice(s)
            Card(Sprites.strainIcon(s), s.name, "${dur(s.seconds)} · ${s.yield}g × ${money(s.price)}", if (owned) "Unlocked. It's on the Holiday seed shelf." else null,
                "$p", enabled = !owned && f.tokens(h) >= p, actionIcon = tok) {
                buy(f.buyHolidayStrain(h, s), "${s.name} unlocked! Plant it from Seeds › Holiday.")
            }
        }
        item { Section("Decor") }
        items(h.decor) { d ->
            Store.rev
            val have = f.decorInv[d.name] ?: 0
            Card(DecorArt.sprite(d), d.title, d.desc, if (have > 0) "$have ready to place from Build" else null,
                "${Holiday.DECOR}", enabled = f.tokens(h) >= Holiday.DECOR, actionIcon = tok) {
                buy(f.buyHolidayDecor(h, d), "${d.title} bought. Place it from Build › Decor.")
            }
        }
        item { Section("Pet") }
        item {
            val p = h.pet
            val owned = p.name in f.pets
            Card(People.pet(p, 0), p.title, "Wanders your farms. Every farm.", if (owned) "Yours" else null,
                "${Holiday.PET}", enabled = !owned && f.tokens(h) >= Holiday.PET, actionIcon = tok) {
                buy(f.buyPet(h), "Your ${p.title.lowercase()} moved in!")
            }
        }
    }
}

/** Pot Bucks: earned from levels, dailies, challenges and the odd lucky harvest; spent on boosts. */
@Composable
internal fun BucksPanel(ui: UiState) {
    val f = Store.farm
    Store.rev
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(Modifier.fillMaxWidth().frame(Color(0xFF1E2A1E), Color(0xFF5AC05A)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Px(Coins.bucks, 28.dp); Spacer(Modifier.width(8.dp))
                T("Pot Bucks come from levelling up, the daily bonus, challenges and lucky harvests.", 10.sp, Dim, Modifier.weight(1f), bold = false)
                Spacer(Modifier.width(6.dp))
                T("${f.potBucks}", 18.sp, Color(0xFF8AE07A))
            }
        }
        items(BucksItem.entries) { b ->
            Store.rev
            val icon = when (b) {
                BucksItem.SEEDS -> Sprites.strainIcon(Strains.exotics.first())
                BucksItem.CASH -> Sprites.coin
                BucksItem.ENVELOPE -> Sprites.lock
                else -> Coins.bucks
            }
            Card(icon, b.title, b.desc, null, "${b.price}", enabled = f.potBucks >= b.price, actionIcon = Coins.bucks) {
                val r = f.buyWithBucks(b, Store.now()); report(r)
                if (r == Result.OK) Store.toast("${b.title}: done.") else if (r == Result.NO_CASH) Store.toast("Not enough Pot Bucks")
            }
        }
    }
}

@Composable
private fun Section(s: String) = T(s, 13.sp, Gold)
