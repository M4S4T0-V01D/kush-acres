package dev.m4s4t0.kushacres.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Every sound in the game, made from sine waves and noise. No Android here, so it is unit tested. */
enum class Sfx {
    TAP, OPEN, CLOSE, PLANT, WATER, HARVEST, GOLDEN, COIN, CASH, LEVEL_UP, BUILD, DEMOLISH, ERROR, SHOO, COLLECT,
    EVENT, RAID, BUY, UNLOCK
}

object Synth {
    const val RATE = 22050
    private const val TAU = (2 * PI).toFloat()

    /** Frequency of a MIDI note. */
    fun hz(note: Int) = 440f * 2f.pow((note - 69) / 12f)

    private class Buf(seconds: Float) {
        val s = FloatArray((seconds * RATE).toInt())
        /** Adds f(t, i) from `at` seconds for `len` seconds. */
        inline fun add(at: Float, len: Float, f: (t: Float) -> Float) {
            val a = (at * RATE).toInt()
            val n = (len * RATE).toInt()
            for (i in 0 until n) { val j = a + i; if (j in s.indices) s[j] += f(i / RATE.toFloat()) }
        }
    }

    private fun bell(freq: Float, t: Float, decay: Float) =
        (sin(TAU * freq * t) + 0.45f * sin(TAU * freq * 2.76f * t) * exp(-t * decay * 1.8f) +
            0.2f * sin(TAU * freq * 5.4f * t) * exp(-t * decay * 3f)) * exp(-t * decay) * attack(t, 0.004f)

    private fun attack(t: Float, a: Float) = if (t < a) t / a else 1f
    private fun square(phase: Float) = if ((phase % 1f) < 0.5f) 1f else -1f
    /** A softened square: round edges, less buzz. */
    private fun soft(phase: Float) = sin(TAU * phase).let { it + sin(3 * TAU * phase) / 3f + sin(5 * TAU * phase) / 7f } * 0.8f

    private fun lowpass(x: FloatArray, cut: Float) {
        val a = 1f - exp(-TAU * cut / RATE)
        var y = 0f
        for (i in x.indices) { y += a * (x[i] - y); x[i] = y }
    }

    private fun noise(seed: Int, n: Int): FloatArray { val r = Random(seed); return FloatArray(n) { r.nextFloat() * 2 - 1 } }

    /** Peak to `peak`, so every effect sits at the same loudness. */
    private fun norm(x: FloatArray, peak: Float = 0.8f): FloatArray {
        var m = 1e-6f
        for (v in x) m = maxOf(m, kotlin.math.abs(v))
        for (i in x.indices) x[i] = x[i] / m * peak
        return x
    }

    fun sfx(s: Sfx): FloatArray = when (s) {
        Sfx.TAP -> Buf(0.05f).apply { add(0f, 0.05f) { t -> sin(TAU * 1400 * t) * exp(-t * 120) } }.s.let { norm(it, 0.5f) }
        Sfx.OPEN -> Buf(0.16f).apply {
            add(0f, 0.08f) { t -> soft(hz(72) * t) * exp(-t * 30) }
            add(0.06f, 0.1f) { t -> soft(hz(79) * t) * exp(-t * 28) }
        }.s.also { lowpass(it, 3000f) }.let { norm(it, 0.45f) }
        Sfx.CLOSE -> Buf(0.16f).apply {
            add(0f, 0.08f) { t -> soft(hz(79) * t) * exp(-t * 30) }
            add(0.06f, 0.1f) { t -> soft(hz(72) * t) * exp(-t * 28) }
        }.s.also { lowpass(it, 2500f) }.let { norm(it, 0.4f) }
        Sfx.PLANT -> {
            val b = Buf(0.16f); val n = noise(1, b.s.size); lowpass(n, 900f)
            b.add(0f, 0.16f) { t -> n[(t * RATE).toInt().coerceAtMost(n.size - 1)] * exp(-t * 35) * 2.5f }
            b.add(0f, 0.14f) { t -> sin(TAU * (190 - t * 400) * t) * exp(-t * 28) }
            norm(b.s, 0.7f)
        }
        Sfx.WATER -> Buf(0.32f).apply {
            for ((k, at) in floatArrayOf(0f, 0.07f, 0.13f, 0.2f).withIndex()) {
                val f0 = 500f + k * 140
                add(at, 0.09f) { t -> sin(TAU * (f0 + t * 5000) * t) * exp(-t * 40) * attack(t, 0.005f) }
            }
        }.s.let { norm(it, 0.55f) }
        Sfx.HARVEST -> Buf(0.3f).apply {
            add(0f, 0.06f) { t -> sin(TAU * (300 + t * 3000) * t) * exp(-t * 60) }
            add(0.03f, 0.27f) { t -> bell(hz(84), t, 14f) * 0.6f }
            add(0.03f, 0.27f) { t -> bell(hz(91), t, 16f) * 0.3f }
        }.s.let { norm(it, 0.7f) }
        Sfx.GOLDEN -> Buf(0.9f).apply {
            intArrayOf(84, 88, 91, 96, 100).forEachIndexed { i, n -> add(i * 0.07f, 0.6f) { t -> bell(hz(n), t, 6f) } }
        }.s.let { norm(it, 0.75f) }
        Sfx.COIN -> Buf(0.32f).apply {
            add(0f, 0.06f) { t -> square(hz(83) * t) * 0.5f }
            add(0.06f, 0.26f) { t -> square(hz(88) * t) * 0.5f * exp(-t * 12) }
        }.s.also { lowpass(it, 5000f) }.let { norm(it, 0.4f) }
        Sfx.CASH -> {
            val b = Buf(0.7f); val n = noise(2, b.s.size)
            b.add(0f, 0.05f) { t -> n[(t * RATE).toInt()] * exp(-t * 90) }
            b.add(0.05f, 0.04f) { t -> n[(t * RATE).toInt() + 999] * exp(-t * 90) * 0.7f }
            b.add(0.1f, 0.6f) { t -> bell(hz(91), t, 5f) + bell(hz(96), t, 5f) * 0.7f }
            norm(b.s, 0.7f)
        }
        Sfx.LEVEL_UP -> Buf(1.3f).apply {
            val notes = intArrayOf(72, 76, 79, 84)
            notes.forEachIndexed { i, n -> add(i * 0.1f, 0.3f) { t -> soft(hz(n) * t) * exp(-t * 6) * 0.6f } }
            for (n in intArrayOf(72, 76, 79, 84)) add(0.42f, 0.85f) { t -> soft(hz(n) * t) * exp(-t * 3) * 0.35f * attack(t, 0.02f) }
            add(0.42f, 0.85f) { t -> bell(hz(96), t, 4f) * 0.5f }
        }.s.also { lowpass(it, 4500f) }.let { norm(it, 0.75f) }
        Sfx.BUILD -> {
            val b = Buf(0.42f); val n = noise(3, b.s.size); lowpass(n, 1800f)
            for (k in 0 until 3) {
                val at = k * 0.13f
                b.add(at, 0.1f) { t -> (n[((at + t) * RATE).toInt().coerceAtMost(n.size - 1)] * 2 + sin(TAU * 140 * t)) * exp(-t * 45) }
            }
            norm(b.s, 0.7f)
        }
        Sfx.DEMOLISH -> {
            val b = Buf(0.55f); val n = noise(4, b.s.size); lowpass(n, 700f)
            b.add(0f, 0.55f) { t -> n[(t * RATE).toInt().coerceAtMost(n.size - 1)] * exp(-t * 6) * (1 + sin(TAU * 18 * t) * 0.5f) }
            b.add(0f, 0.3f) { t -> sin(TAU * (110 - t * 120) * t) * exp(-t * 10) * 0.6f }
            norm(b.s, 0.75f)
        }
        Sfx.ERROR -> Buf(0.28f).apply {
            add(0f, 0.12f) { t -> soft(hz(58) * t) * exp(-t * 12) }
            add(0.12f, 0.16f) { t -> soft(hz(53) * t) * exp(-t * 12) }
        }.s.also { lowpass(it, 1500f) }.let { norm(it, 0.45f) }
        Sfx.SHOO -> {
            val b = Buf(0.36f); val n = noise(5, b.s.size)
            // A swept band of noise: a whoosh.
            var lp = 0f; var lp2 = 0f
            for (i in b.s.indices) {
                val t = i / RATE.toFloat()
                val a = 1f - exp(-TAU * (600 + t * 5000) / RATE)
                lp += a * (n[i] - lp); lp2 += 0.08f * (lp - lp2)
                b.s[i] = (lp - lp2) * sin(PI.toFloat() * t / 0.36f)
            }
            norm(b.s, 0.6f)
        }
        Sfx.COLLECT -> Buf(0.5f).apply {
            add(0f, 0.08f) { t -> sin(TAU * (400 + t * 4000) * t) * exp(-t * 40) }
            add(0.06f, 0.44f) { t -> bell(hz(86), t, 8f) * 0.6f }
            add(0.13f, 0.37f) { t -> bell(hz(93), t, 8f) * 0.5f }
        }.s.let { norm(it, 0.65f) }
        Sfx.EVENT -> Buf(0.8f).apply {
            add(0f, 0.6f) { t -> bell(hz(81), t, 5f) }
            add(0.15f, 0.65f) { t -> bell(hz(88), t, 5f) }
        }.s.let { norm(it, 0.6f) }
        Sfx.RAID -> Buf(1.4f).apply {
            var ph = 0f
            add(0f, 1.4f) { t ->
                ph += (650f + 250f * sin(TAU * 1.4f * t)) / RATE
                soft(ph) * attack(t, 0.05f) * (if (t > 1.2f) (1.4f - t) / 0.2f else 1f)
            }
        }.s.also { lowpass(it, 2200f) }.let { norm(it, 0.45f) }
        Sfx.BUY -> Buf(0.25f).apply {
            add(0f, 0.04f) { t -> sin(TAU * 1600 * t) * exp(-t * 90) }
            add(0.03f, 0.22f) { t -> bell(hz(88), t, 14f) * 0.6f }
        }.s.let { norm(it, 0.55f) }
        Sfx.UNLOCK -> Buf(0.8f).apply {
            intArrayOf(79, 83, 86, 91).forEachIndexed { i, n -> add(i * 0.06f, 0.55f) { t -> bell(hz(n), t, 7f) } }
        }.s.let { norm(it, 0.6f) }
    }

    /**
     * A slow reggae loop, 16 bars of one-drop: kick and rim together on beat 3, an offbeat guitar chop with an organ
     * bubble, a deep bass line, a lead that answers itself in bars 5–8 and 13–16, dub echo on the lead and rim, and
     * vinyl crackle. Every area has its own [Tune], so each loop is in its own key, speed and voice.
     */
    fun music(tune: Tune = Tunes.of("home")): FloatArray {
        if (tune.style == Style.LOFI) return lofi(tune)
        // Dub: the skank goes through the echo, the band drops out to bass and drums for bars 9–12, sparser hats.
        val dub = tune.style == Style.DUB
        val beat = 60f / tune.bpm
        val bar = beat * 4
        val bars = 16
        val b = Buf(bar * bars)
        val wet = Buf(bar * bars)
        val r = Random(tune.seed)
        val sc = if (tune.minor) intArrayOf(0, 2, 3, 5, 7, 8, 10) else intArrayOf(0, 2, 4, 5, 7, 9, 11)
        fun note(deg: Int, base: Int) = base + 12 * Math.floorDiv(deg, 7) + sc[Math.floorMod(deg, 7)]
        var bassBase = tune.root - 24; while (bassBase < 33) bassBase += 12
        var chordBase = tune.root; while (chordBase < 57) chordBase += 12
        val leadBase = chordBase + 12
        val hat = noise(tune.seed + 3, RATE).let { n -> FloatArray(n.size) { i -> if (i == 0) 0f else n[i] - n[i - 1] } }
        val snap = noise(tune.seed + 4, RATE)
        fun rel(t: Float, len: Float) = if (t > len - 0.05f) ((len - t) / 0.05f).coerceAtLeast(0f) else 1f

        // Two bass riffs, picked for this tune: (beat, length, scale step over the chord root).
        val riffs = listOf(
            listOf(Triple(0f, 1.5f, 0), Triple(1.5f, 0.5f, 0), Triple(2f, 1f, 4), Triple(3f, 0.5f, 2), Triple(3.5f, 0.5f, 1)),
            listOf(Triple(0f, 0.75f, 0), Triple(1f, 0.5f, 2), Triple(1.5f, 1f, 4), Triple(3f, 0.5f, 7), Triple(3.5f, 0.5f, 4)),
            listOf(Triple(0.5f, 1f, 0), Triple(1.5f, 0.5f, 0), Triple(2.5f, 0.5f, 2), Triple(3f, 1f, 4)),
            listOf(Triple(0f, 2f, 0), Triple(2.5f, 0.5f, -1), Triple(3f, 1f, 0)),
            listOf(Triple(0f, 1f, 0), Triple(1f, 0.5f, 4), Triple(1.5f, 0.5f, 5), Triple(2f, 1f, 4), Triple(3.5f, 0.5f, 2)),
        )
        val riffA = riffs[r.nextInt(riffs.size)]
        val riffB = riffs[r.nextInt(riffs.size)]
        // A two-bar motif from the pentatonic scale, played again with a new ending.
        val pent = if (tune.minor) intArrayOf(0, 2, 3, 4, 6) else intArrayOf(0, 1, 2, 4, 5)
        class N(val at: Float, val len: Float, val deg: Int)
        fun phrase(): List<N> {
            val out = ArrayList<N>()
            var at = 0f; var i = r.nextInt(2, 5)
            while (at < 7f) {
                val len = listOf(0.5f, 0.5f, 1f, 1f, 1.5f, 2f)[r.nextInt(6)].coerceAtMost(8f - at)
                if (r.nextFloat() < 0.74f) out += N(at, len, 7 * Math.floorDiv(i, 5) + pent[Math.floorMod(i, 5)])
                i = (i + r.nextInt(-2, 3)).coerceIn(0, 8)
                at += len
            }
            return out
        }
        val call = phrase(); val answer = call.filter { it.at < 4f } + phrase().filter { it.at >= 4f }

        for (bi in 0 until bars) {
            val at = bi * bar
            val deg = tune.prog[(bi / tune.bars) % tune.prog.size]
            val chord = intArrayOf(note(deg, chordBase), note(deg + 2, chordBase), note(deg + 4, chordBase))
            val drop = dub && bi in 8..11
            // The skank: a muted guitar chop on 2 and 4, an organ bubbling on the offbeats.
            val sk = if (dub) wet else b
            if (!drop) for (k in intArrayOf(1, 3)) for ((j, n) in chord.withIndex()) {
                if (dub && k == 1 && bi % 2 == 1) continue
                val f = hz(n)
                sk.add(at + k * beat + j * 0.006f, 0.16f) { t -> guitar(f, t) * 0.07f * rel(t, 0.16f) }
            }
            if (!drop && !(dub && bi % 2 == 1)) for (e in 0 until 8) if (e % 2 == 1 || tune.bubble) for (n in chord) {
                val f = hz(n - if (e % 2 == 0) 12 else 0)
                val len = beat * 0.42f
                b.add(at + e * beat / 2, len) { t -> organ(f, t) * (if (e % 2 == 1) 0.032f else 0.02f) * attack(t, 0.01f) * rel(t, len) }
            }
            // Bass.
            val riff = if (bi % 4 == 3) riffB else riffA
            for ((pos, len, step) in riff) {
                val f = hz(note(deg + step, bassBase))
                val l = len * beat
                val vol = if (dub) 0.44f else 0.36f
                b.add(at + pos * beat, l) { t -> (sin(TAU * f * t) + 0.35f * sin(TAU * 2 * f * t) + 0.1f * sin(TAU * 3 * f * t)) * exp(-t * 0.7f) * attack(t, 0.012f) * rel(t, l) * vol }
            }
            // One drop: nothing on 1, kick and rim on 3, swung hats, a cross-stick fill now and then.
            b.add(at + 2 * beat, 0.4f) { t -> sin(TAU * (62 - t * 50) * t) * exp(-t * 8) * 0.62f }
            wet.add(at + 2 * beat, 0.12f) { t -> (sin(TAU * 1650 * t) * 0.6f + snap[(t * RATE).toInt()]) * exp(-t * 55) * 0.2f }
            if (bi % 4 == 3 || (dub && bi % 2 == 1)) wet.add(at + 3.5f * beat, 0.1f) { t -> (sin(TAU * 1650 * t) * 0.6f + snap[(t * RATE).toInt()]) * exp(-t * 60) * 0.1f }
            for (e in 0 until 8) {
                if (dub && e % 2 == 0) continue
                val te = at + e * beat / 2 + if (e % 2 == 1) beat * 0.12f else 0f
                val open = e == 7 && bi % 2 == 1
                b.add(te, if (open) 0.3f else 0.06f) { t -> hat[(t * RATE).toInt()] * exp(-t * (if (open) 10f else 80f)) * (if (e % 2 == 0) 0.05f else 0.035f) }
            }
            // The lead: the call in bars 5–8, the answer in 13–16.
            val ph = when (bi) { in 4..7 -> call; in 12..15 -> answer; else -> null }
            if (ph != null && bi % 2 == 0) for (n in ph) {
                val f = hz(note(n.deg, leadBase))
                val l = n.len * beat * 0.95f
                wet.add(at + n.at * beat, l) { t -> lead(tune.lead, f, t) * rel(t, l) * 0.1f }
            }
        }
        // Dub echo, a dotted eighth, carried round the loop so the tail wraps without a gap.
        val d = (beat * 0.75f * RATE).toInt()
        val n = wet.s.size
        val echo = wet.s.copyOf()
        repeat(2) { for (i in 0 until n) echo[i] = wet.s[i] + tune.dub * echo[(i - d + n) % n] }
        for (i in 0 until n) echo[i] -= wet.s[i]
        lowpass(echo, if (dub) 1200f else 1600f)
        for (i in 0 until n) b.s[i] += wet.s[i] + echo[i]
        lowpass(b.s, 3200f)
        val v = Random(tune.seed + 1)
        for (i in b.s.indices) {
            b.s[i] += (v.nextFloat() - 0.5f) * 0.006f
            if (v.nextInt(9000) == 0) b.s[i] += (v.nextFloat() - 0.5f) * 0.25f
        }
        // Fade the very ends a touch so the loop point never clicks.
        val fade = RATE / 100
        for (i in 0 until fade) { val k = i / fade.toFloat(); b.s[i] *= k; b.s[b.s.size - 1 - i] *= k }
        return norm(b.s, 0.7f)
    }

    /**
     * Lo-fi hip hop: a lazy swung boom-bap beat, seventh chords on a warbly electric piano, a round sine bass, a few
     * lead notes through a short echo, and a dusty record with plenty of crackle, all under a dark tape filter.
     */
    private fun lofi(tune: Tune): FloatArray {
        val beat = 60f / tune.bpm
        val bar = beat * 4
        val bars = 16
        val b = Buf(bar * bars)
        val wet = Buf(bar * bars)
        val r = Random(tune.seed)
        val sc = if (tune.minor) intArrayOf(0, 2, 3, 5, 7, 8, 10) else intArrayOf(0, 2, 4, 5, 7, 9, 11)
        fun note(deg: Int, base: Int) = base + 12 * Math.floorDiv(deg, 7) + sc[Math.floorMod(deg, 7)]
        var bassBase = tune.root - 24; while (bassBase < 33) bassBase += 12
        var chordBase = tune.root; while (chordBase < 52) chordBase += 12
        val leadBase = chordBase + 12
        val swing = beat * 0.17f
        fun eighth(e: Int) = e * beat / 2 + if (e % 2 == 1) swing else 0f
        fun rel(t: Float, len: Float) = if (t > len - 0.06f) ((len - t) / 0.06f).coerceAtLeast(0f) else 1f
        val hat = noise(tune.seed + 3, RATE).let { n -> FloatArray(n.size) { i -> if (i == 0) 0f else n[i] - n[i - 1] } }
        val snare = noise(tune.seed + 4, RATE).also { lowpass(it, 4000f) }
        // Two kick patterns, in beats; the second comes round every fourth bar.
        val kicks = listOf(floatArrayOf(0f, 2.5f), floatArrayOf(0f, 1.75f, 2.5f), floatArrayOf(0f, 0.75f, 2.5f, 3.25f))
        val kickA = kicks[r.nextInt(2)]; val kickB = kicks[2]
        val pent = if (tune.minor) intArrayOf(0, 2, 3, 4, 6) else intArrayOf(0, 1, 2, 4, 5)
        class N(val at: Float, val len: Float, val deg: Int)
        fun phrase(): List<N> {
            val out = ArrayList<N>()
            var at = 0.5f; var i = r.nextInt(3, 6)
            while (at < 7f) {
                val len = listOf(0.5f, 1f, 1f, 1.5f, 2f)[r.nextInt(5)].coerceAtMost(8f - at)
                if (r.nextFloat() < 0.6f) out += N(at, len, 7 * Math.floorDiv(i, 5) + pent[Math.floorMod(i, 5)])
                i = (i + r.nextInt(-2, 3)).coerceIn(0, 8)
                at += len
            }
            return out
        }
        val call = phrase(); val answer = phrase()

        for (bi in 0 until bars) {
            val at = bi * bar
            val deg = tune.prog[(bi / tune.bars) % tune.prog.size]
            // Seventh chords, rolled a little, on the one and pushed ahead of beat 3.
            val chord = IntArray(4) { note(deg + it * 2, chordBase) }
            for ((pos, len) in listOf(0f to beat * 2.3f, 2.5f to beat * 1.4f)) for ((j, n) in chord.withIndex()) {
                val f = hz(n)
                val v = if (pos == 0f) 0.07f else 0.05f
                b.add(at + pos * beat + (if (pos > 0) swing else 0f) + j * 0.014f, len) { t -> keys(f, t) * v * rel(t, len) }
            }
            // Bass: the root, a push before 3, the fifth to walk back.
            for ((pos, len, step) in listOf(Triple(0f, 1.6f, 0), Triple(2.5f, 0.5f, 0), Triple(3f, 1f, if (bi % 2 == 0) 4 else 2))) {
                val f = hz(note(deg + step, bassBase))
                val l = len * beat
                b.add(at + pos * beat + (if (pos % 1f != 0f) swing else 0f), l) { t -> (sin(TAU * f * t) + 0.15f * sin(TAU * 2 * f * t)) * exp(-t * 1.1f) * attack(t, 0.01f) * rel(t, l) * 0.4f }
            }
            // Boom bap.
            val kick = if (bi % 4 == 3) kickB else kickA
            for (k in kick) b.add(at + k * beat + (if (k % 1f != 0f) swing / 2 else 0f), 0.35f) { t -> sin(TAU * (75 - t * 70) * t) * exp(-t * 11) * 0.75f }
            for (k in intArrayOf(1, 3)) b.add(at + k * beat + 0.012f, 0.25f) { t ->
                (snare[(t * RATE).toInt()] * exp(-t * 20) * 0.45f + sin(TAU * 185 * t) * exp(-t * 30) * 0.3f) * 0.6f
            }
            for (e in 0 until 8) {
                val soft = e % 2 == 1
                b.add(at + eighth(e), 0.05f) { t -> hat[(t * RATE).toInt()] * exp(-t * 90) * (if (soft) 0.025f else 0.04f) }
            }
            val ph = when (bi) { in 4..7 -> call; in 12..15 -> answer; else -> null }
            if (ph != null && bi % 2 == 0) for (n in ph) {
                val f = hz(note(n.deg, leadBase))
                val l = n.len * beat * 0.9f
                val off = if (n.at % 1f != 0f) swing else 0f
                wet.add(at + n.at * beat + off, l) { t -> lead(tune.lead, f, t) * rel(t, l) * 0.085f }
            }
        }
        // A short, dark echo on the lead.
        val d = (beat * 0.5f * RATE).toInt()
        val n = wet.s.size
        val echo = wet.s.copyOf()
        repeat(2) { for (i in 0 until n) echo[i] = wet.s[i] + 0.35f * echo[(i - d + n) % n] }
        for (i in 0 until n) echo[i] -= wet.s[i]
        lowpass(echo, 1400f)
        for (i in 0 until n) b.s[i] += wet.s[i] + echo[i]
        lowpass(b.s, 2300f)
        val v = Random(tune.seed + 1)
        for (i in b.s.indices) {
            b.s[i] += (v.nextFloat() - 0.5f) * 0.012f
            if (v.nextInt(2500) == 0) b.s[i] += (v.nextFloat() - 0.5f) * 0.3f
        }
        val fade = RATE / 100
        for (i in 0 until fade) { val k = i / fade.toFloat(); b.s[i] *= k; b.s[b.s.size - 1 - i] *= k }
        return norm(b.s, 0.7f)
    }

    /** An electric piano: a soft tine with a little bell on the attack and a slow tremolo. */
    private fun keys(f: Float, t: Float) = (sin(TAU * f * t) + 0.25f * sin(TAU * 2 * f * t) * exp(-t * 3) + 0.12f * sin(TAU * 7.1f * f * t) * exp(-t * 18)) *
        exp(-t * 1.3f) * attack(t, 0.004f) * (1 + 0.12f * sin(TAU * 4.5f * t))

    private fun guitar(f: Float, t: Float) = (sin(TAU * f * t) + 0.5f * sin(TAU * 2 * f * t) + 0.3f * sin(TAU * 3 * f * t) + 0.2f * sin(TAU * 4 * f * t)) * exp(-t * 16) * attack(t, 0.002f)
    private fun organ(f: Float, t: Float) = (sin(TAU * f * t) + 0.7f * sin(TAU * 2 * f * t) + 0.4f * sin(TAU * 3 * f * t) + 0.25f * sin(TAU * 4 * f * t)) * (1 + 0.15f * sin(TAU * 6 * t))

    private fun lead(l: Lead, f: Float, t: Float): Float {
        val vib = 0.02f * sin(TAU * 5.5f * t) * attack(t, 0.25f)
        val ph = f * t + vib
        return when (l) {
            Lead.MELODICA -> soft(ph) * exp(-t * 0.5f) * attack(t, 0.03f)
            Lead.STEEL -> (sin(TAU * ph) + 0.5f * sin(TAU * 2 * ph) * exp(-t * 4) + 0.35f * sin(TAU * 3.98f * f * t) * exp(-t * 6)) * exp(-t * 2.6f) * attack(t, 0.003f) * 1.3f
            Lead.BELL -> bell(f, t, 2.4f) * 1.2f
            Lead.SYNTH -> (sin(TAU * ph) + sin(TAU * 2 * ph) / 2 + sin(TAU * 3 * ph) / 3 + sin(TAU * 4 * ph) / 4) * exp(-t * 0.6f) * attack(t, 0.02f) * 0.8f
            Lead.HORN -> (sin(TAU * ph) + (0.6f * sin(TAU * 2 * ph) + 0.45f * sin(TAU * 3 * ph) + 0.3f * sin(TAU * 4 * ph) + 0.2f * sin(TAU * 5 * ph)) * attack(t, 0.08f)) * exp(-t * 0.4f) * attack(t, 0.04f) * 0.7f
            Lead.FLUTE -> (sin(TAU * ph) + 0.12f * sin(TAU * 2 * ph) + 0.05f * sin(TAU * 1731 * t) * sin(TAU * 97 * t)) * exp(-t * 0.4f) * attack(t, 0.05f) * 1.1f
            Lead.ORGAN -> organ(f, t) * 0.55f * attack(t, 0.01f) * exp(-t * 0.3f)
            Lead.GUITAR -> guitar(f, t) * 1.4f * exp(t * 11)
        }
    }

    /** 16-bit mono PCM WAV. */
    fun wav(x: FloatArray): ByteArray {
        val data = x.size * 2
        val out = java.nio.ByteBuffer.allocate(44 + data).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        out.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(RATE).putInt(RATE * 2).putShort(2).putShort(16)
        out.put("data".toByteArray()).putInt(data)
        for (v in x) out.putShort((v.coerceIn(-1f, 1f) * 32767).toInt().toShort())
        return out.array()
    }
}

/** Background loops under the music. */
enum class Amb { DAY, NIGHT, RAIN }

object Ambience {
    private const val RATE = Synth.RATE
    private const val TAU = (2 * PI).toFloat()

    /** A 16 s seamless loop. */
    fun loop(a: Amb): FloatArray {
        val n = RATE * 16
        val x = FloatArray(n)
        val r = Random(a.ordinal + 40)
        fun put(at: Int, len: Int, f: (Float) -> Float) { for (i in 0 until len) x[(at + i) % n] += f(i / RATE.toFloat()) }
        when (a) {
            Amb.DAY -> {
                // Soft wind, then birds: little chirp phrases, two kinds.
                var lp = 0f; var lp2 = 0f
                for (i in 0 until n) { lp += 0.02f * (r.nextFloat() * 2 - 1 - lp); lp2 += 0.3f * (lp - lp2); x[i] = lp2 * 1.2f }
                repeat(14) {
                    val at = r.nextInt(n); val base = 2600f + r.nextFloat() * 1600; val notes = 2 + r.nextInt(4); val kind = r.nextInt(2)
                    for (k in 0 until notes) put(at + k * (RATE * (0.09f + kind * 0.06f)).toInt(), (RATE * 0.07f).toInt()) { t ->
                        val f = if (kind == 0) base + t * 9000 else base * 1.2f - t * 12000
                        sin(TAU * f * t + 2f * sin(TAU * 60 * t)) * sin(PI.toFloat() * t / 0.07f) * 0.12f
                    }
                }
            }
            Amb.NIGHT -> {
                for ((ci, pitch) in floatArrayOf(4300f, 4700f).withIndex()) {
                    var at = (ci * 0.27f * RATE).toInt()
                    val gap = (RATE * (0.55f + ci * 0.13f)).toInt()
                    while (at < n) {
                        for (p in 0 until 3) put(at + p * (RATE / 30), RATE / 45) { t -> sin(TAU * pitch * t) * sin(PI.toFloat() * t * 45) * 0.08f }
                        at += gap + r.nextInt(RATE / 20)
                    }
                }
                var lp = 0f
                for (i in 0 until n) { lp += 0.01f * (r.nextFloat() * 2 - 1 - lp); x[i] += lp * 0.6f }
            }
            Amb.RAIN -> {
                var lp = 0f; var hp = 0f
                for (i in 0 until n) { lp += 0.25f * (r.nextFloat() * 2 - 1 - lp); hp += 0.02f * (lp - hp); x[i] = (lp - hp) * 0.5f }
                repeat(160) {
                    val at = r.nextInt(n); val f = 1500f + r.nextFloat() * 2500
                    put(at, RATE / 40) { t -> sin(TAU * (f - t * 20000) * t) * exp(-t * 200) * 0.12f }
                }
            }
        }
        var m = 1e-6f
        for (v in x) m = maxOf(m, kotlin.math.abs(v))
        for (i in x.indices) x[i] = x[i] / m * 0.6f
        return x
    }
}
