package dev.m4s4t0.kushacres.audio

/** The voice that carries a tune's melody. */
enum class Lead { MELODICA, STEEL, BELL, SYNTH, HORN, FLUTE, ORGAN, GUITAR }

/**
 * Everything that sets one reggae loop apart: [root] is the tonic as a MIDI note, [prog] the chords as scale steps
 * (0 = I), each held for [bars] bars. [dub] is how much the echo feeds back; [bubble] adds organ on the downbeats too.
 */
class Tune(
    val root: Int, val bpm: Int, val minor: Boolean, val prog: IntArray, val lead: Lead, val seed: Int,
    val bars: Int = 1, val dub: Float = 0.4f, val bubble: Boolean = false,
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
        "timesquare" to Tune(57, 80, false, intArrayOf(0, 4, 5, 3), Lead.HORN, 151, bubble = true),
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

    val areas: Set<String> get() = byArea.keys

    /** Any area without its own tune still gets one of its own, made from its name. */
    fun of(area: String): Tune = byArea[area] ?: area.hashCode().let { h ->
        val leads = Lead.entries
        Tune(50 + Math.floorMod(h, 11), 64 + Math.floorMod(h / 11, 16), h % 2 == 0, intArrayOf(0, 3, 4, 3), leads[Math.floorMod(h / 7, leads.size)], h)
    }
}
