package dev.m4s4t0.kushacres.model

import dev.m4s4t0.kushacres.audio.Radio
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * A townsfolk request pinned to the board: hand over [need] in one go for cash, XP and a [prize]: "track:ID" (a radio
 * track), "gems", "bucks" or "seeds:ID", [amount] of it.
 */
class Note(val who: String, val why: String, val need: Need, val prize: String, val amount: Int) {
    var done = false
}

/** What handing a note in paid. */
class NotePay(val cash: Long, val xp: Long, val prize: String)

/**
 * The town board, open from [LEVEL]: [NOTES] small requests from the townsfolk each day, filled straight from the
 * stash. Smaller than contracts and with no clock on them, but each one pays a prize, and the board is the only place
 * some radio tracks come from. A new day brings a fresh board.
 */
object Board {
    const val LEVEL = 3
    const val NOTES = 3
    const val PAY = 1.6

    private val WHY = listOf(
        "for my birthday party", "for book club", "for the band's tour bus", "to help me sleep", "for my grandma's knees",
        "for a camping trip", "for open mic night", "for my art class", "for a beach bonfire", "for movie night",
        "for the bowling team", "to calm my nerves before the big game", "for a road trip", "for my yoga retreat",
    )

    fun notes(f: Farm, now: Long): List<Note> {
        if (f.level < LEVEL) return emptyList()
        val day = localDay(now)
        if (f.boardDay != day) {
            f.boardDay = day
            f.board.clear(); f.board += make(f)
        }
        return f.board
    }

    private fun round10(x: Double) = (ceil(x / 10) * 10).roundToInt().coerceAtLeast(10)

    private fun make(f: Farm): List<Note> {
        val r = f.rng
        val ladder = Strains.ladder.filter { f.owned(it) }.takeLast(4)
        if (ladder.isEmpty()) return emptyList()
        val classes = CupClass.entries.filter { it != CupClass.OPEN && Contracts.fitting(f, it).isNotEmpty() }
        val kitchens = Facility.entries.filter { fac -> var n = 0; f.eachSite { n += f.count(fac) }; n > 0 }
        val people = Farm.BUYERS.shuffled(r)
        val whys = WHY.shuffled(r)
        val exotics = Strains.exotics.filter { f.unlocked(it) }
        // At most one track a day, so they last a while.
        var track = Radio.prize(f, r).takeIf { r.nextInt(10) < 6 }
        return List(NOTES) { i ->
            val need = when (if (i == 0) 0 else r.nextInt(3)) {
                1 -> classes.randomOrNull(r)?.let { c -> Need("class:${c.name}", round10(Contracts.perHour(f, Contracts.fitting(f, c).maxBy { it.price }) * 0.8)) }
                2 -> kitchens.randomOrNull(r)?.let { fac -> Need("goods:${fac.name}", 2 + f.level / 8 + r.nextInt(3)) }
                else -> null
            } ?: ladder[r.nextInt(ladder.size)].let { s -> Need(s.id, round10(Contracts.perHour(f, s) * (0.5 + r.nextDouble() * 0.6))) }
            val prize = when {
                track != null && i == NOTES - 1 -> "track:$track".also { track = null }
                else -> when (r.nextInt(3)) {
                    0 -> "gems"
                    1 -> "bucks"
                    // Seed stock only counts for exotics; the rest are bought as they're planted.
                    else -> exotics.randomOrNull(r)?.let { "seeds:${it.id}" } ?: "gems"
                }
            }
            val amount = when {
                prize == "gems" -> 1 + r.nextInt(3)
                prize == "bucks" -> 2 + r.nextInt(4)
                prize.startsWith("seeds:") -> 2 + r.nextInt(3)
                else -> 1
            }
            Note(people[i % people.size], whys[i % whys.size], need, prize, amount)
        }
    }

    /** Everything the stash and goods hold towards [n]. */
    fun have(f: Farm, n: Need) = Contracts.sources(f, n).sumOf { it.second }
    fun canFill(f: Farm, note: Note) = !note.done && have(f, note.need) >= note.need.amount

    fun prizeLabel(prize: String, amount: Int): String = when {
        prize.startsWith("track:") -> "♪ \"${Radio[prize.removePrefix("track:")]?.name}\""
        prize == "gems" -> "$amount gem" + if (amount == 1) "" else "s"
        prize == "bucks" -> "$amount Pot Bucks"
        prize.startsWith("seeds:") -> "$amount ${Strains[prize.removePrefix("seeds:")]?.name ?: ""} seeds"
        else -> prize
    }

    /** Hands [note] in from the stash, or returns null if there isn't enough. */
    fun fill(f: Farm, note: Note): NotePay? {
        if (!canFill(f, note)) return null
        val n = note.need
        var value = 0.0
        for ((key, have) in Contracts.sources(f, n)) {
            if (n.done) break
            val k = min(have, n.left)
            val goods = n.what.startsWith("goods:")
            val box = if (goods) f.goods else f.stash
            if (have == k) box.remove(key) else box[key] = have - k
            value += k * (if (goods) f.goodsPrice(key) else (Strains[key]?.price ?: 0L) * f.priceMult())
            n.got += k
        }
        note.done = true
        val cash = max(1L, nice(value * PAY))
        val xp = max(1L, (Farm.xpNeed(f.level) * 0.01).roundToLong())
        f.earnCash(cash)
        f.events += FarmEvent.Cash(cash)
        f.addXp(xp)
        val p = note.prize
        when {
            p.startsWith("track:") -> f.tracks += p.removePrefix("track:")
            p == "gems" -> f.gems += note.amount
            p == "bucks" -> f.potBucks += note.amount
            p.startsWith("seeds:") -> p.removePrefix("seeds:").let { f.seeds[it] = (f.seeds[it] ?: 0) + note.amount }
        }
        f.boardDone++
        return NotePay(cash, xp, p)
    }

    // ---- Saving ----

    fun save(f: Farm): JSONObject = JSONObject().put("day", f.boardDay).put("done", f.boardDone)
        .put("notes", JSONArray().apply {
            f.board.forEach { n ->
                put(JSONObject().put("who", n.who).put("why", n.why).put("w", n.need.what).put("a", n.need.amount).put("g", n.need.got)
                    .put("p", n.prize).put("n", n.amount).put("d", n.done))
            }
        })

    fun load(f: Farm, o: JSONObject?) {
        f.board.clear(); f.boardDay = -1L; f.boardDone = 0
        if (o == null) return
        f.boardDay = o.optLong("day", -1L); f.boardDone = o.optInt("done")
        o.optJSONArray("notes")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let { j ->
                f.board += Note(j.optString("who"), j.optString("why"), Need(j.optString("w"), j.optInt("a", 10), j.optInt("g")),
                    j.optString("p", "gems"), j.optInt("n", 1)).apply { done = j.optBoolean("d") }
            }
        }
    }
}
