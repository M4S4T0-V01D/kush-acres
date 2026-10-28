package dev.m4s4t0.kushacres.model

import kotlin.math.floor
import kotlin.math.min

/**
 * Lab storefronts: a work building ([WorkHouse.STORE]) that sells lab-bred (hybrid) bud from the stash by itself, offline
 * too, a little over market and without wearing demand down. Each one grows from a roadside stand into a flagship store
 * ([Tile.lvl] 1..[MAX]), and every store level on every farm speeds up research in the Lab. Fractional grams build up
 * in the tile's [Tile.progress].
 */
object Storefronts {
    val NAMES = listOf("Roadside Stand", "Kiosk", "Lab Shop", "Lab Store", "Flagship Store")
    const val MAX = 5
    const val PER_FARM = 4
    /** Grams an hour at each level, before a worker staffs it. */
    private val RATE = intArrayOf(20, 45, 100, 220, 480)
    /** The farm level each store level needs, and what it costs to reach it from the one below. */
    val LEVELS = intArrayOf(8, 12, 18, 26, 34)
    private val COST = longArrayOf(0, 60_000, 400_000, 2_500_000, 15_000_000)
    const val STAFFED = 1.5

    fun name(lvl: Int) = NAMES[(lvl - 1).coerceIn(0, MAX - 1)]
    fun rate(lvl: Int, staffed: Boolean) = RATE[(lvl - 1).coerceIn(0, MAX - 1)] * (if (staffed) STAFFED else 1.0)
    /** Over the market price: 25% for a stand up to 45% for a flagship. */
    fun premium(lvl: Int) = 1.2 + 0.05 * lvl
    /** What going up from [lvl] costs, or null at the top. */
    fun upgradeCost(lvl: Int): Long? = if (lvl >= MAX) null else COST[lvl]

    fun isStore(t: Tile) = t.kind == Kind.HOUSE && t.item == WorkHouse.STORE.name

    fun upgrade(f: Farm, x: Int, y: Int): Result {
        val t = f.tile(x, y)
        if (!isStore(t)) return Result.BLOCKED
        val cost = upgradeCost(t.lvl) ?: return Result.LIMIT
        if (f.level < LEVELS[t.lvl]) return Result.LOCKED
        if (f.cash < cost) return Result.NO_CASH
        f.spend(cost)
        t.lvl++
        return Result.OK
    }

    /** Every store level on every farm you own. */
    fun levels(f: Farm) = f.sites.sumOf { s -> s.tiles.sumOf { if (isStore(it)) it.lvl else 0 } }
    fun stores(f: Farm) = f.sites.sumOf { s -> s.tiles.count { isStore(it) } }
    fun chemists(f: Farm) = f.sites.sumOf { it.crew[Crew.CHEMIST.ordinal] }
    /**
     * How much faster than plain the Lab researches: +10% per store level anywhere, +50% per Lab Chemist, at most
     * [MAX_SPEED] times so a cross always takes a while.
     */
    fun researchSpeed(f: Farm) = minOf(MAX_SPEED, (1 + 0.1 * levels(f) + 0.5 * chemists(f)) * Legacy.mult(f, LegacyPerk.LAB_NOTES, 0.5))
    const val MAX_SPEED = 4.0
    /** Plain research time: about an hour for the first crosses, up to seven for the last. */
    fun researchMinutes(s: Strain) = 30L + 5L * s.level
    fun researchMillis(f: Farm, s: Strain) = (researchMinutes(s) * 60_000L / researchSpeed(f)).toLong()

    /** The lab bud a store sells next: the dearest hybrid in the stash. */
    fun next(f: Farm): Strain? = f.stash.keys.mapNotNull { Strains[it] }.filter { it.hybrid }.maxByOrNull { it.price }

    /** Grams an hour this farm's stores sell together. */
    fun farmRate(f: Farm) = f.houses().filter { it.house == WorkHouse.STORE }.sumOf { rate(f.tile(it.x, it.y).lvl, it.staffed) }

    /** The active farm's stores sell for [t0]..[t1]; returns the cash taken. */
    fun sell(f: Farm, t0: Long, t1: Long): Long {
        val spots = f.houses().filter { it.house == WorkHouse.STORE }
        if (spots.isEmpty()) return 0
        val hours = (t1 - t0) / 3_600_000.0
        var mult = 0.0
        var total = 0L
        for (sp in spots) {
            val t = f.tile(sp.x, sp.y)
            t.progress += rate(t.lvl, sp.staffed) * hours
            var n = floor(t.progress).toInt()
            if (n <= 0) continue
            while (n > 0) {
                val s = next(f) ?: break
                val have = f.stash[s.id] ?: 0
                val k = min(have, n)
                if (mult == 0.0) mult = f.priceMult()
                val got = Math.round(k * s.price * mult * premium(t.lvl))
                if (have - k == 0) f.stash.remove(s.id) else f.stash[s.id] = have - k
                f.earnCash(got)
                f.bump(Stat.SOLD, k.toLong()); f.bump(Stat.STORE_GRAMS, k.toLong())
                total += got
                n -= k; t.progress -= k
            }
            // Nothing on the shelf: don't bank sales for later.
            if (n > 0) t.progress -= floor(t.progress)
        }
        f.away.shop += total
        return total
    }
}
