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
     * A lazy lo-fi loop: 16 bars at 80 bpm (48 s) of electric piano chords, a round bass, swung brushed drums,
     * a sleepy pentatonic tune and vinyl crackle.
     */
    fun music(seed: Int = 7): FloatArray {
        val beat = 60f / 80f
        val bar = beat * 4
        val bars = 16
        val b = Buf(bar * bars)
        val r = Random(seed)
        // Fmaj7, Em7, Dm7, Cmaj7 / Bbmaj7, Am7, Gm7, C7 — as MIDI notes.
        val chords = arrayOf(
            intArrayOf(53, 57, 60, 64), intArrayOf(52, 55, 59, 62), intArrayOf(50, 53, 57, 60), intArrayOf(48, 52, 55, 59),
            intArrayOf(46, 50, 53, 57), intArrayOf(45, 48, 52, 55), intArrayOf(43, 46, 50, 53), intArrayOf(48, 52, 55, 58),
        )
        fun keys(f: Float, t: Float) = (sin(TAU * f * t + 0.6f * sin(TAU * f * 2 * t) * exp(-t * 3)) + 0.15f * sin(TAU * f * 3 * t)) *
            exp(-t * 0.9f) * attack(t, 0.01f)
        val kickN = noise(11, RATE); val hatN = noise(12, RATE)
        for (bi in 0 until bars) {
            val at = bi * bar
            val ch = chords[bi % chords.size]
            // Chords on 1, a ghost stab on the "and" of 2.
            for ((k, n) in ch.withIndex()) {
                val f = hz(n + 12)
                b.add(at + k * 0.012f, bar) { t -> keys(f, t) * 0.11f }
                b.add(at + beat * 2.5f, beat) { t -> keys(f, t) * 0.05f * exp(-t * 4) }
            }
            // Bass: root on 1, fifth on the "and" of 3.
            val root = hz(ch[0] - 12)
            b.add(at, beat * 2.4f) { t -> sin(TAU * root * t) * exp(-t * 1.4f) * attack(t, 0.01f) * 0.32f }
            b.add(at + beat * 2.5f, beat * 1.3f) { t -> sin(TAU * root * 1.5f * t) * exp(-t * 2.5f) * attack(t, 0.01f) * 0.22f }
            // Drums, swung eighths.
            for (e in 0 until 8) {
                val swing = if (e % 2 == 1) beat * 0.16f else 0f
                val te = at + e * beat / 2 + swing
                if (e == 0 || e == 5 || (e == 3 && bi % 2 == 1)) b.add(te, 0.25f) { t -> sin(TAU * (95 - t * 120) * t) * exp(-t * 14) * 0.5f }
                if (e == 2 || e == 6) b.add(te, 0.18f) { t -> kickN[(t * RATE).toInt()] * exp(-t * 22) * 0.16f }
                b.add(te, 0.05f) { t -> hatN[(t * RATE).toInt()] * exp(-t * 90) * (if (e % 2 == 0) 0.07f else 0.045f) }
            }
            // A tune over the second half of the loop: notes from F major pentatonic, mostly stepwise.
            if (bi >= 4) {
                val scale = intArrayOf(65, 67, 69, 72, 74, 77, 79)
                var idx = r.nextInt(scale.size)
                var tb = 0f
                while (tb < 4f) {
                    val len = if (r.nextFloat() < 0.6f) 0.5f else 1f
                    if (r.nextFloat() < 0.72f) {
                        val f = hz(scale[idx] + 12)
                        b.add(at + tb * beat, len * beat * 1.5f) { t -> (sin(TAU * f * t) + 0.3f * sin(TAU * f * 2 * t)) * exp(-t * 2.2f) * attack(t, 0.008f) * 0.07f }
                    }
                    idx = (idx + r.nextInt(-2, 3)).coerceIn(0, scale.size - 1)
                    tb += len
                }
            }
        }
        lowpass(b.s, 2600f)
        // Vinyl: soft hiss and the odd crackle.
        val v = Random(seed + 1)
        for (i in b.s.indices) {
            b.s[i] += (v.nextFloat() - 0.5f) * 0.006f
            if (v.nextInt(9000) == 0) b.s[i] += (v.nextFloat() - 0.5f) * 0.25f
        }
        // Fade the very ends a touch so the loop point never clicks.
        val fade = RATE / 100
        for (i in 0 until fade) { val k = i / fade.toFloat(); b.s[i] *= k; b.s[b.s.size - 1 - i] *= k }
        return norm(b.s, 0.7f)
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
