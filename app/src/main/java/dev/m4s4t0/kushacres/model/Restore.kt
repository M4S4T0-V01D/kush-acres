package dev.m4s4t0.kushacres.model

import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** What one landmark step paid. */
class StepPay(val cash: Long, val xp: Long, val finished: Boolean)

/**
 * Landmark quests, from [LEVEL]: every farm's landmark can be done up in [STEPS] steps, each paid for with a load of
 * bud or goods shipped in parts like a contract. Each step pays cash and XP; finishing all of them lights the
 * landmark up for good and makes that farm's plants yield [YIELD] more.
 */
object Restore {
    const val LEVEL = 4
    const val STEPS = 3
    const val YIELD = 0.1
    const val PAY = 1.8

    private val TITLES = listOf("Clear the weeds", "Patch it up", "Grand reopening")
    private val LINES = listOf(
        "The %s is buried in brambles. Pay a crew in bud to clear it.",
        "Fix the boards, paint and lamps. The builders want paying in kind.",
        "Throw a party at the %s for the whole town. Bring the good stuff.",
    )

    fun step(f: Farm, site: String) = f.marks[site] ?: 0
    fun done(f: Farm, site: String) = step(f, site) >= STEPS
    fun title(i: Int) = TITLES[i.coerceIn(0, STEPS - 1)]
    fun line(i: Int, name: String) = LINES[i.coerceIn(0, STEPS - 1)].format(name)

    private fun round10(x: Double) = (ceil(x / 10) * 10).roundToInt().coerceAtLeast(10)

    /** The current step's need on [site], made the first time it's asked for. */
    fun need(f: Farm, site: String): Need? {
        if (f.level < LEVEL || done(f, site)) return null
        f.markNeeds[site]?.let { return it }
        val i = step(f, site)
        val ladder = Strains.ladder.filter { f.owned(it) }.takeLast(3)
        if (ladder.isEmpty()) return null
        val r = f.rng
        val n = when (i) {
            0 -> ladder[r.nextInt(ladder.size)].let { Need(it.id, round10(Contracts.perHour(f, it) * 2.0)) }
            1 -> Facility.entries.filter { fac -> var k = 0; f.eachSite { k += f.count(fac) }; k > 0 }.randomOrNull(r)
                ?.let { Need("goods:${it.name}", max(4, (it.units * 60.0 / it.minutes * 2.5).roundToInt())) }
                ?: ladder.last().let { Need(it.id, round10(Contracts.perHour(f, it) * 3.0)) }
            else -> (Strains.all.filter { it.site == site && f.owned(it) }.maxByOrNull { it.level } ?: ladder.last())
                .let { Need(it.id, round10(Contracts.perHour(f, it) * 4.0)) }
        }
        f.markNeeds[site] = n
        return n
    }

    /** What's in the stash or goods towards the current step. */
    fun have(f: Farm, site: String) = need(f, site)?.let { n -> Contracts.sources(f, n).sumOf { it.second } } ?: 0

    /** Ships what there is towards [site]'s step; returns the pay once the step is complete, otherwise null. */
    fun ship(f: Farm, site: String): StepPay? {
        val n = need(f, site) ?: return null
        val goods = n.what.startsWith("goods:")
        for ((key, have) in Contracts.sources(f, n)) {
            if (n.done) break
            val k = min(have, n.left)
            val box = if (goods) f.goods else f.stash
            if (have == k) box.remove(key) else box[key] = have - k
            f.markValue[site] = (f.markValue[site] ?: 0.0) + k * (if (goods) f.goodsPrice(key) else (Strains[key]?.price ?: 0L) * f.priceMult())
            n.got += k
        }
        if (!n.done) return null
        val i = step(f, site)
        val cash = max(1L, nice((f.markValue[site] ?: 0.0) * PAY))
        val xp = max(1L, (Farm.xpNeed(f.level) * 0.03 * (i + 1)).roundToLong())
        f.marks[site] = i + 1
        f.markNeeds.remove(site); f.markValue.remove(site)
        f.earnCash(cash); f.events += FarmEvent.Cash(cash)
        f.addXp(xp)
        if (i == STEPS - 1) f.potBucks += 10
        return StepPay(cash, xp, i + 1 >= STEPS)
    }

    // ---- Saving ----

    fun save(f: Farm): JSONObject = JSONObject().apply {
        for (site in f.marks.keys + f.markNeeds.keys) {
            val o = JSONObject().put("s", f.marks[site] ?: 0).put("v", f.markValue[site] ?: 0.0)
            f.markNeeds[site]?.let { o.put("w", it.what).put("a", it.amount).put("g", it.got) }
            put(site, o)
        }
    }

    fun load(f: Farm, o: JSONObject?) {
        f.marks.clear(); f.markNeeds.clear(); f.markValue.clear()
        if (o == null) return
        o.keys().forEach { site ->
            val j = o.getJSONObject(site)
            j.optInt("s").takeIf { it > 0 }?.let { f.marks[site] = it.coerceAtMost(STEPS) }
            if (j.has("w")) f.markNeeds[site] = Need(j.getString("w"), j.optInt("a", 10), j.optInt("g"))
            j.optDouble("v", 0.0).takeIf { it > 0 }?.let { f.markValue[site] = it }
        }
    }
}
