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

/**
 * A contract. [tag] marks the special ones, which don't count towards [Contracts.taken]: "story:N" (chapter N of the
 * town's story), "weekly" (a standing order, due at the end of the week) and "event:ID" (a holiday id or a [Season] name).
 */
class Contract(val kind: Deal, val buyer: String, val town: String, val needs: List<Need>, val mult: Double, val tag: String = "") {
    /** When it runs out, once taken (0 while it's still an offer). */
    var until = 0L
    /** What the shipped bud and goods are worth at market; the contract pays [mult] times this when it's filled. */
    var value = 0.0
    val done get() = needs.all { it.done }
    val share: Float get() = needs.sumOf { min(it.got, it.amount) }.toFloat() / needs.sumOf { it.amount }.coerceAtLeast(1)
    val chapter: Int get() = if (tag.startsWith("story:")) tag.removePrefix("story:").toIntOrNull() ?: 0 else -1
    val weekly get() = tag == "weekly"
    val event get() = tag.startsWith("event:")
    val holiday: Holiday? get() = if (event) Holiday[tag.removePrefix("event:")] else null
    val season: Season? get() = if (event) Season.entries.firstOrNull { it.name == tag.removePrefix("event:") } else null
    /** What it's called on screen. */
    val title: String get() = when {
        chapter >= 0 -> Chains.chapter(town, chapter).title
        weekly -> "Standing order"
        holiday != null -> "${holiday!!.title} special"
        season != null -> Contracts.FESTIVALS[season!!.ordinal]
        else -> kind.title
    }
    val hours: Int get() = when {
        chapter >= 0 -> max(kind.hours, 6)
        else -> kind.hours
    }
}

/** What filling a contract paid. */
class DealPay(val cash: Long, val bucks: Long, val xp: Long, val rep: Int, val tierUp: Boolean, val tokens: Long = 0, val token: Holiday? = null,
    val keepsake: Keepsake? = null)

/**
 * Farm contracts, offered at each town's dispensary from [LEVEL]: [OFFERS] a day per town, up to [taken] at a time
 * across all towns. Ship in parts from the stash; when everything's in, the buyer pays well over market and the
 * town's reputation goes up. Running out of time only costs reputation: what was shipped is paid at market.
 */
object Contracts {
    const val LEVEL = 8
    const val OFFERS = 3
    const val LAPSE_REP = 30
    /** The tier a town starts posting a standing weekly order at, and how big one is. */
    const val WEEKLY_TIER = 4
    const val WEEKLY_WORK = 16.0
    val FESTIVALS = listOf("Spring Fair", "Summer Fest", "Harvest Fair", "Winter Warmer")

    /** Reputation each tier starts at, and its name. */
    val TIERS = intArrayOf(0, 100, 300, 700, 1500, 3000)
    val TIER_NAMES = listOf("Stranger", "Known", "Trusted", "Partner", "Regular", "Legend")

    fun taken(f: Farm) = if (f.level >= 40) 3 else 2
    /** Contracts in hand that count towards [taken]; story, weekly and event ones don't. */
    fun held(f: Farm) = f.deals.count { it.tag.isEmpty() }
    fun week(now: Long) = localDay(now) / 7
    /** When the local week ends. */
    fun weekEnd(now: Long): Long = now + (7 - Math.floorMod(localDay(now), 7L)) * 86_400_000L - Math.floorMod(now + java.util.TimeZone.getDefault().getOffset(now), 86_400_000L)
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
            f.offers[town] = if (f.level < LEVEL) ArrayList() else make(f, town, now)
        }
        val list = f.offers[town] ?: return emptyList()
        // The next chapter turns up as soon as the town trusts you enough, not just at the start of a day.
        val ch = Chains.done(f, town)
        if (f.level >= LEVEL && ch < Chains.STEPS && tier(f, town) >= Chains.tierFor(ch) &&
            list.none { it.chapter >= 0 } && f.deals.none { it.town == town && it.chapter >= 0 }) list.add(0, story(f, town, ch))
        return list
    }

    /** Grams of [s] the farm makes in an hour, roughly, if a share of its plots grow it. */
    fun perHour(f: Farm, s: Strain): Double {
        var plots = 0
        f.eachSite { plots += f.count(Kind.PLOT) }
        return max(4, plots) * 0.4 * s.yield * f.yieldMult() * 3600.0 / s.seconds
    }

    private fun round10(x: Double) = (ceil(x / 10) * 10).roundToInt().coerceAtLeast(10)

    private fun ladder(f: Farm) = Strains.ladder.filter { f.owned(it) }.takeLast(5)
    private fun classes(f: Farm) = CupClass.entries.filter { c -> c != CupClass.OPEN && fitting(f, c).isNotEmpty() }
    private fun kitchens(f: Farm) = Facility.entries.filter { fac -> var n = 0; f.eachSite { n += f.count(fac) }; n > 0 }
    private fun locals(f: Farm) = f.sites.mapNotNull { st -> Strains.all.filter { it.site == st.def.id && f.owned(it) }.maxByOrNull { it.level } }
    private fun labs(f: Farm) = Strains.hybridList.filter { it.id in f.bred }

    /** What a contract of kind [k] wants, [work] hours of output scaled by [size]. */
    private fun needs(f: Farm, k: Deal, work: Double, size: Double): List<Need> {
        val r = f.rng
        val ladder = ladder(f)
        fun bulk() = ladder[r.nextInt(ladder.size)].let { s -> listOf(Need(s.id, round10(perHour(f, s) * work * size))) }
        return when (k) {
            Deal.BULK, Deal.RUSH -> bulk()
            Deal.SPECIAL -> {
                val classes = classes(f).ifEmpty { return bulk() }
                val c = classes[r.nextInt(classes.size)]
                val best = fitting(f, c).maxBy { it.level }
                listOf(Need("class:${c.name}", round10(perHour(f, best) * work * size)))
            }
            Deal.KITCHEN -> {
                val kitchens = kitchens(f).ifEmpty { return bulk() }
                val fac = kitchens[r.nextInt(kitchens.size)]
                var n = 0; f.eachSite { n += f.count(fac) }
                listOf(Need("goods:${fac.name}", max(2, (fac.units * 60.0 / fac.minutes * n * work * 0.5 * size).roundToInt())))
            }
            Deal.PAIR -> locals(f).takeIf { it.size >= 2 }?.shuffled(r)?.take(2)?.map { Need(it.id, round10(perHour(f, it) * work / 2 * size)) } ?: bulk()
        }
    }

    private fun make(f: Farm, town: String, now: Long): ArrayList<Contract> {
        val r = f.rng
        val size = sizeMult(tier(f, town))
        val pay = payMult(tier(f, town))
        val kinds = buildList {
            add(Deal.BULK); add(Deal.RUSH)
            if (classes(f).isNotEmpty()) add(Deal.SPECIAL)
            if (kitchens(f).isNotEmpty()) add(Deal.KITCHEN)
            if (locals(f).size >= 2) add(Deal.PAIR)
        }.shuffled(r)
        // Always something different to pick from: one of each kind before any repeats.
        val out = ArrayList<Contract>()
        val buyers = Farm.BUYERS.shuffled(r)
        for (i in 0 until OFFERS) {
            val k = kinds[i % kinds.size]
            out += Contract(k, buyers[i % buyers.size], town, needs(f, k, k.work, size), k.mult * pay * (0.95 + 0.1 * r.nextDouble()))
        }
        // One event contract a day: the holiday's while one is on, otherwise the season's fair.
        val ev = Holiday.at(now)?.id ?: Season.at(now).name
        val ek = kinds[r.nextInt(kinds.size)]
        out += Contract(ek, buyers[OFFERS % buyers.size], town, needs(f, ek, ek.work, size), ek.mult * pay * 1.1, "event:$ev")
        // Regulars and legends post one big standing order a week.
        if (tier(f, town) >= WEEKLY_TIER && f.weekly[town] != week(now) && f.deals.none { it.town == town && it.weekly })
            out += Contract(Deal.BULK, Towns[town].names[1], town, needs(f, Deal.BULK, WEEKLY_WORK, size), 2.2 * pay, "weekly")
        return out
    }

    /** Chapter [i] of [town]'s story. */
    private fun story(f: Farm, town: String, i: Int): Contract {
        val ch = Chains.chapter(town, i)
        val size = sizeMult(tier(f, town))
        val needs = when (ch.want) {
            Want.LOCAL -> Strains.all.filter { it.site == town && f.owned(it) }.maxByOrNull { it.level }
                ?.let { listOf(Need(it.id, round10(perHour(f, it) * ch.work * size))) }
            Want.LAB -> labs(f).maxByOrNull { it.price }?.let { listOf(Need("lab:any", round10(perHour(f, it) * ch.work * size))) }
            else -> null
        } ?: needs(f, ch.want.deal, ch.work, size)
        return Contract(ch.want.deal, Chains.who(town), town, needs, ch.want.deal.mult * payMult(tier(f, town)) * Chains.PAY, "story:$i")
    }

    private fun cupClass(id: String) = CupClass.entries.firstOrNull { it.name == id }

    /** Strains of class [c] you can grow somewhere. */
    fun fitting(f: Farm, c: CupClass) = Strains.all.filter { it.holiday == null && f.owned(it) && c.fits(it) }

    /** What a need is called on screen. */
    fun label(n: Need): String = when {
        n.what.startsWith("class:") -> "Any " + (cupClass(n.what.removePrefix("class:"))?.desc?.lowercase() ?: "strain")
        n.what.startsWith("goods:") -> "Any " + (Facility.of(n.what.removePrefix("goods:"))?.product?.lowercase() ?: "goods")
        n.what.startsWith("lab:") -> "Any lab strain"
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
            n.what.startsWith("lab:") -> n.amount * (labs(f).maxOfOrNull { it.price } ?: 0L) * f.priceMult()
            else -> n.amount * (Strains[n.what]?.price ?: 0L) * f.priceMult()
        }
    })

    fun take(f: Farm, town: String, i: Int, now: Long): Result {
        if (f.level < LEVEL) return Result.LOCKED
        val list = f.offers[town] ?: return Result.BLOCKED
        val c = list.getOrNull(i) ?: return Result.BLOCKED
        if (!canTake(f, c)) return Result.LIMIT
        list.removeAt(i)
        c.until = if (c.weekly) weekEnd(now) else now + c.hours * 3_600_000L
        if (c.weekly) f.weekly[town] = week(now)
        f.deals += c
        return Result.OK
    }

    /** Reputation [c] earns when filled. */
    private fun base(c: Contract) = (if (c.weekly) 80 else if (c.chapter >= 0) 50 else c.kind.rep)
    /** What [c] earns [f] in reputation, with the Known Face perk. */
    fun gain(f: Farm, c: Contract) = Math.round(base(c) * Legacy.mult(f, LegacyPerk.KNOWN_FACE, 0.25)).toInt()

    /** The line under a contract's title. */
    fun about(c: Contract): String = when {
        c.chapter >= 0 -> Chains.line(c.town, c.chapter)
        c.weekly -> "A big regular order for a trusted supplier, due by the end of the week"
        c.holiday != null -> "${c.kind.desc}. Pays ${c.holiday!!.tokens.lowercase()} too"
        c.season != null -> "${c.kind.desc}. +${Chains.BUCKS} extra Pot Bucks"
        else -> c.kind.desc
    }

    /** Room in hand for [c]: plain ones up to [taken], one event at a time; story and weekly ones always fit. */
    fun canTake(f: Farm, c: Contract) = when {
        c.tag.isEmpty() -> held(f) < taken(f)
        c.event -> f.deals.none { it.event }
        else -> true
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
        n.what.startsWith("lab:") -> f.stash.entries.filter { e -> e.value > 0 && Strains[e.key]?.hybrid == true }
            .sortedByDescending { Strains[it.key]!!.price }.map { it.key to it.value }
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
        val work = if (c.weekly) WEEKLY_WORK else c.kind.work
        var bucks = 1L + (work / 2).roundToLong()
        if (c.chapter >= 0 || c.season != null) bucks += Chains.BUCKS
        val xp = max(1L, (Farm.xpNeed(f.level) * 0.012 * work).roundToLong())
        val gain = gain(f, c)
        f.rep[c.town] = rep(f, c.town) + gain
        f.earnCash(cash); f.potBucks += bucks
        f.events += FarmEvent.Cash(cash)
        f.bump(Stat.CONTRACTS)
        if (auto) f.away.deals++
        f.addXp(xp)
        val h = c.holiday
        val tokens = if (h == null) 0L else 2 * f.orderTokens(c.needs.mapNotNull { Strains[it.what]?.level }.maxOrNull() ?: f.level, c.needs.sumOf { it.amount })
        f.addTokens(h, tokens)
        var keep: Keepsake? = null
        if (c.chapter >= 0) {
            f.chains[c.town] = max(Chains.done(f, c.town), c.chapter + 1)
            f.bump(Stat.CHAPTERS)
            if (Chains.done(f, c.town) >= Chains.STEPS) {
                keep = Chains.keepsake(c.town)
                f.events += FarmEvent.Keepsake(Towns[c.town].town, keep)
            }
        }
        val pay = DealPay(cash, bucks, xp, gain, tier(f, c.town) > before, tokens, h, keep)
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
        .put("u", c.until).put("v", c.value).put("g", c.tag)
        .put("n", JSONArray().apply { c.needs.forEach { put(JSONObject().put("w", it.what).put("a", it.amount).put("g", it.got)) } })

    private fun fromJson(o: JSONObject): Contract? {
        val k = Deal.of(o.optString("k")) ?: return null
        val arr = o.optJSONArray("n") ?: return null
        val needs = (0 until arr.length()).map { arr.getJSONObject(it).let { n -> Need(n.getString("w"), n.getInt("a"), n.optInt("g")) } }
        return Contract(k, o.optString("b"), o.optString("t", "home"), needs, o.optDouble("m", k.mult), o.optString("g", "")).apply {
            until = o.optLong("u"); value = o.optDouble("v", 0.0)
        }
    }

    fun save(f: Farm): JSONObject = JSONObject()
        .put("deals", JSONArray().apply { f.deals.forEach { put(toJson(it)) } })
        .put("rep", JSONObject().apply { f.rep.forEach { (k, v) -> put(k, v) } })
        .put("chains", JSONObject().apply { f.chains.forEach { (k, v) -> put(k, v) } })
        .put("weekly", JSONObject().apply { f.weekly.forEach { (k, v) -> put(k, v) } })
        .put("offers", JSONObject().apply {
            f.offers.forEach { (town, list) -> put(town, JSONObject().put("day", f.offerDay[town] ?: -1L).put("list", JSONArray().apply { list.forEach { put(toJson(it)) } })) }
        })

    fun load(f: Farm, o: JSONObject?) {
        f.deals.clear(); f.rep.clear(); f.offers.clear(); f.offerDay.clear(); f.chains.clear(); f.weekly.clear()
        if (o == null) return
        o.optJSONObject("chains")?.let { r -> r.keys().forEach { f.chains[it] = r.optInt(it).coerceIn(0, Chains.STEPS) } }
        o.optJSONObject("weekly")?.let { r -> r.keys().forEach { f.weekly[it] = r.optLong(it) } }
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
