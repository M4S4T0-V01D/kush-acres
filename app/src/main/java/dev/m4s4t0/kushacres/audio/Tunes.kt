package dev.m4s4t0.kushacres.audio

import kotlin.random.Random

/** The voice that carries a tune's melody. */
enum class Lead { MELODICA, STEEL, BELL, SYNTH, HORN, FLUTE, ORGAN, GUITAR }

/** How a loop is played: one-drop reggae, stripped-back echoing dub, or dusty lo-fi hip hop. */
enum class Style { REGGAE, DUB, LOFI }

/** The reggae drum pattern: kick on 3 only, kick on 1 and 3, kick on every beat, or a dancehall bounce. */
enum class Groove { ONEDROP, ROCKERS, STEPPERS, DANCEHALL }

/**
 * Everything about a loop's arrangement beyond its key and voice, each part a choice from [SIZES]: the drums, the chop
 * (or the lo-fi chord instrument), the bass line, the hats, extra percussion, how the lead is laid out, one extra
 * layer, and the lead's rhythm. See [Synth.music] for what each number plays.
 */
class Feel(val drums: Int, val chop: Int, val bass: Int, val hats: Int, val perc: Int, val plan: Int, val extra: Int, val rhythm: Int) {
    val parts get() = intArrayOf(drums, chop, bass, hats, perc, plan, extra, rhythm)

    companion object {
        val SIZES = intArrayOf(4, 5, 10, 4, 5, 4, 5, 3)
        /** Any two tunes of one style differ in at least this many parts. */
        const val APART = 4
    }
}

/**
 * Everything that sets one loop apart: [root] is the tonic as a MIDI note, [prog] the chords as scale steps
 * (0 = I), each held for [bars] bars. [dub] is how much the echo feeds back; [bubble] adds organ on the downbeats too.
 */
class Tune(
    val root: Int, val bpm: Int, val minor: Boolean, val prog: IntArray, val lead: Lead, val seed: Int,
    val bars: Int = 1, val dub: Float = 0.4f, val bubble: Boolean = false, val style: Style = Style.REGGAE,
    /** The drums, for reggae and dub; null picks one. */
    val groove: Groove? = null,
)

/** A tune for every place: the home farm, town, and each other farm. */
object Tunes {
    private val byArea = mapOf(
        "home" to Tune(55, 72, false, intArrayOf(0, 3, 0, 4), Lead.MELODICA, 7, bars = 2),
        "town" to Tune(60, 78, false, intArrayOf(0, 5, 3, 4), Lead.HORN, 21, bubble = true),
        "redwood" to Tune(50, 68, true, intArrayOf(0, 6), Lead.FLUTE, 31, bars = 2),
        "mesa" to Tune(52, 70, true, intArrayOf(0, 3), Lead.GUITAR, 41, bars = 2),
        "bayou" to Tune(57, 66, true, intArrayOf(0, 3, 4, 0), Lead.MELODICA, 51, dub = 0.55f),
        "coast" to Tune(53, 74, false, intArrayOf(0, 4, 5, 3), Lead.STEEL, 61),
        "frost" to Tune(59, 64, true, intArrayOf(0, 5, 6, 6), Lead.BELL, 71, dub = 0.55f),
        "emerald" to Tune(58, 72, false, intArrayOf(0, 3), Lead.ORGAN, 81, bars = 2, bubble = true),
        "volcano" to Tune(60, 70, true, intArrayOf(0, 5, 3, 4), Lead.HORN, 91),
        "neon" to Tune(54, 76, true, intArrayOf(0, 5), Lead.SYNTH, 101, bars = 2, dub = 0.5f),
        "fort20" to Tune(50, 72, true, intArrayOf(0, 5, 3, 4), Lead.HORN, 421),
        "highland" to Tune(50, 70, false, intArrayOf(0, 3, 4, 3), Lead.FLUTE, 111),
        "crater" to Tune(51, 64, true, intArrayOf(0, 6, 5, 6), Lead.SYNTH, 121, dub = 0.6f),
        "spooky" to Tune(50, 64, true, intArrayOf(0, 5, 3, 4), Lead.BELL, 131, dub = 0.6f),
        "northpole" to Tune(60, 76, false, intArrayOf(0, 3, 4, 0), Lead.BELL, 141, bubble = true),
        "timesquare" to Tune(57, 80, false, intArrayOf(0, 4, 5, 3), Lead.HORN, 151, bubble = true, groove = Groove.DANCEHALL),
        "lovers" to Tune(51, 68, false, intArrayOf(0, 5, 1, 4), Lead.FLUTE, 161),
        "clover" to Tune(55, 78, false, intArrayOf(0, 3, 0, 4), Lead.GUITAR, 171),
        "highhill" to Tune(57, 70, true, intArrayOf(0, 3), Lead.MELODICA, 181, bars = 2, dub = 0.5f),
        "meadow" to Tune(53, 74, false, intArrayOf(0, 3, 1, 4), Lead.STEEL, 191),
        "liberty" to Tune(50, 76, false, intArrayOf(0, 4, 3, 4), Lead.HORN, 201),
        "harvest" to Tune(52, 70, false, intArrayOf(0, 3, 5, 4), Lead.GUITAR, 211),
        "zion" to Tune(52, 66, true, intArrayOf(0, 3), Lead.MELODICA, 221, bars = 2, dub = 0.6f),
        "manor" to Tune(48, 60, true, intArrayOf(0, 1, 5, 4), Lead.ORGAN, 231, dub = 0.7f),
        "workshop" to Tune(62, 82, false, intArrayOf(0, 4, 5, 3), Lead.BELL, 241, bubble = true),
        "rooftop" to Tune(56, 84, true, intArrayOf(0, 5, 6, 4), Lead.SYNTH, 251),
        "rosegarden" to Tune(53, 66, false, intArrayOf(0, 5, 3, 4), Lead.STEEL, 261),
        "rainbowend" to Tune(57, 82, false, intArrayOf(0, 4, 0, 3), Lead.FLUTE, 271),
        "cloudnine" to Tune(49, 62, false, intArrayOf(0, 3), Lead.SYNTH, 281, bars = 2, dub = 0.7f),
        "warren" to Tune(58, 80, false, intArrayOf(0, 3, 4, 0), Lead.MELODICA, 291, bubble = true),
        "frontier" to Tune(53, 78, false, intArrayOf(0, 3, 4, 4), Lead.GUITAR, 301),
        "orchard" to Tune(55, 70, false, intArrayOf(0, 5, 3, 4), Lead.STEEL, 311),
    )

    /** Radio-only tracks, won by levelling and from the town board: (id, name, tune). */
    val extras = listOf(
        Triple("lofi1", "Rolling Papers", Tune(57, 82, true, intArrayOf(0, 3, 6, 2), Lead.BELL, 501, style = Style.LOFI)),
        Triple("dub1", "Echo Chamber", Tune(50, 66, true, intArrayOf(0, 3), Lead.MELODICA, 511, bars = 2, dub = 0.72f, style = Style.DUB)),
        Triple("lofi2", "Rainy Window", Tune(53, 76, false, intArrayOf(0, 5, 1, 4), Lead.FLUTE, 521, style = Style.LOFI)),
        Triple("dub2", "Steppers Delight", Tune(52, 72, true, intArrayOf(0, 5, 3, 4), Lead.HORN, 531, dub = 0.68f, style = Style.DUB, groove = Groove.STEPPERS)),
        Triple("lofi3", "Late Night Munchies", Tune(55, 70, false, intArrayOf(0, 3, 1, 4), Lead.STEEL, 541, style = Style.LOFI)),
        Triple("reggae1", "Sunday Skank", Tune(60, 80, false, intArrayOf(0, 3, 4, 3), Lead.ORGAN, 551, bubble = true, groove = Groove.ONEDROP)),
        Triple("dub3", "Smoke Signals", Tune(48, 62, true, intArrayOf(0, 6), Lead.SYNTH, 561, bars = 2, dub = 0.75f, style = Style.DUB)),
        Triple("lofi4", "Cloud Study", Tune(58, 84, true, intArrayOf(0, 5, 3, 6), Lead.SYNTH, 571, style = Style.LOFI)),
        Triple("lofi5", "Couch Lock", Tune(51, 68, true, intArrayOf(0, 3, 6, 4), Lead.GUITAR, 581, style = Style.LOFI)),
        Triple("dub4", "King Tubby's Garden", Tune(53, 68, false, intArrayOf(0, 4, 5, 3), Lead.MELODICA, 591, dub = 0.7f, style = Style.DUB, groove = Groove.ROCKERS)),
    )
    private val extraTunes = extras.associate { it.first to it.third }

    val areas: Set<String> get() = byArea.keys

    /**
     * Each tune's [Feel], dealt in order so that it sits at least [Feel.APART] parts away from every tune of its style
     * dealt before it (the farthest of many tries, if none is that far).
     */
    private val feels: Map<Tune, Feel> by lazy {
        val out = LinkedHashMap<Tune, Feel>()
        for (t in byArea.values + extras.map { it.third }) {
            val r = Random(t.seed * 7919 + 13)
            val same = out.filterKeys { it.style == t.style }.values
            fun gap(c: Feel) = same.minOfOrNull { o -> c.parts.indices.count { c.parts[it] != o.parts[it] } } ?: 99
            var best = deal(t, r)
            var g = gap(best)
            repeat(400) { if (g < Feel.APART + 1) { val c = deal(t, r); val cg = gap(c); if (cg > g) { best = c; g = cg } } }
            out[t] = best
        }
        out
    }

    private fun deal(t: Tune, r: Random): Feel {
        val z = Feel.SIZES.map { r.nextInt(it) }
        return Feel(if (t.style != Style.LOFI) t.groove?.ordinal ?: z[0] else z[0], z[1], z[2], z[3], z[4], z[5], z[6], z[7])
    }

    fun feel(t: Tune): Feel = feels[t] ?: deal(t, Random(t.seed * 7919 + 13))

    /** Any area without its own tune still gets one of its own, made from its name. */
    fun of(area: String): Tune = byArea[area] ?: extraTunes[area] ?: area.hashCode().let { h ->
        val leads = Lead.entries
        Tune(50 + Math.floorMod(h, 11), 64 + Math.floorMod(h / 11, 16), h % 2 == 0, intArrayOf(0, 3, 4, 3), leads[Math.floorMod(h / 7, leads.size)], h)
    }
}
