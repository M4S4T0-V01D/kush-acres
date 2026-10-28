package dev.m4s4t0.kushacres.model

import kotlin.random.Random

/**
 * The trimming game at harvest, always optional: shears sweep along the bud, and each snip while they're over a sugar
 * leaf takes it off cleanly. A clean leaf is a little more bud from the plant (see [Farm.harvest]); a snip over bare
 * bud is a miss, and [MISSES] of them end it early.
 */
class Trim(rng: Random) {
    /** The leaves' centres along the bud, 0..1, left to right. */
    val leaves: List<Float>
    val cut = BooleanArray(Farm.TRIM_MAX)
    var misses = 0; private set
    val clean get() = cut.count { it }
    val done get() = clean == leaves.size || misses >= MISSES

    init {
        // Even slots with a little jitter, so leaves never overlap but aren't evenly spaced either.
        val slot = 1f / Farm.TRIM_MAX
        leaves = List(Farm.TRIM_MAX) { i -> slot * (i + 0.5f) + (rng.nextFloat() - 0.5f) * (slot - 2 * HALF) }
    }

    /** A snip with the shears at [pos] (0..1); true if it took a leaf. */
    fun snip(pos: Float): Boolean {
        if (done) return false
        val i = leaves.indices.firstOrNull { !cut[it] && kotlin.math.abs(leaves[it] - pos) <= HALF }
        if (i == null) { misses++; return false }
        cut[i] = true
        return true
    }

    companion object {
        /** Half a leaf's width, as a share of the bud. */
        const val HALF = 0.05f
        const val MISSES = 3
        /** Seconds for the shears to sweep one way; they speed up a little with each clean leaf. */
        const val SWEEP = 1.3f

        /** Sweeps per second with [clean] leaves off. */
        fun speed(clean: Int) = 1 / (SWEEP * (1 - 0.08f * clean))

        /** Where the shears are after [sweeps] sweeps, bouncing between the ends. */
        fun shears(sweeps: Float): Float {
            val p = sweeps % 2f
            return if (p < 1) p else 2 - p
        }
    }
}
