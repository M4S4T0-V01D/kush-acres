package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.model.Branch
import dev.m4s4t0.kushacres.model.Legacy
import dev.m4s4t0.kushacres.model.LegacyPerk
import dev.m4s4t0.kushacres.model.Progress
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.StartMap
import dev.m4s4t0.kushacres.model.Strains

/** The Legacy part of the Stats tab: the bonus so far, the perk tree, heirlooms, and selling the farm. */
@Composable
internal fun LegacySection() {
    Store.rev
    val f = Store.farm
    T("Legacy", 14.sp, Gold)
    Row2("Times sold", "${f.legacy}")
    Row2("Legacy bonus", "Prices +${25 * f.legacy}%, growth and XP +${10 * f.legacy}%", Leaf)
    Row2("Best level", "${f.bestLevel}")
    if (f.legacy > 0) Row2("This farm", f.startMap.title, Sky)
    if (f.heirlooms.isNotEmpty()) Row2("Heirlooms", f.heirlooms.mapNotNull { Strains[it]?.name }.joinToString(), Gold)
    if (f.legacy > 0 || f.canSellFarm()) {
        Spacer(Modifier.height(8.dp))
        val pts = Legacy.points(f)
        T(if (pts > 0) "Perk tree: $pts point${if (pts == 1) "" else "s"} to spend" else "Perk tree", 12.sp, if (pts > 0) Leaf else Cream)
        T("Every sale gives a point. Each perk needs the one above it.", 10.sp, Dim, bold = false)
        for (b in Branch.entries) {
            Spacer(Modifier.height(4.dp))
            T(b.title, 11.sp, Gold)
            for (p in LegacyPerk.entries.filter { it.branch == b }) PerkRow(p)
        }
    }
    Spacer(Modifier.height(8.dp))
    SellFarm()
}

@Composable
private fun PerkRow(p: LegacyPerk) {
    Store.rev
    val f = Store.farm
    val has = Legacy.has(f, p)
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T(p.title, 11.sp, if (has) Gold else Cream)
            T(p.desc, 9.sp, Dim, bold = false)
        }
        when {
            has -> T("✓", 14.sp, Gold)
            Legacy.canTake(f, p) -> Btn("Take", color = Leaf) {
                val r = Legacy.take(f, p)
                if (r == Result.OK) { Audio.play(Sfx.LEVEL_UP); Store.toast("${p.title}: ${p.desc}"); Store.changed() }
                report(r)
            }
            else -> T("Locked", 10.sp, Dim)
        }
    }
}

@Composable
private fun SellFarm() {
    Store.rev
    val f = Store.farm
    var confirm by remember { mutableStateOf(false) }
    var map by remember { mutableStateOf(StartMap.HOMESTEAD) }
    val keep = remember { mutableStateListOf<String>() }
    if (!f.canSellFarm()) {
        T("Reach level ${Progress.LEGACY_LEVEL} to sell the farm and start over with a permanent bonus.", 10.sp, Dim, bold = false)
        return
    }
    if (!confirm) {
        Btn("Sell the farm…", Modifier.fillMaxWidth(), color = Bad) {
            confirm = true; keep.clear(); keep += f.heirlooms.filter { id -> Legacy.eligible(f).any { it.id == id } }
        }
        return
    }
    T("Start over at level 1. You keep trophies, stats, hybrids, decor, paint and exotic seeds, every sale gets +25% for good, " +
        "and you earn a perk point.", 11.sp, Cream, bold = false)
    Spacer(Modifier.height(6.dp))
    T("Where to start", 12.sp, Gold)
    for (m in StartMap.entries) {
        val on = m == map
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T(m.title, 11.sp, if (on) Gold else Cream)
                T(m.desc, 9.sp, Dim, bold = false)
            }
            Btn(if (on) "Picked" else "Pick", color = if (on) Gold else WoodEdge) { map = m }
        }
    }
    Spacer(Modifier.height(6.dp))
    val slots = Legacy.slots(f)
    T("Heirlooms (${keep.size}/$slots)", 12.sp, Gold)
    T("Kept strains are unlocked from level 1 and yield ${Math.round((Legacy.HEIRLOOM_YIELD - 1) * 100)}% more.", 10.sp, Dim, bold = false)
    for (pair in Legacy.eligible(f).reversed().chunked(2)) Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (s in pair) {
            val on = s.id in keep
            Btn(s.name, Modifier.weight(1f), enabled = on || keep.size < slots, color = if (on) Gold else WoodEdge) {
                if (on) keep.remove(s.id) else if (keep.size < slots) keep += s.id
            }
        }
        if (pair.size == 1) Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Btn("Keep farming", Modifier.weight(1f), color = WoodEdge) { confirm = false }
        Btn("Sell it", Modifier.weight(1f), color = Bad) {
            confirm = false
            val r = f.sellFarm(Store.now(), map, keep.toList())
            if (r == Result.OK) { Audio.play(Sfx.LEVEL_UP); Store.toast("Fresh start at ${map.title}! Legacy ${f.legacy}: spend your perk point in Stats") }
            report(r)
        }
    }
}
