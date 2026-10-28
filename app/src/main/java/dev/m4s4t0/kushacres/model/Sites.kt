package dev.m4s4t0.kushacres.model

import kotlin.math.abs
import kotlin.math.floor
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
) {
    companion object {
        /** How big a newly bought site starts. */
        const val START_LAND = 8
        const val REALTOR_LEVEL = 15
    }
}

object Sites {
    val HOME = SiteDef("home", "Home Acres", "Where it all started.", 1, 0, 0f, 0f,
        0xFF62AE45.toInt(), 0xFF3E7D3C.toInt(), 0xFF2B2118.toInt(), 0xFF3B2D20.toInt(), 0xFF8A6238.toInt(), 3)

    val all: List<SiteDef> = listOf(
        HOME,
        SiteDef("redwood", "Redwood Hollow", "Giant trees, cool shade, a quiet pond.", 15, 120_000, 0.5f, 0.5f,
            0xFF4F9A3E.toInt(), 0xFF2F6236.toInt(), 0xFF16241A.toInt(), 0xFF213624.toInt(), 0xFF5E8A4E.toInt(), 5, pines = true,
            roof = 2, wall = 5, features = listOf(Feature.Lake(0.18f, 0.2f, 4f), Feature.Lake(0.85f, 0.75f, 3f), Feature.Rocks(0.03f))),
        SiteDef("mesa", "Dusty Mesa", "Sun-baked sand, boulders and one sweet oasis.", 19, 400_000, 1f, 0f,
            0xFFCFAE6A.toInt(), 0xFFAE8A4E.toInt(), 0xFF3A1E14.toInt(), 0xFF52291B.toInt(), 0xFFC0703E.toInt(), 0,
            roof = 0, wall = 0, features = listOf(Feature.Lake(0.3f, 0.75f, 3f), Feature.Rocks(0.09f))),
        SiteDef("bayou", "Bayou Bend", "Muggy swamp with a lazy river. Plots by water never dry out.", 23, 1_200_000, 0f, 1f,
            0xFF5C8C3A.toInt(), 0xFF3C6630.toInt(), 0xFF102624.toInt(), 0xFF173A36.toInt(), 0xFF3E8C7E.toInt(), 4,
            roof = 4, wall = 4, features = listOf(Feature.River(true, 0.62f, 2.5f), Feature.Lake(0.2f, 0.25f, 3.5f),
                Feature.Lake(0.8f, 0.2f, 2.5f), Feature.Lake(0.9f, 0.9f, 3f))),
        SiteDef("coast", "Sunset Coast", "Ocean on two sides and a sky on fire every evening.", 27, 4_000_000, 1f, 1f,
            0xFF86BE52.toInt(), 0xFF5E9440.toInt(), 0xFF3A1A22.toInt(), 0xFF52242E.toInt(), 0xFFE07A5A.toInt(), 2,
            roof = 1, wall = 1, features = listOf(Feature.Coast(0, 7), Feature.Coast(2, 6), Feature.Rocks(0.02f))),
        SiteDef("frost", "Frostpeak", "High, cold and clean. Pines, snow and a frozen tarn.", 31, 10_000_000, 0.5f, 0f,
            0xFFDCE6EC.toInt(), 0xFFB4C4D0.toInt(), 0xFF141E2C.toInt(), 0xFF1E2C40.toInt(), 0xFF7AA8D0.toInt(), 4, pines = true,
            roof = 1, wall = 2, features = listOf(Feature.Lake(0.2f, 0.85f, 3.5f), Feature.Rocks(0.06f))),
        SiteDef("emerald", "Emerald Triangle", "Legendary soil. A river runs right through it.", 35, 30_000_000, 0f, 0.5f,
            0xFF3FA84A.toInt(), 0xFF2A7436.toInt(), 0xFF0E2418.toInt(), 0xFF153626.toInt(), 0xFF3CC07A.toInt(), 3,
            roof = 2, wall = 1, features = listOf(Feature.River(false, 0.25f, 2f), Feature.Lake(0.8f, 0.7f, 2.5f))),
        SiteDef("volcano", "Volcano Isle", "Black ash, warm ground, sea all round.", 40, 80_000_000, 0.5f, 0.5f,
            0xFF6E645A.toInt(), 0xFF4A423E.toInt(), 0xFF2A0E0C.toInt(), 0xFF3E1612.toInt(), 0xFFD04A2A.toInt(), 1,
            roof = 4, wall = 5, features = listOf(Feature.Coast(0, 4), Feature.Coast(1, 4), Feature.Coast(2, 4), Feature.Coast(3, 4),
                Feature.Rocks(0.08f))),
        SiteDef("neon", "Neon Valley", "Glowing lakes and grass with a funny tint.", 45, 200_000_000, 1f, 0.5f,
            0xFF5FA87A.toInt(), 0xFF3A6E5A.toInt(), 0xFF1E1028.toInt(), 0xFF2C1840.toInt(), 0xFFC060E8.toInt(), 2,
            roof = 3, wall = 3, features = listOf(Feature.Lake(0.15f, 0.2f, 3f), Feature.Lake(0.25f, 0.8f, 3.5f),
                Feature.Lake(0.75f, 0.1f, 2.5f))),
        SiteDef("highland", "Highland Terraces", "Rocky slopes with a stream off the peaks.", 51, 600_000_000, 0.5f, 1f,
            0xFF8AB04E.toInt(), 0xFF5E7E3A.toInt(), 0xFF1A1E24.toInt(), 0xFF262C36.toInt(), 0xFF8A9AB0.toInt(), 3, pines = true,
            roof = 4, wall = 2, features = listOf(Feature.River(true, 0.15f, 1.5f), Feature.Rocks(0.1f))),
        SiteDef("crater", "Cosmic Crater", "Something fell here long ago. The plants like it.", 58, 1_800_000_000, 0.5f, 0.5f,
            0xFF7A70A8.toInt(), 0xFF4E4878.toInt(), 0xFF0E1030.toInt(), 0xFF181C48.toInt(), 0xFF7A88F0.toInt(), 1,
            roof = 3, wall = 2, features = listOf(Feature.Rim(13f), Feature.Lake(0.08f, 0.1f, 2.5f), Feature.Lake(0.92f, 0.9f, 2.5f))),
    )

    /** Holiday farms, one per holiday, priced in its tokens. They aren't on the realtor's list. */
    val holidays: List<SiteDef> = listOf(
        SiteDef("spooky", "Spooky Hollow", "Dead trees, a foggy pond and something moving in the pumpkins.", 1, 500, 0.5f, 0.5f,
            0xFF5A6A3A.toInt(), 0xFF2E3A2A.toInt(), 0xFF1A1020.toInt(), 0xFF2A1A34.toInt(), 0xFFFF8A1E.toInt(), 4,
            roof = 3, wall = 5, features = listOf(Feature.Lake(0.2f, 0.75f, 3f), Feature.Rocks(0.05f)), holiday = "halloween"),
        SiteDef("northpole", "North Pole Acres", "Snow, candy-striped fences and an elf union.", 1, 500, 0.5f, 0f,
            0xFFEAF2F6.toInt(), 0xFFC4D4DE.toInt(), 0xFF2A0E12.toInt(), 0xFF3E141A.toInt(), 0xFFE03A3A.toInt(), 5, pines = true,
            roof = 0, wall = 1, features = listOf(Feature.Lake(0.8f, 0.8f, 3f)), holiday = "christmas"),
        SiteDef("timesquare", "Midnight Square", "The party never ends. Neither does the confetti.", 1, 500, 0.5f, 0.5f,
            0xFF5A8A6A.toInt(), 0xFF3A5A4A.toInt(), 0xFF101024.toInt(), 0xFF1A1A3A.toInt(), 0xFFFFD23A.toInt(), 1,
            roof = 4, wall = 1, features = listOf(Feature.Rocks(0.02f)), holiday = "newyear"),
        SiteDef("lovers", "Lovers' Lane", "Pink blossoms over a heart-shaped pond.", 1, 500, 0f, 0.5f,
            0xFF7ABE6A.toInt(), 0xFF5A9A5A.toInt(), 0xFF3A1024.toInt(), 0xFF521A34.toInt(), 0xFFFF6AA0.toInt(), 3,
            roof = 3, wall = 3, features = listOf(Feature.Lake(0.8f, 0.3f, 3.5f)), holiday = "valentine"),
        SiteDef("clover", "Clover Glen", "Forty shades of green and a rainbow most mornings.", 1, 500, 1f, 0.5f,
            0xFF3AB04A.toInt(), 0xFF26803A.toInt(), 0xFF0E2A14.toInt(), 0xFF163A1E.toInt(), 0xFF5AFF6A.toInt(), 3,
            roof = 2, wall = 1, features = listOf(Feature.River(true, 0.2f, 2f), Feature.Rocks(0.04f)), holiday = "stpatrick"),
        SiteDef("highhill", "420 Hill", "The highest hill in the county, in every sense.", 1, 500, 0.5f, 1f,
            0xFF6ABE3A.toInt(), 0xFF4A8A30.toInt(), 0xFF1A2A10.toInt(), 0xFF26401A.toInt(), 0xFF9AFF3A.toInt(), 3,
            roof = 2, wall = 4, features = listOf(Feature.Lake(0.15f, 0.15f, 3f)), holiday = "fourtwenty"),
        SiteDef("meadow", "Easter Meadow", "Pastel flowers, rabbit holes and eggs in odd places.", 1, 500, 0f, 0f,
            0xFF9AD27A.toInt(), 0xFF7AB05A.toInt(), 0xFF3A2A4A.toInt(), 0xFF4E3A60.toInt(), 0xFFC0A0FF.toInt(), 3,
            roof = 3, wall = 3, features = listOf(Feature.Lake(0.75f, 0.8f, 3f)), holiday = "easter"),
        SiteDef("liberty", "Liberty Ranch", "Wide open prairie and fireworks every night.", 1, 500, 1f, 1f,
            0xFFA8B05A.toInt(), 0xFF7E8A42.toInt(), 0xFF101A3A.toInt(), 0xFF1A2A52.toInt(), 0xFFE03A3A.toInt(), 1,
            roof = 1, wall = 1, features = listOf(Feature.River(false, 0.15f, 2f)), holiday = "july4"),
        SiteDef("harvest", "Harvest Homestead", "Golden fields, red barns, a big table.", 1, 500, 0f, 1f,
            0xFFB8A050.toInt(), 0xFF8A7038.toInt(), 0xFF2A1A0E.toInt(), 0xFF3E2814.toInt(), 0xFFE0903A.toInt(), 3,
            roof = 0, wall = 5, features = listOf(Feature.Lake(0.85f, 0.2f, 2.5f), Feature.Rocks(0.03f)), holiday = "thanksgiving"),
    )

    /** Farms bought with Pot Bucks (their price is in Pot Bucks). Not on the realtor's list either. */
    val premium: List<SiteDef> = listOf(
        SiteDef("zion", "Zion Gardens", "Red, gold and green as far as you can see, and a bassline from the hills.", 12, 250, 0.5f, 0.5f,
            0xFF4AB04A.toInt(), 0xFF2E8A3A.toInt(), 0xFF2A1A0E.toInt(), 0xFF3E2814.toInt(), 0xFFFFD23A.toInt(), 4,
            roof = 2, wall = 3, features = listOf(Feature.River(true, 0.75f, 2f), Feature.Rocks(0.03f)), holiday = "bucks"),
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

/** One farm you own: its own land and everything on it. Cash, stash, helpers and the rest are shared. */
class Site(val def: SiteDef) {
    val tiles = Array(Farm.MAX * Farm.MAX) { Tile() }
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
    /** Top-left corner of the owned square. */
    val ox get() = cx?.let { (it - land / 2).coerceIn(0, Farm.MAX - land) } ?: ((Farm.MAX - land) * def.ax).roundToInt()
    val oy get() = cy?.let { (it - land / 2).coerceIn(0, Farm.MAX - land) } ?: ((Farm.MAX - land) * def.ay).roundToInt()
}
