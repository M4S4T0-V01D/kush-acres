package dev.m4s4t0.kushacres.model

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

/** Where a seed hunt goes. Longer trips cost more and bring back more, with better odds of a rare find. */
enum class Trip(val title: String, val desc: String, val hours: Int) {
    HILLS("Hill Walk", "A quick look round the hills", 1),
    VALLEY("Valley Trek", "Old grow spots down the valley", 4),
    JUNGLE("Jungle Expedition", "Deep into the jungle, where the landraces grow wild", 8);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
    }
}

/** What a scout brought back. [rare] is true when the seeds are a tier above what the black market sells you. */
class Loot(val trip: Trip, val strain: String, val seeds: Int, val rare: Boolean, val cash: Long, val bucks: Long, val gems: Long, val xp: Long)

/**
 * Seed hunts: from [LEVEL] you can send a scout off on a [Trip]. One at a time; when they're back, claim what they
 * found: exotic seeds, some cash, Pot Bucks, XP and now and then a gem.
 */
object Hunt {
    const val LEVEL = 12

    fun cost(f: Farm, t: Trip): Long = nice((150.0 + 30.0 * f.level * f.level) * t.hours.toDouble().pow(0.85))

    /** The exotic seeds a hunt can find: the best you could buy, and the one above it for a rare find. */
    private fun exotics() = BmItem.entries.filter { it.strain != null && it.site == null }
    fun common(f: Farm): BmItem = exotics().last { it.level <= max(6, f.level) }
    fun rareOf(f: Farm): BmItem = exotics().firstOrNull { it.level > max(6, f.level) } ?: common(f)
    fun rareChance(t: Trip) = 0.04 * t.hours

    fun away(f: Farm) = Trip.of(f.hunt) != null
    fun back(f: Farm, now: Long) = away(f) && now >= f.huntBack

    fun send(f: Farm, t: Trip, now: Long): Result {
        if (f.level < LEVEL) return Result.LOCKED
        if (away(f)) return Result.BUSY
        val c = cost(f, t)
        if (f.cash < c) return Result.NO_CASH
        f.spend(c)
        f.hunt = t.name; f.huntBack = now + t.hours * 3_600_000L
        return Result.OK
    }

    /** Brings the scout home; null while they're still out (or none was sent). */
    fun claim(f: Farm, now: Long): Loot? {
        val t = Trip.of(f.hunt) ?: return null
        if (now < f.huntBack) return null
        val r = f.rng
        val rare = r.nextDouble() < rareChance(t)
        val pack = if (rare) rareOf(f) else common(f)
        val seeds = when (t) { Trip.HILLS -> 2; Trip.VALLEY -> 4; Trip.JUNGLE -> 7 } + r.nextInt(3)
        val cash = nice(cost(f, t) * (0.7 + 0.6 * r.nextDouble()))
        val bucks = 1L + r.nextInt(t.hours)
        val gems = if (r.nextDouble() < 0.05 * t.hours) 1L else 0L
        val xp = max(1L, (Farm.xpNeed(f.level) * 0.02 * t.hours.toDouble().pow(0.7)).roundToLong())
        val strain = pack.strain!!
        f.seeds[strain] = (f.seeds[strain] ?: 0) + seeds
        f.cash += cash; f.potBucks += bucks; f.gems += gems
        f.hunt = null; f.huntBack = 0
        f.bump(Stat.HUNTS)
        f.addXp(xp)
        return Loot(t, strain, seeds, rare, cash, bucks, gems, xp)
    }
}
