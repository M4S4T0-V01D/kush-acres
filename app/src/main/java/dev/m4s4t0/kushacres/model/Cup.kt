package dev.m4s4t0.kushacres.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

/** What the judges look for in a given week. Any strain can enter; one that fits the class scores [Cup.THEME_BONUS] more. */
enum class CupClass(val title: String, val desc: String, val fits: (Strain) -> Boolean) {
    OPEN("Open Class", "Anything goes: the judges score the bud alone", { false }),
    FAST("Speed Class", "Fast strains", { Trait.FAST in it.traits }),
    BIG("Heavyweight Class", "Big strains", { Trait.BIG in it.traits }),
    LUCKY("Lucky Class", "Lucky strains", { Trait.GOLDEN in it.traits }),
    HARDY("Hardy Class", "Hardy strains", { Trait.HARDY in it.traits }),
    THRIFTY("Budget Class", "Thrifty strains", { Trait.THRIFTY in it.traits }),
    HYBRID("Breeders' Class", "Hybrids from the lab", { it.hybrid }),
    LOCAL("Terroir Class", "Strains native to one of the farms", { it.site != null }),
}

/** A placing's prize. [place] 1–3; anything lower is an honourable mention. */
class CupPrize(val place: Int, val cash: Long, val bucks: Long, val gems: Long, val xp: Long)

/**
 * The Cannabis Cup: a weekly contest. Each week has a class and three rival growers with set scores. You get
 * [ENTRIES] entries a week, each costing some grams of one strain; the judges score it on the spot. A placing pays
 * only when it beats your best placing that week, so the most a week pays is the first-place prize plus whatever
 * lower places came before it.
 */
object Cup {
    const val LEVEL = 10
    const val ENTRIES = 3
    const val THEME_BONUS = 15
    private val RIVALS = listOf(
        "Big Sal", "Dr. Greenleaf", "Ma Kettle", "Johnny Haze", "The Reverend", "Skunk Patel", "Lady Indica",
        "Old Man Riggs", "Cosmic Carla", "Professor Purp", "Dusty Rhodes", "Kaya Jones",
    )

    /** Weeks start on Monday, local time (the Unix epoch fell on a Thursday). */
    fun week(now: Long): Long = (localDay(now) + 3) / 7
    /** Milliseconds until the next week's cup opens. */
    fun endsIn(now: Long): Long {
        val local = now + java.util.TimeZone.getDefault().getOffset(now)
        return ((week(now) + 1) * 7 - 3) * 86_400_000L - local
    }

    fun theme(week: Long): CupClass {
        val r = Random(week * 7907 + 13)
        // Open class one week in three; otherwise a set class.
        return if (r.nextInt(3) == 0) CupClass.OPEN else CupClass.entries[1 + r.nextInt(CupClass.entries.size - 1)]
    }

    /** The week's three rivals, best first, with scores that make first place a stretch. */
    fun rivals(week: Long): List<Pair<String, Int>> {
        val r = Random(week * 104_729 + 7)
        val names = RIVALS.shuffled(r).take(3)
        val first = 86 + r.nextInt(8); val second = first - 7 - r.nextInt(6); val third = second - 7 - r.nextInt(6)
        return names.zip(listOf(first, second, third))
    }

    fun place(week: Long, score: Int) = 1 + rivals(week).count { it.second >= score }

    /** Grams it takes to enter a strain. */
    fun grams(s: Strain) = max(10, s.yield * 2)

    /**
     * The judges' score before their mood on the day (which adds 0–[LUCK]): how fine the strain is next to the best
     * one you can grow (up to 72), its genes, and whether it fits the class or is in season.
     */
    fun base(f: Farm, s: Strain, week: Long): Int {
        val top = Strains.ladder.last { f.unlocked(it) }
        val fine = 60.0 * min(1.2, s.price.toDouble() / top.price)
        val genes = 4 * s.traits.size + (if (s.hybrid) 4 else 0) + (if (s.exotic) 6 else 0)
        val fits = if (theme(week).fits(s)) THEME_BONUS else 0
        val season = if (f.favourite(s)) 6 else 0
        return (fine + genes + fits + season).roundToInt()
    }

    const val LUCK = 15

    fun used(f: Farm, now: Long) = if (f.cupWeek == week(now)) f.cupScores.size else 0
    fun left(f: Farm, now: Long) = ENTRIES - used(f, now)
    /** Your best placing this week (1 is first), or 4 for none yet. */
    fun best(f: Farm, now: Long): Int = if (f.cupWeek != week(now)) 4 else f.cupScores.minOfOrNull { place(f.cupWeek, it.second) }?.coerceAtMost(4) ?: 4

    fun prize(f: Farm, place: Int): CupPrize {
        val cash = nice((300.0 + 60.0 * f.level * f.level) * when (place) { 1 -> 3.0; 2 -> 1.5; 3 -> 0.8; else -> 0.0 })
        val xp = (Farm.xpNeed(f.level) * when (place) { 1 -> 0.15; 2 -> 0.08; 3 -> 0.04; else -> 0.01 }).roundToLong()
        return CupPrize(place, if (place <= 3) cash else 0, when (place) { 1 -> 15L; 2 -> 6L; 3 -> 3L; else -> 0L }, if (place == 1) 2 else 0, max(1, xp))
    }

    /** Enters [grams] of strain [id]; on OK the judged entry is the last of [Farm.cupScores]. */
    fun enter(f: Farm, id: String, now: Long): Result {
        if (f.level < LEVEL) return Result.LOCKED
        val s = Strains[id] ?: return Result.BLOCKED
        val wk = week(now)
        if (f.cupWeek != wk) { f.cupWeek = wk; f.cupScores.clear() }
        if (f.cupScores.size >= ENTRIES) return Result.LIMIT
        val g = grams(s)
        val have = f.stash[id] ?: 0
        if (have < g) return Result.NO_BUD
        val before = best(f, now)
        if (have == g) f.stash.remove(id) else f.stash[id] = have - g
        val score = base(f, s, wk) + f.rng.nextInt(LUCK + 1)
        f.cupScores += id to score
        val place = min(4, place(wk, score))
        val p = prize(f, place)
        if (place < before) {
            f.cash += p.cash; f.potBucks += p.bucks; f.gems += p.gems
            f.bump(Stat.PLACED)
            if (place == 1) f.bump(Stat.CUPS)
        }
        f.addXp(if (place < before) p.xp else prize(f, 4).xp)
        return Result.OK
    }
}
