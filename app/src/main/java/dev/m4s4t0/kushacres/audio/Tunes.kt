package dev.m4s4t0.kushacres.audio

import kotlin.random.Random

/** The voice that carries a tune's melody. */
enum class Lead { MELODICA, STEEL, BELL, SYNTH, HORN, FLUTE, ORGAN, GUITAR, PULSE, SAW }

/**
 * How a loop is played: its name on the radio, its tempo range and the leads that suit it (for [Tunes.remix]), and the
 * melody rhythms it draws on ([Melodist.CELLS]; null leaves that to the feel). Reggae, dub and lo-fi have their own
 * renderers in [Synth]; the rest are played by [Band], from the recipes of the bountiful-music project.
 */
enum class Style(val label: String, val bpm: IntRange, val leads: List<Lead>, val cells: List<String>? = null) {
    REGGAE("Reggae", 68..82, listOf(Lead.MELODICA, Lead.HORN, Lead.ORGAN, Lead.GUITAR, Lead.STEEL, Lead.FLUTE)),
    DUB("Dub", 62..72, listOf(Lead.MELODICA, Lead.HORN, Lead.SYNTH, Lead.BELL, Lead.FLUTE)),
    LOFI("Lo-fi", 68..84, listOf(Lead.BELL, Lead.FLUTE, Lead.STEEL, Lead.SYNTH, Lead.GUITAR)),
    HOUSE("House", 118..126, listOf(Lead.ORGAN, Lead.STEEL, Lead.HORN, Lead.SYNTH, Lead.BELL), listOf("house", "fresh", "phonk")),
    ITALO("Italo", 114..124, listOf(Lead.SAW, Lead.SYNTH, Lead.BELL, Lead.PULSE), listOf("bouncy", "house", "fresh")),
    SYNTHPOP("Synth-pop", 104..118, listOf(Lead.SAW, Lead.BELL, Lead.SYNTH, Lead.PULSE), listOf("fresh", "bouncy", "house")),
    DARKWAVE("Darkwave", 98..116, listOf(Lead.SYNTH, Lead.ORGAN, Lead.BELL, Lead.SAW), listOf("smooth", "dub", "fresh")),
    /** Played at half its real speed: a bar holds two bars of breakbeat. */
    JUNGLE("Jungle", 82..88, listOf(Lead.FLUTE, Lead.SYNTH, Lead.BELL, Lead.HORN), listOf("smooth", "dub", "fresh")),
    FUSION("Fusion", 94..112, listOf(Lead.SAW, Lead.HORN, Lead.STEEL, Lead.FLUTE, Lead.SYNTH), listOf("latin", "phonk", "bouncy")),
    CHIP("Chiptune", 120..150, listOf(Lead.PULSE, Lead.BELL), listOf("bouncy", "phonk", "fresh")),
}

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
        "town" to Tune(60, 104, false, intArrayOf(0, 5, 1, 4), Lead.HORN, 21, style = Style.FUSION),
        "redwood" to Tune(50, 74, true, intArrayOf(0, 6), Lead.FLUTE, 31, bars = 2, style = Style.LOFI),
        "mesa" to Tune(52, 70, true, intArrayOf(0, 3), Lead.GUITAR, 41, bars = 2, dub = 0.55f, style = Style.DUB),
        "bayou" to Tune(57, 66, true, intArrayOf(0, 3, 4, 0), Lead.MELODICA, 51, dub = 0.6f, style = Style.DUB),
        "coast" to Tune(53, 122, false, intArrayOf(0, 4, 5, 3), Lead.STEEL, 61, style = Style.HOUSE),
        "frost" to Tune(59, 112, true, intArrayOf(0, 5, 3, 6), Lead.BELL, 71, style = Style.SYNTHPOP),
        "emerald" to Tune(58, 72, false, intArrayOf(0, 3), Lead.ORGAN, 81, bars = 2, bubble = true),
        "volcano" to Tune(50, 86, true, intArrayOf(0, 5, 3, 4), Lead.HORN, 91, style = Style.JUNGLE),
        "neon" to Tune(54, 120, true, intArrayOf(0, 5), Lead.SAW, 101, bars = 2, style = Style.ITALO),
        "fort20" to Tune(50, 132, true, intArrayOf(0, 5, 3, 4), Lead.PULSE, 421, style = Style.CHIP),
        "highland" to Tune(50, 100, false, intArrayOf(0, 3, 1, 4), Lead.FLUTE, 111, style = Style.FUSION),
        "crater" to Tune(51, 112, true, intArrayOf(0, 6, 5, 6), Lead.SYNTH, 121, dub = 0.6f, style = Style.DARKWAVE),
        "spooky" to Tune(50, 104, true, intArrayOf(0, 5, 6, 4), Lead.BELL, 131, dub = 0.6f, style = Style.DARKWAVE),
        "northpole" to Tune(60, 140, false, intArrayOf(0, 4, 5, 3), Lead.BELL, 141, style = Style.CHIP),
        "timesquare" to Tune(57, 124, false, intArrayOf(0, 4, 5, 3), Lead.HORN, 151, style = Style.HOUSE),
        "lovers" to Tune(51, 72, false, intArrayOf(0, 5, 1, 4), Lead.FLUTE, 161, style = Style.LOFI),
        "clover" to Tune(55, 78, false, intArrayOf(0, 3, 0, 4), Lead.GUITAR, 171),
        "highhill" to Tune(57, 70, true, intArrayOf(0, 3), Lead.MELODICA, 181, bars = 2, dub = 0.5f, style = Style.DUB),
        "meadow" to Tune(53, 96, false, intArrayOf(0, 3, 1, 4), Lead.STEEL, 191, style = Style.FUSION),
        "liberty" to Tune(50, 126, false, intArrayOf(0, 4, 3, 4), Lead.ORGAN, 201, style = Style.HOUSE),
        "harvest" to Tune(52, 70, false, intArrayOf(0, 3, 5, 4), Lead.GUITAR, 211),
        "zion" to Tune(52, 66, true, intArrayOf(0, 3), Lead.MELODICA, 221, bars = 2, dub = 0.6f, style = Style.DUB),
        "manor" to Tune(48, 100, true, intArrayOf(0, 1, 5, 4), Lead.ORGAN, 231, dub = 0.7f, style = Style.DARKWAVE),
        "workshop" to Tune(62, 116, false, intArrayOf(0, 4, 5, 3), Lead.SAW, 241, style = Style.SYNTHPOP),
        "rooftop" to Tune(56, 87, true, intArrayOf(0, 5, 6, 4), Lead.SYNTH, 251, style = Style.JUNGLE),
        "rosegarden" to Tune(53, 70, false, intArrayOf(0, 5, 3, 4), Lead.STEEL, 261, style = Style.LOFI),
        "rainbowend" to Tune(57, 118, false, intArrayOf(0, 5, 3, 4), Lead.SYNTH, 271, style = Style.ITALO),
        "cloudnine" to Tune(49, 85, false, intArrayOf(0, 3), Lead.FLUTE, 281, bars = 2, dub = 0.7f, style = Style.JUNGLE),
        "warren" to Tune(58, 150, false, intArrayOf(0, 3, 4, 0), Lead.PULSE, 291, style = Style.CHIP),
        "frontier" to Tune(53, 78, false, intArrayOf(0, 3, 4, 4), Lead.GUITAR, 301),
        "orchard" to Tune(55, 76, false, intArrayOf(0, 5, 3, 4), Lead.GUITAR, 311, style = Style.LOFI),
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
        Triple("house1", "Warehouse Kush", Tune(55, 124, true, intArrayOf(0, 5, 3, 4), Lead.ORGAN, 601, style = Style.HOUSE)),
        Triple("chip1", "8-Bit Blunt", Tune(60, 144, false, intArrayOf(0, 5, 3, 4), Lead.PULSE, 611, style = Style.CHIP)),
        Triple("italo1", "Neon Nights '84", Tune(52, 120, true, intArrayOf(0, 3, 5, 4), Lead.SAW, 621, style = Style.ITALO)),
        Triple("synth1", "Moscow Haze", Tune(57, 110, true, intArrayOf(0, 5, 2, 6), Lead.BELL, 631, style = Style.SYNTHPOP)),
        Triple("jungle1", "Amen Garden", Tune(55, 86, true, intArrayOf(0, 3), Lead.BELL, 641, bars = 2, style = Style.JUNGLE)),
        Triple("dark1", "Velvet Fog", Tune(53, 108, true, intArrayOf(0, 6, 5, 4), Lead.SAW, 651, dub = 0.6f, style = Style.DARKWAVE)),
        Triple("fusion1", "Tokyo Terpenes", Tune(58, 108, false, intArrayOf(1, 4, 0, 5), Lead.SAW, 661, style = Style.FUSION)),
    )
    private val extraTunes = extras.associate { it.first to it.third }

    val areas: Set<String> get() = byArea.keys

    /**
     * Which take on the music plays: 0 the tunes as written above, anything else a fresh one of every tune (see
     * [remake]), each keeping its style. Set by Settings' "New songs".
     */
    @Volatile var mix = 0

    /** Every tune of one take, and their [Feel]s. */
    private class Take(val salt: Int) {
        val tunes: Map<String, Tune> = (byArea + extraTunes).mapValues { (_, t) -> remake(t, salt) }
        /**
         * Each tune's [Feel], dealt in order so that it sits at least [Feel.APART] parts away from every tune of its
         * style dealt before it (the farthest of many tries, if none is that far).
         */
        val feels: Map<Tune, Feel> = LinkedHashMap<Tune, Feel>().also { out ->
            for (t in tunes.values) {
                val r = Random(t.seed * 7919 + 13)
                val same = out.filterKeys { it.style == t.style }.values
                fun gap(c: Feel) = same.minOfOrNull { o -> c.parts.indices.count { c.parts[it] != o.parts[it] } } ?: 99
                var best = deal(t, r)
                var g = gap(best)
                repeat(400) { if (g < Feel.APART + 1) { val c = deal(t, r); val cg = gap(c); if (cg > g) { best = c; g = cg } } }
                out[t] = best
            }
        }
    }
    /** The written take, and the last few remixes. */
    private val takes = LinkedHashMap<Int, Take>()
    private fun take(salt: Int): Take = synchronized(takes) {
        takes.getOrPut(salt) { Take(salt) }.also { if (takes.size > 3) takes.keys.firstOrNull { it != 0 && it != salt }?.let(takes::remove) }
    }

    private val PROGS = listOf(
        intArrayOf(0, 3, 4, 3), intArrayOf(0, 5, 3, 4), intArrayOf(0, 4, 5, 3), intArrayOf(0, 3, 0, 4), intArrayOf(0, 5, 1, 4),
        intArrayOf(0, 3, 6, 4), intArrayOf(0, 6, 5, 6), intArrayOf(1, 4, 0, 5), intArrayOf(0, 2, 3, 4), intArrayOf(0, 3), intArrayOf(0, 5), intArrayOf(0, 6),
    )

    /** A new tune in [t]'s style: its own key, tempo, chords, lead and seed. Salt 0 is [t] itself. */
    private fun remake(t: Tune, salt: Int): Tune {
        if (salt == 0) return t
        val r = Random(t.seed * 1_000_003 + salt)
        val prog = PROGS[r.nextInt(PROGS.size)]
        val minor = if (t.style == Style.DARKWAVE) true else r.nextBoolean()
        return Tune(
            48 + r.nextInt(14), t.style.bpm.first + r.nextInt(t.style.bpm.last - t.style.bpm.first + 1), minor, prog,
            t.style.leads[r.nextInt(t.style.leads.size)], 1 + r.nextInt(1_000_000), bars = if (prog.size == 2) 2 else 1,
            dub = t.dub, bubble = r.nextBoolean(), style = t.style, groove = t.groove,
        )
    }

    private fun deal(t: Tune, r: Random): Feel {
        val z = Feel.SIZES.map { r.nextInt(it) }
        return Feel(if (t.style != Style.LOFI) t.groove?.ordinal ?: z[0] else z[0], z[1], z[2], z[3], z[4], z[5], z[6], z[7])
    }

    fun feel(t: Tune): Feel = take(mix).feels[t] ?: synchronized(takes) { takes.values.firstNotNullOfOrNull { it.feels[t] } } ?: deal(t, Random(t.seed * 7919 + 13))

    /** [area]'s tune in take [salt]; any area without its own tune still gets one, made from its name. */
    fun of(area: String, salt: Int = mix): Tune = take(salt).tunes[area] ?: area.hashCode().let { h ->
        val leads = Lead.entries
        Tune(50 + Math.floorMod(h, 11), 64 + Math.floorMod(h / 11, 16), h % 2 == 0, intArrayOf(0, 3, 4, 3), leads[Math.floorMod(h / 7, leads.size)], h)
    }
}
