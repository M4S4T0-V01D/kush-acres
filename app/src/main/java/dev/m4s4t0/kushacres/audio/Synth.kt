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

    /** Bass riffs, a bar each: (beat, length, scale step over the chord root). */
    private val RIFFS = listOf(
        listOf(Triple(0f, 1.5f, 0), Triple(1.5f, 0.5f, 0), Triple(2f, 1f, 4), Triple(3f, 0.5f, 2), Triple(3.5f, 0.5f, 1)),
        listOf(Triple(0f, 0.75f, 0), Triple(1f, 0.5f, 2), Triple(1.5f, 1f, 4), Triple(3f, 0.5f, 7), Triple(3.5f, 0.5f, 4)),
        listOf(Triple(0.5f, 1f, 0), Triple(1.5f, 0.5f, 0), Triple(2.5f, 0.5f, 2), Triple(3f, 1f, 4)),
        listOf(Triple(0f, 2f, 0), Triple(2.5f, 0.5f, -1), Triple(3f, 1f, 0)),
        listOf(Triple(0f, 1f, 0), Triple(1f, 0.5f, 4), Triple(1.5f, 0.5f, 5), Triple(2f, 1f, 4), Triple(3.5f, 0.5f, 2)),
        // Steppers: pumping eighths, root and octave.
        listOf(Triple(0f, 0.5f, 0), Triple(0.5f, 0.5f, 7), Triple(1f, 0.5f, 0), Triple(1.5f, 0.5f, 7), Triple(2f, 0.5f, 0), Triple(2.5f, 0.5f, 7), Triple(3f, 0.5f, 4), Triple(3.5f, 0.5f, 2)),
        listOf(Triple(0f, 3f, 0), Triple(3f, 1f, -3)),
        listOf(Triple(0f, 0.5f, 0), Triple(0.5f, 0.5f, 1), Triple(1f, 0.5f, 2), Triple(1.5f, 1f, 4), Triple(2.5f, 0.5f, 2), Triple(3f, 1f, 0)),
        listOf(Triple(0.5f, 0.5f, 0), Triple(1f, 0.5f, 0), Triple(2f, 0.75f, 4), Triple(2.75f, 0.75f, 3), Triple(3.5f, 0.5f, 2)),
        listOf(Triple(0f, 0.75f, 0), Triple(0.75f, 0.25f, 0), Triple(1f, 0.5f, 2), Triple(2f, 0.75f, 0), Triple(2.75f, 0.25f, 0), Triple(3f, 0.5f, 4), Triple(3.5f, 0.5f, 5)),
    )

    /**
     * A slow reggae loop, 16 bars: a deep bass line, a chop on the offbeats, a lead with an echo, and vinyl crackle.
     * Every tune has its own key, speed and lead voice, and its [Feel] picks the rest:
     * - drums: 0 one drop (kick and rim on 3), 1 rockers (kick on 1 and 3), 2 steppers (kick on every beat), 3 dancehall;
     * - chop: 0 guitar on 2 and 4, 1 double chop, 2 piano stabs, 3 organ-led, 4 ska upstrokes on every offbeat;
     * - hats: 0 swung eighths, 1 sixteenths, 2 open on the offbeats, 3 a triplet shuffle;
     * - percussion: 0 none, 1 shaker, 2 hand drums, 3 cowbell and woodblock, 4 tambourine;
     * - extra: 0 none, 1 a bell arpeggio, 2 a soft pad, 3 horn stabs, 4 dub sirens and echo throws.
     * Dub strips the chop back, sends it through the echo and drops the band out for bars 9–12.
     */
    fun music(tune: Tune = Tunes.of("home")): FloatArray {
        val fe = Tunes.feel(tune)
        if (tune.style == Style.LOFI) return lofi(tune, fe)
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
        val snr = noise(tune.seed + 5, RATE).also { lowpass(it, 5000f) }
        fun rel(t: Float, len: Float) = if (t > len - 0.05f) ((len - t) / 0.05f).coerceAtLeast(0f) else 1f
        fun rim(t: Float) = (sin(TAU * 1650 * t) * 0.6f + snap[(t * RATE).toInt()]) * exp(-t * 55)

        val riffA = RIFFS[fe.bass]
        val riffB = RIFFS[(fe.bass + 3 + fe.drums) % RIFFS.size]
        val melody = Melodist.line(tune, fe, rich = false)
        // The extra layer's voice: something the lead isn't.
        val other = if (tune.lead == Lead.BELL) Lead.STEEL else Lead.BELL
        val swing = beat * (if (fe.rhythm == 1) 0.06f else 0.12f)

        for (bi in 0 until bars) {
            val at = bi * bar
            val deg = tune.prog[(bi / tune.bars) % tune.prog.size]
            val chord = intArrayOf(note(deg, chordBase), note(deg + 2, chordBase), note(deg + 4, chordBase))
            val drop = dub && bi in 8..11
            val sk = if (dub) wet else b
            // The chop.
            if (!drop) {
                val hits: List<Pair<Float, Float>> = when (fe.chop) {
                    1 -> listOf(1f to 0.1f, 1.5f to 0.08f, 3f to 0.1f, 3.5f to 0.08f)
                    3 -> listOf(3f to 0.14f)
                    4 -> listOf(0.5f to 0.09f, 1.5f to 0.09f, 2.5f to 0.09f, 3.5f to 0.09f)
                    else -> listOf(1f to 0.16f, 3f to 0.16f)
                }
                for ((k, len) in hits) {
                    if (dub && k < 2f && bi % 2 == 1) continue
                    for ((j, n) in chord.withIndex()) {
                        val f = hz(n + if (fe.chop == 4) 12 else 0)
                        if (fe.chop == 2) sk.add(at + k * beat + j * 0.004f, 0.3f) { t -> piano(f, t) * 0.08f * rel(t, 0.3f) }
                        else sk.add(at + k * beat + j * 0.006f, len) { t -> guitar(f, t) * (if (fe.chop == 4) 0.055f else 0.07f) * rel(t, len) }
                    }
                }
            }
            // The organ bubble: on the offbeats, the downbeats too if the tune asks or the organ leads the chop.
            val bubble = tune.bubble || fe.chop == 3
            if (!drop && !(dub && bi % 2 == 1)) for (e in 0 until 8) if (e % 2 == 1 || bubble) for (n in chord) {
                val f = hz(n - if (e % 2 == 0) 12 else 0)
                val len = beat * 0.42f
                val v = (if (e % 2 == 1) 0.032f else 0.02f) * (if (fe.chop == 3) 1.5f else 1f)
                b.add(at + e * beat / 2, len) { t -> organ(f, t) * v * attack(t, 0.01f) * rel(t, len) }
            }
            // Bass.
            val riff = if (bi % 4 == 3) riffB else riffA
            for ((pos, len, step) in riff) {
                val f = hz(note(deg + step, bassBase))
                val l = len * beat
                val vol = if (dub) 0.44f else 0.36f
                b.add(at + pos * beat, l) { t -> (sin(TAU * f * t) + 0.35f * sin(TAU * 2 * f * t) + 0.1f * sin(TAU * 3 * f * t)) * exp(-t * 0.7f) * attack(t, 0.012f) * rel(t, l) * vol }
            }
            // Kick and snare.
            val kicks = when (fe.drums) { 1 -> floatArrayOf(0f, 2f); 2 -> floatArrayOf(0f, 1f, 2f, 3f); 3 -> floatArrayOf(0f, 1.5f, 2.5f); else -> floatArrayOf(2f) }
            for (k in kicks) b.add(at + k * beat, 0.4f) { t -> sin(TAU * (62 - t * 50) * t) * exp(-t * 8) * (if (fe.drums == 2 && k != 2f) 0.5f else 0.62f) }
            when (fe.drums) {
                2 -> b.add(at + 2 * beat, 0.25f) { t -> (snr[(t * RATE).toInt()] * exp(-t * 16) * 0.5f + sin(TAU * 190 * t) * exp(-t * 22) * 0.4f) * 0.4f }
                3 -> for (k in intArrayOf(1, 3)) b.add(at + k * beat, 0.25f) { t -> (snr[(t * RATE).toInt()] * exp(-t * 18) * 0.5f + sin(TAU * 200 * t) * exp(-t * 25) * 0.4f) * 0.38f }
                else -> wet.add(at + 2 * beat, 0.12f) { t -> rim(t) * 0.2f }
            }
            if (bi % 4 == 3 || (dub && bi % 2 == 1)) wet.add(at + 3.5f * beat, 0.1f) { t -> rim(t) * 0.1f }
            // Hats.
            val fill = bi % 8 == 7 && fe.hats % 2 == 0
            when (fe.hats) {
                1 -> for (s in 0 until 16) {
                    if (dub && s % 4 == 0) continue
                    b.add(at + s * beat / 4, 0.05f) { t -> hat[(t * RATE).toInt()] * exp(-t * 90) * (if (s % 2 == 0) 0.04f else 0.02f) }
                }
                2 -> for (e in 0 until 8) {
                    val open = e % 2 == 1
                    if (dub && !open) continue
                    b.add(at + e * beat / 2, if (open) 0.22f else 0.05f) { t -> hat[(t * RATE).toInt()] * exp(-t * (if (open) 16f else 90f)) * (if (open) 0.04f else 0.025f) }
                }
                3 -> for (k in 0 until 4) for (tr in intArrayOf(0, 2)) {
                    if (dub && tr == 0 && k % 2 == 0) continue
                    b.add(at + (k + tr / 3f) * beat, 0.06f) { t -> hat[(t * RATE).toInt()] * exp(-t * 80) * (if (tr == 0) 0.045f else 0.03f) }
                }
                else -> for (e in 0 until 8) {
                    if (dub && e % 2 == 0) continue
                    val te = at + e * beat / 2 + if (e % 2 == 1) swing else 0f
                    val open = e == 7 && bi % 2 == 1
                    b.add(te, if (open) 0.3f else 0.06f) { t -> hat[(t * RATE).toInt()] * exp(-t * (if (open) 10f else 80f)) * (if (e % 2 == 0) 0.05f else 0.035f) }
                }
            }
            // A tom fill into every eighth bar.
            if (fill) for ((i, f) in floatArrayOf(210f, 180f, 150f, 120f).withIndex()) b.add(at + (3f + i * 0.25f) * beat, 0.3f) { t -> sin(TAU * (f - t * 60) * t) * exp(-t * 10) * 0.3f }
            // Percussion.
            when (fe.perc) {
                1 -> for (s in 0 until 16) b.add(at + s * beat / 4, 0.08f) { t -> hat[(t * RATE).toInt() + 3000] * attack(t, 0.012f) * exp(-t * 40) * (if (s % 4 == 2) 0.03f else 0.016f) }
                2 -> {
                    for (k in floatArrayOf(0f, 2f)) b.add(at + k * beat, 0.4f) { t -> sin(TAU * 105 * t + 0.6f * exp(-t * 30)) * exp(-t * 9) * 0.22f }
                    for (k in floatArrayOf(0.5f, 1.5f, 1.75f, 2.5f, 3.5f)) b.add(at + k * beat, 0.2f) { t -> sin(TAU * 245 * t + 0.4f * exp(-t * 40)) * exp(-t * 18) * (if (k % 1f == 0.75f) 0.08f else 0.13f) }
                }
                3 -> {
                    for (k in intArrayOf(1, 3)) b.add(at + k * beat, 0.2f) { t -> (sin(TAU * 545 * t) + 0.6f * sin(TAU * 815 * t)) * exp(-t * 16) * 0.05f }
                    for (k in floatArrayOf(0.75f, 2.75f)) wet.add(at + k * beat, 0.08f) { t -> sin(TAU * 1100 * t) * exp(-t * 50) * 0.08f }
                }
                4 -> for (k in floatArrayOf(1f, 3f, 3.5f)) b.add(at + k * beat, 0.18f) { t -> (hat[(t * RATE).toInt() + 7000] * 0.6f + sin(TAU * 6200 * t) * 0.3f) * exp(-t * 22) * (if (k == 3.5f) 0.03f else 0.05f) }
            }
            // The extra layer.
            when (fe.extra) {
                1 -> if (bi >= 8 && !drop) for (e in 0 until 8) {
                    val f = hz(note(deg + intArrayOf(0, 2, 4, 7, 4, 2, 4, 7)[e], leadBase))
                    wet.add(at + e * beat / 2, beat * 0.5f) { t -> lead(other, f, t) * rel(t, beat * 0.5f) * 0.03f }
                }
                2 -> if (!drop) for (n in chord) { val f = hz(n - 12); b.add(at, bar) { t -> pad(f, t, bar) * 0.03f } }
                3 -> if (bi % 2 == 1 && !drop) for (n in chord) { val f = hz(n); wet.add(at + 3.5f * beat, 0.3f) { t -> lead(Lead.HORN, f, t) * rel(t, 0.3f) * 0.045f } }
                4 -> if (bi % 8 == 0) wet.add(at, bar) { t -> sin(TAU * (700f * t + 260f * (1 - kotlin.math.cos(TAU * 1.5f * t)) / (TAU * 1.5f))) * attack(t, 0.1f) * rel(t, bar) * 0.04f }
                    else if (bi % 4 == 2) wet.add(at + 3.5f * beat, 0.12f) { t -> rim(t) * 0.22f }
            }
            // The lead.
            for (n in melody.bar(bi, drop)) {
                val l = n.len * beat * 0.95f
                for ((h, v) in if (fe.plan == 3) listOf(0 to 0.1f, 2 to 0.05f) else listOf(0 to 0.1f)) {
                    val f = hz(note(n.deg + h, leadBase))
                    wet.add(n.at * beat, l) { t -> lead(tune.lead, f, t) * rel(t, l) * v }
                }
            }
        }
        // The echo, carried round the loop so the tail wraps without a gap: a dotted eighth, or an eighth for busy leads.
        val d = (beat * (if (fe.rhythm == 1) 0.5f else 0.75f) * RATE).toInt()
        val n = wet.s.size
        val echo = wet.s.copyOf()
        val fb = if (fe.extra == 4) (tune.dub + 0.1f).coerceAtMost(0.8f) else tune.dub
        repeat(2) { for (i in 0 until n) echo[i] = wet.s[i] + fb * echo[(i - d + n) % n] }
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
     * Lo-fi hip hop: a lazy swung beat, seventh chords, a bass, a few lead notes through a short echo, and a dusty
     * record, all under a dark tape filter. The [Feel] picks:
     * - drums: 0 boom bap, 1 half time, 2 jazzy ride and cross-stick, 3 loose and dragged;
     * - chords (chop): 0 electric piano, 1 nylon guitar picking, 2 felt piano, 3 vibraphone, 4 a pad with piano;
     * - bass: 0 round sine, 1 walking upright, 2 long sub;
     * - hats: 0 eighths, 1 sixteenths, 2 none, 3 eighths with an open hat;
     * - percussion: 0 none, 1 shaker, 2 rain, 3 an old crackly record, 4 finger snaps;
     * - extra: 0 none, 1 tape wobble, 2 a pad under it all, 3 a bell counter-melody, 4 ninth chords.
     */
    private fun lofi(tune: Tune, fe: Feel): FloatArray {
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
        val swing = beat * (0.12f + 0.04f * fe.rhythm)
        fun eighth(e: Int) = e * beat / 2 + if (e % 2 == 1) swing else 0f
        fun rel(t: Float, len: Float) = if (t > len - 0.06f) ((len - t) / 0.06f).coerceAtLeast(0f) else 1f
        val hat = noise(tune.seed + 3, RATE).let { n -> FloatArray(n.size) { i -> if (i == 0) 0f else n[i] - n[i - 1] } }
        val snare = noise(tune.seed + 4, RATE).also { lowpass(it, 4000f) }
        val kicks = listOf(floatArrayOf(0f, 2.5f), floatArrayOf(0f, 1.75f, 2.5f), floatArrayOf(0f, 0.75f, 2.5f, 3.25f))
        val kickA = kicks[r.nextInt(2)]; val kickB = kicks[2]
        val melody = Melodist.line(tune, fe, rich = true)
        val drag = if (fe.drums == 3) 0.035f else 0.012f
        fun snareHit(t: Float) = (snare[(t * RATE).toInt()] * exp(-t * 20) * 0.45f + sin(TAU * 185 * t) * exp(-t * 30) * 0.3f) * 0.6f

        for (bi in 0 until bars) {
            val at = bi * bar
            val deg = tune.prog[(bi / tune.bars) % tune.prog.size]
            val chord = IntArray(if (fe.extra == 4) 5 else 4) { note(deg + it * 2, chordBase) }
            // Chords.
            when (fe.chop) {
                1 -> for (e in 0 until 8) {
                    val f = hz(chord[intArrayOf(0, 1, 2, 3, 2, 1, 2, 3)[e] % chord.size])
                    b.add(at + eighth(e), beat * 0.9f) { t -> nylon(f, t) * 0.09f * rel(t, beat * 0.9f) }
                }
                2 -> for ((pos, len) in listOf(0f to beat * 1.9f, 1.5f to beat * 2.3f)) for ((j, n) in chord.withIndex()) {
                    val f = hz(n)
                    b.add(at + pos * beat + (if (pos % 1f != 0f) swing else 0f) + j * 0.01f, len) { t -> piano(f, t) * (if (pos == 0f) 0.07f else 0.045f) * rel(t, len) }
                }
                3 -> for (pos in floatArrayOf(0f, 2f)) for ((j, n) in chord.withIndex()) {
                    val f = hz(n)
                    b.add(at + pos * beat + j * 0.02f, beat * 1.9f) { t -> vibes(f, t) * 0.06f * rel(t, beat * 1.9f) }
                }
                4 -> {
                    for (n in chord) { val f = hz(n); b.add(at, bar) { t -> pad(f, t, bar) * 0.045f } }
                    val f = hz(chord.last() + 12); b.add(at + 2.5f * beat + swing, beat) { t -> piano(f, t) * 0.06f * rel(t, beat) }
                }
                else -> for ((pos, len) in listOf(0f to beat * 2.3f, 2.5f to beat * 1.4f)) for ((j, n) in chord.withIndex()) {
                    val f = hz(n)
                    val v = if (pos == 0f) 0.07f else 0.05f
                    b.add(at + pos * beat + (if (pos > 0) swing else 0f) + j * 0.014f, len) { t -> keys(f, t) * v * rel(t, len) }
                }
            }
            if (fe.extra == 2 && fe.chop != 4) for (n in chord) { val f = hz(n - 12); b.add(at, bar) { t -> pad(f, t, bar) * 0.025f } }
            // Bass.
            val line = when (fe.bass % 3) {
                1 -> listOf(Triple(0f, 1f, 0), Triple(1f, 1f, 2), Triple(2f, 1f, 4), Triple(3f, 1f, if (bi % 2 == 0) 5 else -1))
                2 -> listOf(Triple(0f, 3f, 0), Triple(3.5f, 0.5f, 4))
                else -> listOf(Triple(0f, 1.6f, 0), Triple(2.5f, 0.5f, 0), Triple(3f, 1f, if (bi % 2 == 0) 4 else 2))
            }
            for ((pos, len, step) in line) {
                val f = hz(note(deg + step, bassBase))
                val l = len * beat * 0.95f
                val off = if (pos % 1f != 0f) swing else 0f
                if (fe.bass % 3 == 1) b.add(at + pos * beat + off, l) { t -> (sin(TAU * f * t) + 0.3f * sin(TAU * 2 * f * t) * exp(-t * 6)) * exp(-t * 3f) * attack(t, 0.006f) * rel(t, l) * 0.5f }
                else b.add(at + pos * beat + off, l) { t -> (sin(TAU * f * t) + 0.15f * sin(TAU * 2 * f * t)) * exp(-t * (if (fe.bass % 3 == 2) 0.4f else 1.1f)) * attack(t, 0.01f) * rel(t, l) * 0.4f }
            }
            // Drums.
            when (fe.drums) {
                1 -> {
                    for (k in floatArrayOf(0f, 2.75f)) b.add(at + k * beat, 0.4f) { t -> sin(TAU * (70 - t * 60) * t) * exp(-t * 9) * 0.8f }
                    b.add(at + 2 * beat + drag, 0.35f) { t -> snareHit(t) * 1.3f }
                }
                2 -> {
                    for ((i, k) in floatArrayOf(0f, 1f, 1.67f, 2f, 3f, 3.67f).withIndex()) b.add(at + k * beat, 0.6f) { t ->
                        (hat[(t * RATE).toInt()] * 0.4f + sin(TAU * 3200 * t) * 0.25f + sin(TAU * 4710 * t) * 0.15f) * exp(-t * 7) * (if (i % 3 == 2) 0.035f else 0.05f)
                    }
                    for (k in intArrayOf(1, 3)) b.add(at + k * beat, 0.1f) { t -> (sin(TAU * 1500 * t) + snare[(t * RATE).toInt()]) * exp(-t * 60) * 0.12f }
                    for (k in floatArrayOf(0f, 2.5f)) b.add(at + k * beat + (if (k % 1f != 0f) swing else 0f), 0.35f) { t -> sin(TAU * (70 - t * 60) * t) * exp(-t * 11) * 0.5f }
                }
                else -> {
                    val kick = if (fe.drums == 3) floatArrayOf(0f, 0.75f, 2.25f, 2.5f) else if (bi % 4 == 3) kickB else kickA
                    for (k in kick) b.add(at + k * beat + (if (k % 1f != 0f) swing / 2 else 0f), 0.35f) { t -> sin(TAU * (75 - t * 70) * t) * exp(-t * 11) * 0.75f }
                    for (k in intArrayOf(1, 3)) b.add(at + k * beat + drag, 0.25f) { t -> snareHit(t) }
                }
            }
            if (fe.drums != 2) when (fe.hats) {
                1 -> for (s in 0 until 16) b.add(at + s * beat / 4 + (if (s % 2 == 1) swing / 2 else 0f), 0.04f) { t -> hat[(t * RATE).toInt()] * exp(-t * 100) * (if (s % 2 == 0) 0.032f else 0.018f) }
                2 -> {}
                else -> for (e in 0 until 8) {
                    val open = fe.hats == 3 && e == 7
                    val jit = if (fe.drums == 3) r.nextFloat() * 0.02f else 0f
                    b.add(at + eighth(e) + jit, if (open) 0.25f else 0.05f) { t -> hat[(t * RATE).toInt()] * exp(-t * (if (open) 12f else 90f)) * (if (e % 2 == 1) 0.025f else 0.04f) }
                }
            }
            when (fe.perc) {
                1 -> for (e in 0 until 8) b.add(at + eighth(e), 0.09f) { t -> hat[(t * RATE).toInt() + 4000] * attack(t, 0.015f) * exp(-t * 35) * 0.02f }
                4 -> for (k in intArrayOf(1, 3)) b.add(at + k * beat + 0.02f, 0.08f) { t -> (snare[(t * RATE).toInt() + 5000] * 0.6f + sin(TAU * 2100 * t) * 0.4f) * exp(-t * 55) * 0.12f }
            }
            if (fe.extra == 3 && bi >= 8) for ((e, s) in intArrayOf(4, 2, 0, 2).withIndex()) {
                val f = hz(note(deg + s + 7, chordBase))
                wet.add(at + e * beat + 0.5f * beat + swing, beat) { t -> bell(f, t, 3f) * rel(t, beat) * 0.035f }
            }
            for (n in melody.bar(bi, false)) {
                val l = n.len * beat * 0.9f
                val off = if (n.at % 1f == 0.5f) swing else 0f
                for ((h, v) in if (fe.plan == 3) listOf(0 to 0.085f, 2 to 0.04f) else listOf(0 to 0.085f)) {
                    val f = hz(note(n.deg + h, leadBase))
                    wet.add(n.at * beat + off, l) { t -> lead(tune.lead, f, t) * rel(t, l) * v }
                }
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
        lowpass(b.s, if (fe.perc == 3) 1900f else 2300f)
        // Tape wobble: read the loop back a little early and late, a whole number of slow wobbles round it.
        if (fe.extra == 1) {
            val x = b.s.copyOf()
            val cyc = maxOf(1, (n / RATE.toFloat() / 2.2f).toInt())
            val depth = RATE * 0.0025f
            for (i in 0 until n) {
                val p = i + depth * sin(TAU * cyc * i / n)
                val i0 = Math.floorMod(kotlin.math.floor(p).toInt(), n); val fr = p - kotlin.math.floor(p)
                b.s[i] = x[i0] * (1 - fr) + x[(i0 + 1) % n] * fr
            }
        }
        val v = Random(tune.seed + 1)
        var lp = 0f
        for (i in b.s.indices) {
            b.s[i] += (v.nextFloat() - 0.5f) * (if (fe.perc == 3) 0.02f else 0.012f)
            if (v.nextInt(if (fe.perc == 3) 900 else 2500) == 0) b.s[i] += (v.nextFloat() - 0.5f) * 0.3f
            if (fe.perc == 2) { lp += 0.3f * (v.nextFloat() * 2 - 1 - lp); b.s[i] += lp * 0.03f; if (v.nextInt(3000) == 0) b.s[i] += (v.nextFloat() - 0.5f) * 0.12f }
        }
        val fade = RATE / 100
        for (i in 0 until fade) { val k = i / fade.toFloat(); b.s[i] *= k; b.s[b.s.size - 1 - i] *= k }
        return norm(b.s, 0.7f)
    }

    /** A felt piano: soft hammer, quick fade. */
    private fun piano(f: Float, t: Float) = (sin(TAU * f * t) + 0.4f * sin(TAU * 2 * f * t) * exp(-t * 2) + 0.15f * sin(TAU * 3 * f * t) * exp(-t * 4)) * exp(-t * 1.8f) * attack(t, 0.003f)
    /** A nylon-string guitar pluck. */
    private fun nylon(f: Float, t: Float) = (sin(TAU * f * t) + 0.4f * sin(TAU * 2 * f * t) * exp(-t * 5) + 0.15f * sin(TAU * 3 * f * t) * exp(-t * 8)) * exp(-t * 3.5f) * attack(t, 0.003f)
    /** A vibraphone, its motor wobbling. */
    private fun vibes(f: Float, t: Float) = (sin(TAU * f * t) + 0.2f * sin(TAU * 4 * f * t) * exp(-t * 6)) * exp(-t * 1.2f) * (1 + 0.3f * sin(TAU * 5.5f * t)) * attack(t, 0.003f)
    /** A soft pad of two detuned voices, swelling in and out over [len] seconds. */
    private fun pad(f: Float, t: Float, len: Float) = (sin(TAU * f * t) + sin(TAU * f * 1.006f * t) + 0.3f * sin(TAU * 2 * f * t)) * 0.5f *
        (t / 0.4f).coerceAtMost(1f) * ((len - t) / 0.4f).coerceIn(0f, 1f)

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
