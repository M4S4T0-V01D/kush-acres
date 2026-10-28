package dev.m4s4t0.kushacres.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/** How much goes up in smoke in one sitting, and how long the buzz lasts before strain and comfort. */
enum class SeshSize(val title: String, val grams: Int, val minutes: Int, val blurb: String) {
    JOINT("Joint", 5, 10, "A quick one on the porch"),
    BLUNT("Blunt", 25, 25, "Pass it around"),
    HOTBOX("Hotbox", 120, 60, "Close every window"),
}

/** A crate on its way to the out-of-town distributor; pays [pay] cash and [xp] when it gets there at [at]. */
class Shipment(val strain: String, val grams: Int, val pay: Long, val xp: Long, val at: Long)

/** Other ways to use a stockpile: smoke it, ship it wholesale, or give it to the clinic. */
object Sesh {
    /** Buffs from smoking never stack past this many minutes. */
    const val MAX_MINUTES = 240
    const val SHIP_MIN = 200
    const val SHIP_HOURS = 3
    /** Share of base price the distributor pays; it never touches dispensary demand. */
    const val SHIP_RATE = 0.65
    const val DONATE_MIN = 10

    /** What each trait does to you. Hybrids carry several traits, so they give several buffs. */
    fun buffFor(t: Trait): Buff = when (t) {
        Trait.FAST -> Buff.HIGH_GROW
        Trait.BIG -> Buff.HIGH_YIELD
        Trait.GOLDEN -> Buff.HIGH_GOLD
        Trait.HARDY -> Buff.HIGH_XP
        Trait.THRIFTY -> Buff.HIGH_PRICE
    }
    fun buffs(s: Strain): List<Buff> = s.traits.sortedBy { it.ordinal }.map { buffFor(it) }

    /** Minutes of buff before what's already running: better strains and a comfier home last longer. */
    fun minutes(f: Farm, s: Strain, size: SeshSize): Int {
        val strain = 1 + 0.02 * s.level + (if (s.holiday != null || s.exotic) 0.25 else 0.0)
        val comfort = 1 + 0.5 * Interiors.score(f, RoomKind.HOME) / Interiors.MAX_SCORE
        return (size.minutes * strain * comfort).toInt()
    }

    /** Minutes a sesh actually adds to [b]: less the longer it's already running, so piling on fades out. */
    fun adds(f: Farm, b: Buff, base: Int, now: Long): Int {
        val left = max(0L, (f.buffs[b] ?: 0L) - now) / 60_000L
        val room = (MAX_MINUTES - left).coerceAtLeast(0L)
        if (room == 0L) return 0
        return min(room, max(1L, base * room / MAX_MINUTES)).toInt()
    }

    fun smoke(f: Farm, strain: String, size: SeshSize, now: Long): Result {
        val s = Strains[strain] ?: return Result.NO_BUD
        if ((f.stash[strain] ?: 0) < size.grams) return Result.NO_BUD
        val base = minutes(f, s, size)
        val gains = buffs(s).map { it to adds(f, it, base, now) }
        if (gains.all { it.second == 0 }) return Result.LIMIT
        take(f, strain, size.grams)
        gains.forEach { (b, m) -> if (m > 0) f.addBuff(b, now, m) }
        f.bump(Stat.SMOKED, size.grams.toLong())
        return Result.OK
    }

    fun shipPay(f: Farm, s: Strain, grams: Int): Long = (s.price * f.priceMult() * SHIP_RATE * grams).roundToLong()
    fun shipXp(s: Strain, grams: Int): Long = max(1L, s.xp0.toLong() * grams / max(1, s.yield) / 4)

    fun ship(f: Farm, strain: String, grams: Int, now: Long): Result {
        val s = Strains[strain] ?: return Result.NO_BUD
        if (f.shipment != null) return Result.BUSY
        if (grams < SHIP_MIN || (f.stash[strain] ?: 0) < grams) return Result.NO_BUD
        f.shipment = Shipment(strain, grams, shipPay(f, s, grams), shipXp(s, grams), now + SHIP_HOURS * 3_600_000L)
        take(f, strain, grams)
        return Result.OK
    }

    /** Pays out a shipment that's arrived; called from [Farm.tick]. */
    fun arrive(f: Farm, now: Long): Shipment? {
        val sh = f.shipment ?: return null
        if (now < sh.at) return null
        f.shipment = null
        f.earnCash(sh.pay)
        f.events += FarmEvent.Cash(sh.pay)
        f.bump(Stat.SHIPPED, sh.grams.toLong())
        f.addXp(sh.xp)
        return sh
    }

    /** The clinic's town: wherever you are, if it has one, or Kushville. */
    fun clinicTown(f: Farm): String = f.site.def.id.takeIf { it in Towns.ids } ?: "home"
    fun donateRep(s: Strain, grams: Int): Int = max(1, grams * (5 + s.level) / 100)
    fun donateBucks(grams: Int): Long = (grams / 150L).coerceIn(1L, 10L)
    fun donateXp(s: Strain, grams: Int): Long = max(1L, s.xp0.toLong() * grams / max(1, s.yield) / 3)

    class Gift(val rep: Int, val bucks: Long, val xp: Long, val town: String)

    fun donate(f: Farm, strain: String, grams: Int): Gift? {
        val s = Strains[strain] ?: return null
        if (grams < DONATE_MIN || (f.stash[strain] ?: 0) < grams) return null
        take(f, strain, grams)
        val town = clinicTown(f)
        val g = Gift(donateRep(s, grams), donateBucks(grams), donateXp(s, grams), town)
        f.rep[town] = Contracts.rep(f, town) + g.rep
        f.potBucks += g.bucks
        f.events += FarmEvent.Bucks(g.bucks)
        f.bump(Stat.DONATED, grams.toLong())
        f.addXp(g.xp)
        return g
    }

    private fun take(f: Farm, strain: String, n: Int) {
        val have = f.stash[strain] ?: 0
        if (have - n <= 0) f.stash.remove(strain) else f.stash[strain] = have - n
    }
}
