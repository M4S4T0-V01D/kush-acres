package dev.m4s4t0.kushacres.model

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/** ROCK can be cleared for cash; WATER stays and keeps the plots beside it wet. */
enum class Kind { GRASS, PLOT, SPRINKLER, LAMP, SHACK, PART, FACILITY, DECOR, PATH, ROCK, WATER, HOUSE, POWER, SPECIAL, VEHICLE, CAB, WING, STAIRS }

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
    /** Mould from damp weather: like bugs, the plant stops until it's scraped off. */
    var mould = false
    /** For [Kind.PART], [Kind.CAB] and [Kind.WING]: index of the shack's, vehicle's or work building's anchor square. */
    var anchor = -1
    /** The growing plant's phenotype (see [Pheno]). */
    var pheno = 0
    /** Wall-clock millis when the plant ripened (0 if not yet, or unknown): picked soon after, it's at its peak. */
    var ripeAt = 0L
    /** Clean snips banked by trimming while it grew (up to [Farm.TRIM_BANK]), and the growth it can be trimmed again at. */
    var trim = 0
    var trimAt = 0.0

    fun ready(): Boolean {
        if (kind != Kind.PLOT) return false
        val s = strain?.let { Strains[it] } ?: return false
        return progress >= s.seconds
    }

    fun clear() {
        kind = Kind.GRASS; strain = null; progress = 0.0; wetUntil = 0; last = null
        item = null; doneAt = 0; amount = 0; lvl = 1; pest = false; mould = false; anchor = -1; pheno = 0; ripeAt = 0; trim = 0; trimAt = 0.0
    }

    fun copyFrom(o: Tile) {
        kind = o.kind; strain = o.strain; progress = o.progress; wetUntil = o.wetUntil; last = o.last
        item = o.item; doneAt = o.doneAt; amount = o.amount; lvl = o.lvl; pest = o.pest; mould = o.mould; anchor = o.anchor; pheno = o.pheno; ripeAt = o.ripeAt; trim = o.trim; trimAt = o.trimAt
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
    class LevelUp(val level: Int, val bonus: Long, val unlocks: List<String>, val bucks: Long = 0, val gems: Long = 0) : FarmEvent()
    class Gems(val amount: Long) : FarmEvent()
    /** Pot Bucks or holiday tokens picked up; [token] is the holiday id, or null for Pot Bucks. */
    class Bucks(val amount: Long, val token: String? = null) : FarmEvent()
    class Harvest(val x: Int, val y: Int, val grams: Int, val xp: Int, val golden: Boolean, val perfect: Boolean = false, val trim: Int = 0) : FarmEvent()
    class Cash(val amount: Long) : FarmEvent()
    class Award(val trophy: Trophy, val xp: Long) : FarmEvent()
    class Goal(val goal: FarmGoal, val site: String, val xp: Long) : FarmEvent()
    class Bred(val strain: Strain) : FarmEvent()
    class Keepsake(val town: String, val keepsake: dev.m4s4t0.kushacres.model.Keepsake) : FarmEvent()
    /** A new best phenotype worth showing off. */
    class Find(val strain: Strain, val pheno: Int) : FarmEvent()
}

enum class Result { OK, NO_CASH, LOCKED, BLOCKED, STASH_FULL, NOT_READY, NO_SEEDS, SHACK, LIMIT, NO_BUD, NO_SUPPLY, BUSY, NO_POWER }

/** What the helpers got done while you were away. */
class Away(var millis: Long = 0, var harvests: Int = 0, var grams: Int = 0, var replants: Int = 0, var xp: Long = 0,
           var sold: Long = 0, var batches: Int = 0, var deals: Int = 0, var shop: Long = 0) {
    /** Filled in by the caller after catching up: cash gained, levels gained, plants ripe and waiting. */
    var earned = 0L
    var levels = 0
    var ripe = 0
    fun any() = harvests > 0 || sold > 0 || shop > 0 || batches > 0 || deals > 0 || levels > 0 || (millis >= 10 * 60_000L && (earned > 0 || ripe > 0))
}

/**
 * The whole game state and rules, with no Android in it. Time is always passed in (wall-clock millis), so growth,
 * the market and the helpers all carry on correctly while the app is closed. Random events only happen while you
 * play ([rollEvent]).
 */
class Farm(var rng: Random = Random.Default) {
    companion object {
        const val MAX = 40
        const val PEAK_MIN = 3 * 60_000L
        const val PEAK_MAX = 30 * 60_000L
        const val PEAK_YIELD = 1.15
        const val PEAK_XP = 1.1
        const val TRIM_MAX = 5
        const val TRIM_STEP = 0.02
        const val TRIM_BANK = 15
        const val MAX_LEVEL = 80
        const val MAX_ORDERS = 6
        /** Wet soil grows this much faster. */
        const val WET_BOOST = 1.5
        const val LAMP_BOOST = 0.25
        /** Growth beside something big that shades the plot. */
        const val SHADE = 0.95
        /** Delivering an order pays this many times selling it on the spot. */
        const val DELIVERY_PAY = 1.8
        /** Holiday tokens for a finished job and for finishing all three, before the level bonus. */
        const val QUEST_TOKENS = 4
        const val BONUS_TOKENS = 10
        const val SPRINKLER_LEVEL = 6
        const val LAMP_LEVEL = 10
        val FARMHAND_LEVEL = Helper.FARMHAND.level
        val FARMHAND_COST = Helper.FARMHAND.cost
        val MANAGER_LEVEL = Helper.MANAGER.level
        val MANAGER_COST = Helper.MANAGER.cost
        const val GOLDEN_CHANCE = 0.04
        const val FAVOURITE_PRICE = 1.3
        const val FAVOURITE_GROW = 1.15
        const val BLACK_MARKET_LEVEL = 6
        const val EVENT_LEVEL = 3
        const val RUNNER_MILLIS = 6 * 60_000L
        val WORKER_LEVEL = WorkHouse.TOWER.level
        const val BM_ROTATION = 3 * 3_600_000L
        const val STRANGER_SEEDS = 3
        const val FESTIVAL_LAND = 16
        const val FESTIVAL_HOURS = 3

        /** Land sizes, with the level and cash each expansion needs. */
        val LAND = listOf(Triple(6, 1, 0L), Triple(8, 5, 2_000L), Triple(10, 12, 25_000L), Triple(12, 20, 250_000L),
            Triple(14, 30, 2_500_000L), Triple(16, 40, 25_000_000L), Triple(18, 44, 60_000_000L),
            Triple(20, 48, 150_000_000L), Triple(22, 52, 400_000_000L), Triple(24, 56, 1_000_000_000L),
            Triple(26, 60, 2_500_000_000L), Triple(28, 63, 6_000_000_000L), Triple(30, 66, 15_000_000_000L),
            Triple(32, 69, 35_000_000_000L), Triple(34, 72, 80_000_000_000L), Triple(36, 75, 180_000_000_000L),
            Triple(38, 78, 400_000_000_000L), Triple(40, 80, 1_000_000_000_000L))

        fun xpNeed(level: Int): Long = (8.0 * level * level + 0.6 * level.toDouble().pow(3.2)).roundToLong()

        val BUYERS = listOf("Stoner Steve", "Auntie Mae", "DJ Haze", "Big Tony", "Professor Puff", "Skater Kai",
            "Granny Rose", "Chef Mo", "Hippie Jan", "Lil Kush", "Doc Green", "Mellow Mel", "Couch Carl", "Biker Bree",
            "Yoga Yuki", "Trucker Tom", "Rasta Ray", "Glam Gigi", "Nerdy Ned", "Surfer Sol")
        private val CELEBS = listOf("Snoop Dogg-ish", "A Famous Rapper", "Movie Star Max", "Rockstar Roxy", "Pop Idol Pia")
    }

    /** Every farm you own; [site] is the one on screen. The tile/land/paint fields below are the active site's. */
    val sites = arrayListOf(Site(Sites.HOME))
    var site = sites[0]
    /** 0 for the farm up top; n when floor n of its bunker is on screen, and then the fields below are that floor's. */
    var floor = 0
    val under: Floor? get() = if (floor > 0) site.floors.getOrNull(floor - 1) else null
    val tiles: Array<Tile> get() = under?.tiles ?: site.tiles
    var land: Int
        get() = under?.size ?: site.land
        set(v) { site.land = v }
    /** Top-left corner of the owned square on the active site's map. */
    val ox get() = under?.o ?: site.ox
    val oy get() = under?.o ?: site.oy
    var cash = 30L
    var level = 1
        set(v) { field = v; Strains.scaleLevel = v }
    var xp = 0L
    /** Violet's upgrades and the shack tier belong to the farm on screen. */
    val upgrades get() = site.upgrades
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
    var shack: Int
        get() = site.shack
        set(v) { site.shack = v }
    /** The biggest shack on any farm: helpers live there, and the house trophies count it. */
    fun bestShack() = sites.maxOf { it.shack }
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
    /** An order out for delivery arrives (and pays [deliverPay]) at this time; 0 when it isn't on the road. */
    val deliverAt = LongArray(MAX_ORDERS)
    val deliverPay = LongArray(MAX_ORDERS)
    var lastTick = 0L
    var nextEventAt = 0L
    var happening: Happening? = null
    var bmRot = -1L
    val bmBought = HashSet<Int>()
    /** Back-room staples bought this rotation, by item name. */
    val bmStaple = HashMap<String, Int>()
    /** What the last Mystery Crate held, for the toast. */
    var bmNote = ""

    var totalHarvests = 0L
    var totalEarned = 0L

    // ---- Kept when the farm is sold ----
    val stats = LongArray(Stat.entries.size)
    val trophies = LinkedHashSet<String>()
    /** Hybrids made in the lab; they stay unlocked for good. */
    val bred = LinkedHashSet<String>()
    /** Every strain ever harvested, for the collection book. */
    val grown = LinkedHashSet<String>()
    var legacy = 0
    /** Legacy perks bought with the points selling the farm gives. */
    val legacyPerks = LinkedHashSet<LegacyPerk>()
    /** Ladder strains kept from the last sale: unlocked from level 1, and they yield a little more. */
    val heirlooms = LinkedHashSet<String>()
    /** Phenotypes: the best one harvested of each strain, the ones kept as mothers, and strains still popping seeds. */
    val finds = LinkedHashMap<String, Int>()
    val mothers = LinkedHashMap<String, Int>()
    val hunting = LinkedHashSet<String>()
    var startMap = StartMap.HOMESTEAD
    var bestLevel = 1
    var streak = 0
    var bestStreak = 0
    var lastDay = -1L
    var questDay = -1L
    val quests = ArrayList<Quest>()
    var questBonus = false
    /** The Cannabis Cup week the entries in [cupScores] belong to, and each entry (strain id, judged score). */
    var cupWeek = -1L
    val cupScores = ArrayList<Pair<String, Int>>()
    /** The [Trip] a seed-hunt scout is out on (null when home) and when they get back. */
    var hunt: String? = null
    var huntBack = 0L
    /** Farm contracts: today's offers per town (and the day they're for), the ones taken, and each town's reputation. */
    val offers = HashMap<String, ArrayList<Contract>>()
    val offerDay = HashMap<String, Long>()
    val deals = ArrayList<Contract>()
    val rep = HashMap<String, Int>()
    /** Story chapters finished per town, and the week each town's standing order was last taken. */
    val chains = HashMap<String, Int>()
    val weekly = HashMap<String, Long>()
    /** The last contract filled, for the screen to show once; and how many have lapsed since it last looked. */
    var lastDeal: DealPay? = null
    var lapsed = 0
    /** The hybrid the Lab is working out (null when idle) and when it's done. */
    var research: String? = null
    var researchAt = 0L
    var away = Away()
    val events = ArrayList<FarmEvent>()
    /** Pot Bucks: the rare currency from level-ups, dailies, challenges, events and lucky harvests. */
    var potBucks = 0L
    /** Kush gems: the hardest currency to get, spent on special buildings. */
    var gems = 0L
    /** When the gem mine next turns up a gem (0 until one is built). */
    var mineAt = 0L
    /** Holiday tokens (candy corn and the rest), keyed by holiday id. */
    val tokens = LinkedHashMap<String, Long>()
    /** Holiday strains bought from a holiday shop; they stay for good. */
    val unlockedStrains = LinkedHashSet<String>()
    /** Holiday pets owned, by name; they wander every farm. */
    val pets = LinkedHashSet<String>()
    /** Your farmer, or null until one has been made. */
    var farmer: Farmer? = null
    /** A hippie festival runs on [festivalSite] until [festivalUntil]. */
    var festivalUntil = 0L
    var festivalSite: String? = null

    fun tile(x: Int, y: Int): Tile = tiles[y * MAX + x]
    fun inLand(x: Int, y: Int) = x in ox until ox + land && y in oy until oy + land
    /** Calls [f] for every owned square of the active site. */
    inline fun eachLand(f: (x: Int, y: Int, t: Tile) -> Unit) {
        val x0 = ox; val y0 = oy; val n = land
        for (y in y0 until y0 + n) for (x in x0 until x0 + n) f(x, y, tile(x, y))
    }
    /** Runs [f] with each owned site made active in turn, then puts the one on screen back. */
    inline fun eachSite(f: (Site) -> Unit) {
        val keep = site; val down = floor
        try { floor = 0; for (s in sites) { site = s; f(s) } } finally { site = keep; floor = down }
    }
    /** Runs [f] on each floor of the active farm's bunker in turn, then goes back to where it was. */
    inline fun eachFloor(f: (Int) -> Unit) {
        val down = floor
        try { for (i in site.floors.indices) { floor = i + 1; f(floor) } } finally { floor = down }
    }
    /** Runs [f] up top, for the things that only happen there. */
    inline fun <T> surface(f: () -> T): T {
        val down = floor
        try { floor = 0; return f() } finally { floor = down }
    }
    fun owns(id: String) = sites.any { it.def.id == id }

    fun newGame(now: Long) {
        Strains.scaleLevel = level
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
    /** The stash is shared, so every farm's storage adds up: its Stash upgrade and its shack. */
    fun stashCap(): Int = sites.sumOf { siteStash(it) }
    fun siteStash(s: Site = site): Int = (60 * 1.6.pow(s.upgrades[Upgrade.STASH.ordinal]) * (1 + 0.2 * s.shack) * (1 + bunkerStash(s))).toInt()
    /** A bunker is storage too: [Bunker.STASH] more stash per square across of each floor. */
    fun bunkerStash(s: Site = site) = Bunker.STASH * s.floors.sumOf { it.size }
    fun stashUsed(): Int = stash.values.sum()
    fun season(): Season = Season.at(lastTick)
    fun waterMillis(): Long = (20 + 10L * up(Upgrade.MULCH)) * 60_000L * (if (season() == Season.SUMMER) 2 else 1) / (if (on(Buff.HEATWAVE)) 2 else 1)
    fun yieldMult(): Double = (1 + 0.08 * up(Upgrade.GENETICS)) * Legacy.mult(this, LegacyPerk.BIG_BUDS, 0.1) * (if (season() == Season.AUTUMN) 1.2 else 1.0) * (if (on(Buff.HARVEST_MOON)) 1.5 else 1.0)
    fun priceMult(): Double {
        val have = placedDecor()
        var deco = perkWide(Perk.SELL, have) + Chains.bonus(this, Perk.SELL)
        if (Decor.FLAMINGO in have) deco += 0.03
        if (Decor.NEON in have) deco += 0.05
        if (Decor.VAN in have) deco += 0.06
        if (Decor.STATUE in have) deco += 0.08
        return (1 + 0.05 * up(Upgrade.CRED)) * (1 + deco) * (if (on(Buff.RUSH)) 1.5 else 1.0) * (if (on(Buff.TIP)) 1.25 else 1.0) *
            (if (on(Buff.FESTIVAL)) 1.5 else 1.0) * (1 + 0.25 * legacy) * Legacy.mult(this, LegacyPerk.SILVER_TONGUE, 0.15) * (if (season() == Season.WINTER) 1.25 else 1.0) *
            (if (built(Special.DISPENSARY)) 1.15 else 1.0) * (if (built(Special.MONUMENT)) 1.25 else 1.0)
    }
    fun xpMult(): Double {
        val have = placedDecor()
        var deco = perkWide(Perk.XP, have) + Chains.bonus(this, Perk.XP)
        if (Decor.FLAG in have) deco += 0.05
        if (Decor.FROG in have) deco += 0.08
        if (Decor.KOI in have) deco += 0.10
        if (Decor.DISCO in have) deco += 0.10
        val rvs = sites.count { s -> s.tiles.any { it.kind == Kind.VEHICLE && it.item == Vehicle.RV.name } }
        return (1 + 0.05 * shack) * (1 + deco + 0.05 * rvs) * (1 + 0.1 * legacy) * Legacy.mult(this, LegacyPerk.QUICK_STUDY, 0.15) * (if (on(Buff.FOCUS)) 1.5 else 1.0) *
            (if (built(Special.ACADEMY)) 1.25 else 1.0) * (if (built(Special.MONUMENT)) 1.1 else 1.0)
    }
    /** Farm-wide holiday decor bonus of kind [p]: each kind of decor counts once, from any farm. */
    private fun perkWide(p: Perk, on: Set<Decor>) = Decor.perks[p]?.sumOf { if (it in on) it.amt else 0.0 } ?: 0.0
    /** Every kind of decor placed on any farm you own. */
    private fun placedDecor(): Set<Decor> {
        val names = HashSet<String>()
        for (s in sites) for (t in s.tiles) if (t.kind == Kind.DECOR || t.kind == Kind.PATH) t.item?.let { names += it }
        return names.mapNotNullTo(HashSet()) { Decor.of(it) }
    }
    /** Neighbour holiday decor bonus of kind [p] at x,y (up to two of each kind count). */
    private fun perkNear(x: Int, y: Int, p: Perk) = Decor.perks[p]?.sumOf { it.amt * decorAround(x, y, it) } ?: 0.0
    fun orderSlots(): Int = min(MAX_ORDERS, 2 + level / 6)
    fun unlocked(s: Strain) = when {
        s.holiday != null -> s.id in unlockedStrains
        s.hybrid -> s.id in bred
        s.exotic -> s.site == null || site.def.id == s.site
        s.site != null -> s.level <= level && site.def.id == s.site
        else -> s.level <= level || s.id in heirlooms
    }
    /** Whether [s] counts as yours for the lab: native strains only need their farm owned, not on screen. */
    fun owned(s: Strain) = when {
        s.site != null -> owns(s.site) && s.level <= level
        else -> unlocked(s)
    }
    fun tokens(h: Holiday) = tokens[h.id] ?: 0L
    fun addTokens(h: Holiday?, n: Long) { if (h != null && n > 0) tokens[h.id] = tokens(h) + n }
    /** Holiday tokens grow with the level of what earned them: 1× at the bottom of the ladder, 5× at level 80. */
    fun tokenMult(lvl: Int) = 1.0 + lvl.coerceAtLeast(0) / 20.0
    /** Tokens an order pays: more for bigger orders and higher strains. Selling the same bud to Dex pays half. */
    fun orderTokens(o: Order): Long = orderTokens(o.items.maxOfOrNull { Strains[it.strain]?.level ?: 0 } ?: 0, o.items.sumOf { it.grams })
    fun orderTokens(lvl: Int, grams: Int): Long = ((3 + grams / 20.0) * tokenMult(lvl)).roundToLong()
    /** Tokens for a daily, a job or the jobs bonus, by the player's level. */
    fun rewardTokens(base: Int): Long = if (holiday() == null) 0 else (base * tokenMult(level)).roundToLong()
    fun holiday(): Holiday? = Holiday.at(lastTick)
    fun stat(st: Stat) = stats[st.ordinal]
    fun bump(st: Stat, n: Long = 1) { stats[st.ordinal] += n }
    /** Cash from selling: counts towards lifetime earnings. */
    private fun earn(x: Long) { cash += x; totalEarned += x; bump(Stat.EARNED, x) }
    fun earnCash(x: Long) = earn(x)
    /** Cash paid out for anything: counts towards lifetime spending. */
    fun spend(x: Long) { cash -= x; bump(Stat.SPENT, x) }
    fun count(kind: Kind) = tiles.count { it.kind == kind }
    /** This farm's [f]s, up top and in its bunker. */
    fun count(f: Facility) = (sequenceOf(site.tiles) + site.floors.asSequence().map { it.tiles }).sumOf { ts -> ts.count { it.kind == Kind.FACILITY && it.item == f.name } }
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
    fun count(g: Generator) = tiles.count { it.kind == Kind.POWER && it.item == g.name }
    /** Each extra generator of a kind on the same farm costs twice the last one. */
    fun generatorCost(g: Generator): Long = nice(g.cost * 2.0.pow(count(g)))
    /** A ground colour costs more the further you get; going back to the farm's own colour is free. */
    fun groundCost(): Long = nice(1000.0 * 1.2.pow(level))

    // ---- Power and special buildings ----

    /** Bumped whenever buildings change, so each farm's power and specials are worked out again. */
    var layoutRev = 0L
    private fun scan(s: Site): Site {
        if (s.scanned == layoutRev) return s
        var made = Generator.GRID; var used = 0
        val sp = HashSet<String>()
        for (t in s.tiles) when (t.kind) {
            Kind.POWER -> Generator.of(t.item)?.let { made += it.watts }
            Kind.LAMP -> used += Generator.LAMP
            Kind.FACILITY -> Facility.of(t.item)?.let { used += Generator.draw(it, t.lvl) }
            Kind.SPECIAL -> t.item?.let { sp += it }
            else -> {}
        }
        used += s.floors.sumOf { it.size * it.size } * Bunker.WATTS
        for (fl in s.floors) for (t in fl.tiles) if (t.kind == Kind.FACILITY) Facility.of(t.item)?.let { used += Generator.draw(it, t.lvl) }
        if (Special.SUBSTATION.name in sp) made += 150
        s.made = made; s.used = used; s.specials = sp; s.scanned = layoutRev
        return s
    }
    fun powerMade(s: Site = site) = scan(s).made
    fun powerUsed(s: Site = site) = scan(s).used
    fun powered(s: Site = site) = scan(s).let { it.used <= it.made }
    /** Whether [sp] stands on the farm being worked on. */
    fun here(sp: Special) = sp.name in scan(site).specials
    /** Whether [sp] stands on any farm you own. */
    fun built(sp: Special) = sites.any { sp.name in scan(it).specials }
    /** Whether [sp] can go on this farm: one of each per farm, and the farm-wide ones only once anywhere. */
    fun canPlace(sp: Special) = !here(sp) && (sp.local || !built(sp))

    fun buildPower(x: Int, y: Int, g: Generator): Result {
        if (!inLand(x, y) || tile(x, y).kind != Kind.GRASS) return Result.BLOCKED
        if (level < g.level) return Result.LOCKED
        val cost = generatorCost(g)
        if (cash < cost) return Result.NO_CASH
        spend(cost)
        tile(x, y).apply { kind = Kind.POWER; item = g.name; lvl = 1 }
        layoutRev++
        return Result.OK
    }

    fun buildSpecial(x: Int, y: Int, sp: Special, now: Long): Result {
        if (!inLand(x, y) || tile(x, y).kind != Kind.GRASS) return Result.BLOCKED
        if (level < sp.level) return Result.LOCKED
        if (!canPlace(sp)) return Result.LIMIT
        if (gems < sp.gems || cash < sp.cash) return Result.NO_CASH
        gems -= sp.gems
        spend(sp.cash)
        tile(x, y).apply { kind = Kind.SPECIAL; item = sp.name; lvl = 1 }
        layoutRev++
        if (sp == Special.MINE) mineAt = now + Special.MINE_MILLIS
        return Result.OK
    }

    /** Paints this farm's grass or soil ([dirt]); null puts back the farm's own colour, for free. */
    fun paintGround(dirt: Boolean, color: Int?, bucks: Boolean): Result {
        if ((if (dirt) site.dirt else site.grass) == color) return Result.BLOCKED
        if (color != null) {
            if (bucks) { if (potBucks < GROUND_BUCKS) return Result.NO_CASH; potBucks -= GROUND_BUCKS }
            else { val c = groundCost(); if (cash < c) return Result.NO_CASH; spend(c) }
        }
        if (dirt) site.dirt = color else site.grass = color
        return Result.OK
    }
    fun nextLand(): Triple<Int, Int, Long>? = if (floor > 0) null else LAND.firstOrNull { it.first > land }
    fun nextShack(): ShackLevel? = SHACKS.getOrNull(shack + 1)

    fun price(strain: String): Double {
        val s = Strains[strain] ?: return 0.0
        return s.price * priceMult() * (demand[strain] ?: 1.0) * (if (favourite(s)) FAVOURITE_PRICE else 1.0)
    }

    /** This week's in-season strains: three of the ones you can buy, sell for more and grow faster. */
    fun favourites(): Set<String> {
        val key = lastTick / Season.WEEK * 1000 + level
        if (key != favKey) {
            val pool = Strains.all.filter { !it.exotic && it.holiday == null && it.bucks == 0L && it.site == null && it.level <= max(level, 3) }
            favCache = pool.shuffled(Random(key / 1000 * 31 + 7)).take(3).map { it.id }.toSet(); favKey = key
        }
        return favCache
    }
    private var favKey = -1L
    private var favCache = emptySet<String>()
    fun favourite(s: Strain) = s.id in favourites()
    /** Growth from the strain's own genes and the season. */
    fun genes(s: Strain) = (if (Trait.FAST in s.traits) 1.1 else 1.0) * (if (favourite(s)) FAVOURITE_GROW else 1.0)
    fun seedCost(s: Strain): Long = if (Trait.THRIFTY in s.traits) max(1, s.seed * 3 / 4) else s.seed

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
        return s.price * f.grams * f.mult * priceMult() * (if (on(Buff.HIPPIES)) 2.0 else 1.0) * (if (built(Special.CURING)) 1.25 else 1.0)
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

    fun sprinkled(x: Int, y: Int) = floor > 0 || around(x, y, Kind.SPRINKLER) > 0 || barrelBeside(x, y) || houseNear(x, y, WorkHouse.TOWER) > 0 || here(Special.WELL)
    /** Grow lamps lighting x,y; none when the farm is short of power. */
    fun lamps(x: Int, y: Int) = if (!powered()) 0 else if (floor > 0) 2 else min(2, around(x, y, Kind.LAMP))
    fun protected(x: Int, y: Int) = decorAround(x, y, Decor.SCARECROW) > 0

    /** Growth speed multiplier before water and buffs. */
    fun speed(x: Int, y: Int): Double = (1 + 0.06 * up(Upgrade.NUTRIENTS)) * (1 + LAMP_BOOST * lamps(x, y)) *
        (1 + 0.05 * decorAround(x, y, Decor.GNOME) + 0.10 * decorAround(x, y, Decor.TIKI) + 0.15 * decorAround(x, y, Decor.LAVA) +
            0.20 * decorAround(x, y, Decor.WINDMILL) + 0.25 * decorAround(x, y, Decor.UFO) + perkNear(x, y, Perk.GROW)) *
        (1 + 0.2 * houseNear(x, y, WorkHouse.GROW)) * (1 + 0.1 * crew(Crew.BOTANIST)) *
        (1 + 0.1 * legacy) * Legacy.mult(this, LegacyPerk.GREEN_THUMB, 0.15) * (if (floor > 0) (if (powered()) 1.0 else Bunker.DARK) else when (season()) { Season.SPRING -> 1.15; Season.WINTER -> 0.9; else -> 1.0 }) *
        (if (here(Special.VAULT)) 1.3 else 1.0) * (if (built(Special.TEMPLE)) 1.1 else 1.0) * (if (shaded(x, y)) SHADE else 1.0)

    /** Whether something big (the shack, a vehicle, a 2×2 work building) stands beside x,y, shading it. */
    fun shaded(x: Int, y: Int) = neighbours(x, y) {
        when (it.kind) {
            Kind.SHACK, Kind.PART, Kind.VEHICLE, Kind.CAB, Kind.WING -> true
            Kind.HOUSE -> WorkHouse.of(it.item)?.size == 2
            else -> false
        }
    } > 0

    fun goldenChance(x: Int, y: Int): Double = GOLDEN_CHANCE + 0.03 * decorAround(x, y, Decor.BEEHIVE) +
        0.06 * decorAround(x, y, Decor.ALIEN) + 0.08 * decorAround(x, y, Decor.TOTEM) + perkNear(x, y, Perk.GOLD) + (if (on(Buff.LUCKY)) 0.21 else 0.0) + (if (on(Buff.DUST)) 0.15 else 0.0) +
        (if (here(Special.DOME)) 0.08 else 0.0) + (if (Legacy.has(this, LegacyPerk.GOLDEN_TOUCH)) 0.05 else 0.0)

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

    private fun anyHelper() = helpers.any { it } || sites.any { s -> s.workers > 0 || s.crew.any { it > 0 } || s.tiles.any { it.kind == Kind.VEHICLE } }

    fun tick(now: Long) {
        if (lastTick == 0L || now <= lastTick) { lastTick = now; return }
        val total = now - lastTick
        // Without helpers nothing depends on the order things happen in, so one step is exact. With them, step
        // through the gap so offline harvests, replants and sales land roughly when they would have.
        layoutRev++
        val helping = anyHelper()
        val step = if (helping) max(30_000L, total / max(200, 2000 / sites.size)) else total
        var t = lastTick
        while (t < now) {
            val t1 = min(now, t + step)
            eachSite { grow(t, t1); Storefronts.sell(this, t, t1); eachFloor { grow(t, t1) } }
            recoverDemand(t1 - t)
            refillOrders(t1)
            lastTick = t1
            if (helping) work(t1)
            t = t1
        }
        if (total > 60_000L && helping) away.millis += total
        lastTick = now
        buffs.entries.removeAll { it.value <= now }
        if (mineAt > 0 && built(Special.MINE)) {
            var n = 0L
            while (now >= mineAt) { n++; mineAt += Special.MINE_MILLIS }
            if (n > 0) { gems += n; events += FarmEvent.Gems(n) }
        }
        if (research != null && now >= researchAt) finishResearch()
        Contracts.expire(this, now)
        refreshQuests(now)
    }

    private fun grow(t0: Long, t1: Long) {
        val dt = (t1 - t0) / 1000.0
        val b = boost(t0, t1)
        eachLand { x, y, t ->
            if (t.kind != Kind.PLOT || t.pest || t.mould) return@eachLand
            val s = t.strain?.let { Strains[it] } ?: return@eachLand
            if (t.progress >= s.seconds) return@eachLand
            val wet = if (sprinkled(x, y)) dt else ((min(t.wetUntil, t1) - t0) / 1000.0).coerceIn(0.0, dt)
            val gain = (wet * WET_BOOST + (dt - wet)) * speed(x, y) * genes(s) * b
            val need = s.seconds - t.progress
            t.progress = min(s.seconds.toDouble(), t.progress + gain)
            // When in this stretch it ripened, so a long time away doesn't count as just ripe.
            if (t.progress >= s.seconds && gain > 0) t.ripeAt = t0 + ((t1 - t0) * (need / gain)).toLong()
        }
    }

    private fun recoverDemand(ms: Long) {
        // Half-life of 45 minutes back towards full demand.
        val k = exp(-ln(2.0) * ms * (if (built(Special.EXCHANGE)) 3 else 1) / (45 * 60_000.0))
        val it = demand.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val d = 1 - (1 - e.value) * k
            if (d > 0.999) it.remove() else e.setValue(d)
        }
    }

    private fun refillOrders(now: Long) {
        for (i in 0 until orderSlots()) {
            if (deliverAt[i] > 0 && now >= deliverAt[i]) arrive(i, now)
            if (orders[i] == null) {
                if (orderAt[i] == 0L) orderAt[i] = now + 30_000L
                else if (now >= orderAt[i]) { orders[i] = newOrder(); orderAt[i] = 0L }
            }
        }
    }

    /** Everything the hired help does each step. */
    private fun work(now: Long) {
        val active = site; val down = floor
        // Pops for the other farms (and floors) would show up in the wrong place.
        fun quiet(from: Int) { for (i in events.size - 1 downTo from) if (events[i] is FarmEvent.Harvest) events.removeAt(i) }
        eachSite { s ->
            val before = events.size
            if (farmhand) pick(now)
            if (has(Helper.COOK)) { cook(now); eachFloor { cook(now) } }
            if (s !== active || down != 0) quiet(before)
            if (farmhand) eachFloor { n -> val b = events.size; pick(now); if (s !== active || n != down) quiet(b) }
        }
        eachSite { s -> if (s.workers > 0) waterIdle(now) }
        eachSite { s ->
            if (s.crew[Crew.GUARD.ordinal] > 0) eachLand { _, _, t -> if (t.pest || t.mould) { t.pest = false; t.mould = false; bump(Stat.PESTS) } }
            val before = events.size
            drive(now)
            if (s !== active) for (i in events.size - 1 downTo before) if (events[i] is FarmEvent.Cash) events.removeAt(i)
        }
        if (has(Helper.FETCHER)) fetch()
        if (has(Helper.RUNNER) && runnerOn && now >= runnerAt) {
            runnerAt = now + runnerMillis()
            away.sold += run()
        }
    }

    /** The farmhand picks everything ready and clears pests; with the Grow Manager the same strain goes back in. */
    private fun pick(now: Long) {
        eachLand { x, y, t ->
            if (t.kind != Kind.PLOT) return@eachLand
            if (t.pest || t.mould) { t.pest = false; t.mould = false; bump(Stat.PESTS); return@eachLand }
            if (!t.ready()) return@eachLand
            val before = events.size
            if (harvest(x, y, now, byHand = false) != Result.OK) return
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
            spend(unit * n)
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
            if (spare > 0) got += sell(k, spare, byHand = false)
        }
        return got + sellAllGoods()
    }

    // ---- Random events (only while playing) ----

    /** Call while the game is open. Returns true when a new [happening] needs showing. */
    fun rollEvent(now: Long): Boolean = surface { rollUp(now) }
    private fun rollUp(now: Long): Boolean {
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
        .filter { (x, y) -> tile(x, y).let { it.kind == Kind.PLOT && it.strain != null && !it.ready() && !it.pest && !it.mould } }

    fun makeEvent(now: Long, force: EventKind? = null): Happening? {
        val dog = placed(Decor.DOG)
        val pests = growing().filter { (x, y) -> !protected(x, y) && !hardy(x, y) }
        val choices = ArrayList<Pair<EventKind, Int>>()
        if (level >= 5 && cash >= 500 && !built(Special.TEMPLE) && !on(Buff.SCANNER)) choices += EventKind.RAID to (if (dog) 1 else 2)
        if (pests.isNotEmpty()) choices += EventKind.PESTS to 3
        if (count(Kind.PLOT) > 0) choices += EventKind.RAIN to 2
        choices += EventKind.RUSH to 2
        choices += EventKind.LUCKY to 2
        choices += EventKind.VIBES to 2
        if (tiles.any { it.kind == Kind.FACILITY }) choices += EventKind.HIPPIES to 2
        if (level >= BLACK_MARKET_LEVEL) choices += EventKind.STRANGER to 2
        choices += EventKind.CELEB to 1
        if (stash.isNotEmpty()) choices += EventKind.TRADER to 2
        choices += EventKind.METEOR to 1
        choices += EventKind.TOURISTS to 2
        if (count(Kind.PLOT) > 0) choices += EventKind.HEATWAVE to 1
        if (growing().isNotEmpty()) choices += EventKind.MOON to 1
        if (stash.isNotEmpty()) choices += EventKind.GRANDMA to 2
        if (count(Kind.PLOT) > 0) choices += EventKind.STORM to 1
        choices += EventKind.POTBUCKS to 1
        if (holiday() != null) choices += EventKind.HOLIDAY to 4
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
                val item = BmItem.entries.filter { it.strain != null && it.site == null && it.level <= level }.randomOrNull(rng) ?: return null
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
            EventKind.METEOR -> {
                val c = nice(100.0 + 40.0 * level * level); val pb = 2 + level / 20
                earn(c); potBucks += pb
                events += FarmEvent.Cash(c)
                Happening(kind, amount = c, grams = pb)
            }
            EventKind.TOURISTS -> { addBuff(Buff.TOURISTS, now, 30); Happening(kind, amount = 30) }
            EventKind.HEATWAVE -> { addBuff(Buff.HEATWAVE, now, 20); Happening(kind, amount = 20) }
            EventKind.MOON -> { addBuff(Buff.HARVEST_MOON, now, 30); Happening(kind, amount = 30) }
            EventKind.GRANDMA -> {
                val (s, have) = stash.entries.random(rng).toPair()
                val g = min(have, max(1, (Strains[s]?.yield ?: 5) * (1 + rng.nextInt(2))))
                Happening(kind, amount = (price(s) * g * 3).roundToLong(), strain = s, grams = g)
            }
            EventKind.STORM -> {
                var n = 0
                eachLand { _, _, t ->
                    if (t.kind != Kind.PLOT) return@eachLand
                    t.wetUntil = max(t.wetUntil, now + waterMillis() * 2)
                    if (t.pest) { t.pest = false; n++ }
                }
                Happening(kind, plots = n)
            }
            EventKind.POTBUCKS -> {
                val pb = 1L + rng.nextInt(3)
                potBucks += pb
                Happening(kind, amount = pb)
            }
            EventKind.HOLIDAY -> {
                val h = holiday() ?: return null
                val tk = 8L + rng.nextInt(8)
                val pick = stash.entries.randomOrNull(rng)
                if (pick == null) { addTokens(h, tk / 2); Happening(kind, amount = tk / 2, strain = null) }
                else Happening(kind, amount = tk, strain = pick.key, grams = min(pick.value, 3 + rng.nextInt(6)))
            }
        }
    }

    fun addBuff(b: Buff, now: Long, minutes: Int) {
        buffs[b] = max(buffs[b] ?: 0L, now) + minutes * 60_000L
    }

    /** Answers the current event. [accept] means bribe / buy / sell; returns what happened, for a toast. */
    fun resolve(accept: Boolean, now: Long): String = surface { resolveUp(accept, now) }
    private fun resolveUp(accept: Boolean, now: Long): String {
        val h = happening ?: return ""
        happening = null
        if (h.kind == EventKind.RAID) bump(Stat.RAIDS)
        return when (h.kind) {
            EventKind.RAID -> when {
                has(Helper.LOOKOUT) -> "Your lookout saw them coming. Nothing found."
                accept && envelopes > 0 -> { envelopes--; "The envelope did the talking. They left smiling." }
                accept && cash >= h.amount -> { spend(h.amount); "Paid ${h.amount}. \"Nice farm you got here.\"" }
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
                    spend(h.amount)
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
            EventKind.GRANDMA -> {
                val s = h.strain!!
                if (!accept) "Grandma tuts and toddles off."
                else if ((stash[s] ?: 0) < h.grams) "You don't have that much anymore."
                else {
                    val left = (stash[s] ?: 0) - h.grams
                    if (left == 0) stash.remove(s) else stash[s] = left
                    earn(h.amount)
                    events += FarmEvent.Cash(h.amount)
                    "Grandma paid triple and pinched your cheek."
                }
            }
            EventKind.HOLIDAY -> {
                val hol = holiday()
                val s = h.strain
                when {
                    s == null || hol == null -> ""
                    !accept -> "They wander off to the next farm."
                    (stash[s] ?: 0) < h.grams -> "You don't have that much anymore."
                    else -> {
                        val left = (stash[s] ?: 0) - h.grams
                        if (left == 0) stash.remove(s) else stash[s] = left
                        addTokens(hol, h.amount)
                        "Got ${h.amount} ${hol.token}!"
                    }
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
            if (cash < seedCost(s)) return Result.NO_CASH
            spend(seedCost(s))
        }
        t.strain = strain; t.progress = 0.0; t.last = strain; t.pest = false; t.mould = false; t.trim = 0; t.trimAt = 0.0
        t.pheno = mothers[strain]?.takeIf { strain !in hunting }?.let { it or Pheno.CLONE } ?: Pheno.roll(rng, phenoLuck(s))
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

    /** Plots on this farm with a ripe plant waiting. */
    fun ripeCount(): Int { var n = 0; eachLand { _, _, t -> if (t.ready()) n++ }; return n }

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

    /** Damp weather (rain, fog, storms) now and then grows mould on a half-grown plant with no lamp over it. Once a minute. */
    fun damp(now: Long, w: Weather): Int {
        if (level < EVENT_LEVEL || now < mouldAt) return 0
        mouldAt = now + 60_000L
        val odds = when (w) { Weather.RAIN -> 0.08; Weather.FOG -> 0.12; Weather.STORM -> 0.2; else -> return 0 }
        if (Random.nextDouble() >= odds) return 0
        val (x, y) = growing().filter { (x, y) ->
            val t = tile(x, y); val s = Strains[t.strain!!] ?: return@filter false
            t.progress >= s.seconds / 2 && lamps(x, y) == 0 && Trait.HARDY !in s.traits
        }.randomOrNull() ?: return 0
        tile(x, y).mould = true
        return 1
    }
    private var mouldAt = 0L
    private fun hardy(x: Int, y: Int) = tile(x, y).strain?.let { Strains[it] }?.let { Trait.HARDY in it.traits } == true

    /** Shoos bugs or scrapes off mould. */
    fun clearPest(x: Int, y: Int): Result {
        val t = tile(x, y)
        if (!t.pest && !t.mould) return Result.BLOCKED
        t.pest = false; t.mould = false
        bump(Stat.PESTS)
        addXp(1)
        return Result.OK
    }

    /** How long a ripe plant stays at its peak: a fifth of its growing time, 3 to 30 minutes. */
    fun peakMillis(s: Strain) = (s.seconds * 200L).coerceIn(PEAK_MIN, PEAK_MAX)

    /** Millis left in the plant's peak window, or 0 when it's past (or not ripe). */
    fun peakLeft(x: Int, y: Int, now: Long): Long {
        val t = tile(x, y)
        if (!t.ready() || t.ripeAt <= 0) return 0
        val s = Strains[t.strain!!] ?: return 0
        return (t.ripeAt + peakMillis(s) - now).coerceAtLeast(0)
    }

    /** How much growth a strain needs between trims: a fifth to a third of its grow, fixed per strain. */
    fun trimGap(s: Strain) = s.seconds * (0.2 + 0.045 * Math.floorMod(s.id.hashCode(), 4))

    /** A growing plant can be trimmed once it's a little way in, again after each [trimGap], until it ripens. */
    fun canTrim(x: Int, y: Int): Boolean {
        val t = tile(x, y)
        val s = t.strain?.let { Strains[it] } ?: return false
        return t.kind == Kind.PLOT && !t.ready() && !t.pest && !t.mould && t.trim < TRIM_BANK &&
            t.progress >= max(t.trimAt, s.seconds * 0.1)
    }

    /** Growth left before [canTrim], in base-speed seconds; 0 if it can be trimmed now. */
    fun trimWait(x: Int, y: Int): Double {
        val t = tile(x, y)
        val s = t.strain?.let { Strains[it] } ?: return 0.0
        return (max(t.trimAt, s.seconds * 0.1) - t.progress).coerceAtLeast(0.0)
    }

    /** Banks [snips] clean leaves (0 to [TRIM_MAX]) from one go at the trimming game: [TRIM_STEP] more bud each at harvest. */
    fun trim(x: Int, y: Int, snips: Int): Result {
        if (!canTrim(x, y)) return Result.NOT_READY
        val t = tile(x, y)
        val s = Strains[t.strain!!]!!
        val n = snips.coerceIn(0, TRIM_MAX)
        t.trim = (t.trim + n).coerceAtMost(TRIM_BANK)
        t.trimAt = t.progress + trimGap(s)
        if (n == TRIM_MAX) bump(Stat.CLEAN_TRIMS)
        return Result.OK
    }

    /**
     * Picks a ripe plant. By hand at its peak it gives [PEAK_YIELD] more bud and [PEAK_XP] more XP; whatever was trimmed
     * while it grew gives [TRIM_STEP] more per clean leaf, picked by hand or not.
     */
    fun harvest(x: Int, y: Int, now: Long, byHand: Boolean = true): Result {
        val t = tile(x, y)
        if (t.kind != Kind.PLOT) return Result.BLOCKED
        val s = t.strain?.let { Strains[it] } ?: return Result.BLOCKED
        if (!t.ready()) return Result.NOT_READY
        val pheno = t.pheno
        val peak = byHand && peakLeft(x, y, now) > 0
        val snips = t.trim
        val golden = rng.nextDouble() < goldenChance(x, y) * (if (Trait.GOLDEN in s.traits) 1.5 else 1.0) + Pheno.golden(pheno)
        val mult = yieldMult() * Pheno.yieldMult(pheno) * (if (Trait.BIG in s.traits) 1.12 else 1.0) * (if (s.id in heirlooms) Legacy.HEIRLOOM_YIELD else 1.0) * (1 + 0.08 * crew(Crew.TRIMMER)) * (1 + 0.1 * decorAround(x, y, Decor.COMPOST) + 0.2 * decorAround(x, y, Decor.GREENHOUSE) +
            0.25 * decorAround(x, y, Decor.CRYSTAL) + perkNear(x, y, Perk.YIELD)) * (1 + 0.15 * houseNear(x, y, WorkHouse.TRIM)) *
            (if (here(Special.TERRACE)) 1.25 else 1.0) * (if (peak) PEAK_YIELD else 1.0) * (1 + TRIM_STEP * snips)
        var grams = max(1, (s.yield * mult * (0.9 + rng.nextDouble() * 0.25)).roundToLong().toInt())
        if (golden) grams *= 3
        if (stashUsed() + grams > stashCap()) return Result.STASH_FULL
        stash[s.id] = (stash[s.id] ?: 0) + grams
        t.strain = null; t.progress = 0.0; t.pheno = 0; t.ripeAt = 0; t.trim = 0; t.trimAt = 0.0
        if (peak) bump(Stat.PEAKS)
        if (Pheno.set(pheno) && !Pheno.clone(pheno)) find(s, if (golden) Pheno.shine(pheno) else pheno)
        totalHarvests++
        site.harvests++
        grown += s.id
        bump(Stat.HARVESTS); bump(Stat.GRAMS, grams.toLong())
        if (golden) bump(Stat.GOLDEN)
        if (s.exotic) bump(Stat.EXOTIC)
        val xp = (s.xp * xpMult() * Pheno.xpMult(pheno) * (if (peak) PEAK_XP else 1.0)).roundToLong()
        events += FarmEvent.Harvest(x, y, grams, xp.toInt(), golden, peak, snips)
        if (rng.nextDouble() < (if (golden) 0.1 else 0.01)) { potBucks++; events += FarmEvent.Bucks(1) }
        if (golden && rng.nextDouble() < 0.03) { gems++; events += FarmEvent.Gems(1) }
        // Picking by hand during a holiday: a good chance of a few tokens.
        if (byHand && rng.nextDouble() < (if (golden) 0.8 else 0.35)) holiday()?.let { h ->
            val tk = ((1 + rng.nextInt(3)) * tokenMult(s.level)).roundToLong()
            addTokens(h, tk); events += FarmEvent.Bucks(tk, h.id)
        }
        addXp(xp)
        return Result.OK
    }

    /** Lab crosses roll better phenos for each parent kept as a mother. */
    fun phenoLuck(s: Strain) = s.parents?.toList()?.count { it in mothers } ?: 0

    private fun find(s: Strain, p: Int) {
        if (Pheno.score(p) <= Pheno.score(finds[s.id] ?: 0)) return
        finds[s.id] = p
        if (Pheno.yieldGrade(p) >= 3 || Pheno.potency(p) >= 3 || Pheno.colour(p) > 0) events += FarmEvent.Find(s, p)
    }

    fun motherSlots() = 2 + level / 10

    /** Keeps the best find of [id] as its mother, so planting it gives clones. */
    fun keepMother(id: String): Result {
        val p = finds[id] ?: return Result.BLOCKED
        if (mothers[id] == p) return Result.BLOCKED
        if (id !in mothers && mothers.size >= motherSlots()) return Result.LIMIT
        mothers[id] = p and Pheno.CLONE.inv()
        hunting -= id
        return Result.OK
    }

    fun dropMother(id: String): Result {
        if (mothers.remove(id) == null) return Result.BLOCKED
        hunting -= id
        return Result.OK
    }

    /** With a mother kept: plant seeds to hunt for a better pheno, or clones of her. */
    fun toggleHunt(id: String) { if (id !in mothers) return; if (!hunting.remove(id)) hunting += id }

    fun addXp(n: Long) {
        xp += n
        while (level < MAX_LEVEL && xp >= xpNeed(level)) {
            xp -= xpNeed(level)
            level++
            bestLevel = max(bestLevel, level)
            val bonus = 25L * level * level
            cash += bonus
            val pb = 2L + level / 10
            potBucks += pb
            val g = if (level % 5 == 0) 2L else 0L
            gems += g
            events += FarmEvent.LevelUp(level, bonus, unlocksAt(level), pb, g)
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
        WorkHouse.entries.filter { it.level == lvl }.forEach { out += it.title }
        Crew.entries.filter { it.level == lvl }.forEach { out += it.title + " (crew)" }
        Vehicle.entries.filter { it.level == lvl }.forEach { out += it.title }
        Generator.entries.filter { it.level == lvl }.forEach { out += it.title }
        Special.entries.filter { it.level == lvl }.forEach { out += "${it.title} (special)" }
        if (lvl == WORKER_LEVEL) out += "Workers"
        if (lvl == SPRINKLER_LEVEL) out += "Sprinklers"
        if (lvl == LAMP_LEVEL) out += "Grow lamps"
        if (lvl == BLACK_MARKET_LEVEL) out += "The black market"
        if (lvl == EVENT_LEVEL) out += "Random events"
        if (lvl == Progress.BREED_LEVEL) out += "The breeding lab"
        if (lvl == Cup.LEVEL) out += "The Cannabis Cup"
        if (lvl == Hunt.LEVEL) out += "Seed hunts"
        if (lvl == Contracts.LEVEL) out += "Farm contracts"
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
        spend(cost)
        tile(x, y).kind = kind
        layoutRev++
        return Result.OK
    }

    /** Facilities go on grass up top, or on an empty plot in the bunker. */
    fun canFacility(x: Int, y: Int) = inLand(x, y) && tile(x, y).let { if (floor > 0) it.kind == Kind.PLOT && it.strain == null else it.kind == Kind.GRASS }

    fun buildFacility(x: Int, y: Int, f: Facility): Result {
        if (!canFacility(x, y)) return Result.BLOCKED
        if (level < f.level) return Result.LOCKED
        val cost = facilityCost(f)
        if (cash < cost) return Result.NO_CASH
        spend(cost)
        tile(x, y).apply { clear(); kind = Kind.FACILITY; item = f.name; lvl = 1 }
        layoutRev++
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
        else { if (cash < d.cost) return Result.NO_CASH; spend(d.cost) }
        tile(x, y).apply { kind = if (d.path) Kind.PATH else Kind.DECOR; item = d.name }
        return Result.OK
    }

    /** Clears a square back to grass. Plots must be empty; decor goes back in your inventory; buildings can only be moved. */
    fun demolish(x: Int, y: Int): Result {
        if (floor > 0) return Result.BLOCKED
        val t = tile(x, y)
        when (t.kind) {
            Kind.GRASS, Kind.SHACK, Kind.PART, Kind.FACILITY, Kind.HOUSE, Kind.ROCK, Kind.WATER, Kind.POWER, Kind.SPECIAL, Kind.VEHICLE, Kind.CAB, Kind.WING, Kind.STAIRS -> return Result.BLOCKED
            Kind.PLOT -> if (t.strain != null || count(Kind.PLOT) <= 1) return Result.BLOCKED
            Kind.DECOR, Kind.PATH -> { val d = t.item ?: ""; decorInv[d] = (decorInv[d] ?: 0) + 1 }
            else -> {}
        }
        t.clear()
        layoutRev++
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
        // Down in the bunker, only a facility moves, swapping places with an empty plot.
        if (floor > 0) {
            val a = tile(fx, fy); val b = tile(tx, ty)
            if (a.kind != Kind.FACILITY || !inLand(tx, ty) || b.kind != Kind.PLOT || b.strain != null) return Result.BLOCKED
            val keep = Tile().also { it.copyFrom(a) }
            a.copyFrom(b); b.copyFrom(keep)
            layoutRev++
            return Result.OK
        }
        val from = tile(fx, fy)
        if (from.kind == Kind.SHACK || from.kind == Kind.PART) {
            val (ax, ay) = shackAt() ?: return Result.BLOCKED
            if (tx == ax && ty == ay) return Result.BLOCKED
            clearShack()
            if (placeShack(tx, ty)) return Result.OK
            placeShack(ax, ay)
            return Result.BLOCKED
        }
        if (from.kind == Kind.VEHICLE || from.kind == Kind.CAB) {
            val a = if (from.kind == Kind.CAB) from.anchor else fy * MAX + fx
            val ax = a % MAX; val ay = a / MAX
            val v = Vehicle.of(tile(ax, ay).item) ?: return Result.BLOCKED
            if (tx == ax && ty == ay) return Result.BLOCKED
            val due = tile(ax, ay).doneAt
            clearVehicle(ax, ay)
            if (placeVehicle(tx, ty, v, due)) { layoutRev++; return Result.OK }
            placeVehicle(ax, ay, v, due)
            return Result.BLOCKED
        }
        if (from.kind == Kind.STAIRS) {
            val a = from.anchor; val ax = a % MAX; val ay = a / MAX; val dir = from.lvl - 1
            if (tx == ax && ty == ay) return Result.BLOCKED
            stairCells(ax, ay, dir).forEach { (cx, cy) -> tile(cx, cy).clear() }
            if (placeStairs(tx, ty, dir)) { layoutRev++; return Result.OK }
            placeStairs(ax, ay, dir)
            return Result.BLOCKED
        }
        if (from.kind == Kind.HOUSE || from.kind == Kind.WING) {
            val (ax, ay) = anchorOf(fx, fy)
            val h = WorkHouse.of(tile(ax, ay).item) ?: return Result.BLOCKED
            if (tx == ax && ty == ay) return Result.BLOCKED
            val keep = Tile().also { it.copyFrom(tile(ax, ay)) }
            val cells = houseCells(ax, ay)
            cells.forEach { (cx, cy) -> tile(cx, cy).clear() }
            site.spots = null
            if (placeHouse(tx, ty, h, keep)) { layoutRev++; return Result.OK }
            // Put it back just as it was (an old one-square building stays one square).
            val a = ay * MAX + ax
            cells.forEachIndexed { i, (cx, cy) -> tile(cx, cy).apply { if (i == 0) copyFrom(keep) else kind = Kind.WING; anchor = a } }
            return Result.BLOCKED
        }
        if (from.kind == Kind.GRASS || from.kind == Kind.ROCK || from.kind == Kind.WATER || !inLand(tx, ty)) return Result.BLOCKED
        val to = tile(tx, ty)
        if (to.kind != Kind.GRASS) return Result.BLOCKED
        val stunt = stuntSeconds(fx, fy)
        to.copyFrom(from)
        from.clear()
        // Digging up a growing plant sets it back.
        if (stunt > 0) to.progress -= stunt
        site.spots = null
        layoutRev++
        return Result.OK
    }

    /** Seconds of growth moving the plant at x,y would cost it: 0 unless it's still growing. */
    fun stuntSeconds(x: Int, y: Int): Double {
        val t = tile(x, y)
        if (t.kind != Kind.PLOT || t.ready()) return 0.0
        val s = t.strain?.let { Strains[it] } ?: return 0.0
        return s.seconds * s.stunt
    }

    /** Pulls up whatever is growing at x,y and throws it on the compost. The plot stays. */
    fun uproot(x: Int, y: Int): Result {
        val t = tile(x, y)
        if (!inLand(x, y) || t.kind != Kind.PLOT || t.strain == null) return Result.BLOCKED
        t.strain = null; t.progress = 0.0; t.pest = false; t.mould = false; t.trim = 0; t.trimAt = 0.0
        return Result.OK
    }

    // ---- Hippie festivals ----

    fun canFestival() = land >= FESTIVAL_LAND
    fun festivalCost(): Long = nice(2000.0 + 400.0 * level * level)
    fun festivalOn(now: Long = lastTick) = festivalUntil > now
    /** Throws a festival on the farm on screen: tents and hippies for [FESTIVAL_HOURS] hours, and prices ×1.5. */
    fun festival(now: Long): Result {
        if (!canFestival()) return Result.LOCKED
        if (festivalOn(now)) return Result.BUSY
        val c = festivalCost()
        if (cash < c) return Result.NO_CASH
        spend(c)
        festivalUntil = now + FESTIVAL_HOURS * 3_600_000L
        festivalSite = site.def.id
        buffs[Buff.FESTIVAL] = festivalUntil
        bump(Stat.EVENTS)
        return Result.OK
    }

    // ---- Holiday shop ----

    private fun payTokens(h: Holiday, n: Long): Result {
        if (!h.active(lastTick) && Holiday.force != h) return Result.LOCKED
        if (tokens(h) < n) return Result.NO_CASH
        tokens[h.id] = tokens(h) - n
        return Result.OK
    }
    fun tokenPrice(s: Strain) = if (s.minutes <= 60) Holiday.QUICK else Holiday.LONG
    fun buyHolidayStrain(h: Holiday, s: Strain): Result {
        if (s.holiday != h.id || s.id in unlockedStrains) return Result.BLOCKED
        payTokens(h, tokenPrice(s)).let { if (it != Result.OK) return it }
        unlockedStrains += s.id
        return Result.OK
    }
    fun buyHolidayDecor(h: Holiday, d: Decor): Result {
        if (d.holiday != h.id) return Result.BLOCKED
        payTokens(h, Holiday.DECOR).let { if (it != Result.OK) return it }
        decorInv[d.name] = (decorInv[d.name] ?: 0) + 1
        return Result.OK
    }
    fun buyPet(h: Holiday): Result {
        val p = h.pet
        if (p.name in pets) return Result.BLOCKED
        payTokens(h, Holiday.PET).let { if (it != Result.OK) return it }
        pets += p.name
        return Result.OK
    }
    /** The holiday farm: like any other farm, but paid for in tokens. */
    fun buyHolidayFarm(h: Holiday): Result {
        val def = h.site
        if (owns(def.id)) return Result.BLOCKED
        payTokens(h, Holiday.FARM).let { if (it != Result.OK) return it }
        addSite(def)
        return Result.OK
    }

    // ---- Pot Bucks shop ----

    fun buyBucksDecor(d: Decor): Result {
        if (d.bucks <= 0) return Result.BLOCKED
        if (potBucks < d.bucks) return Result.NO_CASH
        potBucks -= d.bucks
        decorInv[d.name] = (decorInv[d.name] ?: 0) + 1
        return Result.OK
    }
    fun buyBucksStrain(s: Strain): Result {
        if (s.bucks <= 0 || s.id in unlockedStrains) return Result.BLOCKED
        if (potBucks < s.bucks) return Result.NO_CASH
        potBucks -= s.bucks
        unlockedStrains += s.id
        return Result.OK
    }
    fun buyBucksFarm(def: SiteDef): Result {
        if (owns(def.id)) return Result.BLOCKED
        if (level < def.level) return Result.LOCKED
        if (potBucks < def.price) return Result.NO_CASH
        potBucks -= def.price
        addSite(def)
        return Result.OK
    }

    fun buyWithBucks(item: BucksItem, now: Long): Result {
        if (potBucks < item.price) return Result.NO_CASH
        when {
            item.buff != null -> addBuff(item.buff, now, item.minutes)
            item == BucksItem.SEEDS -> {
                val ex = BmItem.entries.filter { it.strain != null && it.site == null && it.level <= maxOf(6, level) }.last()
                seeds[ex.strain!!] = (seeds[ex.strain] ?: 0) + BmItem.EXOTIC_PACK
            }
            item == BucksItem.ENVELOPE -> envelopes++
            item == BucksItem.CASH -> earn(nice(500.0 + 100.0 * level * level))
            item == BucksItem.GEM -> gems++
        }
        potBucks -= item.price
        return Result.OK
    }

    /** Changes your farmer's look: free the first time, then cash or Pot Bucks. */
    fun restyle(f: Farmer, bucks: Boolean): Result {
        if (farmer != null) {
            if (bucks) { if (potBucks < Farmer.RESTYLE_BUCKS) return Result.NO_CASH; potBucks -= Farmer.RESTYLE_BUCKS }
            else { val c = Farmer.restyleCost(level); if (cash < c) return Result.NO_CASH; spend(c) }
        }
        farmer = f
        return Result.OK
    }

    // ---- Where the farm sits ----

    /** What moving the farm on screen to another spot of its map costs: a fee plus a share of what the land cost. */
    fun relocateCost(): Long = nice(1000.0 + 0.1 * LAND.filter { it.first <= land }.sumOf { it.third } + 0.05 * site.def.price)

    /** Whether the owned square could sit with its middle at cx,cy: no water or rim in the way. */
    fun canPlace(def: SiteDef, cx: Int, cy: Int, n: Int): Boolean {
        val x0 = (cx - n / 2).coerceIn(0, MAX - n); val y0 = (cy - n / 2).coerceIn(0, MAX - n)
        val probe = mapOf(def)
        var bad = 0
        for (y in y0 until y0 + n) for (x in x0 until x0 + n) if (probe[y * MAX + x].kind == Kind.WATER) bad++
        return bad <= n * n / 4
    }

    /** The bare map of [def], with no land cleared: what a picker draws. */
    fun mapOf(def: SiteDef): Array<Tile> = Array(MAX * MAX) { Tile() }.also { Sites.carve(def, it, -10, -10, 0) }

    /**
     * Sells the land the farm on screen sits on and buys the same size somewhere else on the map: everything on it
     * moves across as it is. [free] is for a brand-new game picking its spot.
     */
    fun relocate(cx: Int, cy: Int, free: Boolean = false): Result {
        if (floor > 0) return Result.BLOCKED
        val s = site
        val n = land
        val nx = (cx - n / 2).coerceIn(0, MAX - n); val ny = (cy - n / 2).coerceIn(0, MAX - n)
        if (nx == ox && ny == oy) return Result.BLOCKED
        if (!canPlace(s.def, cx, cy, n)) return Result.BLOCKED
        val c = relocateCost()
        if (!free) { if (cash < c) return Result.NO_CASH; spend(c) }
        val old = Array(n * n) { i -> Tile().also { it.copyFrom(tile(ox + i % n, oy + i / n)) } }
        val dx = nx - ox; val dy = ny - oy
        s.tiles.forEach { it.clear() }
        s.cx = cx; s.cy = cy
        Sites.carve(s.def, s.tiles, nx, ny, n)
        for (i in old.indices) {
            val t = tile(nx + i % n, ny + i / n)
            t.copyFrom(old[i])
            if (t.anchor >= 0) t.anchor += dy * MAX + dx
        }
        s.spots = null
        return Result.OK
    }

    /** Wipes everything and starts a brand-new game; only the farmer's look is kept, for the creator to start from. */
    fun restart(now: Long) {
        restartLook = farmer
        sites.clear(); sites += Site(Sites.HOME); site = sites[0]; floor = 0
        land = 6; cash = 30; level = 1; xp = 0
        upgrades.fill(0); helpers.fill(false); supplies.fill(0)
        stash.clear(); goods.clear(); seeds.clear(); decorInv.clear(); buffs.clear(); demand.clear()
        orders.fill(null); orderAt.fill(0); deliverAt.fill(0); deliverPay.fill(0); envelopes = 0
        shack = 0; happening = null; runnerOn = true; runnerAt = 0; bmBought.clear(); bmStaple.clear(); bmRot = -1L
        totalHarvests = 0; totalEarned = 0; stats.fill(0); trophies.clear(); bred.clear(); grown.clear()
        legacy = 0; legacyPerks.clear(); heirlooms.clear(); finds.clear(); mothers.clear(); hunting.clear(); startMap = StartMap.HOMESTEAD; bestLevel = 1; streak = 0; bestStreak = 0; lastDay = -1L; questDay = -1L
        quests.clear(); questBonus = false; events.clear()
        potBucks = 0; gems = 0; mineAt = 0; tokens.clear(); unlockedStrains.clear(); pets.clear()
        farmer = null; festivalUntil = 0; festivalSite = null
        cupWeek = -1L; cupScores.clear(); hunt = null; huntBack = 0
        offers.clear(); offerDay.clear(); deals.clear(); rep.clear(); chains.clear(); weekly.clear(); lastDeal = null; lapsed = 0
        research = null; researchAt = 0
        lastTick = now; nextEventAt = 0
        newGame(now)
        refreshQuests(now)
    }
    /** The look of the farmer before a restart, to start the creator from. Not saved. */
    var restartLook: Farmer? = null

    fun upgradeShack(): Result {
        val next = nextShack() ?: return Result.BLOCKED
        if (level < next.level) return Result.LOCKED
        if (cash < next.cost) return Result.NO_CASH
        spend(next.cost)
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
        spend(next.third)
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
        spend(c)
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
        if (def.holiday != null) return Result.BLOCKED
        canBuy(def).let { if (it != Result.OK) return it }
        spend(def.price)
        addSite(def)
        return Result.OK
    }

    private fun addSite(def: SiteDef) {
        val s = Site(def)
        s.land = SiteDef.START_LAND
        Sites.carve(def, s.tiles, s.ox, s.oy, s.land)
        sites += s
        site = s
        placeShack(ox, oy)
        for ((x, y) in listOf(3 to 3, 4 to 3, 3 to 4, 4 to 4)) tile(ox + x, oy + y).kind = Kind.PLOT
    }

    fun switchTo(id: String): Boolean {
        val s = sites.firstOrNull { it.def.id == id } ?: return false
        site = s; floor = 0
        return true
    }

    fun buy(u: Upgrade): Result {
        val lvl = up(u)
        if (lvl >= u.max) return Result.BLOCKED
        if (level < u.level) return Result.LOCKED
        val c = u.cost(lvl)
        if (cash < c) return Result.NO_CASH
        spend(c)
        upgrades[u.ordinal]++
        return Result.OK
    }

    fun hire(h: Helper): Result {
        if (has(h)) return Result.BLOCKED
        if (h == Helper.MANAGER && !farmhand) return Result.BLOCKED
        if (level < h.level) return Result.LOCKED
        if (bestShack() < h.shack) return Result.SHACK
        if (cash < h.cost) return Result.NO_CASH
        spend(h.cost)
        helpers[h.ordinal] = true
        if (h == Helper.RUNNER) runnerAt = lastTick + runnerMillis()
        return Result.OK
    }

    fun hireFarmhand() = hire(Helper.FARMHAND)
    fun hireManager() = hire(Helper.MANAGER)

    // ---- Bunkers ----

    fun hasBunker(s: Site = site) = s.floors.isNotEmpty()
    /** Each farm gets one; every bunker already dug makes the next one dearer. */
    fun bunkerCost(): Long = Bunker.COST shl sites.count { it.floors.isNotEmpty() }
    /** The squares a stairwell going down from x,y covers: along x for [dir] 0, along y for 1 (its tiles keep dir + 1 as their level). */
    fun stairCells(x: Int, y: Int, dir: Int) = listOf(x to y, if (dir == 0) x + 1 to y else x to y + 1)
    fun canStairs(x: Int, y: Int, dir: Int) = floor == 0 && stairCells(x, y, dir).all { (cx, cy) -> inLand(cx, cy) && tile(cx, cy).kind == Kind.GRASS }
    /** The active farm's stairwell: its top square and which way it runs, or null. */
    fun stairsAt(): Triple<Int, Int, Int>? {
        val ts = site.tiles
        val i = ts.indices.firstOrNull { ts[it].kind == Kind.STAIRS && ts[it].anchor == it } ?: return null
        return Triple(i % MAX, i / MAX, ts[i].lvl - 1)
    }

    private fun placeStairs(x: Int, y: Int, dir: Int): Boolean {
        if (!canStairs(x, y, dir)) return false
        val a = y * MAX + x
        for ((cx, cy) in stairCells(x, y, dir)) tile(cx, cy).apply { clear(); kind = Kind.STAIRS; anchor = a; lvl = dir + 1 }
        return true
    }

    /** Digs this farm's bunker: a stairwell at x,y running [dir], and a first floor [Bunker.START] across. */
    fun buyBunker(x: Int, y: Int, dir: Int): Result {
        if (floor > 0 || hasBunker()) return Result.BLOCKED
        if (level < Bunker.LEVEL) return Result.LOCKED
        if (!canStairs(x, y, dir)) return Result.BLOCKED
        val c = bunkerCost()
        if (cash < c) return Result.NO_CASH
        spend(c)
        placeStairs(x, y, dir)
        site.floors += Floor().also { it.resize(Bunker.START) }
        layoutRev++
        return Result.OK
    }

    /** Turns the stairwell the other way about its top square, if there's room. */
    fun rotateStairs(): Result {
        val (x, y, dir) = stairsAt() ?: return Result.BLOCKED
        stairCells(x, y, dir).forEach { (cx, cy) -> site.tiles[cy * MAX + cx].clear() }
        if (surface { placeStairs(x, y, 1 - dir) }) return Result.OK
        surface { placeStairs(x, y, dir) }
        return Result.BLOCKED
    }

    /** What widening floor [n] by one square each way costs, or null when it's as big as it gets. */
    fun widenCost(n: Int = floor): Long? {
        val fl = site.floors.getOrNull(n - 1) ?: return null
        return if (fl.size >= Bunker.SIZE_MAX) null else Bunker.sizeCost(n, fl.size)
    }
    fun widen(n: Int = floor): Result {
        val fl = site.floors.getOrNull(n - 1) ?: return Result.BLOCKED
        val c = widenCost(n) ?: return Result.LIMIT
        if (cash < c) return Result.NO_CASH
        spend(c)
        fl.resize(fl.size + 1)
        layoutRev++
        return Result.OK
    }

    /** The next floor down: its number, the level it needs and its cost, or null when the bunker is as deep as it goes. */
    fun nextFloor(): Triple<Int, Int, Long>? {
        val n = site.floors.size + 1
        return if (!hasBunker() || n > Bunker.FLOORS) null else Triple(n, Bunker.floorLevel(n), Bunker.floorCost(n))
    }
    fun dig(): Result {
        val (n, lvl, c) = nextFloor() ?: return Result.LIMIT
        if (level < lvl) return Result.LOCKED
        if (cash < c) return Result.NO_CASH
        spend(c)
        site.floors += Floor().also { it.resize(Bunker.START) }
        layoutRev++
        floor = n
        return Result.OK
    }

    /** Puts floor [n] of this farm's bunker on screen (0 is up top). */
    fun goDown(n: Int): Boolean {
        if (n !in 0..site.floors.size) return false
        floor = n
        return true
    }

    // ---- Work buildings and workers ----

    /** The active farm's work buildings; the first [Site.workers] of them (bunkhouses aside) are staffed. */
    fun houses(): List<HouseSpot> {
        if (floor > 0) return emptyList()
        site.spots?.let { return it }
        val out = ArrayList<HouseSpot>()
        var free = site.workers
        val wings = site.tiles.filter { it.kind == Kind.WING }.mapTo(HashSet()) { it.anchor }
        eachLand { x, y, t ->
            if (t.kind != Kind.HOUSE) return@eachLand
            val h = WorkHouse.of(t.item) ?: return@eachLand
            val staffed = h != WorkHouse.BUNK && free > 0
            if (staffed) free--
            out += HouseSpot(x, y, h, staffed, if (y * MAX + x in wings) 2 else 1)
        }
        site.spots = out
        return out
    }

    /** The strongest [h] reaching x,y on the active farm: 0 none, 1 unstaffed, 2 staffed. */
    fun houseNear(x: Int, y: Int, h: WorkHouse): Int {
        var best = 0
        for (s in houses()) {
            if (s.house != h) continue
            val reach = h.reach + (if (h == WorkHouse.TOWER && s.staffed) 1 else 0)
            // Reach counts from the nearest square of the building's footprint.
            val dx = if (x < s.x) s.x - x else max(0, x - (s.x + s.size - 1))
            val dy = if (y < s.y) s.y - y else max(0, y - (s.y + s.size - 1))
            if (dx <= reach && dy <= reach) best = max(best, h.power(s.staffed))
        }
        return best
    }

    fun count(h: WorkHouse) = tiles.count { it.kind == Kind.HOUSE && it.item == h.name }
    fun houseCost(h: WorkHouse): Long = nice(h.cost * 2.5.pow(count(h)))

    fun buildHouse(x: Int, y: Int, h: WorkHouse): Result {
        if (!fits(x, y, h.size)) return Result.BLOCKED
        if (level < h.level) return Result.LOCKED
        if (count(h) >= (if (h == WorkHouse.STORE) Storefronts.PER_FARM else WorkHouse.MAX_EACH)) return Result.LIMIT
        val cost = houseCost(h)
        if (cash < cost) return Result.NO_CASH
        spend(cost)
        placeHouse(x, y, h, null)
        layoutRev++
        return Result.OK
    }

    /** Whether an [n]×[n] footprint with its top-left at x,y is all free grass. */
    fun fits(x: Int, y: Int, n: Int) = (0 until n * n).all { i -> inLand(x + i % n, y + i / n) && tile(x + i % n, y + i / n).kind == Kind.GRASS }

    /** Puts [h]'s footprint down with its top-left at x,y, carrying over [keep]'s level and timers when it's being moved. */
    private fun placeHouse(x: Int, y: Int, h: WorkHouse, keep: Tile?): Boolean {
        if (!fits(x, y, h.size)) return false
        val a = y * MAX + x
        for (i in 0 until h.size * h.size) tile(x + i % h.size, y + i / h.size).apply { clear(); kind = Kind.WING; anchor = a }
        tile(x, y).apply {
            if (keep != null) copyFrom(keep) else { kind = Kind.HOUSE; item = h.name; lvl = 1; progress = 0.0 }
            anchor = a
        }
        site.spots = null
        return true
    }

    /** The square a building hangs off: a [Kind.WING]'s work building, else x,y itself. */
    fun anchorOf(x: Int, y: Int): Pair<Int, Int> {
        val t = tile(x, y)
        return if (t.kind == Kind.WING && t.anchor >= 0) (t.anchor % MAX) to (t.anchor / MAX) else x to y
    }

    /** The squares the work building at ax,ay covers, its own first. */
    fun houseCells(ax: Int, ay: Int): List<Pair<Int, Int>> {
        val a = ay * MAX + ax
        return listOf(ax to ay) + tiles.indices.filter { tiles[it].kind == Kind.WING && tiles[it].anchor == a }.map { (it % MAX) to (it / MAX) }
    }

    /**
     * For saves from before work buildings took 2×2: spreads each one over the grass around it, keeping the square
     * it stood on as one of its four. One with no room left stays a single square until it's moved.
     */
    fun spreadHouses() {
        eachLand { x, y, t ->
            val h = WorkHouse.of(t.item)
            if (t.kind != Kind.HOUSE || h == null || h.size == 1 || houseCells(x, y).size > 1) return@eachLand
            val keep = Tile().also { it.copyFrom(t) }
            for ((dx, dy) in listOf(0 to 0, -1 to 0, 0 to -1, -1 to -1)) {
                t.clear()
                if (placeHouse(x + dx, y + dy, h, keep)) return@eachLand
            }
            t.copyFrom(keep); t.anchor = y * MAX + x
        }
        site.spots = null
    }

    /** Workers (and crew) the active farm has beds for: one, plus two per bunkhouse and two for an RV. */
    fun workerCap() = 1 + 2 * count(WorkHouse.BUNK) + (if (vehicleAt(Vehicle.RV) != null) 2 else 0)
    /** Beds taken on the active farm, by workers and crew. */
    fun beds() = site.workers + site.crew.sum()
    fun totalWorkers() = sites.sumOf { it.workers }
    fun workerCost(): Long = nice(5_000 * 2.5.pow(totalWorkers()))

    fun hireWorker(): Result {
        if (level < WORKER_LEVEL) return Result.LOCKED
        if (beds() >= workerCap()) return Result.LIMIT
        val c = workerCost()
        if (cash < c) return Result.NO_CASH
        spend(c)
        site.workers++
        site.spots = null
        return Result.OK
    }

    // ---- Crew ----

    fun crew(c: Crew) = site.crew[c.ordinal]
    fun crewCost(c: Crew): Long = nice(workerCost() * (1.5 + 0.5 * c.ordinal))
    fun hireCrew(c: Crew): Result {
        if (level < c.level) return Result.LOCKED
        if (crew(c) >= c.max) return Result.LIMIT
        if (beds() >= workerCap()) return Result.LIMIT
        val k = crewCost(c)
        if (cash < k) return Result.NO_CASH
        spend(k)
        site.crew[c.ordinal]++
        return Result.OK
    }

    // ---- Vehicles ----

    /** The anchor (top-left) square of vehicle [v] on the active farm, or null when it hasn't got one. */
    fun vehicleAt(v: Vehicle): Pair<Int, Int>? {
        val i = tiles.indexOfFirst { it.kind == Kind.VEHICLE && it.item == v.name }
        return if (i < 0) null else (i % MAX) to (i / MAX)
    }
    /** The vehicle covering x,y on the active farm, if any. */
    fun vehicleOn(x: Int, y: Int): Vehicle? {
        val t = tile(x, y)
        return when (t.kind) {
            Kind.VEHICLE -> Vehicle.of(t.item)
            Kind.CAB -> tiles.getOrNull(t.anchor)?.let { Vehicle.of(it.item) }
            else -> null
        }
    }

    /** Buys vehicle [v] and parks it with its top-left at x,y on the active farm. */
    fun buyVehicle(x: Int, y: Int, v: Vehicle, now: Long): Result {
        if (level < v.level) return Result.LOCKED
        if (vehicleAt(v) != null) return Result.LIMIT
        if (cash < v.cost) return Result.NO_CASH
        if (!placeVehicle(x, y, v, now + v.minutes * 60_000L)) return Result.BLOCKED
        spend(v.cost)
        layoutRev++
        return Result.OK
    }

    private fun placeVehicle(x: Int, y: Int, v: Vehicle, due: Long): Boolean {
        val cells = listOf(x to y, x + 1 to y, x to y + 1, x + 1 to y + 1)
        if (cells.any { (cx, cy) -> !inLand(cx, cy) || tile(cx, cy).kind != Kind.GRASS }) return false
        val a = y * MAX + x
        cells.forEachIndexed { i, (cx, cy) ->
            tile(cx, cy).apply { clear(); kind = if (i == 0) Kind.VEHICLE else Kind.CAB; anchor = a }
        }
        tile(x, y).apply { item = v.name; doneAt = due }
        site.spots = null
        return true
    }

    private fun clearVehicle(ax: Int, ay: Int) {
        val a = ay * MAX + ax
        tiles.forEach { if ((it.kind == Kind.VEHICLE || it.kind == Kind.CAB) && it.anchor == a) it.clear() }
    }

    /** How long vehicle [v] waits between jobs on the active farm. */
    fun vehicleMillis(v: Vehicle): Long = (v.minutes * 60_000L * (if (crew(Crew.DRIVER) > 0) 0.6 else 1.0)).toLong()

    /** Vehicles on the active farm do their jobs when they're due. */
    private fun drive(now: Long) {
        for (v in Vehicle.entries) {
            if (v.minutes == 0) continue
            val (ax, ay) = vehicleAt(v) ?: continue
            val t = tile(ax, ay)
            if (now < t.doneAt) continue
            t.doneAt = now + vehicleMillis(v)
            when (v) {
                Vehicle.TRUCK -> {
                    val got = sellAllGoods()
                    val tip = (got * 0.1).roundToLong()
                    if (tip > 0) earn(tip)
                    away.sold += got + tip
                }
                Vehicle.TRACTOR -> eachLand { x, y, p ->
                    val last = p.last
                    if (p.kind == Kind.PLOT && p.strain == null && last != null && plant(x, y, last, now) == Result.OK) away.replants++
                }
                Vehicle.VAN -> {
                    val slot = orders.indices.firstOrNull { i -> orders[i]?.let { canFill(it) } == true && !delivering(i) }
                    if (slot != null) deliver(slot, now)
                    else deals.firstOrNull { Contracts.canShip(this, it) }?.let { Contracts.ship(this, it, now, auto = true) }
                }
                Vehicle.RV -> {}
            }
        }
    }

    /** How often the runner goes to town: tool sheds on any farm speed it up. */
    fun runnerMillis(): Long {
        var best = 0
        eachSite { for (s in houses()) if (s.house == WorkHouse.TOOLS) best = max(best, s.house.power(s.staffed)) }
        return when (best) { 2 -> 3 * 60_000L; 1 -> 4 * 60_000L; else -> RUNNER_MILLIS }
    }

    /** Workers with no building to staff go round with a watering can: a few dry plots each per step. */
    private fun waterIdle(now: Long) {
        val idle = site.workers - houses().count { it.staffed }
        if (idle <= 0) return
        var cans = idle * 3
        eachLand { x, y, t ->
            if (cans <= 0 || t.kind != Kind.PLOT || t.strain == null || t.ready() || wet(x, y, now)) return@eachLand
            t.wetUntil = now + waterMillis()
            cans--
        }
    }

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
        if (Generator.draw(f, t.lvl) > 0 && !powered()) return Result.NO_POWER
        supplies[f.supply.ordinal]--
        if (have == need) stash.remove(strain) else stash[strain] = have - need
        t.strain = strain; t.last = strain
        t.doneAt = now + (f.millis(t.lvl) / (1 + 0.25 * houseNear(x, y, WorkHouse.DRYING)) / (if (here(Special.FACTORY)) 1.4 else 1.0) /
            (if (crew(Crew.CHEMIST) > 0) 1.3 else 1.0)).toLong(); t.amount = f.units(t.lvl)
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
        spend(c)
        t.lvl++
        layoutRev++
        return Result.OK
    }

    fun buySupply(s: Supply, n: Int): Result {
        val c = s.price * n
        if (cash < c) return Result.NO_CASH
        spend(c)
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
        turnOver(now)
        // Each town's dealer stocks their own mix, with the town's special always first.
        val here = site.def.id
        val local = BmItem.entries.filter { it.site == here && it.level <= level }
        val rest = BmItem.entries.filter { it.site == null && !it.staple && it.level <= level }.shuffled(Random(rot * 7919 + 13 + here.hashCode() * 31L))
        return (local + rest).take(if (level >= 30) 8 else 6)
    }

    private fun turnOver(now: Long) {
        val rot = now / BM_ROTATION
        if (rot != bmRot) { bmRot = rot; bmBought.clear(); bmStaple.clear() }
    }

    /** The back room: staples that are always in, at your level. */
    fun bmStaples(): List<BmItem> = BmItem.entries.filter { it.staple && it.level <= level }
    fun bmStaplePrice(item: BmItem, now: Long): Long { turnOver(now); return item.price(level, bmStaple[item.name] ?: 0) }

    fun bmBuyStaple(item: BmItem, now: Long): Result {
        if (level < BLACK_MARKET_LEVEL || level < item.level) return Result.LOCKED
        if (!item.staple) return Result.BLOCKED
        val c = bmStaplePrice(item, now)
        if (cash < c) return Result.NO_CASH
        spend(c)
        bmStaple[item.name] = (bmStaple[item.name] ?: 0) + 1
        bmGive(item, now)
        return Result.OK
    }

    /** Whether [slot] of this town's black-market shelf has been bought this rotation. */
    fun bmSold(slot: Int) = bmKey(slot) in bmBought
    private fun bmKey(slot: Int) = sites.indexOf(site) * 100 + slot

    fun bmRefreshIn(now: Long): Long = BM_ROTATION - now % BM_ROTATION

    fun bmBuy(slot: Int, now: Long): Result {
        if (level < BLACK_MARKET_LEVEL) return Result.LOCKED
        val item = bmStock(now).getOrNull(slot) ?: return Result.BLOCKED
        if (bmSold(slot)) return Result.BLOCKED
        val c = item.price(level)
        if (cash < c) return Result.NO_CASH
        spend(c)
        bmBought += bmKey(slot)
        bmGive(item, now)
        return Result.OK
    }

    private fun bmGive(item: BmItem, now: Long) {
        when {
            item.strain != null -> seeds[item.strain] = (seeds[item.strain] ?: 0) + BmItem.EXOTIC_PACK
            item.buff != null -> addBuff(item.buff, now, item.minutes)
            item.decor != null -> decorInv[item.decor.name] = (decorInv[item.decor.name] ?: 0) + 1
            item == BmItem.ENVELOPE -> envelopes++
            item == BmItem.GEM -> gems++
            item == BmItem.BUGBOMB -> eachLand { _, _, t -> if (t.pest || t.mould) { t.pest = false; t.mould = false; bump(Stat.PESTS) } }
            item == BmItem.SEEDER -> eachLand { _, _, t -> if (t.kind == Kind.PLOT) t.wetUntil = max(t.wetUntil, now + waterMillis()) }
            item == BmItem.BLOOM -> eachLand { _, _, t ->
                val s = t.strain?.let { Strains[it] } ?: return@eachLand
                if (t.kind != Kind.PLOT || t.pest || t.mould || t.progress >= s.seconds) return@eachLand
                t.progress = min(s.seconds.toDouble(), t.progress + BmItem.BLOOM_SECONDS)
                if (t.progress >= s.seconds) t.ripeAt = now
            }
            item == BmItem.CRATE -> bmNote = crate(item.price(level), now)
        }
    }

    /** Opens a Mystery Crate that cost [paid]: usually worth it, now and then a lot more. */
    private fun crate(paid: Long, now: Long): String {
        val roll = rng.nextInt(100)
        return when {
            roll < 35 -> nice(paid * 2.5).let { earn(it); "A roll of bills: +${'$'}$it" }
            roll < 60 -> {
                val pick = BmItem.entries.filter { it.strain != null && it.site == null && it.level <= level }.random(rng)
                seeds[pick.strain!!] = (seeds[pick.strain] ?: 0) + 3
                "3 ${Strains[pick.strain]?.name} seeds"
            }
            roll < 85 -> {
                val b = listOf(Buff.JUICE, Buff.DUST, Buff.TIP, Buff.FOCUS, Buff.VIBES).random(rng)
                addBuff(b, now, 30); "${b.title} for 30 min"
            }
            roll < 97 -> { gems++; "A Kush Gem" }
            else -> { gems += 3; "Jackpot: 3 Kush Gems" }
        }
    }

    // ---- Selling and orders ----

    /** Sells [grams] of a strain gram by gram, each one nudging its demand down. Returns the cash made. */
    fun sell(strain: String, grams: Int, byHand: Boolean = true): Long {
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
        if (byHand) holiday()?.let { h ->
            val tk = max(1L, orderTokens(s.level, n) / 2)
            addTokens(h, tk); events += FarmEvent.Bucks(tk, h.id)
        }
        return got
    }

    fun sellAll(): Long = stash.keys.toList().sumOf { sell(it, stash[it] ?: 0) }

    fun canFill(o: Order) = o.items.all { (stash[it.strain] ?: 0) >= it.grams }
    fun delivering(slot: Int) = deliverAt.getOrElse(slot) { 0L } > 0
    /** How long the van takes with [o]: longer for bigger orders, half as long with a depot. */
    fun deliveryMillis(o: Order): Long {
        val min = min(30.0, 4.0 + 4 * o.items.size + o.items.sumOf { it.grams } / 60.0)
        return (min * 60_000 / (if (built(Special.DEPOT)) 2 else 1)).toLong()
    }
    /** What delivering [o] pays: well over selling on the spot. */
    fun deliveryPay(o: Order): Long = (o.reward * DELIVERY_PAY * (if (built(Special.DEPOT)) 1.2 else 1.0)).roundToLong()
    /** Wait before the next order turns up in a slot; the pirate radio halves it. */
    private fun orderWait(ms: Long) = if (built(Special.RADIO)) ms / 2 else ms

    private fun takeStash(o: Order) {
        for (it in o.items) {
            val left = (stash[it.strain] ?: 0) - it.grams
            if (left == 0) stash.remove(it.strain) else stash[it.strain] = left
        }
    }

    /** Pays out a finished order, sold on the spot or delivered. */
    private fun payOrder(o: Order, base: Long) {
        val pay = if (on(Buff.TOURISTS)) (base * 1.5).roundToLong() else base
        earn(pay)
        bump(Stat.ORDERS)
        events += FarmEvent.Cash(pay)
        holiday()?.let { val tk = orderTokens(o); addTokens(it, tk); events += FarmEvent.Bucks(tk, it.id) }
        if (o.vip) { gems++; events += FarmEvent.Gems(1) }
        addXp((o.xp * xpMult()).roundToLong())
    }

    /** Sells an order on the spot. */
    fun fill(slot: Int, now: Long): Result {
        val o = orders.getOrNull(slot) ?: return Result.BLOCKED
        if (delivering(slot) || !canFill(o)) return Result.BLOCKED
        takeStash(o)
        payOrder(o, o.reward)
        orders[slot] = null
        orderAt[slot] = now + orderWait(60_000L)
        return Result.OK
    }

    /** Sends an order out in the van: the bud goes now, the (bigger) pay when it arrives. */
    fun deliver(slot: Int, now: Long): Result {
        val o = orders.getOrNull(slot) ?: return Result.BLOCKED
        if (delivering(slot) || !canFill(o)) return Result.BLOCKED
        takeStash(o)
        deliverPay[slot] = deliveryPay(o)
        deliverAt[slot] = now + deliveryMillis(o)
        return Result.OK
    }

    private fun arrive(slot: Int, now: Long) {
        val o = orders[slot]
        if (o != null) payOrder(o, deliverPay[slot]) else earn(deliverPay[slot])
        orders[slot] = null
        deliverAt[slot] = 0; deliverPay[slot] = 0
        orderAt[slot] = now + orderWait(60_000L)
    }

    fun trash(slot: Int, now: Long) {
        if (orders.getOrNull(slot) == null || delivering(slot)) return
        orders[slot] = null
        orderAt[slot] = now + orderWait(4 * 60_000L)
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
        potBucks += r.bucks
        if (streak % 7 == 0) { gems++; events += FarmEvent.Gems(1) }
        addTokens(holiday(), r.tokens)
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
        potBucks += 1
        rewardTokens(QUEST_TOKENS).let { if (it > 0) { addTokens(holiday(), it); events += FarmEvent.Bucks(it, holiday()!!.id) } }
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
        potBucks += 5
        rewardTokens(BONUS_TOKENS).let { if (it > 0) { addTokens(holiday(), it); events += FarmEvent.Bucks(it, holiday()!!.id) } }
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
            gems++
            addXp(xp)
        }
        for (s in sites) for (g in FarmGoal.entries) {
            if (g.name in s.goals || !g.applies(s) || g.value(this, s) < g.target) continue
            s.goals += g.name
            if (silent) continue
            val xp = max(5L, (xpNeed(level) * 0.04).roundToLong())
            events += FarmEvent.Goal(g, s.def.name, xp)
            gems++
            addXp(xp)
        }
    }

    /** Grams of each parent a cross takes. */
    fun breedGrams(s: Strain) = max(10, s.yield * 2)
    fun breedFee(s: Strain) = nice(s.seed * 10.0)

    /** Starts researching hybrid [id]: the parents and fee go now, the strain comes when [researchAt] passes. */
    fun breed(id: String, now: Long): Result {
        val s = Strains[id] ?: return Result.BLOCKED
        val (pa, pb) = s.parents ?: return Result.BLOCKED
        if (id in bred || id == research) return Result.BLOCKED
        if (research != null) return Result.BUSY
        if (level < Progress.BREED_LEVEL) return Result.LOCKED
        val a = Strains[pa]!!; val b = Strains[pb]!!
        if (!owned(a) || !owned(b)) return Result.LOCKED
        val g = breedGrams(s)
        if ((stash[pa] ?: 0) < g || (stash[pb] ?: 0) < g) return Result.NO_BUD
        val fee = breedFee(s)
        if (cash < fee) return Result.NO_CASH
        for (p in listOf(pa, pb)) { val left = stash[p]!! - g; if (left == 0) stash.remove(p) else stash[p] = left }
        spend(fee)
        research = id
        researchAt = now + Storefronts.researchMillis(this, s)
        return Result.OK
    }

    private fun finishResearch() {
        val s = research?.let { Strains[it] } ?: run { research = null; return }
        research = null; researchAt = 0
        bred += s.id
        bump(Stat.BRED)
        addXp((s.xp * 3 * xpMult()).roundToLong())
        events += FarmEvent.Bred(s)
    }

    fun canSellFarm() = level >= Progress.LEGACY_LEVEL

    /**
     * Starts over on fresh land for a permanent bonus. The other farms are sold too. Kept: legacy, stats, trophies, hybrids, streak, paint,
     * envelopes, exotic seeds and all decor (placed decor goes back in the inventory). The new land is laid out as [map],
     * and up to [Legacy.slots] of the strains in [keep] become heirlooms in place of the last ones.
     */
    fun sellFarm(now: Long, map: StartMap = StartMap.HOMESTEAD, keep: Collection<String> = emptyList()): Result {
        if (!canSellFarm()) return Result.LOCKED
        val ok = Legacy.eligible(this).map { it.id }.toSet()
        val kept = keep.filter { it in ok }.distinct().take(Legacy.slots(this))
        for (s in sites) for (t in s.tiles) if (t.kind == Kind.DECOR || t.kind == Kind.PATH) t.item?.let { decorInv[it] = (decorInv[it] ?: 0) + 1 }
        // Back to the home farm only; its paint stays.
        site = sites[0]; floor = 0
        sites.retainAll { it === site }
        site.floors.clear()
        tiles.forEach { it.clear() }
        land = 6; cash = 30; level = 1; xp = 0
        upgrades.fill(0); helpers.fill(false); supplies.fill(0)
        stash.clear(); goods.clear(); buffs.clear(); demand.clear()
        orders.fill(null); orderAt.fill(0); deliverAt.fill(0); deliverPay.fill(0); mineAt = 0
        shack = 0; happening = null; runnerOn = true; runnerAt = 0; bmBought.clear(); bmStaple.clear()
        // Towns remember you, but deals in hand go with the farm.
        offers.clear(); offerDay.clear(); deals.clear()
        legacy++
        heirlooms.clear(); heirlooms += kept
        startMap = map
        if (Legacy.has(this, LegacyPerk.OLD_MONEY)) cash += Legacy.OLD_MONEY
        questDay = -1L
        lastTick = now
        newGame(now)
        Legacy.lay(this, map)
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
        val radio = if (built(Special.RADIO)) 1.25 else 1.0
        return Order(BUYERS[rng.nextInt(BUYERS.size)], items, nice(value * (1.4 + rng.nextDouble() * 0.6) * radio), max(1, xp.roundToLong().toInt()))
    }
}

/** Rounds to two significant figures, so prices read cleanly ($1,700, not $1,693). */
fun nice(x: Double): Long {
    if (x < 100) return max(1, x.roundToLong())
    val m = 10.0.pow(log10(x).toInt() - 1)
    return ((x / m).roundToLong() * m).roundToLong()
}
