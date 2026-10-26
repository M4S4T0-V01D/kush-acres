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
    HAYBALE("Hay Bale", "Neighbours grow 12% faster", 1, 0, black = true, holiday = "thanksgiving", perk = Perk.GROW, amt = 0.12);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
        /** Holiday decor with neighbour or farm-wide effects of each kind. */
        val perks: Map<Perk, List<Decor>> = entries.filter { it.perk != null }.groupBy { it.perk!! }
    }
}

/** What a holiday decoration does: GROW, YIELD and GOLD reach the neighbours; XP and SELL are farm-wide. */
enum class Perk { GROW, YIELD, GOLD, XP, SELL }

enum class Helper(val title: String, val job: String, val level: Int, val cost: Long, val shack: Int) {
    RUNNER("Runner", "Sells spare bud and goods in town every 6 minutes, even while you're away", 8, 2_500, 1),
    FETCHER("Fetcher", "Keeps facility supplies stocked from Violet's (10% tip)", 10, 5_000, 1),
    FARMHAND("Farmhand", "Picks everything ready and shoos pests, even while you're away", 12, 6_000, 0),
    COOK("Cook", "Collects facility batches and starts the same again", 16, 40_000, 2),
    LOOKOUT("Lookout", "Spots raids early so they never cost you a thing", 20, 120_000, 3),
    MANAGER("Grow Manager", "Replants the same strain the moment it's picked", 22, 150_000, 0),
}

/**
 * Work buildings: one square each, they speed up the plots or facilities within [reach] squares. A worker staffing
 * one doubles its effect ([staffed]); the bunkhouse makes room for two more workers on its farm instead.
 */
enum class WorkHouse(val title: String, val desc: String, val staffed: String, val level: Int, val cost: Long, val reach: Int) {
    TOWER("Water Tower", "Keeps plots within 2 squares wet", "Reach 3 squares", 9, 8_000, 2),
    GROW("Grow House", "Plots within 2 squares grow 20% faster", "40% faster", 11, 15_000, 2),
    BUNK("Bunkhouse", "Room for 2 more workers on this farm", "No worker needed", 13, 20_000, 0),
    TRIM("Trim Shed", "Plots within 2 squares yield 15% more", "30% more", 16, 60_000, 2),
    DRYING("Drying Barn", "Facilities within 2 squares cook 25% faster", "50% faster", 20, 250_000, 2),
    TOOLS("Tool Shed", "Runner goes every 4 minutes instead of 6", "Every 3 minutes", 24, 900_000, 0);

    /** The effect strength: 1 unstaffed, 2 staffed. */
    fun power(staffed: Boolean) = if (staffed) 2 else 1

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.name == id }
        /** Each farm can hold this many of one kind. */
        const val MAX_EACH = 6
    }
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
}

/** Black-market goods. Seed packs give [EXOTIC_PACK] seeds; prices scale with your level except for seeds. */
enum class BmItem(val title: String, val desc: String, val level: Int, val base: Double, val strain: String? = null, val decor: Decor? = null, val buff: Buff? = null, val minutes: Int = 0) {
    GHOST("Ghost Train seeds", "5 exotic seeds. 40 min, huge yield", 6, 2_500.0, strain = "ghost"),
    DRAGON("Dragon's Breath seeds", "5 exotic seeds. 3 h, fire bud", 12, 40_000.0, strain = "dragon"),
    UNICORN("Unicorn Poop seeds", "5 exotic seeds. 8 h, sparkles", 20, 280_000.0, strain = "unicorn"),
    MOONSHINE("Moonshine Haze seeds", "5 exotic seeds. 12 h, moonlit", 28, 1_300_000.0, strain = "moonshine"),
    KOSMIC("Kosmic Kush seeds", "5 exotic seeds. 16 h, out of this world", 36, 6_000_000.0, strain = "kosmic"),
    JUICE("Grow Juice", "Everything grows ×1.5 for 30 min", 6, 40.0, buff = Buff.JUICE, minutes = 30),
    DUST("Lucky Dust", "+15% golden harvests for 15 min", 6, 50.0, buff = Buff.DUST, minutes = 15),
    TIP("Street Tip", "Prices ×1.25 for 30 min", 6, 45.0, buff = Buff.TIP, minutes = 30),
    FOCUS("Smart Brownie", "XP ×1.5 for 30 min", 10, 60.0, buff = Buff.FOCUS, minutes = 30),
    ENVELOPE("Bribe Envelope", "Makes the next raid go away", 6, 30.0),
    NEON("Neon Leaf Sign", "Decor: everything sells 5% higher", 6, 300.0, decor = Decor.NEON),
    LAVA("Giant Lava Lamp", "Decor: neighbours grow 15% faster", 6, 500.0, decor = Decor.LAVA),
    ALIEN("Alien Statue", "Decor: neighbours +6% golden chance", 6, 900.0, decor = Decor.ALIEN),
    DISCO("Disco Ball", "Decor: +10% XP", 30, 600.0, decor = Decor.DISCO),
    UFO("Crashed UFO", "Decor: neighbours grow 25% faster", 40, 300.0, decor = Decor.UFO),
    CRYSTAL("Grow Crystal", "Decor: neighbours yield 25% more", 50, 120.0, decor = Decor.CRYSTAL);

    fun price(playerLevel: Int): Long = if (strain != null) nice(base) else nice(base * 1.22.pow(playerLevel))

    companion object {
        const val EXOTIC_PACK = 5
    }
}
