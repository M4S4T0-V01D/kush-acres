package dev.m4s4t0.kushacres.model

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/** ROCK can be cleared for cash; WATER stays and keeps the plots beside it wet. */
enum class Kind { GRASS, PLOT, SPRINKLER, LAMP, SHACK, PART, FACILITY, DECOR, PATH, ROCK, WATER }

/**
 * One square of land. Plots grow things; sprinklers, lamps and decor help the squares around them. The shack covers
 * 2×2 squares: its top-left square is [Kind.SHACK] and the other three are [Kind.PART] pointing back at it.
 */
class Tile {
    var kind = Kind.GRASS
    /** What's growing, or the strain a facility is working on; null when empty. */
    var strain: String? = null
    /** Growth so far, in seconds of base-speed growing (the strain is ready at [Strain.seconds]). */
    var progress = 0.0
    /** Wall-clock millis until which the soil stays wet. */
    var wetUntil = 0L
    /** The last strain planted (or processed) here, which the Grow Manager (or Cook) starts again. */
    var last: String? = null
    /** Facility or decor id. */
    var item: String? = null
    /** Facility: when the batch is done, and how many units it makes. */
    var doneAt = 0L
    var amount = 0
    /** Facility level, 1..5. */
    var lvl = 1
    /** Bugs! The plant stops growing until they're cleared. */
    var pest = false
    /** For [Kind.PART]: index of the shack's anchor square. */
    var anchor = -1

    fun ready(): Boolean {
        if (kind != Kind.PLOT) return false
        val s = strain?.let { Strains[it] } ?: return false
        return progress >= s.seconds
    }

    fun clear() {
        kind = Kind.GRASS; strain = null; progress = 0.0; wetUntil = 0; last = null
        item = null; doneAt = 0; amount = 0; lvl = 1; pest = false; anchor = -1
    }

    fun copyFrom(o: Tile) {
        kind = o.kind; strain = o.strain; progress = o.progress; wetUntil = o.wetUntil; last = o.last
        item = o.item; doneAt = o.doneAt; amount = o.amount; lvl = o.lvl; pest = o.pest; anchor = o.anchor
    }
}

enum class Upgrade(val title: String, val desc: String, val max: Int, val baseCost: Double, val growth: Double, val level: Int) {
    STASH("Stash Jars", "Hold 60% more bud", 15, 150.0, 2.4, 1),
    MULCH("Mulch", "Water lasts 10 min longer", 10, 120.0, 2.3, 3),
    NUTRIENTS("Nutrients", "Everything grows 6% faster", 10, 400.0, 2.6, 4),
    GENETICS("Genetics", "8% more bud per harvest", 10, 600.0, 2.7, 6),
    CRED("Street Cred", "Sell for 5% more", 10, 1500.0, 3.0, 8);

    fun cost(lvl: Int): Long = nice(baseCost * growth.pow(lvl))
}

class OrderItem(val strain: String, val grams: Int)

class Order(val buyer: String, val items: List<OrderItem>, val reward: Long, val xp: Int, val vip: Boolean = false)

/** Something the screen should show off: a level-up, a golden harvest, a sale. */
sealed class FarmEvent {
    class LevelUp(val level: Int, val bonus: Long, val unlocks: List<String>) : FarmEvent()
    class Harvest(val x: Int, val y: Int, val grams: Int, val xp: Int, val golden: Boolean) : FarmEvent()
    class Cash(val amount: Long) : FarmEvent()
    class Award(val trophy: Trophy, val xp: Long) : FarmEvent()
}

enum class Result { OK, NO_CASH, LOCKED, BLOCKED, STASH_FULL, NOT_READY, NO_SEEDS, SHACK, LIMIT, NO_BUD, NO_SUPPLY, BUSY }

/** What the helpers got done while you were away. */
class Away(var millis: Long = 0, var harvests: Int = 0, var grams: Int = 0, var replants: Int = 0, var xp: Long = 0,
           var sold: Long = 0, var batches: Int = 0) {
    fun any() = harvests > 0 || sold > 0 || batches > 0
}

/**
 * The whole game state and rules, with no Android in it. Time is always passed in (wall-clock millis), so growth,
 * the market and the helpers all carry on correctly while the app is closed. Random events only happen while you
 * play ([rollEvent]).
 */
class Farm(var rng: Random = Random.Default) {
    companion object {
        const val MAX = 40
        const val MAX_LEVEL = 80
        const val MAX_ORDERS = 6
        /** Wet soil grows this much faster. */
        const val WET_BOOST = 1.5
        const val LAMP_BOOST = 0.25
        const val SPRINKLER_LEVEL = 6
        const val LAMP_LEVEL = 10
        val FARMHAND_LEVEL = Helper.FARMHAND.level
        val FARMHAND_COST = Helper.FARMHAND.cost
        val MANAGER_LEVEL = Helper.MANAGER.level
        val MANAGER_COST = Helper.MANAGER.cost
        const val GOLDEN_CHANCE = 0.04
        const val BLACK_MARKET_LEVEL = 6
        const val EVENT_LEVEL = 3
        const val RUNNER_MILLIS = 6 * 60_000L
        const val BM_ROTATION = 3 * 3_600_000L
        const val STRANGER_SEEDS = 3

        /** Land sizes, with the level and cash each expansion needs. */
        val LAND = listOf(Triple(6, 1, 0L), Triple(8, 5, 2_000L), Triple(10, 12, 25_000L), Triple(12, 20, 250_000L),
            Triple(14, 30, 2_500_000L), Triple(16, 40, 25_000_000L), Triple(18, 44, 60_000_000L),
            Triple(20, 48, 150_000_000L), Triple(22, 52, 400_000_000L), Triple(24, 56, 1_000_000_000L),
            Triple(26, 60, 2_500_000_000L), Triple(28, 63, 6_000_000_000L), Triple(30, 66, 15_000_000_000L),
            Triple(32, 69, 35_000_000_000L), Triple(34, 72, 80_000_000_000L), Triple(36, 75, 180_000_000_000L),
            Triple(38, 78, 400_000_000_000L), Triple(40, 80, 1_000_000_000_000L))

        fun xpNeed(level: Int): Long = (8.0 * level * level + 0.6 * level.toDouble().pow(3.2)).roundToLong()

        private val BUYERS = listOf("Stoner Steve", "Auntie Mae", "DJ Haze", "Big Tony", "Professor Puff", "Skater Kai",
            "Granny Rose", "Chef Mo", "Hippie Jan", "Lil Kush", "Doc Green", "Mellow Mel", "Couch Carl", "Biker Bree",
            "Yoga Yuki", "Trucker Tom", "Rasta Ray", "Glam Gigi", "Nerdy Ned", "Surfer Sol")
        private val CELEBS = listOf("Snoop Dogg-ish", "A Famous Rapper", "Movie Star Max", "Rockstar Roxy", "Pop Idol Pia")
    }

    /** Every farm you own; [site] is the one on screen. The tile/land/paint fields below are the active site's. */
    val sites = arrayListOf(Site(Sites.HOME))
    var site = sites[0]
    val tiles: Array<Tile> get() = site.tiles
    var land: Int
        get() = site.land
        set(v) { site.land = v }
    /** Top-left corner of the owned square on the active site's map. */
    val ox get() = site.ox
    val oy get() = site.oy
    var cash = 30L
    var level = 1
    var xp = 0L
    val upgrades = IntArray(Upgrade.entries.size)
    val helpers = BooleanArray(Helper.entries.size)
    var farmhand: Boolean
        get() = helpers[Helper.FARMHAND.ordinal]
        set(v) { helpers[Helper.FARMHAND.ordinal] = v }
    var manager: Boolean
        get() = helpers[Helper.MANAGER.ordinal]
        set(v) { helpers[Helper.MANAGER.ordinal] = v }
    var runnerOn = true
    var runnerAt = 0L
    val stash = LinkedHashMap<String, Int>()
    /** Facility products, keyed "FACILITY:strain". */
    val goods = LinkedHashMap<String, Int>()
    val supplies = IntArray(Supply.entries.size)
    /** Exotic seeds in your pocket. */
    val seeds = LinkedHashMap<String, Int>()
    /** Decor bought or picked up, waiting to be placed for free. */
    val decorInv = LinkedHashMap<String, Int>()
    var envelopes = 0
    /** Buff → wall-clock millis it lasts until. */
    val buffs = HashMap<Buff, Long>()
    var shack = 0
    var roof: Int
        get() = site.roof
        set(v) { site.roof = v }
    var wall: Int
        get() = site.wall
        set(v) { site.wall = v }
    /** Per-strain market demand, 0.4..1: selling a lot of one strain floods the market; it recovers over time. */
    val demand = HashMap<String, Double>()
    val orders = arrayOfNulls<Order>(MAX_ORDERS)
    val orderAt = LongArray(MAX_ORDERS)
    var lastTick = 0L
    var nextEventAt = 0L
    var happening: Happening? = null
    var bmRot = -1L
    val bmBought = HashSet<Int>()

    var totalHarvests = 0L
    var totalEarned = 0L

    // ---- Kept when the farm is sold ----
    val stats = LongArray(Stat.entries.size)
    val trophies = LinkedHashSet<String>()
    /** Hybrids made in the lab; they stay unlocked for good. */
    val bred = LinkedHashSet<String>()
    var legacy = 0
    var bestLevel = 1
    var streak = 0
    var bestStreak = 0
    var lastDay = -1L
    var questDay = -1L
    val quests = ArrayList<Quest>()
    var questBonus = false
    var away = Away()
    val events = ArrayList<FarmEvent>()

    fun tile(x: Int, y: Int): Tile = tiles[y * MAX + x]
    fun inLand(x: Int, y: Int) = x in ox until ox + land && y in oy until oy + land
    /** Calls [f] for every owned square of the active site. */
    inline fun eachLand(f: (x: Int, y: Int, t: Tile) -> Unit) {
        val x0 = ox; val y0 = oy; val n = land
        for (y in y0 until y0 + n) for (x in x0 until x0 + n) f(x, y, tile(x, y))
    }
    /** Runs [f] with each owned site made active in turn, then puts the one on screen back. */
    inline fun eachSite(f: (Site) -> Unit) {
        val keep = site
        try { for (s in sites) { site = s; f(s) } } finally { site = keep }
    }
    fun owns(id: String) = sites.any { it.def.id == id }

    fun newGame(now: Long) {
        placeShack(ox, oy)
        // A little patch beside the shack, two plants already waiting to be picked.
        for ((i, p) in listOf(2 to 2, 3 to 2, 2 to 3, 3 to 3).withIndex()) {
            val t = tile(ox + p.first, oy + p.second)
            t.kind = Kind.PLOT
            if (i < 2) { t.strain = "ditch"; t.last = "ditch"; t.progress = Strains["ditch"]!!.seconds.toDouble() }
        }
        lastTick = now
        for (i in 0 until orderSlots()) orderAt[i] = now + 20_000L * (i + 1)
    }

    // ---- Numbers ----

    fun up(u: Upgrade) = upgrades[u.ordinal]
    fun has(h: Helper) = helpers[h.ordinal]
    fun on(b: Buff) = (buffs[b] ?: 0L) > lastTick
    fun stashCap(): Int = (60 * 1.6.pow(up(Upgrade.STASH)) * (1 + 0.2 * shack)).toInt()
    fun stashUsed(): Int = stash.values.sum()
    fun season(): Season = Season.at(lastTick)
    fun waterMillis(): Long = (20 + 10L * up(Upgrade.MULCH)) * 60_000L * (if (season() == Season.SUMMER) 2 else 1)
    fun yieldMult(): Double = (1 + 0.08 * up(Upgrade.GENETICS)) * (if (season() == Season.AUTUMN) 1.2 else 1.0)
    fun priceMult(): Double {
        var deco = 0.0
        if (placed(Decor.FLAMINGO)) deco += 0.03
        if (placed(Decor.NEON)) deco += 0.05
        if (placed(Decor.VAN)) deco += 0.06
        if (placed(Decor.STATUE)) deco += 0.08
        return (1 + 0.05 * up(Upgrade.CRED)) * (1 + deco) * (if (on(Buff.RUSH)) 1.5 else 1.0) * (if (on(Buff.TIP)) 1.25 else 1.0) *
            (1 + 0.25 * legacy) * (if (season() == Season.WINTER) 1.25 else 1.0)
    }
    fun xpMult(): Double {
        var deco = 0.0
        if (placed(Decor.FLAG)) deco += 0.05
        if (placed(Decor.FROG)) deco += 0.08
        if (placed(Decor.KOI)) deco += 0.10
        if (placed(Decor.DISCO)) deco += 0.10
        return (1 + 0.05 * shack) * (1 + deco) * (1 + 0.1 * legacy) * (if (on(Buff.FOCUS)) 1.5 else 1.0)
    }
    fun orderSlots(): Int = min(MAX_ORDERS, 2 + level / 6)
    fun unlocked(s: Strain) = s.exotic || (if (s.hybrid) s.id in bred else s.level <= level)
    fun stat(st: Stat) = stats[st.ordinal]
    fun bump(st: Stat, n: Long = 1) { stats[st.ordinal] += n }
    /** Cash from selling: counts towards lifetime earnings. */
    private fun earn(x: Long) { cash += x; totalEarned += x; bump(Stat.EARNED, x) }
    fun count(kind: Kind) = tiles.count { it.kind == kind }
    fun count(f: Facility) = tiles.count { it.kind == Kind.FACILITY && it.item == f.name }
    /** Decor bonuses to prices and XP count from any farm you own. */
    fun placed(d: Decor) = sites.any { s -> s.tiles.any { (it.kind == Kind.DECOR || it.kind == Kind.PATH) && it.item == d.name } }
    fun decorCount() = count(Kind.DECOR)
    fun decorCap() = 3 + 3 * shack

    fun plotCost(): Long {
        val k = max(1, count(Kind.PLOT) - 3)
        return nice(6.0 * k.toDouble().pow(2) * 1.02.pow(k))
    }
    fun sprinklerCost(): Long = nice(400.0 * 1.6.pow(count(Kind.SPRINKLER)))
    fun lampCost(): Long = nice(3000.0 * 1.6.pow(count(Kind.LAMP)))
    fun facilityCost(f: Facility): Long = nice(f.cost * 3.0.pow(count(f)))
    fun nextLand(): Triple<Int, Int, Long>? = LAND.firstOrNull { it.first > land }
    fun nextShack(): ShackLevel? = SHACKS.getOrNull(shack + 1)

    fun price(strain: String): Double {
        val s = Strains[strain] ?: return 0.0
        return s.price * priceMult() * (demand[strain] ?: 1.0)
    }

    fun goodsKey(f: Facility, strain: String) = "${f.name}:$strain"
    fun goodsParts(key: String): Pair<Facility, Strain>? {
        val i = key.indexOf(':')
        if (i < 0) return null
        val f = Facility.of(key.substring(0, i)) ?: return null
        val s = Strains[key.substring(i + 1)] ?: return null
        return f to s
    }
    fun goodsPrice(key: String): Double {
        val (f, s) = goodsParts(key) ?: return 0.0
        return s.price * f.grams * f.mult * priceMult() * (if (on(Buff.HIPPIES)) 2.0 else 1.0)
    }

    private fun neighbours(x: Int, y: Int, test: (Tile) -> Boolean): Int {
        var n = 0
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until MAX && ny in 0 until MAX && test(tile(nx, ny))) n++
        }
        return n
    }

    private fun around(x: Int, y: Int, kind: Kind) = neighbours(x, y) { it.kind == kind }
    fun decorAround(x: Int, y: Int, d: Decor) = min(2, neighbours(x, y) { it.kind == Kind.DECOR && it.item == d.name })

    private fun barrelBeside(x: Int, y: Int): Boolean =
        listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).any { (nx, ny) ->
            nx in 0 until MAX && ny in 0 until MAX &&
                tile(nx, ny).let { it.kind == Kind.WATER || it.kind == Kind.DECOR && it.item == Decor.BARREL.name }
        }

    fun sprinkled(x: Int, y: Int) = around(x, y, Kind.SPRINKLER) > 0 || barrelBeside(x, y)
    fun lamps(x: Int, y: Int) = min(2, around(x, y, Kind.LAMP))
    fun protected(x: Int, y: Int) = decorAround(x, y, Decor.SCARECROW) > 0

    /** Growth speed multiplier before water and buffs. */
    fun speed(x: Int, y: Int): Double = (1 + 0.06 * up(Upgrade.NUTRIENTS)) * (1 + LAMP_BOOST * lamps(x, y)) *
        (1 + 0.05 * decorAround(x, y, Decor.GNOME) + 0.10 * decorAround(x, y, Decor.TIKI) + 0.15 * decorAround(x, y, Decor.LAVA) +
            0.20 * decorAround(x, y, Decor.WINDMILL) + 0.25 * decorAround(x, y, Decor.UFO)) *
        (1 + 0.1 * legacy) * when (season()) { Season.SPRING -> 1.15; Season.WINTER -> 0.9; else -> 1.0 }

    fun goldenChance(x: Int, y: Int): Double = GOLDEN_CHANCE + 0.03 * decorAround(x, y, Decor.BEEHIVE) +
        0.06 * decorAround(x, y, Decor.ALIEN) + 0.08 * decorAround(x, y, Decor.TOTEM) + (if (on(Buff.LUCKY)) 0.21 else 0.0) + (if (on(Buff.DUST)) 0.15 else 0.0)

    /** The share of t0..t1 that a buff covers. */
    private fun cover(b: Buff, t0: Long, t1: Long): Double {
        val u = buffs[b] ?: return 0.0
        if (u <= t0 || t1 <= t0) return 0.0
        return (min(u, t1) - t0).toDouble() / (t1 - t0)
    }
    private fun boost(t0: Long, t1: Long) = 1 + 1.0 * cover(Buff.VIBES, t0, t1) + 0.5 * cover(Buff.JUICE, t0, t1)
    fun boostNow(): Double = 1 + (if (on(Buff.VIBES)) 1.0 else 0.0) + (if (on(Buff.JUICE)) 0.5 else 0.0)

    fun wet(x: Int, y: Int, now: Long) = sprinkled(x, y) || tile(x, y).wetUntil > now

    /** Seconds until the crop at x,y is ready at the current conditions (water running out included). */
    fun eta(x: Int, y: Int, now: Long): Long {
        val t = tile(x, y)
        val s = t.strain?.let { Strains[it] } ?: return 0
        val need = s.seconds - t.progress
        if (need <= 0) return 0
        val r = speed(x, y) * boostNow()
        if (sprinkled(x, y)) return (need / (WET_BOOST * r)).roundToLong()
        val wetLeft = max(0L, t.wetUntil - now) / 1000.0
        return if (wetLeft * WET_BOOST * r >= need) (need / (WET_BOOST * r)).roundToLong()
        else (wetLeft + (need - wetLeft * WET_BOOST * r) / r).roundToLong()
    }

    // ---- Time ----

    private fun anyHelper() = helpers.any { it }

    fun tick(now: Long) {
        if (lastTick == 0L || now <= lastTick) { lastTick = now; return }
        val total = now - lastTick
        // Without helpers nothing depends on the order things happen in, so one step is exact. With them, step
        // through the gap so offline harvests, replants and sales land roughly when they would have.
        val helping = anyHelper()
        val step = if (helping) max(30_000L, total / max(200, 2000 / sites.size)) else total
        var t = lastTick
        while (t < now) {
            val t1 = min(now, t + step)
            eachSite { grow(t, t1) }
            recoverDemand(t1 - t)
            refillOrders(t1)
            lastTick = t1
            if (helping) work(t1)
            t = t1
        }
        if (total > 60_000L && helping) away.millis += total
        lastTick = now
        buffs.entries.removeAll { it.value <= now }
        refreshQuests(now)
    }

    private fun grow(t0: Long, t1: Long) {
        val dt = (t1 - t0) / 1000.0
        val b = boost(t0, t1)
        eachLand { x, y, t ->
            if (t.kind != Kind.PLOT || t.pest) return@eachLand
            val s = t.strain?.let { Strains[it] } ?: return@eachLand
            if (t.progress >= s.seconds) return@eachLand
            val wet = if (sprinkled(x, y)) dt else ((min(t.wetUntil, t1) - t0) / 1000.0).coerceIn(0.0, dt)
            t.progress = min(s.seconds.toDouble(), t.progress + (wet * WET_BOOST + (dt - wet)) * speed(x, y) * b)
        }
    }

    private fun recoverDemand(ms: Long) {
        // Half-life of 45 minutes back towards full demand.
        val k = exp(-ln(2.0) * ms / (45 * 60_000.0))
        val it = demand.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val d = 1 - (1 - e.value) * k
            if (d > 0.999) it.remove() else e.setValue(d)
        }
    }

    private fun refillOrders(now: Long) {
        for (i in 0 until orderSlots()) {
            if (orders[i] == null) {
                if (orderAt[i] == 0L) orderAt[i] = now + 30_000L
                else if (now >= orderAt[i]) { orders[i] = newOrder(); orderAt[i] = 0L }
            }
        }
    }

    /** Everything the hired help does each step. */
    private fun work(now: Long) {
        val active = site
        eachSite { s ->
            val before = events.size
            if (farmhand) pick(now)
            if (has(Helper.COOK)) cook(now)
            // Pops for the other farms would show up in the wrong place.
            if (s !== active) for (i in events.size - 1 downTo before) if (events[i] is FarmEvent.Harvest) events.removeAt(i)
        }
        if (has(Helper.FETCHER)) fetch()
        if (has(Helper.RUNNER) && runnerOn && now >= runnerAt) {
            runnerAt = now + RUNNER_MILLIS
            away.sold += run()
        }
    }

    /** The farmhand picks everything ready and clears pests; with the Grow Manager the same strain goes back in. */
    private fun pick(now: Long) {
        eachLand { x, y, t ->
            if (t.kind != Kind.PLOT) return@eachLand
            if (t.pest) { t.pest = false; bump(Stat.PESTS); return@eachLand }
            if (!t.ready()) return@eachLand
            val before = events.size
            if (harvest(x, y, now) != Result.OK) return
            val h = events.getOrNull(before) as? FarmEvent.Harvest
            away.harvests++
            if (h != null) { away.grams += h.grams; away.xp += h.xp }
            val last = t.last
            if (manager && last != null && plant(x, y, last, now) == Result.OK) away.replants++
        }
    }

    private fun cook(now: Long) {
        eachLand { x, y, t ->
            if (t.kind != Kind.FACILITY) return@eachLand
            if (t.strain != null && now >= t.doneAt && collect(x, y, now) == Result.OK) away.batches++
            val last = t.last
            if (t.strain == null && last != null) startBatch(x, y, last, now)
        }
    }

    private fun fetch() {
        for (f in Facility.entries) {
            if (count(f) == 0) continue
            val s = f.supply
            if (supplies[s.ordinal] >= 5) continue
            val unit = nice(s.price * 1.1)
            val n = min(15 - supplies[s.ordinal], (cash / unit).toInt())
            if (n <= 0) continue
            cash -= unit * n
            supplies[s.ordinal] += n
        }
    }

    /** The runner sells everything not set aside for open orders, plus all goods. Returns the cash made. */
    fun run(): Long {
        val keep = HashMap<String, Int>()
        orders.filterNotNull().forEach { o -> o.items.forEach { keep[it.strain] = (keep[it.strain] ?: 0) + it.grams } }
        var got = 0L
        for (k in stash.keys.toList()) {
            val spare = (stash[k] ?: 0) - (keep[k] ?: 0)
            if (spare > 0) got += sell(k, spare)
        }
        return got + sellAllGoods()
    }

    // ---- Random events (only while playing) ----

    /** Call while the game is open. Returns true when a new [happening] needs showing. */
    fun rollEvent(now: Long): Boolean {
        if (level < EVENT_LEVEL || happening != null) return false
        if (nextEventAt == 0L) { delayEvents(now); return false }
        if (now < nextEventAt) return false
        nextEventAt = now + (8 + rng.nextInt(9)) * 60_000L
        happening = makeEvent(now)
        if (happening != null) bump(Stat.EVENTS)
        return happening != null
    }

    /** After a break, give the player a few minutes before the next event. */
    fun delayEvents(now: Long) { nextEventAt = now + (3 + rng.nextInt(4)) * 60_000L }

    private fun growing() = (0 until land * land).map { ox + it % land to oy + it / land }
        .filter { (x, y) -> tile(x, y).let { it.kind == Kind.PLOT && it.strain != null && !it.ready() && !it.pest } }

    fun makeEvent(now: Long, force: EventKind? = null): Happening? {
        val dog = placed(Decor.DOG)
        val pests = growing().filter { (x, y) -> !protected(x, y) }
        val choices = ArrayList<Pair<EventKind, Int>>()
        if (level >= 5 && cash >= 500) choices += EventKind.RAID to (if (dog) 1 else 2)
        if (pests.isNotEmpty()) choices += EventKind.PESTS to 3
        if (count(Kind.PLOT) > 0) choices += EventKind.RAIN to 2
        choices += EventKind.RUSH to 2
        choices += EventKind.LUCKY to 2
        choices += EventKind.VIBES to 2
        if (tiles.any { it.kind == Kind.FACILITY }) choices += EventKind.HIPPIES to 2
        if (level >= BLACK_MARKET_LEVEL) choices += EventKind.STRANGER to 2
        choices += EventKind.CELEB to 1
        if (stash.isNotEmpty()) choices += EventKind.TRADER to 2
        val kind = force ?: run {
            var r = rng.nextInt(choices.sumOf { it.second })
            choices.first { r -= it.second; r < 0 }.first
        }
        return when (kind) {
            EventKind.RAID -> Happening(kind, amount = max(50L, nice(cash * 0.08)))
            EventKind.PESTS -> {
                val hit = pests.shuffled(rng).take(1 + rng.nextInt(3))
                if (hit.isEmpty()) return null
                hit.forEach { (x, y) -> tile(x, y).pest = true }
                Happening(kind, plots = hit.size)
            }
            EventKind.RAIN -> {
                eachLand { _, _, t ->
                    if (t.kind == Kind.PLOT) t.wetUntil = max(t.wetUntil, now + waterMillis())
                }
                Happening(kind)
            }
            EventKind.RUSH -> { addBuff(Buff.RUSH, now, 10); Happening(kind, amount = 10) }
            EventKind.LUCKY -> { addBuff(Buff.LUCKY, now, 5); Happening(kind, amount = 5) }
            EventKind.VIBES -> { addBuff(Buff.VIBES, now, 10); Happening(kind, amount = 10) }
            EventKind.HIPPIES -> { addBuff(Buff.HIPPIES, now, 10); Happening(kind, amount = 10) }
            EventKind.STRANGER -> {
                val item = BmItem.entries.filter { it.strain != null && it.level <= level }.randomOrNull(rng) ?: return null
                Happening(kind, amount = nice(item.price(level) / BmItem.EXOTIC_PACK * STRANGER_SEEDS * 0.6), strain = item.strain, grams = STRANGER_SEEDS)
            }
            EventKind.CELEB -> {
                val base = newOrder()
                val o = Order(CELEBS[rng.nextInt(CELEBS.size)], base.items.map { OrderItem(it.strain, it.grams * 2) },
                    base.reward * 6, base.xp * 4, vip = true)
                val slot = (0 until orderSlots()).firstOrNull { orders[it] == null } ?: (orderSlots() - 1)
                orders[slot] = o; orderAt[slot] = 0L
                Happening(kind, amount = o.reward)
            }
            EventKind.TRADER -> {
                val (s, g) = stash.maxByOrNull { it.value * (Strains[it.key]?.price ?: 0) }?.toPair() ?: return null
                Happening(kind, amount = (price(s) * g * 2).roundToLong(), strain = s, grams = g)
            }
        }
    }

    fun addBuff(b: Buff, now: Long, minutes: Int) {
        buffs[b] = max(buffs[b] ?: 0L, now) + minutes * 60_000L
    }

    /** Answers the current event. [accept] means bribe / buy / sell; returns what happened, for a toast. */
    fun resolve(accept: Boolean, now: Long): String {
        val h = happening ?: return ""
        happening = null
        if (h.kind == EventKind.RAID) bump(Stat.RAIDS)
        return when (h.kind) {
            EventKind.RAID -> when {
                has(Helper.LOOKOUT) -> "Your lookout saw them coming. Nothing found."
                accept && envelopes > 0 -> { envelopes--; "The envelope did the talking. They left smiling." }
                accept && cash >= h.amount -> { cash -= h.amount; "Paid ${h.amount}. \"Nice farm you got here.\"" }
                else -> {
                    val dog = placed(Decor.DOG)
                    if (rng.nextDouble() < (if (dog) 0.25 else 0.5)) {
                        val share = if (dog) 0.15 else 0.3
                        var lost = 0
                        for (k in stash.keys.toList()) {
                            val have = stash[k] ?: 0
                            val take = (have * share).roundToLong().toInt()
                            lost += take
                            if (have - take <= 0) stash.remove(k) else stash[k] = have - take
                        }
                        "Busted! They took ${lost}g from the stash."
                    } else if (dog) "The dog barked them off. Good boy!" else "They poked around and found nothing. Phew."
                }
            }
            EventKind.STRANGER -> {
                if (!accept) "The stranger shrugs and walks off."
                else if (cash < h.amount) "You can't afford it."
                else {
                    cash -= h.amount
                    val s = h.strain!!
                    seeds[s] = (seeds[s] ?: 0) + h.grams
                    "Got ${h.grams} ${Strains[s]?.name} seeds!"
                }
            }
            EventKind.TRADER -> {
                val s = h.strain!!
                if (!accept) "The trader moves on."
                else if ((stash[s] ?: 0) < h.grams) "You don't have that much anymore."
                else {
                    val left = (stash[s] ?: 0) - h.grams
                    if (left == 0) stash.remove(s) else stash[s] = left
                    earn(h.amount)
                    events += FarmEvent.Cash(h.amount)
                    "Sold ${h.grams}g for double!"
                }
            }
            else -> ""
        }
    }

    // ---- Actions ----

    fun plant(x: Int, y: Int, strain: String, now: Long): Result {
        val t = tile(x, y)
        val s = Strains[strain] ?: return Result.BLOCKED
        if (!inLand(x, y) || t.kind != Kind.PLOT || t.strain != null) return Result.BLOCKED
        if (!unlocked(s)) return Result.LOCKED
        if (s.exotic) {
            val n = seeds[strain] ?: 0
            if (n <= 0) return Result.NO_SEEDS
            if (n == 1) seeds.remove(strain) else seeds[strain] = n - 1
        } else {
            if (cash < s.seed) return Result.NO_CASH
            cash -= s.seed
        }
        t.strain = strain; t.progress = 0.0; t.last = strain; t.pest = false
        bump(Stat.PLANTED)
        return Result.OK
    }

    fun water(x: Int, y: Int, now: Long): Result {
        val t = tile(x, y)
        if (!inLand(x, y) || t.kind != Kind.PLOT || t.ready()) return Result.BLOCKED
        if (sprinkled(x, y)) return Result.BLOCKED
        t.wetUntil = now + waterMillis()
        bump(Stat.WATERED)
        return Result.OK
    }

    /** Rain waters every growing plant for free; how many it reached that were dry. */
    fun rain(now: Long): Int {
        var n = 0
        eachLand { x, y, t ->
            if (t.kind != Kind.PLOT || t.strain == null || t.ready()) return@eachLand
            if (t.wetUntil <= now && !sprinkled(x, y)) n++
            t.wetUntil = maxOf(t.wetUntil, now + 5 * 60_000L)
        }
        return n
    }

    fun clearPest(x: Int, y: Int): Result {
        val t = tile(x, y)
        if (!t.pest) return Result.BLOCKED
        t.pest = false
        bump(Stat.PESTS)
        addXp(1)
        return Result.OK
    }

    fun harvest(x: Int, y: Int, now: Long): Result {
        val t = tile(x, y)
        if (t.kind != Kind.PLOT) return Result.BLOCKED
        val s = t.strain?.let { Strains[it] } ?: return Result.BLOCKED
        if (!t.ready()) return Result.NOT_READY
        val golden = rng.nextDouble() < goldenChance(x, y)
        val mult = yieldMult() * (1 + 0.1 * decorAround(x, y, Decor.COMPOST) + 0.2 * decorAround(x, y, Decor.GREENHOUSE) +
            0.25 * decorAround(x, y, Decor.CRYSTAL))
        var grams = max(1, (s.yield * mult * (0.9 + rng.nextDouble() * 0.25)).roundToLong().toInt())
        if (golden) grams *= 3
        if (stashUsed() + grams > stashCap()) return Result.STASH_FULL
        stash[s.id] = (stash[s.id] ?: 0) + grams
        t.strain = null; t.progress = 0.0
        totalHarvests++
        bump(Stat.HARVESTS); bump(Stat.GRAMS, grams.toLong())
        if (golden) bump(Stat.GOLDEN)
        if (s.exotic) bump(Stat.EXOTIC)
        val xp = (s.xp * xpMult()).roundToLong()
        events += FarmEvent.Harvest(x, y, grams, xp.toInt(), golden)
        addXp(xp)
        return Result.OK
    }

    fun addXp(n: Long) {
        xp += n
        while (level < MAX_LEVEL && xp >= xpNeed(level)) {
            xp -= xpNeed(level)
            level++
            bestLevel = max(bestLevel, level)
            val bonus = 25L * level * level
            cash += bonus
            events += FarmEvent.LevelUp(level, bonus, unlocksAt(level))
        }
        if (level >= MAX_LEVEL) xp = min(xp, xpNeed(level))
    }

    /** Names of what reaching [lvl] unlocks, for the level-up card. */
    fun unlocksAt(lvl: Int): List<String> {
        val out = ArrayList<String>()
        Strains.ladder.filter { it.level == lvl }.forEach { out += "${it.name} seeds" }
        Upgrade.entries.filter { it.level == lvl }.forEach { out += it.title }
        LAND.filter { it.second == lvl && it.third > 0 }.forEach { out += "Land expansion to ${it.first}×${it.first}" }
        SHACKS.filter { it.level == lvl && it.cost > 0 }.forEach { out += "${it.title} upgrade" }
        Facility.entries.filter { it.level == lvl }.forEach { out += it.title }
        Decor.entries.filter { it.level == lvl && !it.black }.forEach { out += it.title }
        Helper.entries.filter { it.level == lvl }.forEach { out += it.title }
        if (lvl == SPRINKLER_LEVEL) out += "Sprinklers"
        if (lvl == LAMP_LEVEL) out += "Grow lamps"
        if (lvl == BLACK_MARKET_LEVEL) out += "The black market"
        if (lvl == EVENT_LEVEL) out += "Random events"
        if (lvl == Progress.BREED_LEVEL) out += "The breeding lab"
        if (lvl == Progress.LEGACY_LEVEL) out += "Selling the farm for a legacy bonus"
        if (lvl % 6 == 0 && 2 + lvl / 6 <= MAX_ORDERS) out += "Another order slot"
        return out
    }

    fun build(x: Int, y: Int, kind: Kind): Result {
        if (!inLand(x, y) || tile(x, y).kind != Kind.GRASS) return Result.BLOCKED
        val (cost, lvl) = when (kind) {
            Kind.PLOT -> plotCost() to 1
            Kind.SPRINKLER -> sprinklerCost() to SPRINKLER_LEVEL
            Kind.LAMP -> lampCost() to LAMP_LEVEL
            else -> return Result.BLOCKED
        }
        if (level < lvl) return Result.LOCKED
        if (cash < cost) return Result.NO_CASH
        cash -= cost
        tile(x, y).kind = kind
        return Result.OK
    }

    fun buildFacility(x: Int, y: Int, f: Facility): Result {
        if (!inLand(x, y) || tile(x, y).kind != Kind.GRASS) return Result.BLOCKED
        if (level < f.level) return Result.LOCKED
        val cost = facilityCost(f)
        if (cash < cost) return Result.NO_CASH
        cash -= cost
        tile(x, y).apply { kind = Kind.FACILITY; item = f.name; lvl = 1 }
        return Result.OK
    }

    /** Puts down decor, from the inventory if you have one, otherwise bought (black-market decor can't be bought here). */
    fun placeDecor(x: Int, y: Int, d: Decor): Result {
        if (!inLand(x, y) || tile(x, y).kind != Kind.GRASS) return Result.BLOCKED
        val owned = decorInv[d.name] ?: 0
        if (owned <= 0) {
            if (d.black) return Result.NO_CASH
            if (level < d.level) return Result.LOCKED
        }
        if (!d.path && decorCount() >= decorCap()) return Result.LIMIT
        if (owned > 0) { if (owned == 1) decorInv.remove(d.name) else decorInv[d.name] = owned - 1 }
        else { if (cash < d.cost) return Result.NO_CASH; cash -= d.cost }
        tile(x, y).apply { kind = if (d.path) Kind.PATH else Kind.DECOR; item = d.name }
        return Result.OK
    }

    /** Clears a square back to grass. Plots must be empty; decor goes back in your inventory; buildings can only be moved. */
    fun demolish(x: Int, y: Int): Result {
        val t = tile(x, y)
        when (t.kind) {
            Kind.GRASS, Kind.SHACK, Kind.PART, Kind.FACILITY, Kind.ROCK, Kind.WATER -> return Result.BLOCKED
            Kind.PLOT -> if (t.strain != null || count(Kind.PLOT) <= 1) return Result.BLOCKED
            Kind.DECOR, Kind.PATH -> { val d = t.item ?: ""; decorInv[d] = (decorInv[d] ?: 0) + 1 }
            else -> {}
        }
        t.clear()
        return Result.OK
    }

    /** The shack's anchor (top-left) square, or null when there isn't one yet. */
    fun shackAt(): Pair<Int, Int>? {
        val i = tiles.indexOfFirst { it.kind == Kind.SHACK }
        return if (i < 0) null else (i % MAX) to (i / MAX)
    }

    /** Puts the shack's 2×2 footprint with its top-left at x,y, if all four squares are free grass. */
    fun placeShack(x: Int, y: Int): Boolean {
        val cells = listOf(x to y, x + 1 to y, x to y + 1, x + 1 to y + 1)
        if (cells.any { (cx, cy) -> !inLand(cx, cy) || tile(cx, cy).kind != Kind.GRASS }) return false
        val a = y * MAX + x
        cells.forEachIndexed { i, (cx, cy) ->
            tile(cx, cy).apply { clear(); kind = if (i == 0) Kind.SHACK else Kind.PART; anchor = a }
        }
        return true
    }

    /** For old saves: finds the first free 2×2 spot for the shack. */
    fun autoPlaceShack(): Boolean {
        if (shackAt() != null) return true
        for (s in 0 until 2 * land) for (x in 0..s) { if (placeShack(ox + x, oy + s - x)) return true }
        return false
    }

    private fun clearShack() {
        tiles.forEach { if (it.kind == Kind.SHACK || it.kind == Kind.PART) it.clear() }
    }

    /** Moves whatever is at (fx,fy) to (tx,ty). The shack moves as a whole, with its top-left going to (tx,ty). */
    fun move(fx: Int, fy: Int, tx: Int, ty: Int): Result {
        val from = tile(fx, fy)
        if (from.kind == Kind.SHACK || from.kind == Kind.PART) {
            val (ax, ay) = shackAt() ?: return Result.BLOCKED
            if (tx == ax && ty == ay) return Result.BLOCKED
            clearShack()
            if (placeShack(tx, ty)) return Result.OK
            placeShack(ax, ay)
            return Result.BLOCKED
        }
        if (from.kind == Kind.GRASS || from.kind == Kind.ROCK || from.kind == Kind.WATER || !inLand(tx, ty)) return Result.BLOCKED
        val to = tile(tx, ty)
        if (to.kind != Kind.GRASS) return Result.BLOCKED
        to.copyFrom(from)
        from.clear()
        return Result.OK
    }

    fun upgradeShack(): Result {
        val next = nextShack() ?: return Result.BLOCKED
        if (level < next.level) return Result.LOCKED
        if (cash < next.cost) return Result.NO_CASH
        cash -= next.cost
        shack++
        return Result.OK
    }

    fun paint(roofIdx: Int, wallIdx: Int) {
        roof = roofIdx.coerceIn(0, ROOF_COLORS.size - 1)
        wall = wallIdx.coerceIn(0, WALL_COLORS.size - 1)
    }

    fun expand(): Result {
        val next = nextLand() ?: return Result.BLOCKED
        if (level < next.second) return Result.LOCKED
        if (cash < next.third) return Result.NO_CASH
        cash -= next.third
        land = next.first
        return Result.OK
    }

    fun rockCost(): Long = nice(200.0 + 40.0 * level * level)

    /** Breaks up a boulder on your land so the square can be built on. */
    fun clearRock(x: Int, y: Int): Result {
        val t = tile(x, y)
        if (!inLand(x, y) || t.kind != Kind.ROCK) return Result.BLOCKED
        val c = rockCost()
        if (cash < c) return Result.NO_CASH
        cash -= c
        t.clear()
        addXp(2)
        return Result.OK
    }

    // ---- Other farms ----

    fun canBuy(def: SiteDef): Result = when {
        owns(def.id) -> Result.BLOCKED
        level < def.level -> Result.LOCKED
        cash < def.price -> Result.NO_CASH
        else -> Result.OK
    }

    /** Buys a farm somewhere else: fresh land with a shack and a little patch, and makes it the one on screen. */
    fun buySite(def: SiteDef): Result {
        canBuy(def).let { if (it != Result.OK) return it }
        cash -= def.price
        val s = Site(def)
        s.land = SiteDef.START_LAND
        Sites.carve(def, s.tiles, s.ox, s.oy, s.land)
        sites += s
        site = s
        placeShack(ox, oy)
        for ((x, y) in listOf(3 to 3, 4 to 3, 3 to 4, 4 to 4)) tile(ox + x, oy + y).kind = Kind.PLOT
        return Result.OK
    }

    fun switchTo(id: String): Boolean {
        val s = sites.firstOrNull { it.def.id == id } ?: return false
        site = s
        return true
    }

    fun buy(u: Upgrade): Result {
        val lvl = up(u)
        if (lvl >= u.max) return Result.BLOCKED
        if (level < u.level) return Result.LOCKED
        val c = u.cost(lvl)
        if (cash < c) return Result.NO_CASH
        cash -= c
        upgrades[u.ordinal]++
        return Result.OK
    }

    fun hire(h: Helper): Result {
        if (has(h)) return Result.BLOCKED
        if (h == Helper.MANAGER && !farmhand) return Result.BLOCKED
        if (level < h.level) return Result.LOCKED
        if (shack < h.shack) return Result.SHACK
        if (cash < h.cost) return Result.NO_CASH
        cash -= h.cost
        helpers[h.ordinal] = true
        if (h == Helper.RUNNER) runnerAt = lastTick + RUNNER_MILLIS
        return Result.OK
    }

    fun hireFarmhand() = hire(Helper.FARMHAND)
    fun hireManager() = hire(Helper.MANAGER)

    // ---- Facilities ----

    fun facility(x: Int, y: Int): Facility? = tile(x, y).takeIf { it.kind == Kind.FACILITY }?.let { Facility.of(it.item) }

    fun gramsFor(f: Facility, lvl: Int) = f.grams * f.units(lvl)

    fun startBatch(x: Int, y: Int, strain: String, now: Long): Result {
        val t = tile(x, y)
        val f = facility(x, y) ?: return Result.BLOCKED
        if (t.strain != null) return Result.BUSY
        val need = gramsFor(f, t.lvl)
        val have = stash[strain] ?: 0
        if (have < need) return Result.NO_BUD
        if (supplies[f.supply.ordinal] <= 0) return Result.NO_SUPPLY
        supplies[f.supply.ordinal]--
        if (have == need) stash.remove(strain) else stash[strain] = have - need
        t.strain = strain; t.last = strain
        t.doneAt = now + f.millis(t.lvl); t.amount = f.units(t.lvl)
        return Result.OK
    }

    fun collect(x: Int, y: Int, now: Long): Result {
        val t = tile(x, y)
        val f = facility(x, y) ?: return Result.BLOCKED
        val s = t.strain?.let { Strains[it] } ?: return Result.BLOCKED
        if (now < t.doneAt) return Result.NOT_READY
        val key = goodsKey(f, s.id)
        goods[key] = (goods[key] ?: 0) + t.amount
        val xp = (s.xp * f.grams * t.amount / s.yield.toDouble() * 0.25 * xpMult()).roundToLong()
        t.strain = null; t.doneAt = 0; t.amount = 0
        bump(Stat.BATCHES)
        addXp(max(1, xp))
        return Result.OK
    }

    fun upgradeFacility(x: Int, y: Int): Result {
        val t = tile(x, y)
        val f = facility(x, y) ?: return Result.BLOCKED
        if (t.lvl >= Facility.MAX_LEVEL) return Result.BLOCKED
        val c = f.upgradeCost(t.lvl)
        if (cash < c) return Result.NO_CASH
        cash -= c
        t.lvl++
        return Result.OK
    }

    fun buySupply(s: Supply, n: Int): Result {
        val c = s.price * n
        if (cash < c) return Result.NO_CASH
        cash -= c
        supplies[s.ordinal] += n
        return Result.OK
    }

    fun sellGoods(key: String, n: Int): Long {
        val have = goods[key] ?: 0
        val k = min(have, n)
        if (k <= 0) return 0
        val got = (goodsPrice(key) * k).roundToLong()
        if (have == k) goods.remove(key) else goods[key] = have - k
        earn(got)
        events += FarmEvent.Cash(got)
        return got
    }

    fun sellAllGoods(): Long = goods.keys.toList().sumOf { sellGoods(it, goods[it] ?: 0) }

    // ---- Black market ----

    fun bmStock(now: Long): List<BmItem> {
        val rot = now / BM_ROTATION
        if (rot != bmRot) { bmRot = rot; bmBought.clear() }
        return BmItem.entries.shuffled(Random(rot * 7919 + 13)).filter { it.level <= level }.take(if (level >= 30) 5 else 4)
    }

    fun bmRefreshIn(now: Long): Long = BM_ROTATION - now % BM_ROTATION

    fun bmBuy(slot: Int, now: Long): Result {
        if (level < BLACK_MARKET_LEVEL) return Result.LOCKED
        val item = bmStock(now).getOrNull(slot) ?: return Result.BLOCKED
        if (slot in bmBought) return Result.BLOCKED
        val c = item.price(level)
        if (cash < c) return Result.NO_CASH
        cash -= c
        bmBought += slot
        when {
            item.strain != null -> seeds[item.strain] = (seeds[item.strain] ?: 0) + BmItem.EXOTIC_PACK
            item.buff != null -> addBuff(item.buff, now, item.minutes)
            item.decor != null -> decorInv[item.decor.name] = (decorInv[item.decor.name] ?: 0) + 1
            item == BmItem.ENVELOPE -> envelopes++
        }
        return Result.OK
    }

    // ---- Selling and orders ----

    /** Sells [grams] of a strain gram by gram, each one nudging its demand down. Returns the cash made. */
    fun sell(strain: String, grams: Int): Long {
        val s = Strains[strain] ?: return 0
        val have = stash[strain] ?: 0
        val n = min(have, grams)
        if (n <= 0) return 0
        var d = demand[strain] ?: 1.0
        val drop = 1 - 0.03 / s.yield
        val p = s.price * priceMult()
        var total = 0.0
        repeat(n) {
            total += p * d
            d = max(0.4, d * drop)
        }
        demand[strain] = d
        if (have - n == 0) stash.remove(strain) else stash[strain] = have - n
        val got = total.roundToLong()
        earn(got)
        bump(Stat.SOLD, n.toLong())
        events += FarmEvent.Cash(got)
        return got
    }

    fun sellAll(): Long = stash.keys.toList().sumOf { sell(it, stash[it] ?: 0) }

    fun canFill(o: Order) = o.items.all { (stash[it.strain] ?: 0) >= it.grams }

    fun fill(slot: Int, now: Long): Result {
        val o = orders.getOrNull(slot) ?: return Result.BLOCKED
        if (!canFill(o)) return Result.BLOCKED
        for (it in o.items) {
            val left = (stash[it.strain] ?: 0) - it.grams
            if (left == 0) stash.remove(it.strain) else stash[it.strain] = left
        }
        earn(o.reward)
        bump(Stat.ORDERS)
        events += FarmEvent.Cash(o.reward)
        addXp((o.xp * xpMult()).roundToLong())
        orders[slot] = null
        orderAt[slot] = now + 60_000L
        return Result.OK
    }

    fun trash(slot: Int, now: Long) {
        if (orders.getOrNull(slot) == null) return
        orders[slot] = null
        orderAt[slot] = now + 4 * 60_000L
    }

    // ---- Daily reward, quests, trophies, the lab and selling the farm ----

    fun canClaimDaily(now: Long) = localDay(now) != lastDay
    /** Which day of the streak claiming now would be. */
    fun dailyDay(now: Long): Int = if (localDay(now) == lastDay + 1) streak + 1 else if (localDay(now) == lastDay) streak else 1

    fun claimDaily(now: Long): DailyReward? {
        val today = localDay(now)
        if (today == lastDay) return null
        streak = if (today == lastDay + 1) streak + 1 else 1
        lastDay = today
        bestStreak = max(bestStreak, streak)
        val r = Progress.reward(this, streak)
        cash += r.cash
        r.buff?.let { addBuff(it, now, r.minutes) }
        r.seeds?.let { seeds[it] = (seeds[it] ?: 0) + r.seedCount }
        bump(Stat.DAYS)
        return r
    }

    fun refreshQuests(now: Long) {
        val d = localDay(now)
        if (d == questDay) return
        questDay = d
        quests.clear(); quests += Progress.newQuests(this, d)
        questBonus = false
    }

    fun questProgress(q: Quest): Long = (stat(q.kind.stat) - q.base).coerceIn(0, q.target)
    fun questDone(q: Quest) = questProgress(q) >= q.target

    fun claimQuest(i: Int): Result {
        val q = quests.getOrNull(i) ?: return Result.BLOCKED
        if (q.claimed) return Result.BLOCKED
        if (!questDone(q)) return Result.NOT_READY
        q.claimed = true
        cash += q.cash
        bump(Stat.QUESTS)
        addXp(q.xp)
        return Result.OK
    }

    fun questBonusCash(): Long = nice((300.0 + 60.0 * level * level))
    fun canClaimQuestBonus() = quests.isNotEmpty() && quests.all { it.claimed } && !questBonus

    fun claimQuestBonus(now: Long): Result {
        if (!canClaimQuestBonus()) return Result.BLOCKED
        questBonus = true
        cash += questBonusCash()
        addBuff(Buff.VIBES, now, 30)
        return Result.OK
    }

    /** Something to claim in the menu: the daily reward, a finished quest or the quest bonus. */
    fun canClaim(now: Long) = canClaimDaily(now) || quests.any { !it.claimed && questDone(it) } || canClaimQuestBonus()

    /** Unlocks every trophy whose goal is met. [silent] skips the reward, for catching up an old save. */
    fun checkTrophies(silent: Boolean = false) {
        for (t in Trophy.entries) {
            if (t.name in trophies || t.value(this) < t.target) continue
            trophies += t.name
            if (silent) continue
            val xp = max(5L, (xpNeed(level) * 0.05).roundToLong())
            events += FarmEvent.Award(t, xp)
            addXp(xp)
        }
    }

    /** Grams of each parent a cross takes. */
    fun breedGrams(s: Strain) = max(10, s.yield * 2)
    fun breedFee(s: Strain) = nice(s.seed * 10.0)

    fun breed(id: String): Result {
        val s = Strains[id] ?: return Result.BLOCKED
        val (pa, pb) = s.parents ?: return Result.BLOCKED
        if (id in bred) return Result.BLOCKED
        if (level < Progress.BREED_LEVEL) return Result.LOCKED
        val a = Strains[pa]!!; val b = Strains[pb]!!
        if (!unlocked(a) || !unlocked(b)) return Result.LOCKED
        val g = breedGrams(s)
        if ((stash[pa] ?: 0) < g || (stash[pb] ?: 0) < g) return Result.NO_BUD
        val fee = breedFee(s)
        if (cash < fee) return Result.NO_CASH
        for (p in listOf(pa, pb)) { val left = stash[p]!! - g; if (left == 0) stash.remove(p) else stash[p] = left }
        cash -= fee
        bred += id
        bump(Stat.BRED)
        addXp((s.xp * 3 * xpMult()).roundToLong())
        return Result.OK
    }

    fun canSellFarm() = level >= Progress.LEGACY_LEVEL

    /**
     * Starts over on fresh land for a permanent bonus. The other farms are sold too. Kept: legacy, stats, trophies, hybrids, streak, paint,
     * envelopes, exotic seeds and all decor (placed decor goes back in the inventory).
     */
    fun sellFarm(now: Long): Result {
        if (!canSellFarm()) return Result.LOCKED
        for (s in sites) for (t in s.tiles) if (t.kind == Kind.DECOR || t.kind == Kind.PATH) t.item?.let { decorInv[it] = (decorInv[it] ?: 0) + 1 }
        // Back to the home farm only; its paint stays.
        site = sites[0]
        sites.retainAll { it === site }
        tiles.forEach { it.clear() }
        land = 6; cash = 30; level = 1; xp = 0
        upgrades.fill(0); helpers.fill(false); supplies.fill(0)
        stash.clear(); goods.clear(); buffs.clear(); demand.clear()
        orders.fill(null); orderAt.fill(0)
        shack = 0; happening = null; runnerOn = true; runnerAt = 0; bmBought.clear()
        legacy++
        questDay = -1L
        lastTick = now
        newGame(now)
        refreshQuests(now)
        return Result.OK
    }

    fun newOrder(): Order {
        // Mostly the newest few strains, so orders keep pace with the farm.
        val pool = Strains.ladder.filter { unlocked(it) }.takeLast(5)
        val roll = rng.nextDouble()
        val n = min(pool.size, if (roll < 0.5) 1 else if (roll < 0.85) 2 else 3)
        val picks = pool.shuffled(rng).take(n)
        val items = picks.map { OrderItem(it.id, (it.yield * (1 + rng.nextInt(3)) * yieldMult()).roundToLong().toInt()) }
        val value = items.sumOf { Strains[it.strain]!!.price * it.grams * priceMult() }
        val xp = items.sumOf { val s = Strains[it.strain]!!; s.xp * it.grams / s.yield.toDouble() } * 0.6
        return Order(BUYERS[rng.nextInt(BUYERS.size)], items, nice(value * (1.4 + rng.nextDouble() * 0.6)), max(1, xp.roundToLong().toInt()))
    }
}

/** Rounds to two significant figures, so prices read cleanly ($1,700, not $1,693). */
fun nice(x: Double): Long {
    if (x < 100) return max(1, x.roundToLong())
    val m = 10.0.pow(log10(x).toInt() - 1)
    return ((x / m).roundToLong() * m).roundToLong()
}
