package dev.m4s4t0.kushacres.audio

import kotlin.math.abs
import kotlin.random.Random

/**
 * Writes a lead the way a songwriter does, after bountiful-music's melodist: a two-bar motif drawn from a rhythm
 * vocabulary, answered, lifted to a climax and brought home by a cadence, with strong and long notes on the chord.
 * Notes are in scale steps over the lead's base note, timed in beats from the start of the loop.
 */
class Melodist(
    private val r: Random,
    /** The chord's root, in scale steps, sounding at a beat. */
    private val chord: (Float) -> Int,
    /** Sevenths count as chord tones (lo-fi). */
    private val rich: Boolean,
) {
    data class Note(val at: Float, val len: Float, val deg: Int)

    private val lo = -2
    private val hi = 11

    private fun tone(d: Int, c: Int) = Math.floorMod(d - c, 7).let { it == 0 || it == 2 || it == 4 || (rich && it == 6) }

    /** On a strong note, the chord tone within two steps that moves least from the last note; otherwise the step itself. */
    private fun fit(d: Int, at: Float, strong: Boolean, prev: Int? = null): Int {
        if (!strong) return d
        val c = chord(at)
        val ref = prev ?: d
        return (d - 2..d + 2).filter { it in lo..hi && tone(it, c) }.minByOrNull { abs(it - ref) + 0.6f * abs(it - d) } ?: d
    }

    /** The next step from [d] in [dir]: a scale step, or on a strong note the nearest other chord tone. */
    private fun away(d: Int, at: Float, strong: Boolean, dir: Int): Int {
        val c = chord(at)
        for (k in 1..3) for (s in intArrayOf(dir, -dir)) {
            val j = d + s * k
            if (j in lo..hi && (!strong || tone(j, c))) return j
        }
        return d
    }

    /** [n] scale offsets: mostly steps, at most one leap, and that filled back. */
    private fun shape(n: Int, dir0: Int): List<Int> {
        val out = mutableListOf(0)
        val leapAt = if (n > 2 && r.nextFloat() < 0.6f) r.nextInt(1, n) else -1
        var dir = dir0
        for (i in 1 until n) {
            val s = when (i) {
                leapAt -> dir * r.nextInt(2, 4)
                leapAt + 1 -> -dir * intArrayOf(1, 1, 2)[r.nextInt(3)]
                else -> dir * r.nextFloat().let { if (it < 0.58f) 1 else if (it < 0.86f) -1 else 2 }
            }
            if (r.nextFloat() < 0.25f) dir = -dir
            out += out.last() + s
        }
        return out
    }

    /** A melody [bars] long in two-bar phrases: call, response, call', cadence, call, response, climax, cadence. */
    fun write(bars: Int, rhythm: String, density: Float): List<Note> {
        val cells = CELLS.getValue(rhythm)
        val dir0 = if (r.nextBoolean()) 1 else -1
        val base = r.nextInt(0, 5)
        val call = listOf(cells.random(r), cells.random(r))
        val resp = listOf(call[0], cells.random(r))
        val motif = shape(maxOf(call[0].size + call[1].size, 4), dir0)
        val out = ArrayList<Note>()
        var climaxes = 0
        for (ph in 0 until bars / 2) {
            val kind = PLAN[ph % PLAN.size]
            val t0 = ph * 8f
            val rr = if (kind == "response" || kind == "cadence") resp else call
            val lift = when (kind) { "response" -> 1; "climax" -> 4 + if (climaxes++ > 0) 2 else 0; else -> 0 }
            val sh = if (kind == "call2") motif.dropLast(2) + shape(3, -dir0).drop(1) else motif
            val evs = rr.flatMapIndexed { bi, cell -> cell.map { (st, ln) -> Triple(bi, st, ln) } }
            var k = 0
            for ((j, e) in evs.withIndex()) {
                val (bi, st, ln) = e
                val last = j == evs.lastIndex
                if (!last && j > 0 && r.nextFloat() > density) continue // room to breathe
                val at = t0 + bi * 4 + st / 4f
                if (at < 0) continue
                val len = if (kind == "cadence" && last) maxOf(ln / 4f, 2f) else ln / 4f
                val off = if (kind == "cadence") sh[k % sh.size] - (if (j >= evs.size / 2) 1 else 0) else sh[k % sh.size] + lift
                val strong = at % 2f == 0f || len >= 1f || last
                val prev = out.lastOrNull()?.deg
                var d = fit((base + off).coerceIn(lo, hi), at, strong, prev)
                // No note-on-note runs.
                if (prev != null && d == prev && ((out.size > 1 && out[out.size - 2].deg == d) || r.nextFloat() < 0.6f))
                    d = away(d, at, strong, if (r.nextFloat() < 0.7f) dir0 else -dir0)
                // The cadence settles on the root or fifth of its chord.
                if (kind == "cadence" && last) { val c = chord(at); d = (d - 4..d + 4).filter { it in lo..hi && Math.floorMod(it - c, 7).let { m -> m == 0 || m == 4 } }.minByOrNull { abs(it - d) } ?: d }
                out += Note(at, len, d)
                k++
            }
        }
        out.sortBy { it.at }
        val notes = tidy(out).filter { it.at < bars * 4 }.toMutableList()
        // A cramped line gets its peak lifted so it spans an octave.
        if (notes.isNotEmpty()) {
            val span = notes.maxOf { it.deg } - notes.minOf { it.deg }
            if (span < 7) {
                val peak = notes.indices.maxBy { notes[it].deg }
                for (i in maxOf(0, peak - 2)..minOf(notes.lastIndex, peak + 2)) {
                    val up = notes[i].deg + 7 - span
                    if (up <= hi) notes[i] = notes[i].copy(deg = fit(up, notes[i].at, true))
                }
            }
        }
        return notes
    }

    /** No overlaps, nothing too short. */
    private fun tidy(x: List<Note>) = x.mapIndexed { i, n ->
        val next = x.getOrNull(i + 1)
        if (next == null) n else n.copy(len = minOf(n.len, next.at - n.at - 0.02f))
    }.filter { it.len > 0.05f }

    /** A variation for repeats: ornament (pickups and passing notes), answer (a new ending), lift (the peak phrase up), sparse (strong beats, held). */
    fun variant(notes: List<Note>, kind: String): List<Note> {
        var out = notes.toMutableList()
        when (kind) {
            "ornament" -> {
                val add = ArrayList<Note>()
                for (i in 0 until out.size - 1) {
                    val a = out[i]; val b = out[i + 1]
                    if (b.at % 1f == 0f && b.at - a.at >= 0.75f && r.nextFloat() < 0.4f) {
                        add += Note(b.at - 0.25f, 0.22f, b.deg + if (r.nextBoolean()) 1 else -1)
                        out[i] = a.copy(len = minOf(a.len, b.at - 0.27f - a.at))
                    } else if (b.at - a.at - a.len >= 0.5f && r.nextFloat() < 0.25f) add += Note(a.at + a.len + 0.02f, 0.22f, a.deg + 1)
                }
                out = (out + add).sortedBy { it.at }.toMutableList()
            }
            "answer" -> if (out.size > 4) for (i in out.size - 3 until out.size - 1) {
                val x = out[i]
                out[i] = x.copy(deg = fit((x.deg + intArrayOf(-2, -1, 1, 2)[r.nextInt(4)]).coerceIn(lo, hi), x.at, x.at % 2f == 0f))
            }
            "lift" -> if (out.isNotEmpty()) {
                val ph = (out.maxBy { it.deg }.at / 8).toInt() * 8
                for ((i, x) in out.withIndex()) if (x.at >= ph && x.at < ph + 8)
                    out[i] = x.copy(deg = if (x.deg + 7 <= hi + 2) x.deg + 7 else fit(minOf(hi, x.deg + 2), x.at, x.at % 2f == 0f))
            }
            "sparse" -> {
                val keep = out.filter { it.at % 2f == 0f || it.len >= 1f }
                out = keep.mapIndexed { i, a -> keep.getOrNull(i + 1)?.let { b -> a.copy(len = maxOf(a.len, minOf(b.at - a.at - 0.05f, 2f))) } ?: a }.toMutableList()
            }
        }
        return tidy(out)
    }

    /** A tune's lead: when it plays by [Feel.plan], its second half a variation, a dub drop sparse. */
    class Line(private val base: List<Note>, private val second: List<Note>, private val sparse: List<Note>, private val plan: Int) {
        fun bar(bi: Int, drop: Boolean): List<Note> {
            val on = when (plan) { 1 -> bi >= 2; 2 -> true; 3 -> bi in 2..7 || bi in 10..15; else -> bi in 4..7 || bi in 12..15 }
            if (!on) return emptyList()
            val src = if (drop) sparse else if (bi >= 8) second else base
            return src.filter { it.at >= bi * 4 && it.at < bi * 4 + 4 }
        }
        /** The notes as heard, for tests. */
        val all get() = base + second
    }

    companion object {
        /** One-bar rhythm cells: (sixteenth, length in sixteenths); a negative start is a pickup from the bar before. */
        val CELLS = mapOf(
            "house" to listOf(listOf(0 to 3, 3 to 3, 6 to 4, 12 to 2, 14 to 2), listOf(2 to 2, 4 to 2, 6 to 3, 10 to 2, 12 to 4), listOf(0 to 2, 3 to 1, 4 to 3, 10 to 6),
                listOf(-1 to 3, 3 to 2, 6 to 2, 8 to 4, 14 to 2), listOf(0 to 4, 6 to 2, 8 to 2, 11 to 5)),
            "smooth" to listOf(listOf(0 to 6, 6 to 2, 8 to 8), listOf(2 to 4, 6 to 2, 8 to 6, 14 to 2), listOf(0 to 3, 3 to 5, 10 to 6), listOf(-2 to 4, 4 to 4, 8 to 8)),
            "bouncy" to listOf(listOf(0 to 2, 2 to 2, 6 to 2, 8 to 2, 11 to 2, 14 to 2), listOf(2 to 1, 3 to 2, 6 to 2, 10 to 2, 12 to 1, 14 to 2),
                listOf(0 to 2, 3 to 2, 6 to 2, 8 to 3, 12 to 2, 14 to 2)),
            "latin" to listOf(listOf(0 to 3, 3 to 3, 6 to 2, 8 to 4, 12 to 4), listOf(0 to 2, 3 to 2, 6 to 4, 10 to 2, 12 to 4), listOf(-1 to 3, 3 to 3, 8 to 3, 11 to 5)),
            "dub" to listOf(listOf(0 to 6, 8 to 4, 14 to 2), listOf(3 to 5, 10 to 6), listOf(0 to 8, 10 to 2, 12 to 4)),
            "phonk" to listOf(listOf(0 to 3, 3 to 3, 6 to 2, 8 to 3, 11 to 3, 14 to 2), listOf(0 to 2, 2 to 2, 4 to 2, 6 to 1, 7 to 1, 8 to 2, 11 to 2, 14 to 2),
                listOf(0 to 2, 3 to 2, 6 to 2, 8 to 2, 10 to 2, 12 to 1, 13 to 1, 14 to 2), listOf(0 to 3, 3 to 3, 6 to 4, 12 to 2, 14 to 2)),
            "fresh" to listOf(listOf(0 to 2, 2 to 2, 4 to 4, 10 to 2, 12 to 4), listOf(0 to 4, 6 to 2, 8 to 2, 10 to 6), listOf(-2 to 2, 2 to 2, 6 to 3, 10 to 2, 12 to 4)),
        )
        private val PLAN = listOf("call", "response", "call2", "cadence", "call", "response", "climax", "cadence")

        /** The lead for [tune]: a rhythm vocabulary picked by its feel's note lengths, its own motif, a variation for the second half. */
        fun line(tune: Tune, fe: Feel, rich: Boolean): Line {
            val r = Random(tune.seed * 31 + 7)
            val rhythm = (tune.style.cells ?: when (fe.rhythm) {
                1 -> listOf("bouncy", "phonk", "house")
                2 -> listOf("smooth", "dub", "fresh")
                else -> listOf("latin", "house", "fresh")
            })[r.nextInt(3)]
            val m = Melodist(r, { at -> tune.prog[((at / 4).toInt() / tune.bars) % tune.prog.size] }, rich)
            val density = when { fe.plan == 2 -> 0.6f; rich -> 0.7f; else -> 0.85f }
            val base = m.write(16, rhythm, density)
            val second = m.variant(base, listOf("answer", "ornament", "lift", "answer")[fe.plan])
            return Line(base, second, m.variant(base, "sparse"), fe.plan)
        }
    }
}
