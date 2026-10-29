package dev.m4s4t0.kushacres.audio

import dev.m4s4t0.kushacres.audio.Synth.Buf
import dev.m4s4t0.kushacres.audio.Synth.RATE
import dev.m4s4t0.kushacres.audio.Synth.TAU
import dev.m4s4t0.kushacres.audio.Synth.attack
import dev.m4s4t0.kushacres.audio.Synth.bell
import dev.m4s4t0.kushacres.audio.Synth.hz
import dev.m4s4t0.kushacres.audio.Synth.keys
import dev.m4s4t0.kushacres.audio.Synth.lead
import dev.m4s4t0.kushacres.audio.Synth.lowpass
import dev.m4s4t0.kushacres.audio.Synth.noise
import dev.m4s4t0.kushacres.audio.Synth.norm
import dev.m4s4t0.kushacres.audio.Synth.organ
import dev.m4s4t0.kushacres.audio.Synth.pad
import dev.m4s4t0.kushacres.audio.Synth.piano
import dev.m4s4t0.kushacres.audio.Synth.soft
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The electronic band: house, italo disco, synth-pop, darkwave, jungle, jazz fusion and chiptune, after the genre
 * recipes of the bountiful-music project (its drum bars, bass styles and arpeggios). A loop is 16 bars like the
 * reggae ones, and the [Feel] varies it within its genre:
 * - drums: four takes on the genre's beat (for jungle, the order of its two break bars);
 * - chop: the chords as 0 stabs, 1 a held pad, 2 electric piano, 3 an arpeggio, 4 a pad with an arpeggio;
 * - bass: one of the genre's bass styles (octaves, offbeats, rolling sixteenths, eighths, a sub, walking, a riff);
 * - hats: four hat patterns; perc: none or one of the genre's percussion (congas, timbales, shaker, cowbell, ...);
 * - extra: 0 none, 1 a counter-arpeggio in the second half, 2 a breakdown with a riser, 3 horn stabs, 4 crashes;
 * - plan and rhythm: how the lead is laid out and which rhythms it uses, as for every tune.
 */
internal object Band {
    private enum class Bass { OCTAVE, OFFBEAT, ROLL, EIGHTS, SUB, WALK, RIFF }

    private val BASSES = mapOf(
        Style.HOUSE to listOf(Bass.OFFBEAT, Bass.RIFF, Bass.ROLL),
        Style.ITALO to listOf(Bass.OCTAVE, Bass.ROLL),
        Style.SYNTHPOP to listOf(Bass.OCTAVE, Bass.EIGHTS, Bass.ROLL),
        Style.DARKWAVE to listOf(Bass.EIGHTS, Bass.OCTAVE, Bass.SUB),
        Style.JUNGLE to listOf(Bass.SUB, Bass.RIFF),
        Style.FUSION to listOf(Bass.WALK, Bass.RIFF, Bass.OCTAVE),
        Style.CHIP to listOf(Bass.OCTAVE, Bass.ROLL, Bass.EIGHTS),
    )

    /** Where the chord stabs fall in a bar of sixteenths. */
    private val STABS = mapOf(
        Style.HOUSE to "..x..x....x..x..", Style.ITALO to "..x...x...x...x.", Style.SYNTHPOP to "x..x..x...x..x..",
        Style.DARKWAVE to "x.......x.......", Style.JUNGLE to "x..........x....", Style.FUSION to "x..x..x...x..x.x", Style.CHIP to "x.x..x.x..x.x...",
    )

    private fun tri(ph: Float) = 4 * abs(ph - kotlin.math.floor(ph) - 0.5f) - 1
    private fun pulse(ph: Float, duty: Float) = if (ph - kotlin.math.floor(ph) < duty) 1f else -1f
    /** A saw of [k] harmonics, each fading [fall] times faster than the one below: a filter closing on a pluck. */
    private fun sawPluck(f: Float, t: Float, k: Int, fall: Float): Float {
        var x = 0f
        for (h in 1..minOf(k, (RATE / 2.2f / f).toInt())) x += sin(TAU * h * f * t) / h * exp(-t * fall * h)
        return x
    }

    fun play(tune: Tune, fe: Feel): FloatArray {
        val g = tune.style
        val beat = 60f / tune.bpm
        val bar = beat * 4
        val bars = 16
        val step = beat / 4
        val drums = Buf(bar * bars)
        val music = Buf(bar * bars)
        val wet = Buf(bar * bars)
        val sc = if (tune.minor) intArrayOf(0, 2, 3, 5, 7, 8, 10) else intArrayOf(0, 2, 4, 5, 7, 9, 11)
        fun note(deg: Int, base: Int) = base + 12 * Math.floorDiv(deg, 7) + sc[Math.floorMod(deg, 7)]
        var bassBase = tune.root - 24; while (bassBase < 33) bassBase += 12
        if (g == Style.CHIP) bassBase += 12
        var chordBase = tune.root; while (chordBase < 55) chordBase += 12
        val leadBase = chordBase + 12
        val hat = noise(tune.seed + 3, RATE).let { n -> FloatArray(n.size) { i -> if (i == 0) 0f else n[i] - n[i - 1] } }
        val snr = noise(tune.seed + 5, RATE).also { lowpass(it, 6000f) }
        fun nz(a: FloatArray, t: Float, off: Int = 0) = a[((t * RATE).toInt() + off) % a.size]
        // Chiptune noise: held for a few samples at a time, like an old sound chip's.
        fun chipNz(t: Float, off: Int = 0) = hat[(((t * RATE).toInt() / 6) * 6 + off) % hat.size] * 0.7f
        fun rel(t: Float, len: Float) = if (t > len - 0.03f) ((len - t) / 0.03f).coerceAtLeast(0f) else 1f
        val rich = g == Style.HOUSE || g == Style.FUSION || g == Style.JUNGLE
        val swing = if (g == Style.HOUSE || g == Style.FUSION) step * 0.16f else 0f
        fun st(s: Float) = s * step + if (s.toInt() % 2 == 1 && s % 1f == 0f) swing else 0f
        val melody = Melodist.line(tune, fe, rich = false)
        val r = Random(tune.seed * 13 + 5)

        // The drum voices.
        fun kick(t: Float) = if (g == Style.CHIP) pulse(45f * t + 120f / 25f * (1 - exp(-t * 25)), 0.5f) * exp(-t * 14) * 0.5f
            else sin(TAU * (46f * t + (if (g == Style.ITALO || g == Style.SYNTHPOP) 110f else 150f) / 30f * (1 - exp(-t * 30)))) * exp(-t * 6.5f) * attack(t, 0.002f)
        fun snare(t: Float) = if (g == Style.CHIP) chipNz(t) * exp(-t * 16) * 0.8f
            else nz(snr, t) * exp(-t * (if (g == Style.JUNGLE) 22f else 15f)) * 0.7f + sin(TAU * 190 * t) * exp(-t * 24) * 0.45f
        fun clap(t: Float): Float {
            var e = exp(-t * 16) * 0.6f
            for (k in 0 until 3) { val o = k * 0.011f; if (t >= o) e += exp(-(t - o) * 110) }
            return nz(hat, t, 9000) * e * 0.5f
        }
        fun hatC(t: Float) = (if (g == Style.CHIP) chipNz(t, 3000) else nz(hat, t)) * exp(-t * 85)
        fun hatO(t: Float) = (if (g == Style.CHIP) chipNz(t, 3000) else nz(hat, t)) * exp(-t * 11)
        fun ride(t: Float) = (nz(hat, t, 2000) * 0.35f + sin(TAU * 3150 * t) * 0.2f + sin(TAU * 4730 * t) * 0.15f) * exp(-t * 7)
        fun conga(f: Float, t: Float) = sin(TAU * f * t + 0.6f * exp(-t * 40)) * exp(-t * 13)
        fun timbale(t: Float) = (sin(TAU * 620 * t) + 0.6f * sin(TAU * 915 * t)) * exp(-t * 11) * 0.5f + nz(snr, t, 4000) * exp(-t * 40) * 0.3f
        fun shaker(t: Float) = nz(hat, t, 5000) * attack(t, 0.012f) * exp(-t * 40)
        fun cowbell(t: Float) = (soft(540 * t) + soft(800 * t)) * exp(-t * 13) * 0.4f
        fun syndrum(t: Float) = sin(TAU * (80f * t + 240f / 11f * (1 - exp(-t * 11)))) * exp(-t * 5)
        fun tom(f: Float, t: Float) = sin(TAU * (f * t + f * 0.6f / 10f * (1 - exp(-t * 10)))) * exp(-t * 8)
        fun crash(t: Float) = (nz(hat, t, 7000) * 0.6f + nz(snr, t, 7000) * 0.2f) * exp(-t * 2.6f)

        // The drum programme: strings of sixteenths (thirty-seconds for jungle, a bar holding two breakbeat bars),
        // 'x' a hit and 'o' a ghost.
        val dsteps = if (g == Style.JUNGLE) 32 else 16
        val dstep = bar / dsteps
        class Hit(val s: Int, val v: Char, val vol: Float)
        fun hits(bi: Int): List<Hit> {
            val out = ArrayList<Hit>()
            fun row(p: String, v: Char, vol: Float = 1f, off: Int = 0) {
                for ((i, c) in p.withIndex()) if (c == 'x') out.add(Hit(off + i, v, vol)) else if (c == 'o') out.add(Hit(off + i, v, vol * 0.35f))
            }
            val odd = bi % 2 == 1
            val turn = bi % 4 == 3
            when (g) {
                Style.HOUSE, Style.ITALO -> {
                    row(if (fe.drums == 1 && turn) "x...x...x...x.x." else "x...x...x...x...", 'k')
                    row("....x.......x...", 'c')
                    if (g == Style.ITALO || fe.drums == 2) row("....x.......x...", 's', 0.6f)
                    if (fe.drums == 3 && bi == 15) row("........xxxxxxxx", 's', 0.5f)
                    when (fe.hats) {
                        0 -> row("..x...x...x...x.", 'o')
                        1 -> { row("xoxoxoxoxoxoxoxo", 'h'); row("..x...x...x...x.", 'o', 0.6f) }
                        2 -> { row("x.x.x.x.x.x.x.x.", 'h'); row("..x...x...x...x.", 'o') }
                        else -> { row("..x...x...x...x.", 'o'); row("xxxxxxxxxxxxxxxx", 'z', 0.7f) }
                    }
                }
                Style.SYNTHPOP, Style.DARKWAVE, Style.CHIP -> {
                    row(when (fe.drums) { 1 -> "x.....x.x.x....."; 2 -> "x...x...x...x..."; 3 -> "x.......x.x...x."; else -> if (g == Style.DARKWAVE) "x.......x......." else "x.......x.x....." }, 'k')
                    row("....x.......x...", 's')
                    if (g != Style.CHIP && fe.drums % 2 == 1) row("....x.......x...", 'c', 0.6f)
                    if (turn && fe.drums != 2) row("..............oo", 's')
                    when (fe.hats) {
                        0 -> { row("x.x.x.x.x.x.x...", 'h'); row("..............x.", 'o') }
                        1 -> row("xoxoxoxoxoxoxoxo", 'h')
                        2 -> row("..x...x...x...x.", 'h')
                        else -> { row("x.x...x.x.x...x.", 'h'); row("......x.......x.", 'o') }
                    }
                }
                Style.JUNGLE -> {
                    val a = Triple("x.x.......x.....", "....x.......x...", ".......o.o.....o")
                    val b = Triple("..x.......x.....", "....x.......x..x", "......o....o..o.")
                    val roll = Triple("x.........x.....", "....x...x.x.xxxx", "..o...o.........")
                    val (one, two) = when (fe.drums) { 1 -> a to a; 2 -> a to (if (turn) roll else b); 3 -> b to a; else -> a to b }
                    for ((half, p) in listOf(one, two).withIndex()) {
                        row(p.first, 'k', off = half * 16); row(p.second, 's', off = half * 16); row(p.third, 's', off = half * 16)
                        when (fe.hats) {
                            1 -> row("xoxoxoxoxoxoxoxo", 'h', 0.8f, half * 16)
                            3 -> { row("x.x.x.x.x.x.x...", 'r', off = half * 16); row("..............x.", 'o', off = half * 16) }
                            else -> row("x.x.x.x.x.x.x.x.", 'r', off = half * 16)
                        }
                    }
                }
                else -> { // Fusion.
                    row(when (fe.drums) { 1 -> "x.....x...x....."; 2 -> if (odd) "x..x....x.x...x." else "x.....x.x..x...."; 3 -> "x...x...x...x..."; else -> if (odd) "x..x....x.x...x." else "x.....x.x..x...." }, 'k')
                    row(if (fe.drums == 2) "........x......." else "....x.......x...", 's')
                    row(".......o.o.....o", 's')
                    when (fe.hats) {
                        0 -> row("xoxoxoxoxoxoxoxo", 'h')
                        1 -> row("x.x.x.x.x.x.x.x.", 'h')
                        2 -> row("x.x.x.x.x.x.x.x.", 'r')
                        else -> { row("xoxoxoxoxoxoxoxo", 'h'); if (odd) row("..............x.", 'o') }
                    }
                }
            }
            // Percussion, in sixteenths whatever the genre.
            val k = if (g == Style.JUNGLE) 2 else 1
            fun perc(p: String, v: Char, vol: Float = 1f) = row(p.map { "$it" + ".".repeat(k - 1) }.joinToString(""), v, vol)
            when (fe.perc) {
                1 -> if (g == Style.ITALO || g == Style.SYNTHPOP || g == Style.DARKWAVE || g == Style.CHIP) { if (odd) perc("..............x.", 'y') }
                    else perc(if (odd) "..xx..x....xx.xx" else "...x..xx..x...x.", 'g')
                2 -> perc("xoxoxoxoxoxoxoxo", 'z')
                3 -> if (g == Style.HOUSE || g == Style.FUSION || g == Style.JUNGLE) perc(".....x...x...x..", 't') else perc("....x.......x...", 'm')
                4 -> if (g == Style.CHIP) perc("..x...x...x...x.", 'b', 0.6f) else if (g == Style.HOUSE) { perc(if (odd) "..xx..x....xx.xx" else "...x..xx..x...x.", 'g'); perc(".....x...x...x..", 't', 0.7f) }
                    else perc("..x...x...x...x.", 'b')
            }
            if (fe.perc != 0 && turn && g != Style.JUNGLE) perc("............xxxx", 'T')
            return out
        }

        // The arrangement: a breakdown takes the kick and snare out for two bars, a riser leading back in.
        val breakdown = fe.extra == 2
        for (bi in 0 until bars) {
            val at = bi * bar
            val deg = tune.prog[(bi / tune.bars) % tune.prog.size]
            val nextDeg = tune.prog[((bi + 1) / tune.bars) % tune.prog.size]
            val chord = IntArray(if (rich) 4 else 3) { note(deg + it * 2, chordBase) }
            val down = breakdown && bi in 8..9

            for (h in hits(bi)) {
                if (down && h.v in "kscTy") continue
                val t0 = at + h.s * dstep + if (h.s % 2 == 1 && dsteps == 16) swing else 0f
                val v = h.vol
                when (h.v) {
                    'k' -> drums.add(t0, 0.45f) { t -> kick(t) * 0.75f * v }
                    's' -> { drums.add(t0, 0.3f) { t -> snare(t) * 0.42f * v }; if (g == Style.DARKWAVE && v > 0.5f) wet.add(t0, 0.3f) { t -> snare(t) * 0.18f } }
                    'c' -> drums.add(t0, 0.3f) { t -> clap(t) * 0.5f * v }
                    'h' -> drums.add(t0, 0.06f) { t -> hatC(t) * 0.13f * v }
                    'o' -> drums.add(t0, 0.3f) { t -> hatO(t) * 0.1f * v }
                    'r' -> drums.add(t0, 0.4f) { t -> ride(t) * 0.12f * v }
                    'z' -> drums.add(t0, 0.1f) { t -> shaker(t) * 0.07f * v }
                    'g' -> { val f = if (h.s % 3 == 0) 210f else 320f; drums.add(t0, 0.25f) { t -> conga(f, t) * 0.18f * v } }
                    't' -> drums.add(t0, 0.3f) { t -> timbale(t) * 0.16f * v }
                    'm' -> drums.add(t0, 0.15f) { t -> shaker(t) * 0.12f * v }
                    'b' -> drums.add(t0, 0.25f) { t -> cowbell(t) * 0.12f * v }
                    'y' -> drums.add(t0, 0.5f) { t -> syndrum(t) * 0.25f * v }
                    'T' -> { val f = 220f - (h.s % 4) * 30f; drums.add(t0, 0.3f) { t -> tom(f, t) * 0.25f * v } }
                }
            }
            if (fe.extra == 4 && bi % 4 == 0) drums.add(at, 0.95f) { t -> crash(t) * 0.12f }
            if (breakdown && bi == 7 || fe.extra == 4 && bi == 15) drums.add(at, bar) { t -> nz(hat, t % 0.9f, 1000) * (t / bar) * (t / bar) * 0.1f }

            // Chords.
            val stabs = STABS.getValue(g)
            fun stab(t0: Float) {
                val len = step * 1.6f
                for ((j, n) in chord.withIndex()) {
                    val f = hz(n)
                    val o = t0 + j * 0.003f
                    when (g) {
                        Style.HOUSE -> if (fe.drums % 2 == 0) music.add(o, len) { t -> piano(f, t) * 0.06f * rel(t, len) }
                            else music.add(o, len) { t -> organ(f, t) * 0.03f * attack(t, 0.005f) * rel(t, len) }
                        Style.FUSION, Style.JUNGLE -> music.add(o, len * 1.5f) { t -> keys(f, t) * 0.06f * rel(t, len * 1.5f) }
                        Style.CHIP -> music.add(o, len) { t -> pulse(f * t, 0.5f) * 0.025f * rel(t, len) }
                        else -> music.add(o, len) { t -> sawPluck(f, t, 6, 4f) * 0.045f * rel(t, len) }
                    }
                }
            }
            fun arp(voiceOf: Lead?, vol: Float, into: Buf) {
                val tones = chord.take(3).toIntArray() + (chord[0] + 12)
                for (s in 0 until 16) {
                    val f = hz(tones[intArrayOf(0, 1, 2, 3, 3, 2, 1, 0)[s % 8].coerceAtMost(tones.size - 1)] + if (s >= 8 && fe.bass % 2 == 1) 12 else 0)
                    val len = step * 0.9f
                    into.add(at + st(s.toFloat()), len) { t ->
                        (when {
                            voiceOf != null -> lead(voiceOf, f, t)
                            g == Style.CHIP -> pulse(f * t, 0.25f) * 0.6f
                            g == Style.ITALO || g == Style.SYNTHPOP -> sawPluck(f, t, 5, 9f)
                            g == Style.DARKWAVE || g == Style.JUNGLE -> bell(f, t, 6f)
                            else -> soft(f * t) * exp(-t * 12)
                        }) * vol * rel(t, len)
                    }
                }
            }
            if (!down || fe.chop == 1 || fe.chop == 4) when (fe.chop) {
                0 -> for ((i, c) in stabs.withIndex()) if (c == 'x') stab(at + st(i.toFloat()))
                1, 4 -> {
                    for (n in chord) { val f = hz(n); music.add(at, bar) { t -> (if (g == Style.CHIP) pulse(f * t, 0.125f) * 0.25f * rel(t, bar) else pad(f, t, bar)) * 0.035f } }
                    if (fe.chop == 4 && !down) arp(null, 0.035f, wet)
                }
                2 -> for ((k, len) in listOf(0f to beat * 1.9f, 2.5f to beat * 1.4f)) for ((j, n) in chord.withIndex()) {
                    val f = hz(n)
                    music.add(at + k * beat + j * 0.005f, len) { t -> (if (g == Style.CHIP) pulse(f * t, 0.5f) * 0.4f * exp(-t * 3) else keys(f, t)) * 0.065f * rel(t, len) }
                }
                else -> arp(null, 0.05f, wet)
            }

            // Bass: (beat, length in beats, MIDI note).
            val root = note(deg, bassBase)
            val line: List<Triple<Float, Float, Int>> = when (BASSES.getValue(g).let { it[fe.bass % it.size] }) {
                Bass.OCTAVE -> List(8) { e -> Triple(e * 0.5f, 0.42f, root + if (e % 2 == 1) 12 else 0) }
                Bass.OFFBEAT -> List(4) { e -> Triple(e + 0.5f, 0.4f, if (e == 3 && bi % 2 == 1) note(deg + 4, bassBase) else root) }
                Bass.ROLL -> (0 until 16).filter { it % 4 != 0 }.map { s -> Triple(s * 0.25f, 0.2f, if (s % 4 == 3 && s > 8) root + 12 else root) }
                Bass.EIGHTS -> List(8) { e -> Triple(e * 0.5f, 0.45f, if (bi % 2 == 1 && e >= 6) note(deg + (if (e == 6) 4 else 2), bassBase) else root) }
                Bass.SUB -> listOf(Triple(0f, 2.7f, root), Triple(2.75f, 1.2f, if (bi % 2 == 1) note(deg + 4, bassBase) else root))
                Bass.WALK -> listOf(Triple(0f, 0.95f, root), Triple(1f, 0.95f, note(deg + 2, bassBase)), Triple(2f, 0.95f, note(deg + 4, bassBase)),
                    Triple(3f, 0.95f, note(nextDeg, bassBase) + if (r.nextBoolean()) 1 else -1))
                Bass.RIFF -> Synth.RIFFS[fe.bass].map { (pos, len, s) -> Triple(pos, len, note(deg + s, bassBase)) }
            }
            if (!down || g == Style.JUNGLE) for ((pos, len, m) in line) {
                val f = hz(m)
                val l = len * beat
                music.add(at + st(pos * 4), l) { t ->
                    when (g) {
                        Style.HOUSE -> (sin(TAU * f * t) + 0.5f * sin(TAU * 2 * f * t) * exp(-t * 8)) * exp(-t * 2f) * 0.42f
                        Style.ITALO, Style.SYNTHPOP, Style.DARKWAVE -> sawPluck(f, t, 6, if (g == Style.DARKWAVE) 5f else 3f) * 0.34f
                        Style.JUNGLE -> (sin(TAU * f * t) + 0.12f * sin(TAU * 2 * f * t)) * 0.5f
                        Style.CHIP -> tri(f * t) * 0.32f
                        else -> (sin(TAU * f * t) + 0.6f * sin(TAU * 2 * f * t) * exp(-t * 6) + 0.3f * sin(TAU * 3 * f * t) * exp(-t * 12)) * exp(-t * 1.5f) * 0.4f
                    } * attack(t, 0.006f) * rel(t, l)
                }
            }

            // The extra layer.
            when (fe.extra) {
                1 -> if (bi >= 8) arp(if (g == Style.CHIP) Lead.PULSE else Lead.BELL, 0.025f, wet)
                3 -> if (bi % 2 == 1 && !down) for (n in chord.take(3)) { val f = hz(n); wet.add(at + st(14f), 0.25f) { t -> lead(Lead.HORN, f, t) * rel(t, 0.25f) * 0.04f } }
            }

            // The lead.
            for (n in melody.bar(bi, false)) {
                val l = n.len * beat * 0.92f
                val off = if (n.at % 0.5f == 0.25f) swing else 0f
                for ((h, v) in if (fe.plan == 3) listOf(0 to 0.09f, 2 to 0.045f) else listOf(0 to 0.09f)) {
                    val f = hz(note(n.deg + h, leadBase))
                    wet.add(n.at * beat + off, l) { t -> lead(tune.lead, f, t) * rel(t, l) * v }
                }
            }
        }

        // Pump the music under the kick, as house and italo records do.
        val pump = when (g) { Style.HOUSE -> 0.5f; Style.ITALO -> 0.35f; Style.SYNTHPOP -> 0.2f; else -> 0f }
        val n = music.s.size
        val per = (beat * RATE).toInt()
        if (pump > 0f) for (i in 0 until n) { val k = 1 - pump * exp(-(i % per) / (RATE * 0.09f)); music.s[i] *= k; wet.s[i] *= k }
        // The echo, carried round the loop so the tail wraps without a gap.
        val d = (beat * 0.75f * RATE).toInt()
        val fb = when (g) { Style.DARKWAVE -> tune.dub.coerceAtLeast(0.55f); Style.JUNGLE -> 0.5f; Style.CHIP -> 0.15f; else -> 0.3f }
        val echo = wet.s.copyOf()
        repeat(2) { for (i in 0 until n) echo[i] = wet.s[i] + fb * echo[(i - d + n) % n] }
        for (i in 0 until n) echo[i] -= wet.s[i]
        lowpass(echo, if (g == Style.CHIP) 5000f else 2200f)
        val out = drums.s
        for (i in 0 until n) out[i] += music.s[i] + wet.s[i] + echo[i]
        lowpass(out, when (g) { Style.CHIP -> 9000f; Style.DARKWAVE -> 4500f; Style.JUNGLE -> 5000f; else -> 6500f })
        val fade = RATE / 100
        for (i in 0 until fade) { val k = i / fade.toFloat(); out[i] *= k; out[n - 1 - i] *= k }
        return norm(out, 0.7f)
    }
}
