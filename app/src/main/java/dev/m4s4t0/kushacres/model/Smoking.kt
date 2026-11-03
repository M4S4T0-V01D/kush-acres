package dev.m4s4t0.kushacres.model

import kotlin.math.abs
import kotlin.math.max

/**
 * What you smoke out of. Better pieces get more from less: [grams] is the share of a sesh's usual grams they need and [time]
 * stretches the buff. Papers come free; the rest are bought once at [level] and stay through a farm sale.
 */
enum class Gear(val title: String, val level: Int, val cost: Long, val grams: Double, val time: Double, val blurb: String) {
    PAPERS("Papers", 1, 0, 1.0, 1.0, "Roll your own. Roll it well and it lasts longer"),
    PIPE("Pipe", 10, 1_500, 0.85, 1.1, "A glass spoon: 15% less bud, 10% longer"),
    BONG("Bong", 40, 60_000, 0.7, 1.25, "Ice catcher and all: 30% less bud, 25% longer"),
    VAPE("Vape", 120, 25_000_000, 0.6, 1.4, "Smooth and clean: 40% less bud, 40% longer"),
    DAB("Dab Rig", 250, 1_500_000_000, 0.45, 1.6, "Torch and nail: 55% less bud, 60% longer");

    /** What getting a sesh ready is called with this piece, for the rolling game. */
    val bowl get() = when (this) { PAPERS -> "joint"; PIPE, BONG -> "bowl"; VAPE -> "chamber"; DAB -> "nail" }
    val prep get() = when (this) { PAPERS -> "Roll"; PIPE, BONG -> "Pack"; VAPE -> "Load"; DAB -> "Heat" }

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }

        fun owns(f: Farm, g: Gear) = g == PAPERS || g in f.gear

        fun buy(f: Farm, g: Gear): Result {
            if (owns(f, g)) return Result.BLOCKED
            if (f.level < g.level) return Result.LOCKED
            if (f.cash < g.cost) return Result.NO_CASH
            f.spend(g.cost)
            f.gear += g
            f.gearOn = g
            return Result.OK
        }

        fun use(f: Farm, g: Gear): Result {
            if (!owns(f, g)) return Result.LOCKED
            f.gearOn = g
            return Result.OK
        }
    }
}

/**
 * The rolling game before a sesh: a needle sweeps a bar and the player stops it [TAPS] times, aiming for the sweet spot in
 * the middle. Each tap's offset is 0 (dead centre) to 1 (the very end); a clean roll stretches the buff by up to [MAX].
 */
object Rolling {
    const val TAPS = 3
    const val MAX = 0.25
    /** Within this of the centre counts as perfect. */
    const val SWEET = 0.08

    /** How good one tap was, 0..1. */
    fun grade(off: Double): Double {
        val d = abs(off).coerceIn(0.0, 1.0)
        return if (d <= SWEET) 1.0 else (1 - (d - SWEET) / (0.7 - SWEET)).coerceIn(0.0, 1.0)
    }

    /** The buff bonus for a roll, 0..[MAX]. Missing taps count as misses. */
    fun bonus(offs: List<Double>): Double = MAX * (0 until TAPS).sumOf { i -> offs.getOrNull(i)?.let { grade(it) } ?: 0.0 } / TAPS

    /** Where the needle is at [t] seconds into tap [n]: a sweep back and forth across -1..1, faster each tap. */
    fun needle(t: Double, n: Int): Double {
        val speed = 0.9 + 0.35 * n
        val p = (t * speed + 0.5) % 2.0
        return if (p < 1) p * 2 - 1 else 3 - p * 2
    }

    fun word(bonus: Double): String = when {
        bonus >= MAX * 0.95 -> "Perfect roll!"
        bonus >= MAX * 0.7 -> "Tight roll"
        bonus >= MAX * 0.4 -> "Decent roll"
        bonus > 0 -> "Bit lumpy"
        else -> "Canoe'd it"
    }
}

/** One page of rewards in the tasting journal: smoke [count] different strains to earn it. */
class TasteMark(val count: Int, val title: String, val bucks: Long)

/** The tasting journal: every strain you've smoked, how often, a few words on each, and trophies for the variety. */
object Tasting {
    val MARKS = listOf(
        TasteMark(3, "Taster", 2), TasteMark(10, "Sampler", 5), TasteMark(25, "Connoisseur", 10),
        TasteMark(50, "Sommelier", 20), TasteMark(100, "Professor of Kush", 40), TasteMark(200, "Living Legend", 80),
    )

    fun kinds(f: Farm) = f.tasted.size

    /** The next trophy, or null when the journal's full of them. */
    fun next(f: Farm): TasteMark? = MARKS.getOrNull(f.tasteMarks)

    /** XP a trophy pays: a share of the level's need, so it's worth it at any level. */
    fun xp(f: Farm, m: TasteMark): Long = max(10L, Farm.xpNeed(f.level) * (MARKS.indexOf(m) + 1) / 20)

    /** Writes a smoke of [strain] in the journal and pays any trophies it reaches; returns them. */
    fun note(f: Farm, strain: String): List<TasteMark> {
        f.tasted[strain] = (f.tasted[strain] ?: 0) + 1
        val got = ArrayList<TasteMark>()
        while (true) {
            val m = next(f) ?: break
            if (kinds(f) < m.count) break
            f.tasteMarks++
            f.potBucks += m.bucks
            f.events += FarmEvent.Bucks(m.bucks)
            f.addXp(xp(f, m))
            got += m
        }
        return got
    }

    private val NOSE = listOf("pine", "lemon", "diesel", "berry", "skunk", "earth", "mango", "pepper", "grape", "mint", "cheese", "lavender", "coffee", "honey", "orange", "cedar")
    private val FEEL = listOf("couch-lock", "giggly", "dreamy", "chatty", "clear-headed", "hungry", "floaty", "creative", "mellow", "electric")

    /** A couple of words on how [s] smells and hits, the same every time. */
    fun notes(s: Strain): String {
        val h = s.id.hashCode().let { if (it < 0) -it else it }
        val a = NOSE[h % NOSE.size]; val b = NOSE[(h / 7 + 3) % NOSE.size].takeIf { it != a } ?: NOSE[(h + 1) % NOSE.size]
        return "$a & $b, ${FEEL[(h / 13) % FEEL.size]}"
    }

    /** A rating out of five, from strength, for the journal. */
    fun stars(s: Strain): Int = if (s.holiday != null || s.exotic) 5 else (1 + (s.tier - 1) / 20).toInt().coerceIn(1, 5)
}
