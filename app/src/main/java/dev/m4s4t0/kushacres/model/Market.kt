package dev.m4s4t0.kushacres.model

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Town news that moves the market for a few hours: a boom puts one kind of strain (a [CupClass]) or one facility's
 * goods up, a glut knocks it down. Made from the clock, so it needs no saving: every [SLOT] there may be a new story,
 * and some slots are quiet. Opens at [LEVEL].
 */
object Market {
    const val LEVEL = 5
    const val SLOT = 4 * 3_600_000L

    /** One story: [what] is "class:FAST" or "goods:ROLLER"; prices of it are times [mult] until [until]. */
    class News(val what: String, val mult: Double, val headline: String, val until: Long) {
        val boom get() = mult > 1
        val percent get() = ((mult - 1) * 100).roundToInt()
    }

    private val BOOMS = listOf(
        "A festival crowd wants %s", "A food blogger raves about %s", "Celebrity seen with %s", "%s are all the rage",
        "Tourists clear the shelves of %s", "The local paper loves %s",
    )
    private val SLUMPS = listOf(
        "A rival floods town with %s", "Glut of %s at the market", "Bad review hits %s", "Nobody wants %s this week",
    )

    private var key = Long.MIN_VALUE
    private var cache: News? = null

    /** What's in the news at [now], if anything. */
    fun news(f: Farm, now: Long): News? {
        if (f.level < LEVEL) return null
        val slot = Math.floorDiv(now, SLOT)
        val k = slot * 1000 + f.level
        if (k != key) { key = k; cache = make(f, slot) }
        return cache
    }

    private fun make(f: Farm, slot: Long): News? {
        val r = Random(slot * 7919 + 13)
        if (r.nextInt(10) < 3) return null
        val targets = CupClass.entries.filter { it != CupClass.OPEN && Contracts.fitting(f, it).isNotEmpty() }.map { "class:${it.name}" } +
            Facility.entries.filter { it.level <= f.level }.map { "goods:${it.name}" }
        if (targets.isEmpty()) return null
        val what = targets[r.nextInt(targets.size)]
        val boom = r.nextInt(3) < 2
        val mult = if (boom) 1.25 + r.nextInt(6) * 0.05 else 0.7 + r.nextInt(4) * 0.05
        val head = (if (boom) BOOMS else SLUMPS).let { it[r.nextInt(it.size)] }
        val name = subject(what)
        val line = head.format(name).replaceFirstChar { it.uppercase() }
        return News(what, mult, line, (slot + 1) * SLOT)
    }

    /** What the story is about, as it reads in a headline. */
    fun subject(what: String): String = when {
        what.startsWith("class:") -> CupClass.entries.firstOrNull { it.name == what.removePrefix("class:") }?.desc?.lowercase() ?: "strains"
        else -> Facility.of(what.removePrefix("goods:"))?.product?.lowercase() ?: "goods"
    }

    /** How the news moves the price of bud of [s]. */
    fun mult(f: Farm, s: Strain, now: Long): Double {
        val n = news(f, now) ?: return 1.0
        if (!n.what.startsWith("class:")) return 1.0
        val c = CupClass.entries.firstOrNull { it.name == n.what.removePrefix("class:") } ?: return 1.0
        return if (s.holiday == null && !s.exotic && c.fits(s)) n.mult else 1.0
    }

    /** How the news moves the price of [fac]'s goods. */
    fun goodsMult(f: Farm, fac: Facility, now: Long): Double {
        val n = news(f, now) ?: return 1.0
        return if (n.what == "goods:${fac.name}") n.mult else 1.0
    }
}
