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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Farmer
import dev.m4s4t0.kushacres.Updater
import androidx.compose.ui.platform.LocalContext
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.model.Progress
import dev.m4s4t0.kushacres.model.QuestKind
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.Season
import dev.m4s4t0.kushacres.model.Stat
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.FarmGoal
import dev.m4s4t0.kushacres.model.Trophy
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx

/** The Farm Book: everything that isn't the farm itself. */
@Composable
internal fun MenuPanel(ui: UiState) {
    Store.rev
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val f = Store.farm
    val now = Store.now()
    val dot = { b: Boolean, n: String -> if (b) "$n •" else n }
    Tabs(listOf(dot(f.canClaimDaily(now), "Daily"), dot(f.quests.any { !it.claimed && f.questDone(it) } || f.canClaimQuestBonus(), "Quests"),
        "Lab", "Strains", "Trophies", "Stats", "Settings"), tab) { tab = it; Audio.play(Sfx.TAP) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        when (tab) {
            0 -> Daily()
            1 -> Quests()
            2 -> Lab()
            3 -> StrainBook()
            4 -> Trophies()
            5 -> Stats()
            else -> Settings(ui)
        }
    }
}

@Composable
private fun Row2(left: String, right: String, color: Color = Cream) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        T(left, 12.sp, Dim, Modifier.weight(1f), bold = false)
        T(right, 12.sp, color)
    }
}

@Composable
private fun Daily() {
    Store.rev
    val f = Store.farm
    val now = Store.now()
    val can = f.canClaimDaily(now)
    val today = f.dailyDay(now)
    val shown = (today - 1) % 7 + 1
    T(if (can) "Day $today of your streak is ready!" else "Come back tomorrow for day ${today + 1}.", 14.sp, if (can) Gold else Cream)
    T("Miss a day and the streak starts over. Day 7 pays the most.", 10.sp, Dim, bold = false)
    Spacer(Modifier.height(10.dp))
    for (d in 1..7) {
        val r = Progress.reward(f, d)
        val done = d < shown || (d == shown && !can)
        val next = d == shown && can
        val extra = listOfNotNull(r.buff?.let { "${it.title} (${r.minutes}m)" }, r.seeds?.let { "${r.seedCount}× ${Strains[it]?.name} seeds" },
            f.holiday()?.takeIf { r.tokens > 0 }?.let { "${r.tokens} ${it.tokens.lowercase()}" }).joinToString(", ")
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp)
                .frame(if (done) Color(0xFF2A3A1E) else WoodIn, if (next) Gold else WoodEdge).padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Px(if (r.seeds != null) Sprites.gift else Sprites.coin, 22.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                T("Day $d  ${money(r.cash)}", 13.sp, if (done) Leaf else Cream)
                if (extra.isNotEmpty()) T("+ $extra", 10.sp, Sky, bold = false)
            }
            if (done) T("✓", 16.sp, Leaf)
        }
    }
    Spacer(Modifier.height(10.dp))
    Btn(if (can) "Claim day $today" else "Claimed", Modifier.fillMaxWidth(), enabled = can, color = Gold, icon = Sprites.gift) {
        val r = f.claimDaily(Store.now()) ?: return@Btn
        Audio.play(Sfx.COLLECT)
        Store.toast("Day ${r.day}: +${money(r.cash)}" + (f.holiday()?.takeIf { r.tokens > 0 }?.let { ", +${r.tokens} ${it.tokens.lowercase()}" } ?: ""))
        Store.changed()
    }
    T("Best streak: ${f.bestStreak} days", 10.sp, Dim, Modifier.padding(top = 6.dp), bold = false)
}

@Composable
private fun Quests() {
    Store.rev
    val f = Store.farm
    val now = Store.now()
    T("Today's jobs", 14.sp, Gold)
    T("New ones every day at midnight. Finish all three for a bonus.", 10.sp, Dim, bold = false)
    Spacer(Modifier.height(8.dp))
    f.quests.forEachIndexed { i, q ->
        val p = f.questProgress(q)
        val done = f.questDone(q)
        val goal = if (q.kind == QuestKind.EARN) money(q.target) else fmt(q.target)
        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp).frame(WoodIn, if (done && !q.claimed) Gold else WoodEdge).padding(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Px(Sprites.scroll, 22.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    T(q.kind.text.format(goal), 13.sp, if (q.claimed) Dim else Cream)
                    T("${money(q.cash)}  +${q.xp} XP" + (f.holiday()?.let { "  +${f.rewardTokens(Farm.QUEST_TOKENS)} ${it.tokens.lowercase()}" } ?: ""), 10.sp, Gold)
                }
                if (q.claimed) T("✓", 16.sp, Leaf)
                else Btn("Claim", enabled = done, color = Gold) { Audio.play(Sfx.COLLECT); report(f.claimQuest(i)) }
            }
            Spacer(Modifier.height(6.dp))
            Bar(p.toFloat() / q.target, if (done) Leaf else Sky)
            T(if (q.kind == QuestKind.EARN) "${money(p)} / $goal" else "${fmt(p)} / $goal", 9.sp, Dim, bold = false)
        }
    }
    Spacer(Modifier.height(8.dp))
    val bonus = f.canClaimQuestBonus()
    Btn(if (f.questBonus) "Bonus claimed" else "All three: ${money(f.questBonusCash())} + Good Vibes" +
        (f.holiday()?.let { " + ${f.rewardTokens(Farm.BONUS_TOKENS)} ${it.tokens.lowercase()}" } ?: ""),
        Modifier.fillMaxWidth(), enabled = bonus, color = Gold) {
        Audio.play(Sfx.LEVEL_UP); report(f.claimQuestBonus(now))
    }
}

@Composable
private fun Bar(k: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).border(1.dp, WoodEdge).background(Color(0xFF14100B))) {
        Box(Modifier.fillMaxWidth(k.coerceIn(0f, 1f)).height(8.dp).background(color))
    }
}

@Composable
private fun Lab() {
    Store.rev
    val f = Store.farm
    if (f.level < Progress.BREED_LEVEL) {
        T("The Lab opens at level ${Progress.BREED_LEVEL}.", 14.sp, Dim)
        T("Cross two strains to make a hybrid that grows faster and sells higher than either parent.", 11.sp, Dim, bold = false)
        return
    }
    T("Cross two strains from your stash. Each hybrid only needs breeding once; then its seeds are in the seed shop for good.", 10.sp, Dim, bold = false)
    Spacer(Modifier.height(8.dp))
    T("Bred ${f.bred.size} of ${Strains.hybridList.size}", 12.sp, Sky)
    Spacer(Modifier.height(6.dp))
    for (s in Strains.hybridList.sortedBy { it.level }) {
        val (pa, pb) = s.parents!!
        val a = Strains[pa]!!; val b = Strains[pb]!!
        val have = s.id in f.bred
        val open = f.unlocked(a) && f.unlocked(b)
        val g = f.breedGrams(s)
        val ha = f.stash[pa] ?: 0; val hb = f.stash[pb] ?: 0
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp).frame(WoodIn, if (have) Color(0xFF5CD0E0) else WoodEdge).padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Px(Sprites.strainIcon(s), 30.dp)
                if (!open && !have) Px(Sprites.lock, 18.dp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                T(if (have || open) s.name else "???", 13.sp, if (have) Color(0xFF5CD0E0) else if (open) Cream else Dim)
                T("${a.name} × ${b.name}", 10.sp, Dim, bold = false)
                if (have || open) T("Genes: " + s.traits.joinToString(", ") { it.label }, 9.sp, Color(0xFF9FD86A), bold = false)
                if (!have && open) T("${g}g each (have ${ha}g, ${hb}g)  ${money(f.breedFee(s))}", 10.sp, if (ha >= g && hb >= g) Leaf else Bad, bold = false)
                if (have) T("${dur(s.minutes * 60L)}  ·  ${money(s.price)}/g", 10.sp, Sky, bold = false)
            }
            if (have) T("✓", 16.sp, Leaf)
            else if (!open) T("Lv ${maxOf(a.level, b.level)}", 12.sp, Bad)
            else Btn("Breed", enabled = ha >= g && hb >= g && f.cash >= f.breedFee(s), color = Gold, icon = Sprites.flask) {
                val r = f.breed(s.id)
                if (r == Result.OK) { Audio.play(Sfx.UNLOCK); Store.toast("New strain: ${s.name}!") }
                report(r)
            }
        }
    }
}

@Composable
private fun StrainBook() {
    Store.rev
    val f = Store.farm
    val all = Strains.all
    val known = all.count { f.unlocked(it) || it.id in f.bred }
    T("$known of ${all.size} strains discovered · ${f.grown.size} grown", 12.sp, Sky)
    Spacer(Modifier.height(6.dp))
    for (s in all) {
        val seen = (if (s.exotic) (f.seeds[s.id] ?: 0) > 0 || f.stash.containsKey(s.id) else f.unlocked(s))
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).background(WoodIn).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Px(if (seen) Sprites.strainIcon(s) else Sprites.lock, 22.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                T(if (seen) s.name else "???", 12.sp, if (!seen) Dim else if (s.exotic) Color(0xFFD86AF0) else if (s.hybrid) Color(0xFF5CD0E0) else Cream)
                val kind = if (s.exotic) "Exotic" else if (s.hybrid) "Hybrid" else "Lv ${s.level}"
                T(if (seen) "$kind · ${dur(s.minutes * 60L)} · ${s.yield}g · ${money(s.price)}/g" else kind, 9.sp, Dim, bold = false)
                if (seen) T(s.traits.joinToString(" · ") { it.label }, 9.sp, Sky, bold = false)
            }
            if (s.id in f.grown) T("✓ Grown", 10.sp, Leaf)
        }
    }
}

@Composable
private fun Trophies() {
    Store.rev
    val f = Store.farm
    T("${f.trophies.size} of ${Trophy.entries.size} trophies", 12.sp, Gold)
    Spacer(Modifier.height(6.dp))
    for (t in Trophy.entries) {
        val got = t.name in f.trophies
        val v = minOf(t.value(f), t.target)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp).frame(WoodIn, if (got) Gold else WoodEdge).padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.graphicsLayer { alpha = if (got) 1f else 0.3f }) { Px(Sprites.trophy, 24.dp) }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                T(t.title, 12.sp, if (got) Gold else Cream)
                T(t.desc, 9.sp, Dim, bold = false)
                if (!got && t.target > 1) { Spacer(Modifier.height(3.dp)); Bar(v.toFloat() / t.target, Sky) }
            }
        }
    }
    for (site in f.sites) {
        val goals = FarmGoal.entries.filter { it.applies(site) }
        Spacer(Modifier.height(10.dp))
        T("${site.def.name} · ${site.goals.size} of ${goals.size} goals", 12.sp, Gold)
        for (g in goals) {
            val got = g.name in site.goals
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp).frame(WoodIn, if (got) Gold else WoodEdge).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    T(g.title, 11.sp, if (got) Gold else Cream)
                    T(g.desc, 9.sp, Dim, bold = false)
                    if (!got) { Spacer(Modifier.height(3.dp)); Bar(minOf(g.value(f, site), g.target).toFloat() / g.target, Sky) }
                }
                if (got) T("✓", 14.sp, Gold)
            }
        }
    }
}

@Composable
private fun Stats() {
    Store.rev
    val f = Store.farm
    var confirm by remember { mutableStateOf(false) }
    T("Legacy", 14.sp, Gold)
    Row2("Times sold", "${f.legacy}")
    Row2("Legacy bonus", "Prices +${25 * f.legacy}%, growth and XP +${10 * f.legacy}%", Leaf)
    Row2("Best level", "${f.bestLevel}")
    Row2("Season", "${f.season().title}: ${f.season().desc}", Sky)
    Row2("Season turns in", dur(Season.left(Store.now()) / 1000))
    Spacer(Modifier.height(8.dp))
    if (f.canSellFarm()) {
        if (!confirm) Btn("Sell the farm…", Modifier.fillMaxWidth(), color = Bad) { confirm = true }
        else {
            T("Start over at level 1 on fresh land. You keep trophies, stats, hybrids, decor, paint and exotic seeds, " +
                "and every sale gets +25% for good.", 11.sp, Cream, bold = false)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Btn("Keep farming", Modifier.weight(1f), color = WoodEdge) { confirm = false }
                Btn("Sell it", Modifier.weight(1f), color = Bad) {
                    confirm = false
                    val r = f.sellFarm(Store.now())
                    if (r == Result.OK) { Audio.play(Sfx.LEVEL_UP); Store.toast("Fresh start! Legacy ${f.legacy}") }
                    report(r)
                }
            }
        }
    } else T("Reach level ${Progress.LEGACY_LEVEL} to sell the farm and start over with a permanent bonus.", 10.sp, Dim, bold = false)
    Spacer(Modifier.height(12.dp))
    T("Lifetime", 14.sp, Gold)
    for (st in Stat.entries) Row2(st.title, if (st == Stat.EARNED || st == Stat.SPENT) money(f.stat(st)) else fmt(f.stat(st)))
    Spacer(Modifier.height(8.dp))
    Btn("Share card", Modifier.fillMaxWidth(), color = Gold) { Store.shareCard = ShareCard.render(f) }
    Row2("Best streak", "${f.bestStreak} days")
}

@Composable
internal fun Settings(ui: UiState) {
    val f = Store.farm
    f.farmer?.let { me ->
        Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Px(People.farmer(me, 0), 40.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                T(me.name, 14.sp, Cream)
                T("New look: ${money(Farmer.restyleCost(f.level))} or ${Farmer.RESTYLE_BUCKS} Pot Bucks", 10.sp, Dim, bold = false)
            }
            Btn("Restyle", color = Gold) { ui.creator = true; ui.panel = Panel.NONE }
        }
        Spacer(Modifier.height(8.dp))
    }
    Level("Music", Audio.music) { Audio.changeMusic(it) }
    Level("Sounds", Audio.sound) { Audio.changeSound(it); Audio.play(Sfx.COIN) }
    if (Audio.CAN_BUZZ) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        T("Vibration", 14.sp, Cream, Modifier.weight(1f))
        Btn(if (Audio.haptics) "On" else "Off", color = if (Audio.haptics) Leaf else WoodEdge) { Audio.changeHaptics(!Audio.haptics) }
    }
    Spacer(Modifier.height(12.dp))
    T("Everything you hear is made by the game itself, note by note.", 10.sp, Dim, bold = false)
    Spacer(Modifier.height(16.dp))
    val ctx = LocalContext.current
    val u = Updater.state
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T("Game version ${Updater.current(ctx)}", 14.sp, Cream)
            T(when (u) {
                Updater.State.Checking -> "Checking GitHub…"
                Updater.State.UpToDate -> "You're up to date"
                is Updater.State.Available -> "${u.version} is ready"
                is Updater.State.Downloading -> "Downloading ${(u.frac * 100).toInt()}%"
                Updater.State.Installing -> "Installing…"
                is Updater.State.Failed -> u.why
                Updater.State.Idle -> "Updates come from GitHub"
            }, 11.sp, if (u is Updater.State.Failed) Bad else Dim, bold = false)
        }
        if (u is Updater.State.Available) Btn("Update", color = Gold) { (ctx as? android.app.Activity)?.let { Updater.install(it) } }
        else Btn("Check", enabled = u !is Updater.State.Checking && u !is Updater.State.Downloading) { Updater.check(ctx, manual = true) }
    }
    Spacer(Modifier.height(20.dp))
    // Two taps, so a stray one can't wipe a farm.
    var sure by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T("Start over", 14.sp, Cream)
            T(if (sure) "Everything goes: cash, farms, strains, trophies. Sure?" else "A brand-new game with a new farmer", 11.sp, if (sure) Bad else Dim, bold = false)
        }
        Btn(if (sure) "Wipe it" else "Restart", color = Bad) {
            if (!sure) sure = true
            else { f.restart(Store.now()); Store.changed(); ui.panel = Panel.NONE; ui.selected = null; ui.mode = Mode.Farm; ui.recenter++ }
        }
    }
}

/** A row of 10 pixel blocks: tap one to set the level, the first one again for silence. */
@Composable
private fun Level(name: String, value: Float, onSet: (Float) -> Unit) {
    val n = Math.round(value * 10)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(name, 14.sp, Cream, Modifier.weight(1f))
            T(if (n == 0) "off" else "${n * 10}%", 12.sp, if (n == 0) Dim else Gold)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..10) Box(
                Modifier.weight(1f).height((10 + i * 2).dp).align(Alignment.Bottom)
                    .frame(if (i <= n) Leaf else Color(0xFF14100B), WoodEdge)
                    .clickable { onSet(if (i == 1 && n == 1) 0f else i / 10f) }
            )
        }
    }
}
