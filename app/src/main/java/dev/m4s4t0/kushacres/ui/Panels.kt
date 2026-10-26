package dev.m4s4t0.kushacres.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.SavePic
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import dev.m4s4t0.kushacres.Updater
import dev.m4s4t0.kushacres.art.Buildings
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.art.DecorArt
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.art.Town
import dev.m4s4t0.kushacres.model.Decor
import dev.m4s4t0.kushacres.model.EventKind
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.Holiday
import dev.m4s4t0.kushacres.model.WorkHouse
import dev.m4s4t0.kushacres.model.Helper
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.ROOF_COLORS
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.model.SHACKS
import dev.m4s4t0.kushacres.model.Strain
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.WALL_COLORS

internal fun report(r: Result) {
    Audio.play(if (r == Result.OK) Sfx.BUY else Sfx.ERROR)
    when (r) {
        Result.OK -> Store.changed()
        Result.NO_CASH -> Store.toast("Not enough cash")
        Result.LOCKED -> Store.toast("Unlocks at a higher level")
        Result.SHACK -> Store.toast("Needs a bigger shack")
        Result.LIMIT -> Store.toast("No room for more decor. Upgrade the shack!")
        Result.NO_BUD -> Store.toast("Not enough of that strain in the stash")
        Result.NO_SUPPLY -> Store.toast("Out of supplies. Violet's in town has them")
        Result.BUSY -> Store.toast("Already working on a batch")
        Result.BLOCKED -> Store.toast("Can't do that here")
        else -> {}
    }
}

@Composable
fun PanelHost(ui: UiState) {
    val close = { ui.panel = Panel.NONE; ui.shop = null }
    BackHandler { if (ui.panel == Panel.TOWN && ui.shop != null) ui.shop = null else close() }
    // Slides up from the bottom bar when it opens.
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 500f)) }
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = rise.value.coerceIn(0f, 1f) }.background(Color(0x99000000))
            .clickable(remember { MutableInteractionSource() }, null) { close() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(if (ui.panel == Panel.TOWN) 0.82f else 0.72f)
                .graphicsLayer { translationY = (1f - rise.value) * size.height * 0.5f }.navigationBarsPadding().padding(6.dp)
                .woodBox(w = 3.dp).clickable(remember { MutableInteractionSource() }, null) {}.padding(10.dp)
        ) {
            val f = Store.farm
            Store.rev
            val title = when (ui.panel) {
                Panel.SEEDS -> "Seeds"
                Panel.ORDERS -> "Orders"
                Panel.TOWN -> ui.shop?.let { shopName(it) } ?: "Main Street"
                Panel.BUILD -> "Build"
                Panel.SHACK -> SHACKS[f.shack].title
                Panel.FACILITY -> ui.fac?.let { (x, y) -> f.facility(x, y)?.title } ?: ""
                Panel.MENU -> "Farm Book"
                Panel.FARMS -> "Your farms"
                Panel.HOLIDAY -> Holiday.at(Store.now())?.let { "${it.title} shop" } ?: "Holidays"
                Panel.BUCKS -> "Pot Bucks"
                Panel.NONE -> ""
            }
            Row(Modifier.fillMaxWidth().frame(Color(0xFF3F2C1A), Color(0xFFB08440)).padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (ui.panel == Panel.TOWN && ui.shop != null) {
                    T("<", 18.sp, Cream, Modifier.clickable { ui.shop = null }.frame(WoodIn, WoodEdge).padding(horizontal = 8.dp))
                    Spacer(Modifier.width(8.dp))
                }
                T(title, if (title.length > 16) 14.sp else 18.sp, Gold, Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                Px(Sprites.coin, 16.dp); Spacer(Modifier.width(4.dp))
                T(money(f.cash), 14.sp, Gold)
                Spacer(Modifier.width(12.dp))
                T("X", 18.sp, Cream, Modifier.clickable { close() }.frame(Color(0xFF6A2A1E), Color(0xFFD8644A)).padding(horizontal = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            when (ui.panel) {
                Panel.SEEDS -> SeedPanel(ui)
                Panel.ORDERS -> Orders()
                Panel.TOWN -> TownPanel(ui)
                Panel.BUILD -> BuildPanel(ui)
                Panel.SHACK -> ShackPanel(ui)
                Panel.FACILITY -> FacilityPanel(ui)
                Panel.MENU -> MenuPanel(ui)
                Panel.FARMS -> FarmsPanel(ui)
                Panel.HOLIDAY -> HolidayPanel(ui)
                Panel.BUCKS -> BucksPanel(ui)
                Panel.NONE -> {}
            }
        }
    }
}

/** The seed menu's shelves: the ladder cut into four, then lab hybrids, then black-market exotics. */
private val SHELVES: List<Pair<String, List<Strain>>> = run {
    val per = (Strains.ladder.size + 3) / 4
    val ladder = Strains.ladder.chunked(per)
    listOf("Street", "Mid", "Top shelf", "Legend").zip(ladder) + listOf("Lab" to Strains.hybridList, "Exotic" to Strains.exotics, "Holiday" to Strains.holidays)
}
private val SHELF_EDGE = listOf(0xFF8AB04E, 0xFF5CA8E0, 0xFFE0A040, 0xFFE8664A, 0xFF5CD0E0, 0xFFD86AF0, 0xFFFF8A1E).map { Color(it) }

@Composable
internal fun SeedPanel(ui: UiState) {
    val f = Store.farm
    Store.rev
    var tab by remember { mutableIntStateOf(SHELVES.indexOfFirst { (_, l) -> l.any { it.id == ui.seed } }.coerceAtLeast(0)) }
    Tabs(SHELVES.mapIndexed { i, (name, l) ->
        when (i) {
            4 -> "$name ${l.count { it.id in f.bred }}"
            5 -> "$name ${l.count { (f.seeds[it.id] ?: 0) > 0 }}"
            6 -> "$name ${l.count { f.unlocked(it) }}"
            else -> "$name ${l.count { f.unlocked(it) }}/${l.size}"
        }
    }, tab) { tab = it }
    val (_, shelf) = SHELVES[tab]
    val edge = SHELF_EDGE[tab]
    // Lab and exotic shelves only show what you can plant; the ladder shows what's coming too.
    val list = when (tab) {
        4 -> shelf.filter { it.id in f.bred }
        5 -> shelf.filter { (f.seeds[it.id] ?: 0) > 0 }
        6 -> shelf.filter { f.unlocked(it) }
        else -> shelf
    }
    if (list.isEmpty()) {
        T(if (tab == 6) "No holiday strains yet. Each holiday's shop sells two, and they stay yours for good."
            else if (tab == 4) "Nothing bred yet. Cross two strains in the Lab and the hybrid lands here."
            else "No exotic seeds. The back-alley dealer in Town sells them now and then.", 12.sp, Dim, bold = false)
        return
    }
    LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(list) { s ->
            Store.rev
            val open = f.unlocked(s)
            val chosen = ui.seed == s.id
            Column(
                Modifier.clickable(enabled = open) { ui.seed = s.id; ui.panel = Panel.NONE }
                    .frame(if (chosen) Color(0xFF4A3A22) else WoodIn, if (chosen) Gold else edge)
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Px(Sprites.strainIcon(s), 36.dp)
                    if (!open) Px(Sprites.lock, 22.dp)
                }
                T(s.name, 11.sp, if (open) Cream else Dim, align = TextAlign.Center)
                if (open) {
                    T(dur(s.seconds), 10.sp, Sky)
                    if (s.exotic) T("${f.seeds[s.id] ?: 0} seeds", 11.sp, Color(0xFFD86AF0))
                    else T(money(s.seed), 11.sp, Gold)
                    T("${s.yield}g × ${money(s.price)}", 9.sp, Dim, bold = false)
                    s.parents?.let { (a, b) -> T("${Strains[a]?.name} × ${Strains[b]?.name}", 8.sp, Color(0xFF5CD0E0), bold = false, align = TextAlign.Center) }
                } else T("Lv ${s.level}", 11.sp, Bad)
            }
        }
    }
}

@Composable
internal fun Orders() {
    val f = Store.farm
    Store.rev
    val now = Store.now()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items((0 until Farm.MAX_ORDERS).toList()) { i ->
            Store.rev
            val o = f.orders[i]
            Column(Modifier.fillMaxWidth().frame(WoodIn, if (o?.vip == true) Gold else WoodEdge).padding(5.dp)) {
                when {
                    i >= f.orderSlots() -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Px(Sprites.lock, 16.dp); Spacer(Modifier.width(6.dp))
                        T("Order slot opens at Lv ${(i - 1) * 6}", 12.sp, Dim)
                    }
                    o == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Px(Sprites.clock, 16.dp); Spacer(Modifier.width(6.dp))
                        val wait = if (f.orderAt[i] == 0L) 30L else (f.orderAt[i] - now) / 1000
                        T("New buyer in ${dur(wait)}", 12.sp, Dim)
                    }
                    else -> {
                        val ok = f.canFill(o)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Px(if (o.vip) Sprites.star else Sprites.hat, 18.dp); Spacer(Modifier.width(6.dp))
                            T(o.buyer, 13.sp, if (o.vip) Gold else Cream, Modifier.weight(1f))
                            T(money(o.reward), 13.sp, Gold)
                            T(" +${o.xp}xp", 11.sp, Sky)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (it in o.items) {
                                val s = Strains[it.strain] ?: continue
                                val have = f.stash[it.strain] ?: 0
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Px(Sprites.strainIcon(s), 18.dp)
                                    T(" $have/${it.grams}g", 11.sp, if (have >= it.grams) Leaf else Bad)
                                }
                            }
                        }
                        T(o.items.joinToString(", ") { Strains[it.strain]?.name ?: "" }, 10.sp, Dim, bold = false)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Btn("Deliver", Modifier.weight(1f), enabled = ok, icon = Sprites.bag) {
                                if (f.fill(i, Store.now()) == Result.OK) Store.changed()
                            }
                            Btn("Pass", color = Bad, icon = Sprites.trash) { f.trash(i, Store.now()); Store.changed() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Card(icon: android.graphics.Bitmap, title: String, desc: String, sub: String?, action: String, enabled: Boolean, locked: Int? = null, actionIcon: android.graphics.Bitmap? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            Px(icon, 30.dp)
            if (locked != null) Px(Sprites.lock, 18.dp)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            T(title, 13.sp, if (locked == null) Cream else Dim)
            T(desc, 10.sp, Dim, bold = false)
            if (sub != null) T(sub, 10.sp, Sky)
        }
        Spacer(Modifier.width(6.dp))
        if (locked != null) T("Lv $locked", 12.sp, Bad)
        else Btn(action, Modifier.widthIn(min = 76.dp), enabled = enabled, color = Gold, icon = actionIcon, onClick = onClick)
    }
}

@Composable
internal fun Tabs(names: List<String>, tab: Int, onTab: (Int) -> Unit) {
    // More than four tabs wrap into two rows, so the names stay readable.
    val per = if (names.size > 4) (names.size + 1) / 2 else names.size
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        names.indices.chunked(per).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { i -> Btn(names[i], Modifier.weight(1f), color = if (tab == i) Gold else WoodEdge) { onTab(i) } }
                repeat(per - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

// ---- Build ----

internal fun startPlacing(ui: UiState, m: Mode) { ui.mode = m; ui.panel = Panel.NONE; ui.shop = null; ui.selected = null }

@Composable
private fun BuildPanel(ui: UiState) {
    var tab by remember { mutableIntStateOf(0) }
    Tabs(listOf("Farm", "Work", "Make", "Decor", "Tools"), tab) { tab = it }
    when (tab) {
        0 -> FarmBuilds(ui)
        1 -> WorkList(ui)
        2 -> FacilityList(ui)
        3 -> DecorList(ui, owned = true)
        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Store.rev
                Card(Sprites.arrow, "Move", "Pick up anything, the shack too, and put it somewhere else. Free.", null, "Move", true) {
                    startPlacing(ui, Mode.Move())
                }
            }
            item {
                Store.rev
                Card(Sprites.trash, "Clear a square", "Remove empty plots, sprinklers or lamps. Decor goes back in your shed.", null, "Clear", true) {
                    startPlacing(ui, Mode.Demolish)
                }
            }
        }
    }
}

@Composable
internal fun FarmBuilds(ui: UiState) {
    val f = Store.farm
    Store.rev
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Store.rev
            Card(Sprites.strainIcon(Strains.all.first()), "Grow plot", "Somewhere to plant. Each one costs a bit more.",
                "You have ${f.count(Kind.PLOT)}", money(f.plotCost()), f.cash >= f.plotCost()) { startPlacing(ui, Mode.Build(Kind.PLOT)) }
        }
        item {
            Store.rev
            Card(Sprites.sprinkler, "Sprinkler", "Waters the 8 squares around it, forever.", "You have ${f.count(Kind.SPRINKLER)}",
                money(f.sprinklerCost()), f.cash >= f.sprinklerCost(), if (f.level < Farm.SPRINKLER_LEVEL) Farm.SPRINKLER_LEVEL else null) { startPlacing(ui, Mode.Build(Kind.SPRINKLER)) }
        }
        item {
            Store.rev
            Card(Sprites.lamp, "Grow lamp", "+25% growth on the 8 squares around it. Stacks twice.", "You have ${f.count(Kind.LAMP)}",
                money(f.lampCost()), f.cash >= f.lampCost(), if (f.level < Farm.LAMP_LEVEL) Farm.LAMP_LEVEL else null) { startPlacing(ui, Mode.Build(Kind.LAMP)) }
        }
    }
}

/** Work buildings and the workers who staff them. */
@Composable
internal fun WorkList(ui: UiState) {
    val f = Store.farm
    Store.rev
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Store.rev
            Column(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Px(People.worker(0, 0), 30.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        T("Workers", 13.sp, if (f.level >= Farm.WORKER_LEVEL) Cream else Dim)
                        T("Each farm hires its own. A worker staffs a building to double what it does; spare ones water dry plots.", 10.sp, Dim, bold = false)
                    }
                    if (f.level < Farm.WORKER_LEVEL) T("Lv ${Farm.WORKER_LEVEL}", 12.sp, Bad)
                }
                if (f.level >= Farm.WORKER_LEVEL) { Spacer(Modifier.height(4.dp)); WorkerButton(Modifier.fillMaxWidth()) }
            }
        }
        items(WorkHouse.entries) { h ->
            Store.rev
            val c = f.houseCost(h)
            val n = f.count(h)
            Card(Buildings.house(h), h.title, h.desc + ". Staffed: " + h.staffed.lowercase() + ".", "You have $n here" + if (n >= WorkHouse.MAX_EACH) " (max)" else "",
                money(c), f.cash >= c && n < WorkHouse.MAX_EACH, if (f.level < h.level) h.level else null) { startPlacing(ui, Mode.PlaceHouse(h)) }
        }
    }
}

@Composable
internal fun FacilityList(ui: UiState) {
    val f = Store.farm
    Store.rev
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Facility.entries) { fac ->
            Store.rev
            val c = f.facilityCost(fac)
            Card(Buildings.facility(fac), fac.title,
                "${fac.desc}. ${fac.grams}g of bud each, worth ×${fac.mult} per gram. Uses 1 ${fac.supply.title.lowercase()} per batch.",
                "${fac.units} ${fac.product.lowercase()} every ${dur(fac.minutes * 60L)} · you have ${f.count(fac)}",
                money(c), f.cash >= c, if (f.level < fac.level) fac.level else null) { startPlacing(ui, Mode.PlaceFacility(fac)) }
        }
    }
}

/** Decor to place. With [owned], your shed of free pieces comes first. */
@Composable
internal fun DecorList(ui: UiState, owned: Boolean) {
    val f = Store.farm
    Store.rev
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Store.rev; T("Decor placed ${f.decorCount()}/${f.decorCap()} (paths don't count)", 11.sp, Sky) }
        if (owned) {
            val shed = f.decorInv.entries.filter { it.value > 0 }.mapNotNull { e -> Decor.of(e.key)?.let { it to e.value } }
            items(shed) { (d, n) ->
                Store.rev
                Card(DecorArt.sprite(d), "${d.title} ×$n", d.desc, "In your shed", "Place", true) { startPlacing(ui, Mode.PlaceDecor(d)) }
            }
        }
        items(Decor.entries.filter { !it.black }) { d ->
            Store.rev
            Card(DecorArt.sprite(d), d.title, d.desc, null, money(d.cost), f.cash >= d.cost, if (f.level < d.level) d.level else null) {
                startPlacing(ui, Mode.PlaceDecor(d))
            }
        }
    }
}

// ---- Shack ----

@Composable
private fun ShackPanel(ui: UiState) {
    val f = Store.farm
    Store.rev
    val cur = SHACKS[f.shack]
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Store.rev
            Row(verticalAlignment = Alignment.CenterVertically) {
                Px(Buildings.shack(f.shack, f.roof, f.wall), 96.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    T(cur.blurb, 12.sp, Cream, bold = false)
                    Spacer(Modifier.height(4.dp))
                    T("Stash ${f.stashCap()}g (+${f.shack * 20}%)", 11.sp, Sky)
                    T("Decor room ${f.decorCount()}/${f.decorCap()}", 11.sp, Sky)
                    val bunks = Helper.entries.filter { it.shack in 1..f.shack }.joinToString { it.title }
                    T(if (bunks.isEmpty()) "No spare bunks yet. Runners and Fetchers need a Cozy Cabin" else "Room for: $bunks", 11.sp, Sky)
                }
            }
        }
        item {
            Store.rev
            val next = f.nextShack()
            if (next == null) T("This is as fancy as it gets.", 13.sp, Gold)
            else Card(Buildings.shack(f.shack + 1, f.roof, f.wall), "Upgrade to ${next.title}", next.blurb,
                "+20% stash, +3 decor, more helpers", money(next.cost), f.cash >= next.cost,
                if (f.level < next.level) next.level else null) {
                val r = f.upgradeShack(); report(r)
                if (r == Result.OK) Store.toast("Welcome to your ${next.title}!")
            }
        }
        item { Store.rev; T("Roof", 12.sp, Dim); Swatches(ROOF_COLORS, f.roof) { f.paint(it, f.wall); Store.changed() } }
        item { Store.rev; T("Walls", 12.sp, Dim); Swatches(WALL_COLORS, f.wall) { f.paint(f.roof, it); Store.changed() } }
        item {
            Store.rev
            f.shackAt()?.let { at ->
                Btn("Move the shack", Modifier.fillMaxWidth(), color = WoodEdge, icon = Sprites.arrow) { startPlacing(ui, Mode.Move(at)) }
            }
        }
    }
}

@Composable
private fun Swatches(colors: IntArray, chosen: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        colors.forEachIndexed { i, c ->
            Box(Modifier.size(36.dp).border(3.dp, if (i == chosen) Gold else WoodEdge).padding(3.dp).background(Color(c)).clickable { onPick(i) })
        }
    }
}

// ---- Facility ----

@Composable
private fun FacilityPanel(ui: UiState) {
    val f = Store.farm
    Store.rev
    val (x, y) = ui.fac ?: return
    val fac = f.facility(x, y) ?: return
    val t = f.tile(x, y)
    val now = Store.now()
    val need = f.gramsFor(fac, t.lvl)
    val sup = f.supplies[fac.supply.ordinal]
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Store.rev
            Row(verticalAlignment = Alignment.CenterVertically) {
                Px(Buildings.facility(fac), 56.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    T("Level ${t.lvl}/${Facility.MAX_LEVEL}", 13.sp, Gold)
                    T("${fac.units(t.lvl)} ${fac.product.lowercase()} from ${need}g in ${dur(fac.millis(t.lvl) / 1000)}", 11.sp, Cream, bold = false)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Px(Town.supply(fac.supply), 16.dp)
                        T(" ${fac.supply.title}: $sup", 11.sp, if (sup > 0) Sky else Bad)
                    }
                }
            }
        }
        item {
            Store.rev
            val s = t.strain?.let { Strains[it] }
            when {
                s != null && now >= t.doneAt -> Btn("Collect ${t.amount} ${s.name} ${fac.product.lowercase()}", Modifier.fillMaxWidth(), color = Gold, icon = Town.goods(fac)) {
                    report(f.collect(x, y, Store.now()))
                }
                s != null -> T("Making ${s.name} ${fac.product.lowercase()}… ${dur((t.doneAt - now) / 1000 + 1)} left", 13.sp, Sky)
                else -> T(if (sup > 0) "Pick a strain from your stash to start a batch" else "Needs ${fac.supply.title.lowercase()}. Buy some at Violet's in town.", 12.sp, Dim)
            }
        }
        if (t.strain == null) {
            val have = f.stash.entries.filter { it.value > 0 }.toList()
            if (have.isEmpty()) item { T("Your stash is empty.", 12.sp, Dim) }
            items(have, key = { it.key }) { e ->
                Store.rev
                val s = Strains[e.key] ?: return@items
                val each = (f.goodsPrice(f.goodsKey(fac, s.id))).toLong()
                Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Px(Sprites.strainIcon(s), 26.dp); Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        T(s.name, 12.sp)
                        T("${e.value}/${need}g · ${money(each)} each", 10.sp, if (e.value >= need) Leaf else Bad)
                    }
                    Btn("Start", enabled = e.value >= need && sup > 0, color = Gold) { report(f.startBatch(x, y, s.id, Store.now())) }
                }
            }
        }
        item {
            Store.rev
            if (t.lvl < Facility.MAX_LEVEL) {
                val c = fac.upgradeCost(t.lvl)
                Card(Buildings.facility(fac), "Upgrade to level ${t.lvl + 1}", "Bigger batches, 20% faster",
                    "${fac.units(t.lvl + 1)} per batch", money(c), f.cash >= c) { report(f.upgradeFacility(x, y)) }
            } else T("Fully upgraded", 12.sp, Gold)
        }
        item {
            Store.rev
            Btn("Visit Violet's for supplies", Modifier.fillMaxWidth(), color = WoodEdge, icon = Town.supply(fac.supply)) {
                ui.shop = dev.m4s4t0.kushacres.art.Keeper.VIOLET; ui.panel = Panel.TOWN
            }
        }
    }
}

// ---- Dialogs ----

@Composable
fun Dialogs() {
    val f = Store.farm
    Store.rev
    val lv = Store.levelUps.firstOrNull()
    val away = Store.awayReport
    val ev = f.happening
    when {
        lv != null -> key(lv.level) { Modal(onClose = { Store.dismissLevelUp() }, party = true) {
            Box(contentAlignment = Alignment.Center) {
                Rays(Modifier.size(120.dp))
                Px(Sprites.star, 48.dp)
            }
            T("LEVEL ${lv.level}!", 24.sp, Gold)
            Spacer(Modifier.height(6.dp))
            T("Bonus: ${money(lv.bonus)}", 15.sp, Leaf)
            if (lv.unlocks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                T("Unlocked", 12.sp, Dim)
                lv.unlocks.forEach { T(it, 13.sp, Cream, align = TextAlign.Center) }
            }
            Spacer(Modifier.height(12.dp))
            Btn("Nice!", Modifier.fillMaxWidth(), color = Gold) { Store.dismissLevelUp() }
        } }
        away != null -> Modal(onClose = { Store.awayReport = null }) {
            Px(Sprites.hat, 40.dp)
            T("While you were away", 16.sp, Gold)
            Spacer(Modifier.height(6.dp))
            T("${dur(away.millis / 1000)} passed", 12.sp, Dim)
            if (away.harvests > 0) T("${away.harvests} harvests · ${away.grams}g", 14.sp, Leaf)
            if (away.replants > 0) T("${away.replants} replanted", 13.sp, Cream)
            if (away.batches > 0) T("${away.batches} batches cooked", 13.sp, Cream)
            if (away.sold > 0) T("Runner sold ${money(away.sold)}", 13.sp, Gold)
            if (away.xp > 0) T("+${away.xp} xp", 13.sp, Sky)
            Spacer(Modifier.height(12.dp))
            Btn("Back to work", Modifier.fillMaxWidth(), color = Gold) { Store.awayReport = null }
        }
        ev != null -> {
            fun answer(yes: Boolean) {
                val msg = f.resolve(yes, Store.now())
                Store.changed()
                if (msg.isNotEmpty()) Store.toast(msg)
            }
            val sName = ev.strain?.let { Strains[it]?.name } ?: ""
            val (title, text) = when (ev.kind) {
                EventKind.RAID -> "Cops at the gate!" to (if (f.envelopes > 0) "You've got a bribe envelope. Hand it over?"
                    else "Officer Doyle wants ${money(ev.amount)} to look the other way. Or risk the search?")
                EventKind.PESTS -> "Bugs!" to "Spider mites hit ${ev.plots} plot${if (ev.plots > 1) "s" else ""}. They stop growing until you shoo them. Tap the plots!"
                EventKind.RAIN -> "Rain shower" to "Every plot got a free watering."
                EventKind.RUSH -> "Rush hour" to "Town's buzzing. Bud sells for ×1.5 for ${ev.amount} min."
                EventKind.LUCKY -> "Lucky streak" to "Harvests have a 25% golden chance for ${ev.amount} min."
                EventKind.HIPPIES -> "Hippie festival" to "Goods sell for double for ${ev.amount} min."
                EventKind.VIBES -> "Good vibes" to "Everything grows twice as fast for ${ev.amount} min."
                EventKind.STRANGER -> "A shady stranger" to "\"Psst. ${ev.grams} $sName seeds, ${money(ev.amount)}. Today only.\""
                EventKind.CELEB -> "Celebrity order!" to "A famous face wants a big order worth ${money(ev.amount)}. Check Orders."
                EventKind.TRADER -> "Travelling trader" to "Offers ${money(ev.amount)} for ${ev.grams}g of $sName. That's double!"
                EventKind.METEOR -> "Meteor strike!" to "A space rock landed out back. You sold the bits for ${money(ev.amount)} and found ${ev.grams} Pot Bucks inside."
                EventKind.TOURISTS -> "Tourist bus" to "A busload of tourists rolled in. Orders pay ×1.5 for ${ev.amount} min."
                EventKind.HEATWAVE -> "Heatwave" to "It's scorching. Water dries out twice as fast for ${ev.amount} min. Keep those plants wet!"
                EventKind.MOON -> "Harvest moon" to "The moon's huge tonight. Harvests yield ×1.5 for ${ev.amount} min."
                EventKind.GRANDMA -> "Grandma's glaucoma" to "\"Sweetie, could you spare ${ev.grams}g of $sName? I'll pay ${money(ev.amount)}.\" That's triple!"
                EventKind.STORM -> "Thunderstorm" to ("Every plot got a long soak." + if (ev.plots > 0) " The wind blew the bugs off ${ev.plots} plot${if (ev.plots > 1) "s" else ""}." else "")
                EventKind.POTBUCKS -> "Lucky find" to "You found ${ev.amount} Pot Buck${if (ev.amount > 1) "s" else ""} in the couch cushions."
                EventKind.HOLIDAY -> (f.holiday()?.let { h ->
                    if (ev.strain == null) "${h.title} visitor" to "A neighbour dropped by and left you ${ev.amount} ${h.token}."
                    else "${h.title} visitor" to "A neighbour in costume trades ${ev.amount} ${h.token} for ${ev.grams}g of $sName."
                } ?: ("Visitor" to "They left."))
            }
            val ask = ev.kind == EventKind.RAID || ev.kind == EventKind.STRANGER || ev.kind == EventKind.TRADER ||
                ev.kind == EventKind.GRANDMA || (ev.kind == EventKind.HOLIDAY && ev.strain != null)
            Modal(onClose = { answer(false) }) {
                Px(when (ev.kind) {
                    EventKind.RAID -> Sprites.lock; EventKind.RAIN -> Sprites.drop; EventKind.PESTS -> Sprites.trash
                    EventKind.STRANGER, EventKind.TRADER -> Sprites.bag; EventKind.CELEB, EventKind.METEOR, EventKind.POTBUCKS, EventKind.HOLIDAY -> Sprites.star
                    EventKind.STORM -> Sprites.drop; EventKind.GRANDMA -> Sprites.bag; else -> Sprites.arrow
                }, 40.dp)
                T(title, 18.sp, Gold, align = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                T(text, 13.sp, Cream, align = TextAlign.Center, bold = false)
                Spacer(Modifier.height(12.dp))
                if (ask) {
                    val yes = when (ev.kind) {
                        EventKind.RAID -> if (f.envelopes > 0) "Envelope" else "Pay ${money(ev.amount)}"
                        EventKind.STRANGER -> "Buy"
                        EventKind.HOLIDAY -> "Trade"
                        else -> "Sell"
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Btn(yes, Modifier.weight(1f), color = Gold) { answer(true) }
                        Btn(if (ev.kind == EventKind.RAID) "Risk it" else "No", Modifier.weight(1f), color = Bad) { answer(false) }
                    }
                } else Btn("OK", Modifier.fillMaxWidth(), color = Gold) { answer(false) }
            }
        }
        Store.shareCard != null -> ShareDialog(Store.shareCard!!)
        else -> UpdateDialog()
    }
}

/** The share card, big, with a button to save it as a picture. */
@Composable
private fun ShareDialog(card: android.graphics.Bitmap) {
    val f = Store.farm
    Modal(onClose = { Store.shareCard = null }) {
        Image(card.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(ShareCard.W / ShareCard.H.toFloat()), filterQuality = FilterQuality.Medium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Btn("Save", Modifier.weight(1f), color = Gold) {
                val at = SavePic.save(card, ShareCard.fileName(f))
                Store.toast(if (at != null) "Saved to $at" else "Couldn't save the card")
                if (at != null) Store.shareCard = null
            }
            Btn("Close", Modifier.weight(1f), color = WoodEdge) { Store.shareCard = null }
        }
    }
}

/** A newer build on GitHub: offer it, then show the download. */
@Composable
private fun UpdateDialog() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val act = ctx as? android.app.Activity ?: return
    when (val s = Updater.state) {
        is Updater.State.Available -> if (!Updater.snoozed) Modal(onClose = { Updater.snoozed = true }) {
            Px(Sprites.arrow, 40.dp)
            T("Update ${s.version} is out!", 17.sp, Gold, align = TextAlign.Center)
            T("You have ${Updater.current(ctx)}", 11.sp, Dim)
            if (s.notes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                T(s.notes.take(400), 11.sp, Cream, Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()), bold = false)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn("Update", Modifier.weight(1f), color = Gold) { Updater.install(act) }
                Btn("Later", Modifier.weight(1f), color = WoodEdge) { Updater.snoozed = true }
            }
        }
        is Updater.State.Downloading -> Modal(onClose = {}) {
            T("Downloading ${s.version}…", 15.sp, Gold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFF14100B))) {
                Box(Modifier.fillMaxWidth(s.frac.coerceIn(0f, 1f)).height(10.dp).background(Leaf))
            }
        }
        else -> {}
    }
}

@Composable
private fun Modal(onClose: () -> Unit, party: Boolean = false, content: @Composable () -> Unit) {
    BackHandler(onBack = onClose)
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f)) }
    Box(
        Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center,
    ) {
        if (party) Confetti()
        Column(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - 0.6f) / 0.3f).coerceIn(0f, 1f) }
            .width(280.dp).woodBox(edge = Gold, w = 3.dp).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}

/** Slowly turning golden rays behind a level-up star. */
@Composable
private fun Rays(modifier: Modifier) {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { val t0 = withFrameMillis { it }; while (true) withFrameMillis { t = (it - t0) / 1000f } }
    Canvas(modifier) {
        val c = center; val r = size.minDimension / 2
        for (i in 0 until 12) {
            val a0 = t * 0.4f + i * (2 * PI.toFloat() / 12); val a1 = a0 + 0.22f
            val p = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + cos(a0) * r, c.y + sin(a0) * r)
                lineTo(c.x + cos(a1) * r, c.y + sin(a1) * r)
                close()
            }
            drawPath(p, Gold.copy(alpha = 0.22f))
        }
    }
}

/** Pixel confetti raining down over the screen. */
@Composable
private fun Confetti() {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { val t0 = withFrameMillis { it }; while (t < 6f) withFrameMillis { t = (it - t0) / 1000f } }
    val colors = remember { listOf(Gold, Leaf, Sky, Bad, Cream, Color(0xFFD08AF0)) }
    Canvas(Modifier.fillMaxSize()) {
        val px = 4.dp.toPx()
        for (i in 0 until 90) {
            val h = (i * 2654435761L).toInt() ushr 8
            val delay = (h % 100) / 100f * 0.8f
            val tt = t - delay
            if (tt < 0) continue
            val x0 = ((h / 100) % 1000) / 1000f * size.width
            val speed = size.height * (0.25f + (h % 37) / 37f * 0.2f)
            val y = -px * 2 + tt * speed
            if (y > size.height) continue
            val x = x0 + sin(tt * 3f + i) * px * 4
            val flip = if (sin(tt * 8f + i) > 0) 1f else 0.4f
            drawRect(colors[i % colors.size], Offset(x, y), Size(px, px * 1.5f * flip),
                alpha = (1f - ((t - 4.5f) / 1.5f).coerceIn(0f, 1f)))
        }
    }
}
