package dev.m4s4t0.kushacres.model

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Bud Drop, the arcade game for the wait while things grow, always optional: buds fall from the top and the player slides a jar
 * along the bottom to catch them. A bud is a point, a golden bud [GOLD_PTS], and a bug in the jar costs one of [LIVES] lives.
 * It all falls faster and thicker the longer the round goes. The first [PAID] rounds a day pay out (see [reward]).
 */
class BudDrop(private val rng: Random) {
    enum class Kind { BUD, GOLD, BUG }
    /** Something falling: [x] across and [y] down the field, both 0..1. */
    class Item(val kind: Kind, val x: Float, var y: Float, val spin: Float)

    val items = ArrayList<Item>()
    /** The jar's centre across the field, 0..1. */
    var jar = 0.5f
        set(v) { field = v.coerceIn(JAR, 1 - JAR) }
    var score = 0; private set
    var lives = LIVES; private set
    var caught = 0; private set
    var clock = 0f; private set
    val done get() = lives <= 0
    private var next = 0.6f

    /** Field heights per second; it starts gentle and climbs. */
    fun speed() = 0.32f + min(clock, 90f) * 0.008f
    /** Seconds between drops. */
    fun gap() = (1.0f - min(clock, 90f) * 0.008f).coerceAtLeast(0.32f)

    /** Runs the round on by [dt] seconds; returns what landed in the jar this step. */
    fun step(dt: Float): List<Kind> {
        if (done) return emptyList()
        clock += dt
        next -= dt
        if (next <= 0) { next += gap(); drop() }
        val got = ArrayList<Kind>()
        val v = speed() * dt
        val it = items.iterator()
        while (it.hasNext()) {
            val i = it.next()
            val was = i.y
            i.y += v * (if (i.kind == Kind.GOLD) 1.25f else 1f)
            // Into the jar as it crosses the rim, if it's over the mouth.
            if (was < RIM && i.y >= RIM && abs(i.x - jar) <= JAR) {
                it.remove(); got += i.kind
                when (i.kind) {
                    Kind.BUD -> { score++; caught++ }
                    Kind.GOLD -> { score += GOLD_PTS; caught++ }
                    Kind.BUG -> lives--
                }
                if (done) break
            } else if (i.y > 1.05f) it.remove()
        }
        return got
    }

    private fun drop() {
        val r = rng.nextFloat()
        val bug = min(0.12f + clock * 0.004f, 0.35f)
        val kind = when { r < bug -> Kind.BUG; r < bug + 0.08f -> Kind.GOLD; else -> Kind.BUD }
        items += Item(kind, 0.06f + rng.nextFloat() * 0.88f, -0.05f, rng.nextFloat())
    }

    companion object {
        const val LIVES = 3
        const val GOLD_PTS = 5
        /** Half the jar's mouth, as a share of the field's width. */
        const val JAR = 0.09f
        /** How far down the field the jar's rim sits. */
        const val RIM = 0.88f
        /** Paid rounds a day. */
        const val PAID = 5
        /** Score that earns the most a round can pay. */
        const val CAP = 60

        /** Cash for a round scoring [score] at [level]: a tenth of a day's login cash per 10 points, up to [CAP]. */
        fun cash(score: Int, level: Int): Long = if (score <= 0) 0 else nice((60.0 + 30.0 * level * level) * min(score, CAP) / 100.0)
        fun xp(score: Int): Long = (min(score, CAP) / 2).toLong()

        /** Paid rounds left today. */
        fun left(f: Farm, now: Long): Int = if (f.dropDay == localDay(now)) (PAID - f.dropPlays).coerceAtLeast(0) else PAID

        /** Banks a finished round: the best score, and the pay if today still has paid rounds. Returns the cash paid. */
        fun reward(f: Farm, score: Int, now: Long): Long {
            val day = localDay(now)
            if (f.dropDay != day) { f.dropDay = day; f.dropPlays = 0 }
            if (score > f.dropBest) f.dropBest = score
            if (f.dropPlays >= PAID) return 0
            f.dropPlays++
            val c = cash(score, f.level)
            if (c > 0) f.earnCash(c)
            xp(score).takeIf { it > 0 }?.let { f.addXp(it) }
            return c
        }

        fun save(f: Farm): JSONObject = JSONObject().put("best", f.dropBest).put("day", f.dropDay).put("plays", f.dropPlays)
        fun load(f: Farm, o: JSONObject?) {
            f.dropBest = o?.optInt("best") ?: 0; f.dropDay = o?.optLong("day", -1L) ?: -1L; f.dropPlays = o?.optInt("plays") ?: 0
        }
    }
}
