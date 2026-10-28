package dev.m4s4t0.kushacres.model

import kotlin.math.pow

/** The house you live in. Each level holds more stash, earns a little more XP and fits more decor. */
class ShackLevel(val title: String, val level: Int, val cost: Long, val blurb: String)

val SHACKS = listOf(
    ShackLevel("Tin Shack", 1, 0, "Corrugated roof, one chair, big dreams."),
    ShackLevel("Cozy Cabin", 5, 1_500, "Logs, a porch and a hammock."),
    ShackLevel("Cottage", 11, 15_000, "Flower boxes and a proper kitchen."),
    ShackLevel("Farmhouse", 18, 150_000, "Two floors and a basement grow room."),
    ShackLevel("Ranch House", 27, 1_500_000, "Wraparound porch, room for the whole crew."),
    ShackLevel("Kush Mansion", 38, 20_000_000, "Columns, a fountain and a gold-leaf roof."),
    ShackLevel("Hilltop Villa", 45, 120_000_000, "Terracotta, terraces and an infinity pool."),
    ShackLevel("Grow Castle", 53, 600_000_000, "Stone towers, a moat, and grow lights in the dungeon."),
    ShackLevel("Sky Tower", 62, 3_000_000_000, "Glass and steel. The penthouse is a greenhouse."),
    ShackLevel("Kush Palace", 72, 15_000_000_000, "Gold domes you can see from the next county."),
)

/** Roof and wall colours you can paint the shack (ARGB). */
val ROOF_COLORS = intArrayOf(0xFF9A4A3A.toInt(), 0xFF3E5E8A.toInt(), 0xFF3F7A3A.toInt(), 0xFF6A4A8A.toInt(), 0xFF3A3A44.toInt(), 0xFFC8A040.toInt())
val WALL_COLORS = intArrayOf(0xFFB8956A.toInt(), 0xFFE8DCC0.toInt(), 0xFF8AAEC8.toInt(), 0xFFE0A0B0.toInt(), 0xFF9AB87A.toInt(), 0xFF8A6A5A.toInt())

/** What facilities eat besides bud: one per batch. */
enum class Supply(val title: String, val price: Long) {
    PAPERS("Rolling Papers", 10),
    ICE("Ice Bags", 60),
    BUTTER("Butter", 300),
    BUTANE("Butane", 2_000),
    CARTS("Empty Carts", 12_000),
}

/**
 * Buildings that turn bud into goods worth more per gram. A batch takes [grams] × units of one strain plus one
 * [supply], and makes units worth price × grams × [mult] each. Upgrading makes bigger, faster batches.
 */
enum class Facility(
    val title: String, val product: String, val desc: String, val level: Int, val cost: Long,
    val grams: Int, val mult: Double, val minutes: Int, val units: Int, val supply: Supply,
) {
    ROLLER("Rolling Table", "Pre-rolls", "Rolls bud into pre-rolls", 4, 400, 2, 1.5, 4, 5, Supply.PAPERS),
    PRESS("Hash Press", "Hash", "Presses bud into hash", 8, 4_000, 5, 1.9, 20, 4, Supply.ICE),
    KITCHEN("Edibles Kitchen", "Edibles", "Bakes bud into edibles", 13, 30_000, 3, 2.3, 45, 8, Supply.BUTTER),
    LAB("Extraction Lab", "Dabs", "Extracts bud into dabs", 19, 250_000, 6, 2.8, 90, 5, Supply.BUTANE),
    VAPE("Vape Cart Lab", "Vape carts", "Fills vape carts", 27, 2_000_000, 4, 3.4, 150, 8, Supply.CARTS);

    fun units(lvl: Int): Int = (units * (1 + 0.5 * (lvl - 1))).toInt()
    fun millis(lvl: Int): Long = (minutes * 60_000L / (1 + 0.2 * (lvl - 1))).toLong()
    fun upgradeCost(lvl: Int): Long = nice(cost * 0.6 * 2.0.pow(lvl))

    companion object {
        const val MAX_LEVEL = 5
        fun of(id: String?) = entries.firstOrNull { it.name == id }
    }
}

/**
 * Things to put around the farm. Neighbour effects reach the eight squares around and stack up to two of a kind;
 * farm-wide effects count once per kind. Paths are decor too, but don't count towards the shack's decor limit.
 */
enum class Decor(
    val title: String, val desc: String, val level: Int, val cost: Long, val black: Boolean = false, val path: Boolean = false,
    /** Holiday decor: bought with that holiday's tokens, then kept like black-market decor. Its effect is [perk] × [amt]. */
    val holiday: String? = null, val perk: Perk? = null, val amt: Double = 0.0,
    /** Pot Bucks decor: bought from the Bucks shop for this many Pot Bucks, then kept like holiday decor. */
    val bucks: Long = 0,
) {
    STONE("Stone Path", "Just looks nice", 1, 5, path = true),
    BRICK("Brick Path", "Just looks nice", 3, 15, path = true),
    FLOWERS("Flower Bed", "Just looks nice", 1, 40),
    BENCH("Bench", "Somewhere to sit and admire", 1, 60),
    GNOME("Garden Gnome", "Neighbours grow 5% faster", 2, 150),
    SCARECROW("Scarecrow", "Keeps pests off neighbours", 3, 300),
    BARREL("Rain Barrel", "Waters the 4 squares beside it", 4, 800),
    FLAMINGO("Pink Flamingo", "Everything sells 3% higher", 5, 1_000),
    FLAG("Rasta Flag", "+5% XP", 7, 2_500),
    COMPOST("Compost Heap", "Neighbours yield 10% more", 9, 6_000),
    BEEHIVE("Beehive", "Neighbours +3% golden chance", 12, 15_000),
    TIKI("Tiki Torch", "Neighbours grow 10% faster", 14, 25_000),
    DOG("Guard Dog", "Raids are rarer and take less", 16, 60_000),
    FROG("Zen Frog Statue", "+8% XP", 20, 200_000),
    VAN("Hippie Van", "Everything sells 6% higher", 24, 600_000),
    WINDMILL("Windmill", "Neighbours grow 20% faster", 30, 1_500_000),
    KOI("Koi Fountain", "+10% XP", 34, 4_000_000),
    STATUE("Golden Leaf Statue", "Everything sells 8% higher", 40, 15_000_000),
    GREENHOUSE("Mini Greenhouse", "Neighbours yield 20% more", 46, 50_000_000),
    TOTEM("Lucky Totem", "Neighbours +8% golden chance", 52, 150_000_000),
    NEON("Neon Leaf Sign", "Everything sells 5% higher", 6, 0, black = true),
    LAVA("Giant Lava Lamp", "Neighbours grow 15% faster", 6, 0, black = true),
    ALIEN("Alien Statue", "Neighbours +6% golden chance", 6, 0, black = true),
    DISCO("Disco Ball", "+10% XP", 30, 0, black = true),
    UFO("Crashed UFO", "Neighbours grow 25% faster", 40, 0, black = true),
    CRYSTAL("Grow Crystal", "Neighbours yield 25% more", 50, 0, black = true),
    PUMPKIN("Jack-o'-Lantern", "Neighbours grow 15% faster", 1, 0, black = true, holiday = "halloween", perk = Perk.GROW, amt = 0.15),
    TOMBSTONE("Spooky Tombstone", "+6% XP", 1, 0, black = true, holiday = "halloween", perk = Perk.XP, amt = 0.06),
    CAULDRON("Witch's Cauldron", "Neighbours yield 15% more", 1, 0, black = true, holiday = "halloween", perk = Perk.YIELD, amt = 0.15),
    XMASTREE("Christmas Tree", "Everything sells 5% higher", 1, 0, black = true, holiday = "christmas", perk = Perk.SELL, amt = 0.05),
    SNOWMAN("Snowman", "Neighbours grow 15% faster", 1, 0, black = true, holiday = "christmas", perk = Perk.GROW, amt = 0.15),
    PRESENTS("Pile of Presents", "Neighbours +5% golden chance", 1, 0, black = true, holiday = "christmas", perk = Perk.GOLD, amt = 0.05),
    BALLDROP("Midnight Ball", "+6% XP", 1, 0, black = true, holiday = "newyear", perk = Perk.XP, amt = 0.06),
    CONFETTI("Confetti Cannon", "Everything sells 4% higher", 1, 0, black = true, holiday = "newyear", perk = Perk.SELL, amt = 0.04),
    HEARTARCH("Heart Arch", "Everything sells 5% higher", 1, 0, black = true, holiday = "valentine", perk = Perk.SELL, amt = 0.05),
    ROSES("Rose Bush", "Neighbours +5% golden chance", 1, 0, black = true, holiday = "valentine", perk = Perk.GOLD, amt = 0.05),
    RAINBOW("Rainbow Arch", "Neighbours +6% golden chance", 1, 0, black = true, holiday = "stpatrick", perk = Perk.GOLD, amt = 0.06),
    CLOVER("Lucky Clover Patch", "Neighbours grow 12% faster", 1, 0, black = true, holiday = "stpatrick", perk = Perk.GROW, amt = 0.12),
    JOINT("Giant Joint", "+8% XP", 1, 0, black = true, holiday = "fourtwenty", perk = Perk.XP, amt = 0.08),
    BONG("Bong Statue", "Neighbours yield 15% more", 1, 0, black = true, holiday = "fourtwenty", perk = Perk.YIELD, amt = 0.15),
    EGG("Giant Easter Egg", "Neighbours +5% golden chance", 1, 0, black = true, holiday = "easter", perk = Perk.GOLD, amt = 0.05),
    BUNNYSTATUE("Bunny Topiary", "Neighbours grow 12% faster", 1, 0, black = true, holiday = "easter", perk = Perk.GROW, amt = 0.12),
    FLAGPOLE("Flagpole", "Everything sells 5% higher", 1, 0, black = true, holiday = "july4", perk = Perk.SELL, amt = 0.05),
    GRILL("BBQ Grill", "+6% XP", 1, 0, black = true, holiday = "july4", perk = Perk.XP, amt = 0.06),
    CORNUCOPIA("Cornucopia", "Neighbours yield 15% more", 1, 0, black = true, holiday = "thanksgiving", perk = Perk.YIELD, amt = 0.15),
    HAYBALE("Hay Bale", "Neighbours grow 12% faster", 1, 0, black = true, holiday = "thanksgiving", perk = Perk.GROW, amt = 0.12),
    SOUNDSYSTEM("Reggae Sound System", "+6% XP", 1, 0, black = true, perk = Perk.XP, amt = 0.06, bucks = 30),
    HAMMOCK("Palm Hammock", "Neighbours grow 12% faster", 1, 0, black = true, perk = Perk.GROW, amt = 0.12, bucks = 25),
    STEELDRUM("Steel Drum", "Neighbours +5% golden chance", 1, 0, black = true, perk = Perk.GOLD, amt = 0.05, bucks = 30),
    MURAL("Rasta Mural", "Neighbours yield 15% more", 1, 0, black = true, perk = Perk.YIELD, amt = 0.15, bucks = 35),
    LION("Lion of Zion", "Everything sells 4% higher", 1, 0, black = true, perk = Perk.SELL, amt = 0.04, bucks = 40),
    SHROOMS("Glowing Mushrooms", "Neighbours grow 15% faster", 1, 0, black = true, perk = Perk.GROW, amt = 0.15, bucks = 45),
    BUDDHA("Chill Buddha", "+8% XP", 1, 0, black = true, perk = Perk.XP, amt = 0.08, bucks = 50),
    TIKIBAR("Tiki Bar", "Everything sells 5% higher", 1, 0, black = true, perk = Perk.SELL, amt = 0.05, bucks = 60);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
        /** Holiday decor with neighbour or farm-wide effects of each kind. */
        val perks: Map<Perk, List<Decor>> = entries.filter { it.perk != null }.groupBy { it.perk!! }
        /** Decor sold for Pot Bucks. */
        val bucksShop = entries.filter { it.bucks > 0 }
    }
}

/** What a holiday decoration does: GROW, YIELD and GOLD reach the neighbours; XP and SELL are farm-wide. */
enum class Perk { GROW, YIELD, GOLD, XP, SELL }

enum class Helper(val title: String, val job: String, val level: Int, val cost: Long, val shack: Int) {
    RUNNER("Runner", "Sells spare bud and goods in town every so often, even while you're away", 8, 2_500, 1),
    FETCHER("Fetcher", "Keeps facility supplies stocked from Violet's, for a tip", 10, 5_000, 1),
    FARMHAND("Farmhand", "Comes round to pick what's ready, shoo pests and scrape mould, even while you're away. Train them to go faster", 12, 6_000, 0),
    COOK("Cook", "Collects facility batches and starts the same again", 16, 40_000, 2),
    LOOKOUT("Lookout", "Spots raids early so they don't cost you a thing", 20, 120_000, 3),
    MANAGER("Grow Manager", "Replants the same strain the moment it's picked", 22, 150_000, 0);

    /** What rank [r] (1 to [RANKS]) does, in a few words for the hire card. */
    fun rankDesc(r: Int): String {
        val i = r.coerceIn(1, RANKS) - 1
        return when (this) {
            RUNNER -> "A trip every ${RUNNER_MIN[i]} min"
            FETCHER -> "Stocks up to ${FETCH_STOCK[i]}, ${FETCH_TIP[i]}% tip"
            FARMHAND -> "Every ${FARMHAND_SECS[i]}s, picks ${FARMHAND_PICKS[i]}" + if (FARMHAND_WAIT[i] > 0) ". Leaves ripe plants ${FARMHAND_WAIT[i]} min for you" else ""
            COOK -> "Every ${COOK_SECS[i]}s, tends " + if (COOK_TENDS[i] >= 99) "every kitchen" else "${COOK_TENDS[i]} kitchen${if (COOK_TENDS[i] == 1) "" else "s"}"
            LOOKOUT -> "Spots ${LOOKOUT_SPOTS[i]}% of raids"
            MANAGER -> if (MANAGER_OFF[i] == 0) "Replants at full seed price" else "Replants ${MANAGER_OFF[i]}% off the seed price"
        }
    }

    /** Training from rank [r] to the next: dearer each time. */
    fun rankCost(r: Int): Long = (cost * 3.0.pow(r)).toLong()
    /** The level a helper needs to reach rank [r]. */
    fun rankLevel(r: Int) = level + 4 * (r - 1)

    companion object {
        const val RANKS = 5
        val RANK_NAMES = listOf("Green", "Steady", "Seasoned", "Pro", "Legend")
        // Everything a helper does by rank, worst first: a new hire is slow, so you still get to work the farm yourself.
        val RUNNER_MIN = intArrayOf(12, 9, 7, 6, 5)
        val FETCH_STOCK = intArrayOf(8, 10, 12, 14, 15)
        val FETCH_TIP = intArrayOf(25, 20, 15, 10, 5)
        val FARMHAND_SECS = intArrayOf(120, 75, 45, 25, 12)
        val FARMHAND_PICKS = intArrayOf(1, 2, 3, 5, 8)
        val FARMHAND_WAIT = intArrayOf(5, 3, 2, 1, 0)
        val COOK_SECS = intArrayOf(180, 120, 90, 60, 30)
        val COOK_TENDS = intArrayOf(1, 2, 3, 5, 99)
        val LOOKOUT_SPOTS = intArrayOf(40, 55, 70, 85, 100)
        val MANAGER_OFF = intArrayOf(0, 5, 10, 15, 20)
    }
}

/**
 * Work buildings: [size]×[size] squares each, they speed up the plots or facilities within [reach] squares of them. A
 * worker staffing one doubles its effect ([staffed]); the bunkhouse makes room for two more workers on its farm instead.
 */
enum class WorkHouse(val title: String, val desc: String, val staffed: String, val level: Int, val cost: Long, val reach: Int) {
    STORE("Lab Storefront", "Sells your lab-bred bud by itself, even while you're away. Upgrades from a stand to a flagship store", "Sells 50% faster", 8, 10_000, 0),
    TOWER("Water Tower", "Keeps plots within 2 squares wet", "Reach 3 squares", 9, 8_000, 2),
    GROW("Grow House", "Plots within 2 squares grow 20% faster", "40% faster", 11, 15_000, 2),
    BUNK("Bunkhouse", "Room for 2 more workers on this farm", "No worker needed", 13, 20_000, 0),
    TRIM("Trim Shed", "Plots within 2 squares yield 15% more", "30% more", 16, 60_000, 2),
    DRYING("Drying Barn", "Facilities within 2 squares cook 25% faster", "50% faster", 20, 250_000, 2),
    TOOLS("Tool Shed", "Runner goes every 4 minutes instead of 6", "Every 3 minutes", 24, 900_000, 0);

    /** The effect strength: 1 unstaffed, 2 staffed. */
    fun power(staffed: Boolean) = if (staffed) 2 else 1
    /** The water tower stands on one square; the rest take 2×2. */
    val size get() = if (this == TOWER) 1 else 2

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
        /** Each farm can hold this many of one kind. */
        const val MAX_EACH = 6
    }
}

/** Big rigs parked on a farm: each covers 2×2 squares and does a job for that farm. One of each per farm. */
enum class Vehicle(val title: String, val desc: String, val level: Int, val cost: Long, val minutes: Int) {
    RV("RV", "Beds for 2 more workers here, and 5% more XP", 18, 400_000, 0),
    TRUCK("Food Truck", "Sells all your cooked goods every 5 minutes, at 10% more", 22, 1_500_000, 5),
    TRACTOR("Tractor", "Every 10 minutes, replants each empty plot here with what grew there last", 26, 6_000_000, 10),
    VAN("Delivery Van", "Sends out a ready order on delivery every 5 minutes", 30, 20_000_000, 5);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
    }
}

/** Specialist workers hired for one farm. They sleep in the same beds as ordinary workers. */
enum class Crew(val title: String, val desc: String, val level: Int, val max: Int) {
    TRIMMER("Trimmer", "Harvests on this farm yield 8% more each", 14, 3),
    GUARD("Guard", "Clears bugs and mould on this farm as soon as they show", 17, 2),
    DRIVER("Driver", "Vehicles on this farm do their job 40% sooner", 20, 1),
    BOTANIST("Botanist", "Plants on this farm grow 10% faster each", 24, 2),
    CHEMIST("Lab Chemist", "Facilities on this farm cook 30% faster, and the Lab researches 50% faster", 28, 1),
}

/** Timed boosts from events and the black market. */
enum class Buff(val title: String) {
    RUSH("Rush hour: prices ×1.5"),
    LUCKY("Lucky streak: 25% golden"),
    HIPPIES("Hippie festival: goods ×2"),
    VIBES("Good vibes: growth ×2"),
    JUICE("Grow Juice: growth ×1.5"),
    DUST("Lucky Dust: +15% golden"),
    TIP("Street Tip: prices ×1.25"),
    FOCUS("Smart Brownie: XP ×1.5"),
    FESTIVAL("Hippie festival: prices ×1.5"),
    HARVEST_MOON("Harvest moon: yield ×1.5"),
    TOURISTS("Tourist bus: orders pay ×1.5"),
    HEATWAVE("Heatwave: water dries ×2 faster"),
    SCANNER("Police scanner: no raids"),
    /** From smoking your own stash, one per strain trait (see [Sesh]). */
    HIGH_GROW("Green thumb high: growth ×1.25"),
    HIGH_YIELD("Munchies: yield ×1.2"),
    HIGH_GOLD("Cosmic high: +10% golden"),
    HIGH_XP("Big thoughts: XP ×1.3"),
    HIGH_PRICE("Silver tongue: prices ×1.15"),
}

enum class EventKind {
    RAID, PESTS, RAIN, RUSH, LUCKY, HIPPIES, VIBES, STRANGER, CELEB, TRADER,
    /** Newer ones. */
    METEOR, TOURISTS, HEATWAVE, MOON, GRANDMA, STORM, POTBUCKS, HOLIDAY,
}

/** A random event waiting on the player. [amount] is a bribe, a price or a payout depending on the kind. */
class Happening(val kind: EventKind, val amount: Long = 0, val strain: String? = null, val grams: Int = 0, val plots: Int = 0)

/** What Pot Bucks buy. */
enum class BucksItem(val title: String, val desc: String, val price: Long, val buff: Buff? = null, val minutes: Int = 0) {
    JUICE("Grow Juice", "Everything grows ×1.5 for 30 min", 5, buff = Buff.JUICE, minutes = 30),
    FOCUS("Smart Brownie", "XP ×1.5 for 30 min", 10, buff = Buff.FOCUS, minutes = 30),
    VIBES("Good Vibes", "Growth ×2 for 20 min", 15, buff = Buff.VIBES, minutes = 20),
    RUSH("Rush Hour", "Prices ×1.5 for 20 min", 15, buff = Buff.RUSH, minutes = 20),
    ENVELOPE("Bribe Envelope", "Makes the next raid go away", 8),
    CASH("Cash bundle", "A stack of cash that grows with your level", 12),
    SEEDS("Exotic seed pack", "5 of the best exotic seeds you can grow", 25),
    GEM("Kush Gem", "One gem, for building specials", 80),
}

/**
 * Power for a farm. Lamps and every facility past the rolling table run on it; each farm has a little from the
 * mains ([GRID]) and generators add [watts]. When a farm uses more than it makes, its lamps go dark and its
 * facilities won't start new batches.
 */
enum class Generator(val title: String, val desc: String, val level: Int, val cost: Long, val watts: Int) {
    GAS("Gas Generator", "Small, loud and smelly", 8, 2_000, 10),
    DIESEL("Diesel Generator", "Runs all night on the farm truck's fuel", 16, 60_000, 30),
    SOLAR("Solar Array", "Free power from the sun", 24, 1_500_000, 80),
    WIND("Wind Turbine", "Big blades, lots of power", 32, 25_000_000, 180),
    STATION("Power Station", "A small power plant of your own", 42, 400_000_000, 450),
    FUSION("Fusion Core", "Don't ask where it came from", 55, 15_000_000_000, 1200);

    companion object {
        /** Free power on every farm, from the mains. */
        const val GRID = 4
        const val LAMP = 1
        fun of(id: String?) = entries.firstOrNull { it.name == id }
        /** What a facility of level [lvl] draws. The rolling table works by hand. */
        fun draw(f: Facility, lvl: Int): Int {
            val base = when (f) { Facility.ROLLER -> 0; Facility.PRESS -> 3; Facility.KITCHEN -> 5; Facility.LAB -> 8; Facility.VAPE -> 12 }
            return (base * (1 + 0.25 * (lvl - 1))).toInt()
        }
    }
}

/**
 * Special buildings: paid for in gems plus cash, one of each per farm. [local] ones help the farm they stand on;
 * the others work everywhere but only count once, wherever you build them.
 */
enum class Special(val title: String, val desc: String, val level: Int, val gems: Long, val cash: Long, val local: Boolean) {
    VAULT("Mother Plant Vault", "Plants on this farm grow 30% faster", 10, 3, 20_000, true),
    WELL("Deep Well", "Every plot on this farm stays wet", 12, 3, 50_000, true),
    RADIO("Pirate Radio", "Orders pay 25% more and come twice as fast", 14, 4, 120_000, false),
    DEPOT("Delivery Depot", "Deliveries arrive in half the time and pay 20% more", 16, 5, 300_000, false),
    TERRACE("Hydro Terraces", "Plants on this farm yield 25% more", 18, 5, 600_000, true),
    DISPENSARY("Dispensary", "Everything sells 15% higher", 20, 8, 1_500_000, false),
    DOME("Sun Dome", "+8% golden harvests on this farm", 22, 6, 3_000_000, true),
    CURING("Curing Vault", "Pre-rolls, hash and the rest sell 25% higher", 24, 8, 6_000_000, false),
    FACTORY("Goods Factory", "Facilities on this farm work 40% faster", 26, 8, 12_000_000, true),
    SUBSTATION("Substation", "+150 power on this farm", 28, 6, 25_000_000, true),
    ACADEMY("Grow Academy", "+25% XP", 30, 10, 60_000_000, false),
    EXCHANGE("Trade Exchange", "The market bounces back three times as fast", 33, 10, 150_000_000, false),
    TEMPLE("Rasta Temple", "No more raids, and plants everywhere grow 10% faster", 36, 12, 400_000_000, false),
    MINE("Gem Mine", "Digs up a gem every 12 hours", 40, 15, 1_000_000_000, false),
    MONUMENT("Golden Kush Monument", "Everything sells 25% higher and +10% XP", 48, 25, 10_000_000_000, false);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
        const val MINE_MILLIS = 12 * 3_600_000L
    }
}

/** Ground colours for sale: grass inside your land and the soil in the plots. Null means the farm's own. */
class Ground(val title: String, val color: Int?)
val GRASS_COLORS = listOf(
    Ground("Natural", null), Ground("Lush", 0xFF3FA040.toInt()), Ground("Lime", 0xFF8ACB3A.toInt()), Ground("Teal", 0xFF2E9A80.toInt()),
    Ground("Autumn", 0xFFC89A3A.toInt()), Ground("Frost", 0xFFB8DCD4.toInt()), Ground("Purple Haze", 0xFF8A5AB0.toInt()),
    Ground("Neon", 0xFF3AE07A.toInt()), Ground("Ash", 0xFF7A8070.toInt()), Ground("Blossom", 0xFFD88AB8.toInt()), Ground("Sand", 0xFFD8C08A.toInt()),
)
val DIRT_COLORS = listOf(
    Ground("Natural", null), Ground("Dark loam", 0xFF6A4A2E.toInt()), Ground("Red clay", 0xFFB0583A.toInt()), Ground("Desert", 0xFFD0A86A.toInt()),
    Ground("Ash", 0xFF707076.toInt()), Ground("Purple", 0xFF8A5A9A.toInt()), Ground("Gold", 0xFFC8A040.toInt()), Ground("Black soil", 0xFF3E322A.toInt()),
)
/** What a ground colour change costs in Pot Bucks (the cash price grows with your level). */
const val GROUND_BUCKS = 8L

/** Black-market goods. Seed packs give [EXOTIC_PACK] seeds; prices scale with your level except for seeds. */
/**
 * The back alley shelf. [staple]s sit in the back room and never sell out, but each one bought costs more until the
 * stock turns over; the rest rotate on the shelf, one of each per rotation.
 */
enum class BmItem(val title: String, val desc: String, val level: Int, val base: Double, val strain: String? = null, val decor: Decor? = null, val buff: Buff? = null, val minutes: Int = 0, val site: String? = null, val staple: Boolean = false) {
    GHOST("Ghost Train seeds", "5 exotic seeds. 40 min, huge yield", 6, 2_500.0, strain = "ghost"),
    DRAGON("Dragon's Breath seeds", "5 exotic seeds. 3 h, fire bud", 12, 40_000.0, strain = "dragon"),
    UNICORN("Unicorn Poop seeds", "5 exotic seeds. 8 h, sparkles", 20, 280_000.0, strain = "unicorn"),
    MOONSHINE("Moonshine Haze seeds", "5 exotic seeds. 12 h, moonlit", 28, 1_300_000.0, strain = "moonshine"),
    KOSMIC("Kosmic Kush seeds", "5 exotic seeds. 16 h, out of this world", 36, 6_000_000.0, strain = "kosmic"),
    JUICE("Grow Juice", "Everything grows ×1.5 for 30 min", 6, 40.0, buff = Buff.JUICE, minutes = 30),
    DUST("Lucky Dust", "+15% golden harvests for 15 min", 6, 50.0, buff = Buff.DUST, minutes = 15),
    TIP("Street Tip", "Prices ×1.25 for 30 min", 6, 45.0, buff = Buff.TIP, minutes = 30),
    FOCUS("Smart Brownie", "XP ×1.5 for 30 min", 10, 60.0, buff = Buff.FOCUS, minutes = 30),
    SCANNER("Police Scanner", "No raids for 2 h", 8, 70.0, buff = Buff.SCANNER, minutes = 120),
    MOONOIL("Moon Oil", "Harvests yield ×1.5 for 20 min", 14, 90.0, buff = Buff.HARVEST_MOON, minutes = 20),
    TICKET("Fake Bus Tickets", "A tour bus shows up: orders pay ×1.5 for 30 min", 12, 70.0, buff = Buff.TOURISTS, minutes = 30),
    BLOOM("Bloom Booster", "Every growing plant on this farm jumps 20 min ahead", 16, 80.0),
    GEM("Hot Gem", "1 Kush Gem, no questions asked", 10, 500.0),
    CRATE("Mystery Crate", "Could be anything. Probably fine", 12, 60.0),
    ENVELOPE("Bribe Envelope", "Makes the next raid go away", 6, 30.0, staple = true),
    BUGBOMB("Bug Bomb", "Clears every pest and mould on this farm", 6, 25.0, staple = true),
    SEEDER("Cloud Seeder", "Waters every plot on this farm", 8, 20.0, staple = true),
    NEON("Neon Leaf Sign", "Decor: everything sells 5% higher", 6, 300.0, decor = Decor.NEON),
    LAVA("Giant Lava Lamp", "Decor: neighbours grow 15% faster", 6, 500.0, decor = Decor.LAVA),
    ALIEN("Alien Statue", "Decor: neighbours +6% golden chance", 6, 900.0, decor = Decor.ALIEN),
    DISCO("Disco Ball", "Decor: +10% XP", 30, 600.0, decor = Decor.DISCO),
    UFO("Crashed UFO", "Decor: neighbours grow 25% faster", 40, 300.0, decor = Decor.UFO),
    CRYSTAL("Grow Crystal", "Decor: neighbours yield 25% more", 50, 120.0, decor = Decor.CRYSTAL),
    // Town specials: always first on their own town's shelf, and the seeds only grow on that farm.
    BIGFOOT("Bigfoot Kush seeds", "Local special. Only grows on Redwood Hollow", 15, 0.0, strain = "bigfoot", site = "redwood"),
    RATTLER("Rattlesnake Rush seeds", "Local special. Only grows on Dusty Mesa", 19, 0.0, strain = "rattler", site = "mesa"),
    GATOR("Gator Bite seeds", "Local special. Only grows on Bayou Bend", 23, 0.0, strain = "gatorbite", site = "bayou"),
    MERMAID("Mermaid Tears seeds", "Local special. Only grows on Sunset Coast", 27, 0.0, strain = "mermaid", site = "coast"),
    VORTEX("Polar Vortex seeds", "Local special. Only grows on Frostpeak", 31, 0.0, strain = "polarvortex", site = "frost"),
    TRUFFLE("Triangle Truffle seeds", "Local special. Only grows on Emerald Triangle", 35, 0.0, strain = "truffle", site = "emerald"),
    PELE("Pele's Fire seeds", "Local special. Only grows on Volcano Isle", 40, 0.0, strain = "pele", site = "volcano"),
    GLOWSTICK("Glowstick OG seeds", "Local special. Only grows on Neon Valley", 45, 0.0, strain = "glowstick", site = "neon"),
    TREBUCHET("Trebuchet Thunder seeds", "Local special. Only grows on Fort 20", 48, 0.0, strain = "trebuchet", site = "fort20"),
    NESSIE("Loch Ness Haze seeds", "Local special. Only grows on Highland Terraces", 51, 0.0, strain = "nessie", site = "highland"),
    ABDUCTION("Alien Abduction seeds", "Local special. Only grows on Cosmic Crater", 58, 0.0, strain = "abduction", site = "crater");

    /** Town specials cost about a third of what a pack grows into at full price. */
    fun price(playerLevel: Int): Long = when {
        site != null && strain != null -> Strains[strain]?.let { nice(EXOTIC_PACK * it.yield * it.price / 3.0) } ?: 0L
        strain != null -> nice(base)
        else -> nice(base * 1.22.pow(playerLevel))
    }

    /** A back-room staple after [bought] of them this rotation. */
    fun price(playerLevel: Int, bought: Int): Long = nice(price(playerLevel) * STAPLE_STEP.pow(bought))

    companion object {
        const val EXOTIC_PACK = 5
        /** Each back-room staple bought makes the next one cost this much more. */
        const val STAPLE_STEP = 1.5
        /** How far a Bloom Booster pushes growing plants, in seconds. */
        const val BLOOM_SECONDS = 20 * 60
    }
}
