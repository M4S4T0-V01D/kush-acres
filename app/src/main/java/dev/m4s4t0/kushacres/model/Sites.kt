package dev.m4s4t0.kushacres.model

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Lay of the land on a site's map, carved out when the site is bought. */
sealed class Feature {
    /** A pond or lake around a point given as fractions of the map, [r] squares across (give or take). */
    class Lake(val x: Float, val y: Float, val r: Float) : Feature()
    /** A winding river crossing the map, [at] a fraction of the way across. */
    class River(val vertical: Boolean, val at: Float, val width: Float) : Feature()
    /** Sea along one side: 0 = low x, 1 = high x, 2 = low y, 3 = high y. */
    class Coast(val side: Int, val depth: Int) : Feature()
    /** Boulders scattered over [share] of the map. */
    class Rocks(val share: Float) : Feature()
    /** A rocky rim [r] squares out from the middle, like the edge of a crater. */
    class Rim(val r: Float) : Feature()
    /** An old mansion, [Manor.SIZE] squares across with its corner at [x],[y]: nobody can build on it or knock it down. */
    class Manor(val x: Int, val y: Int) : Feature() {
        fun has(a: Int, b: Int) = a in x until x + SIZE && b in y until y + SIZE
        companion object { const val SIZE = 4 }
    }
    /**
     * A castle: a stone curtain wall round the whole map with towers along it and a gate in the middle of each side,
     * and a fortress keep, [Castle.KEEP] squares across with its corner at [x],[y]. None of it can be built on or knocked down.
     */
    class Castle(val x: Int, val y: Int) : Feature() {
        fun keep(a: Int, b: Int) = a in x until x + KEEP && b in y until y + KEEP
        private fun rim(a: Int) = a == 0 || a == Farm.MAX - 1
        fun gate(a: Int, b: Int) = rim(a) && b in GATE || rim(b) && a in GATE
        fun wall(a: Int, b: Int) = (rim(a) || rim(b)) && !gate(a, b)
        fun tower(a: Int, b: Int) = wall(a, b) && (rim(a) && b in TOWERS || rim(b) && a in TOWERS)
        fun has(a: Int, b: Int) = keep(a, b) || wall(a, b)
        companion object {
            const val KEEP = 6
            val GATE = 19..20
            val TOWERS = setOf(0, 9, 18, 21, 30, 39)
        }
    }
    /**
     * The map's own landmark (a barn, a lighthouse, a windmill...), [size] squares across with its corner at [x],[y];
     * [id] picks its art. Like the mansion, nobody can build on it or knock it down.
     */
    class Landmark(val id: String, val name: String, val blurb: String, val x: Int, val y: Int, val size: Int = 3) : Feature() {
        fun has(a: Int, b: Int) = a in x until x + size && b in y until y + size
        fun yard(a: Int, b: Int) = a in x - 1..x + size && b in y - 1..y + size
    }
}

/**
 * A place you can farm. [ax]/[ay] say where the owned square sits on the 40×40 map (0 = top/left edge, 1 = far
 * edge); it grows around that point. Colours: [ground]/[wild] grass, and the menus' wood ([wood], [woodIn], [edge]).
 */
class SiteDef(
    val id: String, val name: String, val blurb: String, val level: Int, val price: Long,
    val ax: Float, val ay: Float,
    val ground: Int, val wild: Int,
    val wood: Int, val woodIn: Int, val edge: Int,
    /** Out of 7: how much of the wild is trees ([pines] for conifers only). */
    val trees: Int, val pines: Boolean = false,
    val roof: Int = 0, val wall: Int = 0,
    val features: List<Feature> = emptyList(),
    /** A holiday farm: bought from that holiday's shop for [price] tokens instead of cash. */
    val holiday: String? = null,
    /** How big it starts out when bought. */
    val land: Int = START_LAND,
    /** Haunted woods: dead trees, cobwebs, toadstools and pumpkins instead of leafy trees and bushes. */
    val haunted: Boolean = false,
    /** A garden world: flame trees, palms, flower beds and lanterns in the wilds, flowers underfoot and lilies on the water. */
    val garden: Boolean = false,
) {
    /** The mansion standing on this map, if there is one. */
    val manor get() = features.firstNotNullOfOrNull { it as? Feature.Manor }
    /** The castle walls and keep on this map, if there are any. */
    val castle get() = features.firstNotNullOfOrNull { it as? Feature.Castle }
    /** The map's landmark, if it has one. */
    val landmark get() = features.firstNotNullOfOrNull { it as? Feature.Landmark }
    /** Whether a landmark that never moves (the mansion, the castle, the rest) stands on [x],[y]. */
    fun fixed(x: Int, y: Int) = manor?.has(x, y) == true || castle?.has(x, y) == true || landmark?.has(x, y) == true

    companion object {
        /** How big a newly bought site starts. */
        const val START_LAND = 8
        const val ESTATE_LAND = 12
        const val REALTOR_LEVEL = 15
    }
}

object Sites {
    val HOME = SiteDef("home", "Home Acres", "Where it all started.", 1, 0, 0f, 0f,
        0xFF62AE45.toInt(), 0xFF3E7D3C.toInt(), 0xFF2B2118.toInt(), 0xFF3B2D20.toInt(), 0xFF8A6238.toInt(), 3,
        features = listOf(Feature.Landmark("barn", "Old Red Barn", "Grandpa's barn. It's leaned like that since 1962.", 33, 4)))

    val all: List<SiteDef> = listOf(
        HOME,
        SiteDef("redwood", "Redwood Hollow", "Giant trees, cool shade, a quiet pond.", 15, 120_000, 0.5f, 0.5f,
            0xFF4F9A3E.toInt(), 0xFF2F6236.toInt(), 0xFF16241A.toInt(), 0xFF213624.toInt(), 0xFF5E8A4E.toInt(), 5, pines = true,
            roof = 2, wall = 5, features = listOf(Feature.Lake(0.18f, 0.2f, 4f), Feature.Lake(0.85f, 0.75f, 3f), Feature.Rocks(0.03f),
                Feature.Landmark("lookout", "Fire Lookout", "A ranger's cabin up on stilts, watching the canopy for smoke. Not that smoke.", 26, 8))),
        SiteDef("mesa", "Dusty Mesa", "Sun-baked sand, boulders and one sweet oasis.", 19, 400_000, 1f, 0f,
            0xFFCFAE6A.toInt(), 0xFFAE8A4E.toInt(), 0xFF3A1E14.toInt(), 0xFF52291B.toInt(), 0xFFC0703E.toInt(), 0,
            roof = 0, wall = 0, features = listOf(Feature.Lake(0.3f, 0.75f, 3f), Feature.Rocks(0.09f),
                Feature.Landmark("mission", "Old Mission", "Sun-bleached adobe and a bell nobody's rung in a century.", 14, 8))),
        SiteDef("bayou", "Bayou Bend", "Muggy swamp with a lazy river. Plots by water never dry out.", 23, 1_200_000, 0f, 1f,
            0xFF5C8C3A.toInt(), 0xFF3C6630.toInt(), 0xFF102624.toInt(), 0xFF173A36.toInt(), 0xFF3E8C7E.toInt(), 4,
            roof = 4, wall = 4, features = listOf(Feature.River(true, 0.62f, 2.5f), Feature.Lake(0.2f, 0.25f, 3.5f),
                Feature.Lake(0.8f, 0.2f, 2.5f), Feature.Lake(0.9f, 0.9f, 3f),
                Feature.Landmark("stiltshack", "Stilt Shack", "A tin-roofed shack on stilts. The gators pay rent.", 11, 20))),
        SiteDef("coast", "Sunset Coast", "Ocean on two sides and a sky on fire every evening.", 27, 4_000_000, 1f, 1f,
            0xFF86BE52.toInt(), 0xFF5E9440.toInt(), 0xFF3A1A22.toInt(), 0xFF52242E.toInt(), 0xFFE07A5A.toInt(), 2,
            roof = 1, wall = 1, features = listOf(Feature.Coast(0, 7), Feature.Coast(2, 6), Feature.Rocks(0.02f),
                Feature.Landmark("lighthouse", "Sunset Lighthouse", "Still turns every night, guiding boats past the rocks.", 10, 22))),
        SiteDef("frost", "Frostpeak", "High, cold and clean. Pines, snow and a frozen tarn.", 31, 10_000_000, 0.5f, 0f,
            0xFFDCE6EC.toInt(), 0xFFB4C4D0.toInt(), 0xFF141E2C.toInt(), 0xFF1E2C40.toInt(), 0xFF7AA8D0.toInt(), 4, pines = true,
            roof = 1, wall = 2, features = listOf(Feature.Lake(0.2f, 0.85f, 3.5f), Feature.Rocks(0.06f),
                Feature.Landmark("observatory", "Frostpeak Observatory", "Thin air and clear skies. The telescope sees all the way to the next galaxy.", 28, 18))),
        SiteDef("emerald", "Emerald Triangle", "Legendary soil. A river runs right through it.", 35, 30_000_000, 0f, 0.5f,
            0xFF3FA84A.toInt(), 0xFF2A7436.toInt(), 0xFF0E2418.toInt(), 0xFF153626.toInt(), 0xFF3CC07A.toInt(), 3,
            roof = 2, wall = 1, features = listOf(Feature.River(false, 0.25f, 2f), Feature.Lake(0.8f, 0.7f, 2.5f),
                Feature.Landmark("growdome", "Grow Dome", "A glass dome full of purple light. The old-timers' secret garden.", 20, 26))),
        SiteDef("volcano", "Volcano Isle", "Black ash, warm ground, sea all round.", 40, 80_000_000, 0.5f, 0.5f,
            0xFF6E645A.toInt(), 0xFF4A423E.toInt(), 0xFF2A0E0C.toInt(), 0xFF3E1612.toInt(), 0xFFD04A2A.toInt(), 1,
            roof = 4, wall = 5, features = listOf(Feature.Coast(0, 4), Feature.Coast(1, 4), Feature.Coast(2, 4), Feature.Coast(3, 4),
                Feature.Rocks(0.08f),
                Feature.Landmark("moai", "Stone Heads", "Three stone heads that were here before anyone. They seem to approve.", 27, 9))),
        SiteDef("neon", "Neon Valley", "Glowing lakes and grass with a funny tint.", 45, 200_000_000, 1f, 0.5f,
            0xFF5FA87A.toInt(), 0xFF3A6E5A.toInt(), 0xFF1E1028.toInt(), 0xFF2C1840.toInt(), 0xFFC060E8.toInt(), 2,
            roof = 3, wall = 3, features = listOf(Feature.Lake(0.15f, 0.2f, 3f), Feature.Lake(0.25f, 0.8f, 3.5f),
                Feature.Lake(0.75f, 0.1f, 2.5f),
                Feature.Landmark("diner", "Neon Diner", "Open all night. The pie is legendary and so are the munchies.", 18, 18))),
        SiteDef("fort20", "Fort 20", "Stone walls all the way round and a keep in the middle. Nobody's getting at this harvest.", 48, 420_000_000, 0.5f, 0.66f,
            0xFF6EA84A.toInt(), 0xFF4A7A3A.toInt(), 0xFF1C1A1E.toInt(), 0xFF2A282E.toInt(), 0xFFB0A488.toInt(), 3,
            roof = 4, wall = 2, features = listOf(Feature.Lake(0.15f, 0.82f, 2.5f), Feature.Rocks(0.03f), Feature.Castle(17, 7))),
        SiteDef("highland", "Highland Terraces", "Rocky slopes with a stream off the peaks.", 51, 600_000_000, 0.5f, 1f,
            0xFF8AB04E.toInt(), 0xFF5E7E3A.toInt(), 0xFF1A1E24.toInt(), 0xFF262C36.toInt(), 0xFF8A9AB0.toInt(), 3, pines = true,
            roof = 4, wall = 2, features = listOf(Feature.River(true, 0.15f, 1.5f), Feature.Rocks(0.1f),
                Feature.Landmark("pagoda", "Mountain Pagoda", "Red tiers stacked up to the clouds. Wind chimes and a lot of calm.", 24, 14))),
        SiteDef("crater", "Cosmic Crater", "Something fell here long ago. The plants like it.", 58, 1_800_000_000, 0.5f, 0.5f,
            0xFF7A70A8.toInt(), 0xFF4E4878.toInt(), 0xFF0E1030.toInt(), 0xFF181C48.toInt(), 0xFF7A88F0.toInt(), 1,
            roof = 3, wall = 2, features = listOf(Feature.Rim(13f), Feature.Lake(0.08f, 0.1f, 2.5f), Feature.Lake(0.92f, 0.9f, 2.5f),
                Feature.Landmark("ufo", "The Crashed Saucer", "Whatever fell here is still humming.", 26, 17, 4))),
    )

    /** Holiday farms, one per holiday, priced in its tokens. They aren't on the realtor's list. */
    val holidays: List<SiteDef> = listOf(
        SiteDef("spooky", "Spooky Hollow", "Dead trees, a foggy pond and something moving in the pumpkins.", 1, 500, 0.5f, 0.5f,
            0xFF5A6A3A.toInt(), 0xFF2E3A2A.toInt(), 0xFF1A1020.toInt(), 0xFF2A1A34.toInt(), 0xFFFF8A1E.toInt(), 4,
            roof = 3, wall = 5, features = listOf(Feature.Lake(0.2f, 0.75f, 3f), Feature.Rocks(0.05f),
                Feature.Landmark("crypt", "Hollow Chapel", "A crooked chapel with a window that glows purple at night.", 27, 8)), holiday = "halloween"),
        SiteDef("northpole", "North Pole Acres", "Snow, candy-striped fences and an elf union.", 1, 500, 0.5f, 0f,
            0xFFEAF2F6.toInt(), 0xFFC4D4DE.toInt(), 0xFF2A0E12.toInt(), 0xFF3E141A.toInt(), 0xFFE03A3A.toInt(), 5, pines = true,
            roof = 0, wall = 1, features = listOf(Feature.Lake(0.8f, 0.8f, 3f),
                Feature.Landmark("xmastree", "The Great Tree", "The tallest tree at the pole, lit every night of the year.", 8, 14)), holiday = "christmas"),
        SiteDef("timesquare", "Midnight Square", "The party never ends. Neither does the confetti.", 1, 500, 0.5f, 0.5f,
            0xFF5A8A6A.toInt(), 0xFF3A5A4A.toInt(), 0xFF101024.toInt(), 0xFF1A1A3A.toInt(), 0xFFFFD23A.toInt(), 1,
            roof = 4, wall = 1, features = listOf(Feature.Rocks(0.02f),
                Feature.Landmark("balltower", "Countdown Tower", "The ball drops at midnight. Every midnight.", 28, 10)), holiday = "newyear"),
        SiteDef("lovers", "Lovers' Lane", "Pink blossoms over a heart-shaped pond.", 1, 500, 0f, 0.5f,
            0xFF7ABE6A.toInt(), 0xFF5A9A5A.toInt(), 0xFF3A1024.toInt(), 0xFF521A34.toInt(), 0xFFFF6AA0.toInt(), 3,
            roof = 3, wall = 3, features = listOf(Feature.Lake(0.8f, 0.3f, 3.5f),
                Feature.Landmark("gazebo", "Sweetheart Gazebo", "Fairy lights, a pink roof and a bench built for two.", 20, 26)), holiday = "valentine"),
        SiteDef("clover", "Clover Glen", "Forty shades of green and a rainbow most mornings.", 1, 500, 1f, 0.5f,
            0xFF3AB04A.toInt(), 0xFF26803A.toInt(), 0xFF0E2A14.toInt(), 0xFF163A1E.toInt(), 0xFF5AFF6A.toInt(), 3,
            roof = 2, wall = 1, features = listOf(Feature.River(true, 0.2f, 2f), Feature.Rocks(0.04f),
                Feature.Landmark("roundtower", "Round Tower", "A thousand-year-old stone tower. The door's halfway up, for reasons.", 20, 8, 2)), holiday = "stpatrick"),
        SiteDef("highhill", "420 Hill", "The highest hill in the county, in every sense.", 1, 500, 0.5f, 1f,
            0xFF6ABE3A.toInt(), 0xFF4A8A30.toInt(), 0xFF1A2A10.toInt(), 0xFF26401A.toInt(), 0xFF9AFF3A.toInt(), 3,
            roof = 2, wall = 4, features = listOf(Feature.Lake(0.15f, 0.15f, 3f),
                Feature.Landmark("bong", "Big Bong Monument", "Raised by the county's finest in 1971. It still works.", 18, 16, 2)), holiday = "fourtwenty"),
        SiteDef("meadow", "Easter Meadow", "Pastel flowers, rabbit holes and eggs in odd places.", 1, 500, 0f, 0f,
            0xFF9AD27A.toInt(), 0xFF7AB05A.toInt(), 0xFF3A2A4A.toInt(), 0xFF4E3A60.toInt(), 0xFFC0A0FF.toInt(), 3,
            roof = 3, wall = 3, features = listOf(Feature.Lake(0.75f, 0.8f, 3f),
                Feature.Landmark("egg", "Giant Egg", "Nobody knows who laid it. Nobody wants to meet them.", 18, 14, 2)), holiday = "easter"),
        SiteDef("liberty", "Liberty Ranch", "Wide open prairie and fireworks every night.", 1, 500, 1f, 1f,
            0xFFA8B05A.toInt(), 0xFF7E8A42.toInt(), 0xFF101A3A.toInt(), 0xFF1A2A52.toInt(), 0xFFE03A3A.toInt(), 1,
            roof = 1, wall = 1, features = listOf(Feature.River(false, 0.15f, 2f),
                Feature.Landmark("watertower", "Liberty Water Tower", "Stars, stripes and forty thousand gallons.", 18, 20)), holiday = "july4"),
        SiteDef("harvest", "Harvest Homestead", "Golden fields, red barns, a big table.", 1, 500, 0f, 1f,
            0xFFB8A050.toInt(), 0xFF8A7038.toInt(), 0xFF2A1A0E.toInt(), 0xFF3E2814.toInt(), 0xFFE0903A.toInt(), 3,
            roof = 0, wall = 5, features = listOf(Feature.Lake(0.85f, 0.2f, 2.5f), Feature.Rocks(0.03f),
                Feature.Landmark("windmill", "Harvest Windmill", "Grinds the grain and creaks all night.", 16, 18)), holiday = "thanksgiving"),
        // Each holiday's estate: dearer, and it starts out bigger.
        SiteDef("manor", "Haunted Manor", "A crooked mansion on the hill. The lights flicker on their own.", 1, Holiday.ESTATE, 0.3f, 0.2f,
            0xFF3E4A44.toInt(), 0xFF232430.toInt(), 0xFF140C1A.toInt(), 0xFF22142C.toInt(), 0xFFA43AFF.toInt(), 5,
            roof = 3, wall = 5, features = listOf(Feature.Lake(0.7f, 0.3f, 2.5f), Feature.Rocks(0.06f), Feature.Manor(12, 20)),
            holiday = "halloween", land = SiteDef.ESTATE_LAND, haunted = true),
        SiteDef("workshop", "Santa's Workshop", "Toy sheds, reindeer stables and a chimney that never stops smoking.", 1, Holiday.ESTATE, 0.7f, 0.1f,
            0xFFF4F8FA.toInt(), 0xFFD0DEE6.toInt(), 0xFF1E3A22.toInt(), 0xFF2A4E2E.toInt(), 0xFFF2C440.toInt(), 6, pines = true,
            roof = 0, wall = 1, features = listOf(Feature.Lake(0.2f, 0.2f, 2.5f),
                Feature.Landmark("factory", "Toy Factory", "Santa's main works. The chimney never stops smoking.", 12, 22, 4)), holiday = "christmas", land = SiteDef.ESTATE_LAND),
        SiteDef("rooftop", "Rooftop Gardens", "Grow beds forty floors up, with the best view of the fireworks.", 1, Holiday.ESTATE, 0.3f, 0.7f,
            0xFF6A9A7A.toInt(), 0xFF4A6A5A.toInt(), 0xFF181830.toInt(), 0xFF24244A.toInt(), 0xFFC0C8FF.toInt(), 1,
            roof = 4, wall = 1, features = listOf(Feature.Rocks(0.01f),
                Feature.Landmark("watertank", "Water Tank", "An old wooden rooftop tank, with a sign someone put up.", 26, 8)), holiday = "newyear", land = SiteDef.ESTATE_LAND),
        SiteDef("rosegarden", "Rose Garden Estate", "Hedge mazes, rose arbours and a fountain for wishes.", 1, Holiday.ESTATE, 0.2f, 0.3f,
            0xFF6AB060.toInt(), 0xFF4A8A4A.toInt(), 0xFF4A1430.toInt(), 0xFF5E1E3E.toInt(), 0xFFFF3A6A.toInt(), 4,
            roof = 3, wall = 3, features = listOf(Feature.Lake(0.25f, 0.75f, 3f),
                Feature.Landmark("fountain", "Wishing Fountain", "Toss a coin. The roses heard you.", 26, 22)), holiday = "valentine", land = SiteDef.ESTATE_LAND),
        SiteDef("rainbowend", "Rainbow's End", "The pot of gold is real. The leprechauns want it back.", 1, Holiday.ESTATE, 0.8f, 0.3f,
            0xFF2EA040.toInt(), 0xFF1E7030.toInt(), 0xFF143A1A.toInt(), 0xFF1E4E26.toInt(), 0xFFFFD23A.toInt(), 3,
            roof = 2, wall = 1, features = listOf(Feature.River(false, 0.7f, 2f), Feature.Rocks(0.03f),
                Feature.Landmark("potofgold", "Pot of Gold", "The end of the rainbow. It's real, and it's full.", 8, 10)), holiday = "stpatrick", land = SiteDef.ESTATE_LAND),
        SiteDef("cloudnine", "Cloud Nine", "So high up the clouds roll through the rows.", 1, Holiday.ESTATE, 0.7f, 0.8f,
            0xFF7ACE4A.toInt(), 0xFF5A9E3A.toInt(), 0xFF2A1A3A.toInt(), 0xFF3A2A52.toInt(), 0xFF8A4AE0.toInt(), 3,
            roof = 2, wall = 4, features = listOf(Feature.Lake(0.8f, 0.8f, 3f),
                Feature.Landmark("balloon", "Moored Balloon", "A patchwork balloon tied up at the edge of the sky.", 8, 8)), holiday = "fourtwenty", land = SiteDef.ESTATE_LAND),
        SiteDef("warren", "Bunny Warren", "Rolling hills riddled with burrows, and eggs in every one.", 1, Holiday.ESTATE, 0.3f, 0.3f,
            0xFFA8DA8A.toInt(), 0xFF88BA6A.toInt(), 0xFF4A3A2A.toInt(), 0xFF5E4A36.toInt(), 0xFFFFE07A.toInt(), 4,
            roof = 3, wall = 3, features = listOf(Feature.Lake(0.2f, 0.2f, 2.5f), Feature.Rocks(0.02f),
                Feature.Landmark("burrow", "The Big Burrow", "The head rabbit lives here. Knock first.", 27, 26)), holiday = "easter", land = SiteDef.ESTATE_LAND),
        SiteDef("frontier", "Frontier Ranch", "A thousand acres of open range and a flag on every post.", 1, Holiday.ESTATE, 0.7f, 0.7f,
            0xFFB4B864.toInt(), 0xFF8A924A.toInt(), 0xFF3A1A14.toInt(), 0xFF4E241C.toInt(), 0xFF3A6AE0.toInt(), 1,
            roof = 1, wall = 1, features = listOf(Feature.River(true, 0.8f, 2.5f), Feature.Rocks(0.03f),
                Feature.Landmark("saloon", "Last Chance Saloon", "Swinging doors, a piano and no questions asked.", 8, 8)), holiday = "july4", land = SiteDef.ESTATE_LAND),
        SiteDef("orchard", "Golden Orchard", "Apple trees in every direction and a cider press in the barn.", 1, Holiday.ESTATE, 0.3f, 0.8f,
            0xFFC0A858.toInt(), 0xFF9A7E3E.toInt(), 0xFF3A200E.toInt(), 0xFF4E2E16.toInt(), 0xFFD04A2A.toInt(), 6,
            roof = 0, wall = 5, features = listOf(Feature.Lake(0.2f, 0.8f, 2.5f),
                Feature.Landmark("ciderbarn", "Cider Barn", "Where every apple on the farm ends up.", 26, 8)), holiday = "thanksgiving", land = SiteDef.ESTATE_LAND),
    )

    /** Farms bought with Pot Bucks (their price is in Pot Bucks). Not on the realtor's list either. */
    val premium: List<SiteDef> = listOf(
        SiteDef("zion", "Zion Gardens", "Red, gold and green as far as you can see, and a bassline from the hills.", 12, 250, 0.5f, 0.5f,
            0xFF4AB04A.toInt(), 0xFF2E8A3A.toInt(), 0xFF2A1A0E.toInt(), 0xFF3E2814.toInt(), 0xFFFFD23A.toInt(), 4,
            roof = 2, wall = 3, features = listOf(Feature.River(true, 0.75f, 2f), Feature.Rocks(0.03f), Feature.Lake(0.22f, 0.75f, 2.5f),
                Feature.Landmark("stage", "Sound System Stage", "Speaker stacks taller than the trees. Bass you feel in your chest.", 6, 8)), holiday = "bucks", garden = true),
    )

    private val byId = (all + holidays + premium).associateBy { it.id }
    operator fun get(id: String): SiteDef? = byId[id]

    /** Smooth noise in 0..1, about one bump every 4 squares. */
    private fun noise(x: Float, y: Float, seed: Int): Float {
        fun h(i: Int, j: Int): Float {
            var n = i * 374761393 + j * 668265263 + seed * 2246822519L.toInt()
            n = (n xor (n ushr 13)) * 1274126177
            return ((n xor (n ushr 16)) and 0xFFFF) / 65535f
        }
        val x0 = floor(x).toInt(); val y0 = floor(y).toInt()
        val fx = x - x0; val fy = y - y0
        val sx = fx * fx * (3 - 2 * fx); val sy = fy * fy * (3 - 2 * fy)
        val a = h(x0, y0) + (h(x0 + 1, y0) - h(x0, y0)) * sx
        val b = h(x0, y0 + 1) + (h(x0 + 1, y0 + 1) - h(x0, y0 + 1)) * sx
        return a + (b - a) * sy
    }

    /**
     * Puts [m]'s mansion on [tiles], with a clear yard round it, unless something already stands where it goes:
     * how a farm bought before the mansion was built gets one.
     */
    fun stampManor(m: Feature.Manor, tiles: Array<Tile>): Boolean {
        val n = Farm.MAX
        val wild = setOf(Kind.GRASS, Kind.ROCK, Kind.WATER)
        for (y in m.y - 1..m.y + Feature.Manor.SIZE) for (x in m.x - 1..m.x + Feature.Manor.SIZE) {
            if (x !in 0 until n || y !in 0 until n) continue
            val k = tiles[y * n + x].kind
            if (k == Kind.MANOR) return false
            if (m.has(x, y) && k !in wild) return false
        }
        for (y in m.y - 1..m.y + Feature.Manor.SIZE) for (x in m.x - 1..m.x + Feature.Manor.SIZE) {
            if (x !in 0 until n || y !in 0 until n) continue
            val t = tiles[y * n + x]
            if (m.has(x, y)) { t.clear(); t.kind = Kind.MANOR } else if (t.kind == Kind.ROCK || t.kind == Kind.WATER) t.kind = Kind.GRASS
        }
        return true
    }

    /** Puts [l] on [tiles] with a clear yard round it, the same way as [stampManor]. */
    fun stampLandmark(l: Feature.Landmark, tiles: Array<Tile>): Boolean {
        val n = Farm.MAX
        val wild = setOf(Kind.GRASS, Kind.ROCK, Kind.WATER)
        for (y in l.y - 1..l.y + l.size) for (x in l.x - 1..l.x + l.size) {
            if (x !in 0 until n || y !in 0 until n) continue
            val k = tiles[y * n + x].kind
            if (k == Kind.LANDMARK) return false
            if (l.has(x, y) && k !in wild) return false
        }
        for (y in l.y - 1..l.y + l.size) for (x in l.x - 1..l.x + l.size) {
            if (x !in 0 until n || y !in 0 until n) continue
            val t = tiles[y * n + x]
            if (l.has(x, y)) { t.clear(); t.kind = Kind.LANDMARK } else if (t.kind == Kind.ROCK || t.kind == Kind.WATER) t.kind = Kind.GRASS
        }
        return true
    }

    /**
     * Raises [c]'s walls and keep on [tiles], over whatever wild ground is there, and clears the gates and a yard
     * round the keep. Squares something was built on are left alone.
     */
    fun stampCastle(c: Feature.Castle, tiles: Array<Tile>) {
        val n = Farm.MAX
        val wild = setOf(Kind.GRASS, Kind.ROCK, Kind.WATER, Kind.FORT)
        for (y in 0 until n) for (x in 0 until n) {
            val t = tiles[y * n + x]
            val yard = x in c.x - 1..c.x + Feature.Castle.KEEP && y in c.y - 1..c.y + Feature.Castle.KEEP
            when {
                c.has(x, y) -> if (t.kind in wild) { t.clear(); t.kind = Kind.FORT }
                (c.gate(x, y) || yard) && (t.kind == Kind.ROCK || t.kind == Kind.WATER) -> t.kind = Kind.GRASS
            }
        }
    }

    /** Carves [def]'s features into [tiles]; the starting square at [ox],[oy] and the ring round it stay clear. */
    fun carve(def: SiteDef, tiles: Array<Tile>, ox: Int, oy: Int, land: Int) {
        val m = Farm.MAX
        val seed = def.id.hashCode()
        val r = Random(seed)
        fun set(x: Int, y: Int, k: Kind) { if (x in 0 until m && y in 0 until m) tiles[y * m + x].kind = k }
        for (f in def.features) when (f) {
            is Feature.Lake -> {
                val cx = f.x * (m - 1); val cy = f.y * (m - 1)
                for (y in 0 until m) for (x in 0 until m) {
                    val d = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
                    if (d < f.r + (noise(x / 3f, y / 3f, seed + 11) - 0.5f) * 2.5f) set(x, y, Kind.WATER)
                }
            }
            is Feature.River -> for (a in 0 until m) for (b in 0 until m) {
                val mid = f.at * (m - 1) + sin(a * 0.28f) * 2.2f + (noise(a / 4f, 0.5f, seed + 3) - 0.5f) * 4f
                if (abs(b - mid) < f.width / 2) { if (f.vertical) set(b, a, Kind.WATER) else set(a, b, Kind.WATER) }
            }
            is Feature.Coast -> for (a in 0 until m) {
                val d = (f.depth + (noise(a / 3f, f.side * 7f, seed + 5) - 0.5f) * 3f).roundToInt()
                for (b in 0 until d) when (f.side) {
                    0 -> set(b, a, Kind.WATER); 1 -> set(m - 1 - b, a, Kind.WATER)
                    2 -> set(a, b, Kind.WATER); else -> set(a, m - 1 - b, Kind.WATER)
                }
            }
            is Feature.Rocks -> for (y in 0 until m) for (x in 0 until m) {
                val t = tiles[y * m + x]
                if (t.kind == Kind.GRASS && r.nextFloat() < f.share) t.kind = Kind.ROCK
            }
            is Feature.Rim -> for (y in 0 until m) for (x in 0 until m) {
                val c = (m - 1) / 2f
                val d = sqrt((x - c) * (x - c) + (y - c) * (y - c))
                val t = tiles[y * m + x]
                if (t.kind == Kind.GRASS && abs(d - f.r) < 1f && r.nextFloat() < 0.75f) t.kind = Kind.ROCK
            }
            is Feature.Manor -> stampManor(f, tiles)
            is Feature.Castle -> stampCastle(f, tiles)
            is Feature.Landmark -> stampLandmark(f, tiles)
        }
        for (y in oy - 1..oy + land) for (x in ox - 1..ox + land) {
            if (x !in 0 until m || y !in 0 until m) continue
            val t = tiles[y * m + x]
            if (t.kind == Kind.WATER || t.kind == Kind.ROCK) t.kind = Kind.GRASS
        }
    }
}

/** A work building on a farm, and whether a worker staffs it. */
class HouseSpot(val x: Int, val y: Int, val house: WorkHouse, val staffed: Boolean, val size: Int = 1)

/**
 * One floor of a farm's bunker: a square of grow plots under the farm, [size] across, in the middle of its own grid.
 * Drip lines keep every plot wet and grow lights hang over each one, run on the farm's power ([Bunker.WATTS] a square).
 */
class Floor {
    val tiles = Array(Farm.MAX * Farm.MAX) { Tile() }
    var size = 0
        private set
    val o get() = (Farm.MAX - size) / 2

    /** Grows the floor to [n] across, keeping what's planted and making the new squares plots. */
    fun resize(n: Int) {
        val old = (0 until size * size).map { tiles[(o + it / size) * Farm.MAX + o + it % size] }.map { t -> Tile().also { it.copyFrom(t) } }
        val m = size
        tiles.forEach { it.clear() }
        size = n
        for (i in 0 until n * n) tiles[(o + i / n) * Farm.MAX + o + i % n].kind = Kind.PLOT
        for ((i, t) in old.withIndex()) tiles[(o + i / m) * Farm.MAX + o + i % m].copyFrom(t)
    }
}

object Bunker {
    const val LEVEL = 15
    const val COST = 250_000L
    const val START = 3
    const val SIZE_MAX = 10
    const val FLOORS = 5
    /** Power each bunker square's grow light draws from the farm above. */
    const val WATTS = 1
    /** Growth in a bunker with its lights out. */
    const val DARK = 0.25
    /** Extra stash per square across of each floor, as a share of the farm's own. */
    const val STASH = 0.05
    /** The level each floor after the first needs, and what digging it costs. */
    fun floorLevel(n: Int) = 15 + 10 * (n - 1)
    fun floorCost(n: Int): Long = (2_500_000.0 * 10.0.pow(n - 2)).toLong()
    /** Widening floor [n] from [size] across: dearer than land up top, and dearer still further down. */
    fun sizeCost(n: Int, size: Int): Long = (300_000.0 * 3.0.pow(size - START) * 2.0.pow(n - 1)).toLong()
}

/** An extra square of land [n] across with its top-left at [x],[y], bought on side [dir] of the farm (see [Annex.SIDES]). */
data class Annex(val dir: Int, val x: Int, val y: Int, val n: Int) {
    fun has(a: Int, b: Int) = a in x until x + n && b in y until y + n
    companion object {
        /** Side names by dir: 0 is +x, 1 is +y, 2 is -x, 3 is -y. */
        val SIDES = listOf("East", "South", "West", "North")
    }
}

/** One farm you own: its own land and everything on it. Cash, stash, helpers and the rest are shared. */
class Site(val def: SiteDef) {
    val tiles = Array(Farm.MAX * Farm.MAX) { Tile() }.also { t ->
        // Home is never carved, so its landmark goes straight on.
        if (def === Sites.HOME) def.landmark?.let { Sites.stampLandmark(it, t) }
    }
    var land = 6
    var roof = def.roof
    var wall = def.wall
    /** Workers hired for this farm; they staff its work buildings in order and water plots when there's no building free. */
    var workers = 0
    /** Crew hired for this farm, by [Crew] ordinal. */
    val crew = IntArray(Crew.entries.size)
    /** Plants harvested on this farm, and the [FarmGoal]s it has reached. */
    var harvests = 0L
    val goals = LinkedHashSet<String>()
    /** The shack's tier and Violet's upgrades bought for this farm. */
    var shack = 0
    val upgrades = IntArray(Upgrade.entries.size)
    /** The work buildings on this farm, worked out again whenever one is built, moved or staffed (see Farm.houses). */
    var spots: List<HouseSpot>? = null
    /** The bunker's floors, from the top down; none until one is bought. */
    val floors = ArrayList<Floor>()
    /** Where the player put the middle of the square, or null for the site's own spot ([SiteDef.ax], [SiteDef.ay]). */
    var cx: Int? = null
    var cy: Int? = null
    /** Grass and plot soil colours bought in the shop, or null for the farm's own. */
    var grass: Int? = null
    var dirt: Int? = null
    /** Power made and used, and the specials standing here, worked out by Farm.scan when [scanned] is stale. */
    var scanned = -1L
    var made = 0
    var used = 0
    var specials: Set<String> = emptySet()
    /** Plots bought next door from Mona, at most one per side; they stay where they were bought as the farm grows. */
    val annexes = ArrayList<Annex>()
    /** Top-left corner of the owned square. */
    val ox get() = cx?.let { (it - land / 2).coerceIn(0, Farm.MAX - land) } ?: ((Farm.MAX - land) * def.ax).roundToInt()
    val oy get() = cy?.let { (it - land / 2).coerceIn(0, Farm.MAX - land) } ?: ((Farm.MAX - land) * def.ay).roundToInt()
}
