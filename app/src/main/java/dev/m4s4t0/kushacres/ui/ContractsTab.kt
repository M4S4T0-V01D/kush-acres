package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.model.Chains
import dev.m4s4t0.kushacres.model.Contract
import dev.m4s4t0.kushacres.model.Contracts
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.Towns
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx

/** The dispensary's Contracts tab: this town's reputation, the contracts in hand and today's offers. */
@Composable
internal fun ContractsTab() {
    val f = Store.farm
    Store.rev
    val now = Store.now()
    val town = f.site.def.id
    if (f.level < Contracts.LEVEL) {
        T("Contracts open at level ${Contracts.LEVEL}.", 14.sp, Dim, Modifier.padding(12.dp))
        T("Buyers here want big loads, rush jobs and special orders, and pay well over market for them.", 11.sp, Dim, bold = false)
        return
    }
    if (f.lapsed > 0) {
        T("${f.lapsed} contract${if (f.lapsed > 1) "s" else ""} ran out of time. What you shipped was paid at market.", 11.sp, Bad, bold = false)
        f.lapsed = 0
        Spacer(Modifier.height(4.dp))
    }
    Column(Modifier.vScroll(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RepHeader(town)
        val extra = f.deals.size - Contracts.held(f)
        T("In hand (${Contracts.held(f)}/${Contracts.taken(f)}${if (extra > 0) " + $extra special" else ""})", 13.sp, Cream)
        if (f.deals.isEmpty()) T("Nothing taken yet. Pick one from today's offers below.", 11.sp, Dim, bold = false)
        for (c in f.deals.toList()) Taken(c, now)
        Spacer(Modifier.height(4.dp))
        T("Today's offers in ${Towns[town].town}", 13.sp, Cream)
        val offers = Contracts.offers(f, town, now)
        if (offers.isEmpty()) T("That's everything for today. New offers come tomorrow.", 11.sp, Dim, bold = false)
        offers.forEachIndexed { i, c -> Offer(c, i, town, now) }
        T("Ship in parts from your stash; the Van ships for you. Running out of time costs reputation, and what you sent is paid at market.", 10.sp, Dim, bold = false)
    }
}

@Composable
private fun RepHeader(town: String) {
    Store.rev
    val f = Store.farm
    val rep = Contracts.rep(f, town)
    val tier = Contracts.tier(rep)
    val next = Contracts.TIERS.getOrNull(tier + 1)
    Column(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Px(Sprites.star, 18.dp)
            Spacer(Modifier.width(6.dp))
            T("${Towns[town].town}: ${Contracts.TIER_NAMES[tier]}", 13.sp, Gold, Modifier.weight(1f))
            T("+${(Contracts.payMult(tier) * 100 - 100).toInt()}% pay", 11.sp, Leaf)
        }
        if (next != null) {
            Spacer(Modifier.height(3.dp))
            Bar((rep - Contracts.TIERS[tier]).toFloat() / (next - Contracts.TIERS[tier]), Gold)
            T("$rep / $next rep to ${Contracts.TIER_NAMES[tier + 1]}", 10.sp, Dim, bold = false)
        } else T("$rep rep: they'd do anything for you", 10.sp, Dim, bold = false)
        val done = Chains.done(f, town)
        val keep = Chains.keepsake(town)
        Spacer(Modifier.height(3.dp))
        when {
            done >= Chains.STEPS -> T("Story done. Keepsake: ${keep.name} (${keep.desc})", 10.sp, Leaf, bold = false)
            tier < Chains.tierFor(done) -> T("Story ${done}/${Chains.STEPS}: the next chapter opens at ${Contracts.TIER_NAMES[Chains.tierFor(done)]}. " +
                "Finish it for the ${keep.name}", 10.sp, Dim, bold = false)
            else -> T("Story ${done}/${Chains.STEPS}: a chapter is waiting. Finish the story for the ${keep.name}", 10.sp, Cream, bold = false)
        }
        if (tier < Contracts.WEEKLY_TIER) T("At ${Contracts.TIER_NAMES[Contracts.WEEKLY_TIER]} they post a big standing order every week", 10.sp, Dim, bold = false)
    }
}

@Composable
private fun Needs(c: Contract, showGot: Boolean) {
    for (n in c.needs) {
        val s = Strains[n.what]
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (s != null) Px(Sprites.strainIcon(s), 18.dp) else Px(Sprites.jar, 18.dp)
            Spacer(Modifier.width(5.dp))
            val u = Contracts.unit(n)
            T(Contracts.label(n), 11.sp, Cream, Modifier.weight(1f), bold = false)
            T(if (showGot) "${n.got}/${n.amount}$u" else "${n.amount}$u", 11.sp, if (n.done) Leaf else Cream)
        }
    }
}

@Composable
private fun Taken(c: Contract, now: Long) {
    Store.rev
    val f = Store.farm
    val left = (c.until - now) / 1000
    Column(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(c)
            T("${c.title} for ${c.buyer}", 12.sp, Gold, Modifier.weight(1f))
            Px(Sprites.clock, 14.dp)
            T(" ${dur(left.coerceAtLeast(0))}", 11.sp, if (left < 3600) Bad else Cream)
        }
        T("${Towns[c.town].town} · pays ~${money(Contracts.estimate(Store.farm, c))}", 10.sp, Dim, bold = false)
        Needs(c, true)
        Spacer(Modifier.height(3.dp))
        Bar(c.share, Leaf)
        Spacer(Modifier.height(4.dp))
        Row {
            Btn("Ship", Modifier.weight(1f), enabled = Contracts.canShip(f, c), color = Leaf, icon = Sprites.bag) {
                f.lastDeal = null
                val sent = Contracts.ship(f, c, Store.now())
                val pay = f.lastDeal
                if (pay != null) {
                    Audio.play(Sfx.UNLOCK)
                    Store.toast("Contract filled: ${money(pay.cash)}, ${pay.bucks} Pot Bucks" +
                        (if (pay.token != null) ", ${pay.tokens} ${pay.token.tokens.lowercase()}" else "") + ", +${pay.rep} rep" + if (pay.tierUp) ". Reputation up!" else "")
                    f.lastDeal = null
                } else if (sent > 0) Store.toast("Shipped $sent${if (c.needs.any { it.what.startsWith("goods:") }) "" else "g"}")
                Store.changed()
            }
            Spacer(Modifier.width(4.dp))
            Btn("Drop", color = Bad) {
                Contracts.drop(f, c); Store.toast("Dropped. ${Towns[c.town].town} won't forget it soon (-${Contracts.LAPSE_REP / 2} rep)"); Store.changed()
            }
        }
    }
}

@Composable
private fun Offer(c: Contract, i: Int, town: String, now: Long) {
    Store.rev
    val f = Store.farm
    Column(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(c)
            Column(Modifier.weight(1f)) {
                T("${c.title} for ${c.buyer}", 12.sp, Gold)
                T(Contracts.about(c), 10.sp, if (c.chapter >= 0) Cream else Dim, bold = false)
                T((if (c.weekly) "Until the week ends" else "${hours(c.hours)} to fill") + " · +${Contracts.gain(f, c)} rep", 10.sp, Dim, bold = false)
            }
            T("~${money(Contracts.estimate(f, c))}", 12.sp, Gold)
        }
        Needs(c, false)
        Spacer(Modifier.height(4.dp))
        val full = !Contracts.canTake(f, c)
        Btn(if (!full) "Take it" else if (c.event) "One event at a time" else "Hands full", Modifier.fillMaxWidth(), enabled = !full, color = Gold, icon = Sprites.coin) {
            if (Contracts.take(f, town, i, Store.now()) == dev.m4s4t0.kushacres.model.Result.OK)
                Store.toast(if (c.weekly) "Taken. Due by the end of the week" else "Taken. ${hours(c.hours)} on the clock")
            Store.changed()
        }
    }
}

/** A small mark for the special contracts: a scroll for story chapters, a star for weekly orders, a gift for events. */
@Composable
private fun Badge(c: Contract) {
    val icon = when {
        c.chapter >= 0 -> Sprites.scroll
        c.weekly -> Sprites.star
        c.event -> Sprites.gift
        else -> return
    }
    Px(icon, 16.dp)
    Spacer(Modifier.width(4.dp))
}

private fun hours(h: Int) = if (h >= 48 && h % 24 == 0) "${h / 24} days" else "${h}h"
