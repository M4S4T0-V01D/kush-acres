package dev.m4s4t0.kushacres.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * The kinds of contract a town's buyers offer. [work] is roughly how many hours of the farm's output one asks for,
 * [hours] how long you get once you take it, [mult] how far over market it pays and [rep] the reputation it earns.
 */
enum class Deal(val title: String, val desc: String, val work: Double, val hours: Int, val mult: Double, val rep: Int) {
    BULK("Bulk", "A big load with plenty of time", 8.0, 48, 1.7, 30),
    RUSH("Rush", "Small, but they need it fast", 1.5, 3, 2.6, 40),
    SPECIAL("Specialty", "Any strain that fits what they want", 3.0, 24, 2.2, 35),
    KITCHEN("Kitchen", "Cooked goods from your facilities", 3.0, 24, 1.9, 30),
    PAIR("Two-farm", "Local strains from two of your farms", 3.0, 36, 2.4, 50);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
    }
}

/**
 * One thing a contract wants: a strain id, any strain of a Cup class ("class:FAST") or a facility's goods made from
 * any strain ("goods:ROLLER"). Bud is counted in grams, goods in units.
 */
class Need(val what: String, val amount: Int, var got: Int = 0) {
    val done get() = got >= amount
    val left get() = max(0, amount - got)
}

class Contract(val kind: Deal, val buyer: String, val town: String, val needs: List<Need>, val mult: Double) {
    /** When it runs out, once taken (0 while it's still an offer). */
    var until = 0L
    /** What the shipped bud and goods are worth at market; the contract pays [mult] times this when it's filled. */
    var value = 0.0
    val done get() = needs.all { it.done }
    val share: Float get() = needs.sumOf { min(it.got, it.amount) }.toFloat() / needs.sumOf { it.amount }.coerceAtLeast(1)
}

/** What filling a contract paid. */
class DealPay(val cash: Long, val bucks: Long, val xp: Long, val rep: Int, val tierUp: Boolean)

/**
 * Farm contracts, offered at each town's dispensary from [LEVEL]: [OFFERS] a day per town, up to [taken] at a time
 * across all towns. Ship in parts from the stash; when everything's in, the buyer pays well over market and the
 * town's reputation goes up. Running out of time only costs reputation: what was shipped is paid at market.
 */
object Contracts {
    const val LEVEL = 8
    const val OFFERS = 3
    const val LAPSE_REP = 30

    /** Reputation each tier starts at, and its name. */
    val TIERS = intArrayOf(0, 100, 300, 700, 1500, 3000)
    val TIER_NAMES = listOf("Stranger", "Known", "Trusted", "Partner", "Regular", "Legend")

    fun taken(f: Farm) = if (f.level >= 40) 3 else 2
    fun rep(f: Farm, town: String) = f.rep[town] ?: 0
    fun tier(rep: Int) = TIERS.indexOfLast { rep >= it }
    fun tier(f: Farm, town: String) = tier(rep(f, town))
    /** Better pay and bigger loads as a town gets to know you. */
    fun payMult(tier: Int) = 1 + 0.08 * tier
    fun sizeMult(tier: Int) = 1 + 0.15 * tier

    /** Today's offers in [town], made fresh each day. */
    fun offers(f: Farm, town: String, now: Long): List<Contract> {
        val day = localDay(now)
        if (f.offerDay[town] != day) {
            f.offerDay[town] = day
            f.offers[town] = if (f.level < LEVEL) ArrayList() else make(f, town)
        }
        return f.offers[town] ?: emptyList()
    }

    /** Grams of [s] the farm makes in an hour, roughly, if a share of its plots grow it. */
    fun perHour(f: Farm, s: Strain): Double {
        var plots = 0
        f.eachSite { plots += f.count(Kind.PLOT) }
        return max(4, plots) * 0.4 * s.yield * f.yieldMult() * 3600.0 / s.seconds
    }

    private fun round10(x: Double) = (ceil(x / 10) * 10).roundToInt().coerceAtLeast(10)

    private fun make(f: Farm, town: String): ArrayList<Contract> {
        val r = f.rng
        val size = sizeMult(tier(f, town))
        val pay = payMult(tier(f, town))
        val ladder = Strains.ladder.filter { f.owned(it) }.takeLast(5)
        val classes = CupClass.entries.filter { c -> c != CupClass.OPEN && fitting(f, c).isNotEmpty() }
        val kitchens = Facility.entries.filter { fac -> var n = 0; f.eachSite { n += f.count(fac) }; n > 0 }
        val locals = f.sites.mapNotNull { st -> Strains.all.filter { it.site == st.def.id && f.owned(it) }.maxByOrNull { it.level } }
        val kinds = buildList {
            add(Deal.BULK); add(Deal.RUSH)
            if (classes.isNotEmpty()) add(Deal.SPECIAL)
            if (kitchens.isNotEmpty()) add(Deal.KITCHEN)
            if (locals.size >= 2) add(Deal.PAIR)
        }.shuffled(r)
        // Always something different to pick from: one of each kind before any repeats.
        val out = ArrayList<Contract>()
        val buyers = Farm.BUYERS.shuffled(r)
        for (i in 0 until OFFERS) {
            val k = kinds[i % kinds.size]
            val needs = when (k) {
                Deal.BULK, Deal.RUSH -> { val s = ladder[r.nextInt(ladder.size)]; listOf(Need(s.id, round10(perHour(f, s) * k.work * size))) }
                Deal.SPECIAL -> {
                    val c = classes[r.nextInt(classes.size)]
                    val best = fitting(f, c).maxBy { it.level }
                    listOf(Need("class:${c.name}", round10(perHour(f, best) * k.work * size)))
                }
                Deal.KITCHEN -> {
                    val fac = kitchens[r.nextInt(kitchens.size)]
                    var n = 0; f.eachSite { n += f.count(fac) }
                    listOf(Need("goods:${fac.name}", max(2, (fac.units * 60.0 / fac.minutes * n * k.work * 0.5 * size).roundToInt())))
                }
                Deal.PAIR -> locals.shuffled(r).take(2).map { Need(it.id, round10(perHour(f, it) * k.work / 2 * size)) }
            }
            out += Contract(k, buyers[i % buyers.size], town, needs, k.mult * pay * (0.95 + 0.1 * r.nextDouble()))
        }
        return out
    }

    private fun cupClass(id: String) = CupClass.entries.firstOrNull { it.name == id }

    /** Strains of class [c] you can grow somewhere. */
    fun fitting(f: Farm, c: CupClass) = Strains.all.filter { it.holiday == null && f.owned(it) && c.fits(it) }

    /** What a need is called on screen. */
    fun label(n: Need): String = when {
        n.what.startsWith("class:") -> "Any " + (cupClass(n.what.removePrefix("class:"))?.desc?.lowercase() ?: "strain")
        n.what.startsWith("goods:") -> "Any " + (Facility.of(n.what.removePrefix("goods:"))?.product?.lowercase() ?: "goods")
        else -> Strains[n.what]?.name ?: n.what
    }
    fun unit(n: Need) = if (n.what.startsWith("goods:")) "" else "g"

    /** Roughly what the contract pays when filled, at today's prices. */
    fun estimate(f: Farm, c: Contract): Long = nice(c.mult * c.needs.sumOf { n ->
        when {
            n.what.startsWith("class:") -> n.amount * (cupClass(n.what.removePrefix("class:"))?.let { cl -> fitting(f, cl).maxOfOrNull { it.price } } ?: 0L) * f.priceMult()
            n.what.startsWith("goods:") -> {
                val fac = Facility.of(n.what.removePrefix("goods:")); val s = Strains.ladder.lastOrNull { f.owned(it) }
                if (fac == null || s == null) 0.0 else n.amount * f.goodsPrice(f.goodsKey(fac, s.id))
            }
            else -> n.amount * (Strains[n.what]?.price ?: 0L) * f.priceMult()
        }
    })

    fun take(f: Farm, town: String, i: Int, now: Long): Result {
        if (f.level < LEVEL) return Result.LOCKED
        val list = f.offers[town] ?: return Result.BLOCKED
        val c = list.getOrNull(i) ?: return Result.BLOCKED
        if (f.deals.size >= taken(f)) return Result.LIMIT
        list.removeAt(i)
        c.until = now + c.kind.hours * 3_600_000L
        f.deals += c
        return Result.OK
    }

    /** Can anything in the stash or goods go towards [c] right now? */
    fun canShip(f: Farm, c: Contract) = c.needs.any { n -> !n.done && sources(f, n).isNotEmpty() }

    /** Where a need can be filled from: (stash or goods key, amount there), the dearest first. */
    private fun sources(f: Farm, n: Need): List<Pair<String, Int>> = when {
        n.what.startsWith("class:") -> {
            val c = cupClass(n.what.removePrefix("class:")) ?: return emptyList()
            f.stash.entries.filter { e -> e.value > 0 && Strains[e.key]?.let { it.holiday == null && c.fits(it) } == true }
                .sortedByDescending { Strains[it.key]!!.price }.map { it.key to it.value }
        }
        n.what.startsWith("goods:") -> {
            val pre = n.what.removePrefix("goods:") + ":"
            f.goods.entries.filter { it.key.startsWith(pre) && it.value > 0 }.sortedByDescending { f.goodsPrice(it.key) }.map { it.key to it.value }
        }
        else -> (f.stash[n.what] ?: 0).let { if (it > 0) listOf(n.what to it) else emptyList() }
    }

    /**
     * Ships whatever the stash and goods have towards [c]. Returns how much went (grams and units together); when the
     * contract is filled it pays out and leaves the list (see [complete]).
     */
    fun ship(f: Farm, c: Contract, now: Long, auto: Boolean = false): Int {
        var sent = 0
        for (n in c.needs) {
            for ((key, have) in sources(f, n)) {
                if (n.done) break
                val k = min(have, n.left)
                val goods = n.what.startsWith("goods:")
                val box = if (goods) f.goods else f.stash
                if (have == k) box.remove(key) else box[key] = have - k
                c.value += k * (if (goods) f.goodsPrice(key) else (Strains[key]?.price ?: 0L) * f.priceMult())
                n.got += k; sent += k
            }
        }
        if (c.done) complete(f, c, auto)
        return sent
    }

    private fun complete(f: Farm, c: Contract, auto: Boolean): DealPay {
        f.deals.remove(c)
        val before = tier(f, c.town)
        val cash = nice(c.value * c.mult)
        val work = c.kind.work
        val bucks = 1L + (work / 2).roundToLong()
        val xp = max(1L, (Farm.xpNeed(f.level) * 0.012 * work).roundToLong())
        f.rep[c.town] = rep(f, c.town) + c.kind.rep
        f.earnCash(cash); f.potBucks += bucks
        f.events += FarmEvent.Cash(cash)
        f.bump(Stat.CONTRACTS)
        if (auto) f.away.deals++
        f.addXp(xp)
        val pay = DealPay(cash, bucks, xp, c.kind.rep, tier(f, c.town) > before)
        f.lastDeal = pay
        return pay
    }

    /** Lets a taken contract go now: like running out of time, but only half the reputation lost. */
    fun drop(f: Farm, c: Contract) = lapse(f, c, LAPSE_REP / 2)

    private fun lapse(f: Farm, c: Contract, rep: Int) {
        f.deals.remove(c)
        val paid = c.value.roundToLong()
        if (paid > 0) { f.earnCash(paid); f.events += FarmEvent.Cash(paid) }
        f.rep[c.town] = max(0, rep(f, c.town) - rep)
    }

    /** Called every tick: contracts past their time lapse. */
    fun expire(f: Farm, now: Long) {
        for (c in f.deals.filter { now >= it.until }) { lapse(f, c, LAPSE_REP); f.lapsed++ }
    }

    // ---- Saving ----

    private fun toJson(c: Contract) = JSONObject().put("k", c.kind.name).put("b", c.buyer).put("t", c.town).put("m", c.mult)
        .put("u", c.until).put("v", c.value)
        .put("n", JSONArray().apply { c.needs.forEach { put(JSONObject().put("w", it.what).put("a", it.amount).put("g", it.got)) } })

    private fun fromJson(o: JSONObject): Contract? {
        val k = Deal.of(o.optString("k")) ?: return null
        val arr = o.optJSONArray("n") ?: return null
        val needs = (0 until arr.length()).map { arr.getJSONObject(it).let { n -> Need(n.getString("w"), n.getInt("a"), n.optInt("g")) } }
        return Contract(k, o.optString("b"), o.optString("t", "home"), needs, o.optDouble("m", k.mult)).apply {
            until = o.optLong("u"); value = o.optDouble("v", 0.0)
        }
    }

    fun save(f: Farm): JSONObject = JSONObject()
        .put("deals", JSONArray().apply { f.deals.forEach { put(toJson(it)) } })
        .put("rep", JSONObject().apply { f.rep.forEach { (k, v) -> put(k, v) } })
        .put("offers", JSONObject().apply {
            f.offers.forEach { (town, list) -> put(town, JSONObject().put("day", f.offerDay[town] ?: -1L).put("list", JSONArray().apply { list.forEach { put(toJson(it)) } })) }
        })

    fun load(f: Farm, o: JSONObject?) {
        f.deals.clear(); f.rep.clear(); f.offers.clear(); f.offerDay.clear()
        if (o == null) return
        o.optJSONArray("deals")?.let { a -> for (i in 0 until a.length()) fromJson(a.getJSONObject(i))?.let { f.deals += it } }
        o.optJSONObject("rep")?.let { r -> r.keys().forEach { f.rep[it] = r.optInt(it) } }
        o.optJSONObject("offers")?.let { all ->
            all.keys().forEach { town ->
                val t = all.getJSONObject(town)
                f.offerDay[town] = t.optLong("day", -1)
                val a = t.optJSONArray("list")
                f.offers[town] = ArrayList<Contract>().apply { if (a != null) for (i in 0 until a.length()) fromJson(a.getJSONObject(i))?.let { add(it) } }
            }
        }
    }
}
