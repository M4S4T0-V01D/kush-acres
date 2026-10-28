package dev.m4s4t0.kushacres.model

import kotlin.random.Random

/**
 * Phenotypes: every plant grown from seed rolls its own yield and potency grade, and now and then a rare bud colour.
 * The best find of each strain can be kept as a mother; planting that strain then gives clones of her.
 *
 * A pheno is packed in an Int: yield grade in bits 0-3, potency in 4-7, colour in 8-11, [SET] once rolled and [CLONE]
 * for a cutting. 0 is a plant from before phenos, read as plain.
 */
object Pheno {
    const val SET = 1 shl 12
    const val CLONE = 1 shl 13
    val GRADES = listOf("Runt", "Plain", "Good", "Great", "Elite")
    private val WEIGHTS = listOf(15, 45, 25, 12, 3)
    private val YIELD = listOf(0.9, 1.0, 1.05, 1.12, 1.2)
    /** Potency: XP from the harvest, and a little golden luck. */
    private val POTENCY = listOf(0.9, 1.0, 1.08, 1.18, 1.3)
    private val GOLDEN = listOf(-0.01, 0.0, 0.005, 0.015, 0.03)
    val COLOURS = listOf("", "Purple", "Frosty", "Amber")
    val TINTS = listOf(0, 0xFFA04AE0.toInt(), 0xFFEAF4FF.toInt(), 0xFFF0A82A.toInt())
    const val COLOUR_CHANCE = 0.06
    const val PLAIN = 1

    fun of(yieldGrade: Int, potency: Int, colour: Int = 0, clone: Boolean = false) =
        SET or yieldGrade.coerceIn(0, 4) or (potency.coerceIn(0, 4) shl 4) or (colour.coerceIn(0, 3) shl 8) or (if (clone) CLONE else 0)

    fun set(p: Int) = p and SET != 0
    fun yieldGrade(p: Int) = if (set(p)) p and 0xF else PLAIN
    fun potency(p: Int) = if (set(p)) (p shr 4) and 0xF else PLAIN
    fun colour(p: Int) = if (set(p)) (p shr 8) and 0xF else 0
    fun clone(p: Int) = p and CLONE != 0
    /** How good a find is, for keeping the best; a rare colour breaks ties. */
    fun score(p: Int) = if (!set(p)) -1 else (yieldGrade(p) + potency(p)) * 4 + if (colour(p) > 0) 1 else 0

    fun yieldMult(p: Int) = YIELD[yieldGrade(p)]
    fun xpMult(p: Int) = POTENCY[potency(p)]
    fun golden(p: Int) = GOLDEN[potency(p)]

    private fun grade(rng: Random, luck: Int): Int {
        var best = 0
        repeat(1 + luck) {
            var r = rng.nextInt(WEIGHTS.sum())
            var g = 0
            while (r >= WEIGHTS[g]) { r -= WEIGHTS[g]; g++ }
            best = maxOf(best, g)
        }
        return best
    }

    /** A fresh seed's pheno; each point of [luck] is another roll at each grade, keeping the best. */
    fun roll(rng: Random, luck: Int = 0): Int {
        val colour = if (rng.nextDouble() < COLOUR_CHANCE * (1 + luck)) 1 + rng.nextInt(COLOURS.size - 1) else 0
        return of(grade(rng, luck), grade(rng, luck), colour)
    }

    /** A golden harvest shows the plant at its best: one grade up on both. */
    fun shine(p: Int) = of(yieldGrade(p) + 1, potency(p) + 1, colour(p), clone(p))

    fun label(p: Int): String {
        val c = COLOURS[colour(p)]
        return "${GRADES[yieldGrade(p)]} yield · ${GRADES[potency(p)]} potency" + if (c.isEmpty()) "" else " · $c"
    }
}
