package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.DecorArt
import dev.m4s4t0.kushacres.art.Keeper
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.art.Town
import dev.m4s4t0.kushacres.model.SiteDef
import dev.m4s4t0.kushacres.model.BmItem
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Helper
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.Supply
import dev.m4s4t0.kushacres.model.Upgrade

private val Neon = Color(0xFFD86AF0)

fun shopName(k: Keeper) = when (k) {
    Keeper.VIOLET -> "Violet's Grow Supply"
    Keeper.DEX -> "Dex's Dispensary"
    Keeper.HANK -> "Hank's Hardware"
    Keeper.MONA -> "Mona's Realty"
    Keeper.GUS -> "Gus's Help Wanted"
    Keeper.SHADE -> "The Back Alley"
}

private fun shopLine(k: Keeper) = when (k) {
    Keeper.VIOLET -> "Hey sugar! Supplies, upgrades, and the best seeds in the valley. Grow something pretty for me."
    Keeper.DEX -> "Whatcha got? I'll take bud, hash, dabs, whatever. Fair prices, mostly."
    Keeper.HANK -> "Lumber, lamps, labs. If you can build it, I sell it."
    Keeper.MONA -> "Location, location, location! More land, bigger house. Sign here."
    Keeper.GUS -> "Need a hand? My folks work hard and never ask questions."
    Keeper.SHADE -> "...You didn't see me. Stock turns over every few hours."
}

private fun shopBlurb(k: Keeper) = when (k) {
    Keeper.VIOLET -> "Supplies & upgrades"
    Keeper.DEX -> "Sell bud & goods"
    Keeper.HANK -> "Buildings & decor"
    Keeper.MONA -> "Land & shack"
    Keeper.GUS -> "Hire helpers"
    Keeper.SHADE -> "Black market"
}

@Composable
internal fun TownPanel(ui: UiState) {
    val shop = ui.shop
    if (shop == null) MainStreet(ui) else Column {
        ShopHeader(shop)
        Spacer(Modifier.height(8.dp))
        when (shop) {
            Keeper.VIOLET -> VioletShop(ui)
            Keeper.DEX -> DexShop()
            Keeper.HANK -> HankShop(ui)
            Keeper.MONA -> MonaShop(ui)
            Keeper.GUS -> GusShop()
            Keeper.SHADE -> ShadeShop()
        }
    }
}

@Composable
private fun MainStreet(ui: UiState) {
    val f = Store.farm
    Store.rev
    LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Keeper.entries) { k ->
            Store.rev
            val locked = k == Keeper.SHADE && f.level < Farm.BLACK_MARKET_LEVEL
            Column(
                Modifier.frame(Color(0xFF6A8AA8), if (k == Keeper.SHADE) Neon else WoodEdge)
                    .clickable(enabled = !locked) { ui.shop = k },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Px(Town.front(k), 140.dp)
                    if (locked) Px(Sprites.lock, 36.dp)
                }
                Column(Modifier.fillMaxWidth().background(WoodIn).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    T(shopName(k), 12.sp, if (locked) Dim else Cream, align = TextAlign.Center)
                    T(if (locked) "Opens at Lv ${Farm.BLACK_MARKET_LEVEL}" else shopBlurb(k), 10.sp, if (locked) Bad else Dim)
                }
            }
        }
    }
}

@Composable
private fun ShopHeader(k: Keeper) {
    val rim = if (k == Keeper.SHADE) Neon else Gold
    val name = k.name.lowercase().replaceFirstChar { it.uppercase() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // A big framed portrait with a nameplate under it.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Px(People.portrait(k), 136.dp, Modifier.frame(Color(0xFF14100B), rim, 3.dp))
            T(name, 13.sp, if (k == Keeper.SHADE) Neon else Cream, Modifier.offset(y = (-6).dp).frame(Color(0xFF3F2C1A), rim).padding(horizontal = 10.dp))
        }
        // A speech bubble with its tail pointing at them.
        Spacer(Modifier.width(10.dp).height(16.dp).drawBehind {
            val tail = Path().apply { moveTo(0f, size.height / 2); lineTo(size.width + 2f, 0f); lineTo(size.width + 2f, size.height); close() }
            drawPath(tail, Color(0xFF120D08))
            drawPath(Path().apply { moveTo(5f, size.height / 2); lineTo(size.width + 2f, 4f); lineTo(size.width + 2f, size.height - 4f); close() }, Bubble)
        })
        Column(Modifier.weight(1f).frame(Bubble, Color(0xFFC8A878)).padding(5.dp)) {
            T(shopLine(k), 13.sp, Color(0xFF2B2118), bold = false)
        }
    }
}

private val Bubble = Color(0xFFF5E6C8)

// ---- Violet: supplies and upgrades ----

@Composable
private fun VioletShop(ui: UiState) {
    var tab by remember { mutableIntStateOf(0) }
    Tabs(listOf("Supplies", "Upgrades", "Seeds"), tab) { if (it == 2) { ui.shop = null; ui.panel = Panel.SEEDS } else tab = it }
    if (tab == 0) Supplies() else Upgrades()
}

@Composable
private fun Supplies() {
    val f = Store.farm
    Store.rev
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { T("Every facility batch uses one supply.", 10.sp, Dim, bold = false) }
        items(Supply.entries) { s ->
            Store.rev
            val user = dev.m4s4t0.kushacres.model.Facility.entries.first { it.supply == s }
            val open = f.level >= user.level
            Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Px(Town.supply(s), 28.dp); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    T(s.title, 12.sp, if (open) Cream else Dim)
                    T("For the ${user.title} · ${money(s.price)}", 10.sp, Dim, bold = false)
                    T("You have ${f.supplies[s.ordinal]}", 10.sp, Sky)
                }
                if (!open) T("Lv ${user.level}", 12.sp, Bad)
                else {
                    Btn("×1", enabled = f.cash >= s.price, color = WoodEdge) { report(f.buySupply(s, 1)) }
                    Spacer(Modifier.width(4.dp))
                    Btn("×10", enabled = f.cash >= s.price * 10, color = Gold) { report(f.buySupply(s, 10)) }
                }
            }
        }
    }
}

@Composable
private fun Upgrades() {
    val f = Store.farm
    Store.rev
    val icons = mapOf(Upgrade.STASH to Sprites.jar, Upgrade.MULCH to Sprites.drop, Upgrade.NUTRIENTS to Sprites.arrow,
        Upgrade.GENETICS to Sprites.star, Upgrade.CRED to Sprites.hat)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Upgrade.entries) { u ->
            Store.rev
            val lvl = f.up(u)
            val maxed = lvl >= u.max
            val now = when (u) {
                Upgrade.STASH -> "Holds ${f.stashCap()}g"
                Upgrade.MULCH -> "Water lasts ${f.waterMillis() / 60_000}m"
                Upgrade.NUTRIENTS -> "Growth +${lvl * 6}%"
                Upgrade.GENETICS -> "Yield +${lvl * 8}%"
                Upgrade.CRED -> "Prices +${lvl * 5}%"
            }
            Card(icons[u] ?: Sprites.star, "${u.title}  $lvl/${u.max}", u.desc, now,
                if (maxed) "MAX" else money(u.cost(lvl)), !maxed && f.cash >= u.cost(lvl),
                if (f.level < u.level) u.level else null) { report(f.buy(u)) }
        }
    }
}

// ---- Dex: selling ----

@Composable
private fun DexShop() {
    var tab by remember { mutableIntStateOf(0) }
    Tabs(listOf("Bud", "Goods"), tab) { tab = it }
    if (tab == 0) Selling() else GoodsSelling()
}

@Composable
private fun Selling() {
    val f = Store.farm
    Store.rev
    val entries = f.stash.entries.filter { it.value > 0 }.toList()
    if (entries.isEmpty()) {
        T("Your stash is empty. Harvest some plants first!", 13.sp, Dim, Modifier.padding(12.dp))
        return
    }
    val total = entries.sumOf { (f.price(it.key) * it.value).toLong() }
    Btn("Sell everything (~${money(total)})", Modifier.fillMaxWidth(), color = Gold, icon = Sprites.coin) {
        val got = f.sellAll(); Store.changed(); Store.toast("Sold for ${money(got)}")
    }
    Spacer(Modifier.height(6.dp))
    T("Selling a lot of one strain drops its price. It recovers over time.", 10.sp, Dim, bold = false)
    Spacer(Modifier.height(6.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(entries, key = { it.key }) { e ->
            Store.rev
            val s = Strains[e.key] ?: return@items
            val d = f.demand[e.key] ?: 1.0
            Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Px(Sprites.strainIcon(s), 28.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    T(s.name, 12.sp)
                    T("${e.value}g · ${money(f.price(e.key).toLong())}/g", 11.sp, Gold)
                    T("Demand ${(d * 100).toInt()}%", 10.sp, if (d > 0.9) Leaf else if (d > 0.6) Gold else Bad)
                }
                Btn("1/2", color = WoodEdge) { f.sell(e.key, (e.value + 1) / 2); Store.changed() }
                Spacer(Modifier.width(4.dp))
                Btn("All", color = Gold) { f.sell(e.key, e.value); Store.changed() }
            }
        }
    }
}

@Composable
private fun GoodsSelling() {
    val f = Store.farm
    Store.rev
    val entries = f.goods.entries.filter { it.value > 0 }.toList()
    if (entries.isEmpty()) {
        T(if (f.count(dev.m4s4t0.kushacres.model.Facility.ROLLER) == 0) "No goods yet. Build a Rolling Table at Hank's and turn bud into pre-rolls." else "Nothing to sell right now. Collect batches from your facilities.", 13.sp, Dim, Modifier.padding(12.dp))
        return
    }
    val total = entries.sumOf { (f.goodsPrice(it.key) * it.value).toLong() }
    Btn("Sell all goods (~${money(total)})", Modifier.fillMaxWidth(), color = Gold, icon = Sprites.coin) {
        val got = f.sellAllGoods(); Store.changed(); Store.toast("Sold for ${money(got)}")
    }
    Spacer(Modifier.height(6.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(entries, key = { it.key }) { e ->
            Store.rev
            val (fac, s) = f.goodsParts(e.key) ?: return@items
            Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Px(Town.goods(fac), 28.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    T("${s.name} ${fac.product.lowercase()}", 12.sp)
                    T("×${e.value} · ${money(f.goodsPrice(e.key).toLong())} each", 11.sp, Gold)
                }
                Btn("Sell", color = Gold) { val got = f.sellGoods(e.key, e.value); Store.changed(); Store.toast("Sold for ${money(got)}") }
            }
        }
    }
}

// ---- Hank: building ----

@Composable
private fun HankShop(ui: UiState) {
    var tab by remember { mutableIntStateOf(0) }
    Tabs(listOf("Farm", "Facilities", "Decor"), tab) { tab = it }
    when (tab) {
        0 -> FarmBuilds(ui)
        1 -> FacilityList(ui)
        else -> DecorList(ui, owned = false)
    }
}

// ---- Mona: land and the shack ----

@Composable
private fun MonaShop(ui: UiState) {
    val f = Store.farm
    Store.rev
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Store.rev
            val next = f.nextLand()
            if (next == null) Card(Sprites.map, "Land", "You own the whole valley.", "${f.land}×${f.land}", "MAX", false) {}
            else Card(Sprites.map, "Buy land", "Grow your farm to ${next.first}×${next.first}.", "Now ${f.land}×${f.land}",
                money(next.third), f.cash >= next.third, if (f.level < next.second) next.second else null) {
                val r = f.expand(); report(r)
                if (r == Result.OK) { ui.recenter++; Store.toast("The farm grew to ${f.land}×${f.land}!") }
            }
        }
        item {
            Store.rev
            val next = f.nextShack()
            if (next == null) Card(dev.m4s4t0.kushacres.art.Buildings.shack(f.shack, f.roof, f.wall), "Home", "Nothing left to sell you, darling.", null, "MAX", false) {}
            else Card(dev.m4s4t0.kushacres.art.Buildings.shack(f.shack + 1, f.roof, f.wall), next.title, next.blurb,
                "+20% stash, +3 decor, room for more helpers", money(next.cost), f.cash >= next.cost,
                if (f.level < next.level) next.level else null) {
                val r = f.upgradeShack(); report(r)
                if (r == Result.OK) Store.toast("Welcome to your ${next.title}!")
            }
        }
        item { Btn("Paint & move the shack", Modifier.fillMaxWidth(), color = WoodEdge) { ui.shop = null; ui.panel = Panel.SHACK } }
        item {
            T(if (f.level < SiteDef.REALTOR_LEVEL) "Farms elsewhere: come back at Lv ${SiteDef.REALTOR_LEVEL}, honey. I know some places."
                else "Farms elsewhere. Every one's got its own ground, its own weather in the wood.", 12.sp, Gold)
        }
        farmCards(ui)
    }
}

// ---- Gus: helpers ----

@Composable
private fun GusShop() {
    val f = Store.farm
    Store.rev
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Helper.entries) { h ->
            Store.rev
            val hired = f.has(h)
            val sub = when {
                hired -> "On the payroll"
                f.shack < h.shack -> "Needs a ${dev.m4s4t0.kushacres.model.SHACKS[h.shack].title}"
                h == Helper.MANAGER && !f.farmhand -> "Needs the Farmhand"
                else -> null
            }
            Card(People.helper(h, 0), h.title, h.job, sub, if (hired) "HIRED" else money(h.cost),
                !hired && f.cash >= h.cost && f.shack >= h.shack, if (f.level < h.level) h.level else null) { report(f.hire(h)) }
        }
        if (f.has(Helper.RUNNER)) item {
            Store.rev
            val left = maxOf(0L, f.runnerAt - Store.now()) / 1000
            Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T("Runner", 12.sp)
                    T(if (f.runnerOn) "Next trip in ${dur(left)}. Keeps what open orders need." else "Taking a break", 10.sp, Dim, bold = false)
                }
                Btn(if (f.runnerOn) "On" else "Off", color = if (f.runnerOn) Leaf else Bad) { f.runnerOn = !f.runnerOn; Store.changed() }
            }
        }
    }
}

// ---- Shade: the black market ----

@Composable
private fun ShadeShop() {
    val f = Store.farm
    Store.rev
    val now = Store.now()
    val stock = f.bmStock(now)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Store.rev
            Row(verticalAlignment = Alignment.CenterVertically) {
                Px(Sprites.clock, 16.dp)
                T(" New stock in ${dur(f.bmRefreshIn(now) / 1000)}", 11.sp, Neon, Modifier.weight(1f))
                if (f.envelopes > 0) T("Envelopes: ${f.envelopes}", 11.sp, Sky)
            }
        }
        items(stock.indices.toList()) { i ->
            Store.rev
            val item = stock[i]
            val sold = i in f.bmBought
            val c = item.price(f.level)
            val icon = when {
                item.strain != null -> Strains[item.strain]?.let { Sprites.strainIcon(it) } ?: Sprites.bag
                item.decor != null -> DecorArt.sprite(item.decor)
                item == BmItem.ENVELOPE -> Sprites.bag
                item.buff != null -> Sprites.star
                else -> Sprites.bag
            }
            Card(icon, item.title, item.desc, if (sold) "Sold out" else null, if (sold) "SOLD" else money(c), !sold && f.cash >= c) {
                val r = f.bmBuy(i, Store.now()); report(r)
                if (r == Result.OK) Store.toast(when {
                    item.strain != null -> "Plant them from the Seeds menu"
                    item.decor != null -> "In your shed. Place it from Build > Decor"
                    else -> "Pleasure doing business"
                })
            }
        }
        val exotics = f.seeds.entries.filter { it.value > 0 }
        if (exotics.isNotEmpty()) item {
            Store.rev
            T("Your exotic seeds: " + exotics.joinToString { "${Strains[it.key]?.name} ×${it.value}" }, 11.sp, Neon, bold = false)
        }
    }
}
