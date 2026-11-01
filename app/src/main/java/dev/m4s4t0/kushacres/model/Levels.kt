package dev.m4s4t0.kushacres.model

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The level curve. The game used to stop at level 80; it now runs to [MAX], with everything that unlocks spread along
 * the way. A *tier* is a point on the old 1 to 80 scale: the economy (prices, rewards, costs that grow with you) is
 * tuned in tiers, so formulas that used the level as a measure of how far along you are use [tier] instead.
 * Levels 1 to 8 stay much as they were; later ones stretch out, so old level 40 is now 123 and old 80 is 420.
 */
object Levels {
    const val MAX = 420
    const val OLD_MAX = 80
    private const val STRETCH = (MAX - OLD_MAX).toDouble()
    private const val SPAN = (OLD_MAX - 1).toDouble()

    /** The level for old-scale tier [t]. */
    fun of(t: Double): Int = (t + STRETCH * ((t - 1) / SPAN).pow(2)).roundToInt().coerceIn(1, MAX)
    fun of(t: Int): Int = of(t.toDouble())

    /** How far along level [level] is, on the old 1 to 80 scale (the inverse of [of], unrounded). */
    fun tier(level: Int): Double {
        val l = level.coerceIn(1, MAX) - 1.0
        // level - 1 = SPAN·u + STRETCH·u², with t = 1 + SPAN·u
        val u = (-SPAN + sqrt(SPAN * SPAN + 4 * STRETCH * l)) / (2 * STRETCH)
        return 1 + SPAN * u
    }

    /** How many old levels [every], 2·[every], … [level] has reached, each counted at the level it now maps to. */
    fun steps(level: Int, every: Int): Int = (1..OLD_MAX / every).count { of(every * it) <= level }

    /** How much of a tier one level is worth at [level]: 1 early on, about a tenth near the top. */
    fun step(level: Int): Double { val l = level.coerceIn(1, MAX - 1); return max(0.0, tier(l + 1) - tier(l)) }

    /** How much more XP a tier takes than it used to: none at first, three and a half times at the top. */
    fun stretch(level: Int): Double = 1 + 2.5 * ((tier(level) - 1) / SPAN).pow(2)
}
