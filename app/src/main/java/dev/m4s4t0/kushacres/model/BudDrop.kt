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
    enum class Kind {
        BUD, GOLD, BUG,
        /** Power-ups: pull buds to the jar, slow everything down, widen the mouth, or block the next bug. */
        MAGNET, SLOW, WIDE, SHIELD;
        val power get() = ordinal >= MAGNET.ordinal
    }
    /** Something falling: [x] across and [y] down the field, both 0..1. */
    class Item(val kind: Kind, var x: Float, var y: Float, val spin: Float)

    val items = ArrayList<Item>()
    /** The jar's centre across the field, 0..1. */
    var jar = 0.5f
        set(v) { field = v.coerceIn(half(), 1 - half()) }
    var score = 0; private set
    var lives = LIVES; private set
    var caught = 0; private set
    var clock = 0f; private set
    /** Golden buds and power-ups caught, and bugs a shield stopped, for the daily challenge and the screen. */
    var golds = 0; private set
    var powers = 0; private set
    var blocked = 0; private set
    /** Whether a life has been lost yet. */
    val clean get() = lives == LIVES
    /** The score before the first life was lost (or now, if none has been). */
    val cleanScore get() = if (clean) score else lostAt
    private var lostAt = 0
    /** Seconds left on each timed power-up, and whether a shield is up. */
    var magnet = 0f; private set
    var slow = 0f; private set
    var wide = 0f; private set
    var shield = false; private set
    val done get() = lives <= 0
    private var next = 0.6f

    /** Half the jar's mouth just now: wider under [Kind.WIDE]. */
    fun half() = if (wide > 0) JAR * WIDE_BY else JAR
    /** Field heights per second; it starts gentle and climbs, and a [Kind.SLOW] holds it back. */
    fun speed() = (0.32f + min(clock, 90f) * 0.008f) * (if (slow > 0) SLOW_BY else 1f)
    /** Seconds between drops. */
    fun gap() = (1.0f - min(clock, 90f) * 0.008f).coerceAtLeast(0.32f)

    /** Runs the round on by [dt] seconds; returns what landed in the jar this step. */
    fun step(dt: Float): List<Kind> {
        if (done) return emptyList()
        clock += dt
        magnet = (magnet - dt).coerceAtLeast(0f); slow = (slow - dt).coerceAtLeast(0f)
        if (wide > 0) { wide = (wide - dt).coerceAtLeast(0f); jar = jar }
        next -= dt
        if (next <= 0) { next += gap(); drop() }
        val got = ArrayList<Kind>()
        val v = speed() * dt
        val it = items.iterator()
        while (it.hasNext()) {
            val i = it.next()
            val was = i.y
            i.y += v * (if (i.kind == Kind.GOLD) 1.25f else 1f)
            // The magnet tugs buds across toward the jar as they come down.
            if (magnet > 0 && (i.kind == Kind.BUD || i.kind == Kind.GOLD) && i.y > 0.3f) i.x += (jar - i.x) * min(1f, dt * 3.5f)
            // Into the jar as it crosses the rim, if it's over the mouth.
            if (was < RIM && i.y >= RIM && abs(i.x - jar) <= half()) {
                it.remove(); got += i.kind
                when (i.kind) {
                    Kind.BUD -> { score++; caught++ }
                    Kind.GOLD -> { score += GOLD_PTS; caught++; golds++ }
                    Kind.BUG -> if (shield) { shield = false; blocked++ } else { if (clean) lostAt = score; lives-- }
                    Kind.MAGNET -> { magnet = POWER_SECS; powers++ }
                    Kind.SLOW -> { slow = POWER_SECS; powers++ }
                    Kind.WIDE -> { wide = POWER_SECS; powers++ }
                    Kind.SHIELD -> { shield = true; powers++ }
                }
                if (done) break
            } else if (i.y > 1.05f) it.remove()
        }
        return got
    }

    private fun drop() {
        val r = rng.nextFloat()
        val bug = min(0.12f + clock * 0.004f, 0.35f)
        // Now and then a power-up, once the round's warmed up and none is already falling.
        val power = clock > 8f && items.none { it.kind.power } && r > 1 - POWER_CHANCE
        val kind = when {
            power -> listOf(Kind.MAGNET, Kind.SLOW, Kind.WIDE, Kind.SHIELD)[rng.nextInt(4)]
            r < bug -> Kind.BUG; r < bug + 0.08f -> Kind.GOLD; else -> Kind.BUD
        }
        items += Item(kind, 0.06f + rng.nextFloat() * 0.88f, -0.05f, rng.nextFloat())
    }

    companion object {
        const val LIVES = 3
        const val GOLD_PTS = 5
        /** Half the jar's mouth, as a share of the field's width. */
        const val JAR = 0.09f
        const val WIDE_BY = 1.6f
        const val SLOW_BY = 0.55f
        const val POWER_SECS = 7f
        const val POWER_CHANCE = 0.05f
        /** Pot Bucks for beating the daily challenge, once a day. */
        const val DAILY_BUCKS = 3L
        /** How far down the field the jar's rim sits. */
        const val RIM = 0.88f
        /** Paid rounds a day. */
        const val PAID = 5
        /** Score that earns the most a round can pay. */
        const val CAP = 60

        /** Cash for a round scoring [score] at [level]: a tenth of a day's login cash per 10 points, up to [CAP]. */
        fun cash(score: Int, level: Int): Long = if (score <= 0) 0 else nice((60.0 + 30.0 * Levels.tier(level).let { it * it }) * min(score, CAP) / 100.0)
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

        /** Today's challenge: the same for everyone on the same day. */
        fun challenge(now: Long): Challenge {
            val day = localDay(now)
            val r = Random(day * 7919L + 17)
            val k = Challenge.Kind.entries[r.nextInt(Challenge.Kind.entries.size)]
            return Challenge(k, k.lo + r.nextInt(k.hi - k.lo + 1), day)
        }

        /** Pays the daily challenge if [game] beat it and it hasn't been paid today; returns whether it paid. */
        fun beat(f: Farm, game: BudDrop, now: Long): Boolean {
            val c = challenge(now)
            if (f.dropWon == c.day || !c.met(game)) return false
            f.dropWon = c.day
            f.potBucks += DAILY_BUCKS
            f.events += FarmEvent.Bucks(DAILY_BUCKS)
            f.addXp(xp(CAP))
            return true
        }

        fun save(f: Farm): JSONObject = JSONObject().put("best", f.dropBest).put("day", f.dropDay).put("plays", f.dropPlays)
            .put("won", f.dropWon).put("skin", f.jarSkin.name).put("skins", org.json.JSONArray(f.jarSkins.map { it.name }))
        fun load(f: Farm, o: JSONObject?) {
            f.dropBest = o?.optInt("best") ?: 0; f.dropDay = o?.optLong("day", -1L) ?: -1L; f.dropPlays = o?.optInt("plays") ?: 0
            f.dropWon = o?.optLong("won", -1L) ?: -1L
            f.jarSkins.clear()
            o?.optJSONArray("skins")?.let { a -> for (i in 0 until a.length()) JarSkin.of(a.optString(i))?.let { f.jarSkins += it } }
            f.jarSkin = JarSkin.of(o?.optString("skin"))?.takeIf { JarSkin.owns(f, it) } ?: JarSkin.MASON
        }
    }
}

/** A Bud Drop daily goal: [target] of [kind] in one round, on [day]. */
class Challenge(val kind: Kind, val target: Int, val day: Long) {
    enum class Kind(val text: String, val lo: Int, val hi: Int) {
        SCORE("Score %d points", 30, 55),
        GOLD("Catch %d golden buds", 3, 6),
        CAUGHT("Catch %d buds", 25, 40),
        POWER("Grab %d power-ups", 2, 4),
        CLEAN("Score %d without losing a life", 12, 25),
    }

    val text get() = kind.text.format(target)

    /** How far [game] has got, toward [target]. */
    fun progress(game: BudDrop): Int = when (kind) {
        Kind.SCORE -> game.score
        Kind.GOLD -> game.golds
        Kind.CAUGHT -> game.caught
        Kind.POWER -> game.powers
        // A clean score counts once it's reached, even if a bug gets in later.
        Kind.CLEAN -> game.cleanScore
    }

    fun met(game: BudDrop) = progress(game) >= target
}

/** Looks for the Bud Drop jar: [need] best score unlocks one free, or buy it for [bucks] Pot Bucks (0: not for sale). */
enum class JarSkin(val title: String, val need: Int, val bucks: Long, val glass: Int, val neck: Int) {
    MASON("Mason Jar", 0, 0, 0xFFCFEFFF.toInt(), 0xFFB8C4CC.toInt()),
    AMBER("Apothecary Jar", 20, 0, 0xFFE8A04A.toInt(), 0xFF3A2A20.toInt()),
    HONEY("Honey Pot", 40, 10, 0xFFFFD060.toInt(), 0xFFB0702A.toInt()),
    COOKIE("Cookie Jar", 70, 15, 0xFFF0C8E0.toInt(), 0xFF8C5A3A.toInt()),
    GALAXY("Galaxy Jar", Int.MAX_VALUE, 25, 0xFF9A7AFF.toInt(), 0xFF2A1E5A.toInt()),
    GOLD("Solid Gold Jar", 120, 40, 0xFFFFD84A.toInt(), 0xFFC08A1A.toInt());

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
        fun owns(f: Farm, s: JarSkin) = s == MASON || s in f.jarSkins || f.dropBest >= s.need

        fun buy(f: Farm, s: JarSkin): Result {
            if (owns(f, s)) return Result.BLOCKED
            if (s.bucks <= 0) return Result.LOCKED
            if (f.potBucks < s.bucks) return Result.NO_CASH
            f.potBucks -= s.bucks
            f.jarSkins += s
            f.jarSkin = s
            return Result.OK
        }

        fun use(f: Farm, s: JarSkin): Result {
            if (!owns(f, s)) return Result.LOCKED
            f.jarSkin = s
            return Result.OK
        }
    }
}
