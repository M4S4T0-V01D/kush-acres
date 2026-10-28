package dev.m4s4t0.kushacres.model

/**
 * One strain you can grow. [minutes] is the grow time on dry soil with no boosts; water, nutrients and lamps make it
 * faster. Each strain earns more per hour than the one before it, and gives a bit more XP per hour, so the newest
 * unlock is always worth planting, while the short ones stay best for active play (one watering covers the whole grow).
 */
/** A strain's genes. Breeding two strains passes both parents' traits on to the hybrid. */
enum class Trait(val label: String, val desc: String) {
    FAST("Fast", "Grows 10% faster"),
    BIG("Big", "12% more bud"),
    GOLDEN("Lucky", "Half again the golden chance"),
    HARDY("Hardy", "Never gets bugs or mould"),
    THRIFTY("Thrifty", "Seeds cost a quarter less"),
}

class Strain(
    val id: String,
    val name: String,
    val minutes: Int,
    val level: Int,
    /** Cost to plant one plot, before event scaling (see [seed]). */
    val seed0: Long,
    /** Grams per harvest before Genetics. */
    val yield: Int,
    /** Market price per gram at full demand, before Street Cred and event scaling (see [price]). */
    val price0: Long,
    val xp0: Int,
    /** Sprite colours (ARGB): leaves, buds and the little hairs on them. */
    val leaf: Int,
    val bud: Int,
    val hair: Int,
    val blurb: String,
    /** Black-market only: planted from seeds in your pocket instead of bought per plot. */
    val exotic: Boolean = false,
    /** Bred in the lab from these two strains; planted like a ladder strain once you've made it. */
    val parents: Pair<String, String>? = null,
    /** Native to this farm site: only grows there, once you've reached its level. */
    val site: String? = null,
    /** A holiday strain: unlocked for good from that holiday's shop. */
    val holiday: String? = null,
    /** Pot Bucks strains (holiday "bucks"): unlocked for good for this many Pot Bucks. */
    val bucks: Long = 0,
    /** Hybrids carry both parents' traits; everything else gets one of its own. */
    inherit: Set<Trait>? = null,
    /** Event strains keep pace with the farm: they earn this many times the best ladder strain you've reached. */
    edge: Double? = null,
) {
    val edge: Double = edge ?: if (holiday != null) Strains.EVENT_EDGE else 0.0
    /** Event strains (holiday, Pot Bucks, and lab crosses made from them) scale with the farm's level. */
    val event: Boolean get() = edge > 0
    private var scaledAt = -1
    private var moneyK = 1.0
    private var xpK = 1.0
    private fun scale() {
        val l = Strains.scaleLevel
        if (scaledAt == l) return
        val (m, x) = Strains.best(l)
        moneyK = maxOf(1.0, m * edge / (price0 * yield.toDouble() / minutes))
        xpK = maxOf(1.0, x * edge / (xp0.toDouble() / minutes))
        scaledAt = l
    }
    /** Cost to plant one plot. Scaled event seeds cost up to 15% of what the harvest sells for. */
    val seed: Long get() {
        if (!event) return seed0
        scale()
        return maxOf(seed0 * moneyK, price * yield * Strains.EVENT_SEED * minOf(1.0, moneyK - 1)).toLong()
    }
    /** Market price per gram at full demand, before Street Cred. */
    val price: Long get() = if (!event) price0 else { scale(); (price0 * moneyK).toLong() }
    val xp: Int get() = if (!event) xp0 else { scale(); (xp0 * xpK).toInt() }
    val traits: Set<Trait> = inherit ?: setOf(Trait.entries[Math.floorMod(id.hashCode(), Trait.entries.size)])
    val seconds: Long get() = minutes * 60L
    val hybrid: Boolean get() = parents != null
    /** How long moving a growing plant of this strain sets it back, as a share of its grow time. */
    val stunt: Double get() = when {
        exotic -> 0.5
        holiday != null -> 0.4
        hybrid -> 0.35
        site != null -> 0.3
        else -> 0.15 + level * 0.004
    }
}

object Strains {
    private val base = listOf(
        Strain("ditch", "Ditch Weed", 5, 1, 4, 3, 4, 2, 0xFF6E9A3A.toInt(), 0xFF8DAA4E.toInt(), 0xFFC9A55B.toInt(),
            "Grows by the highway. Nobody's proud of it."),
        Strain("schwag", "Schwag", 12, 2, 14, 4, 10, 4, 0xFF5E8C36.toInt(), 0xFF9AAF52.toInt(), 0xFFB9853F.toInt(),
            "Seeds, stems and dreams."),
        Strain("mids", "Mids", 25, 3, 43, 4, 31, 8, 0xFF4F9A3E.toInt(), 0xFFA6C25E.toInt(), 0xFFD98B3A.toInt(),
            "Honest, middle-of-the-road bud."),
        Strain("lemon", "Lemon Haze", 35, 4, 74, 5, 45, 11, 0xFF5C9A34.toInt(), 0xFFE8E25A.toInt(), 0xFFF2C24A.toInt(),
            "Zesty. Makes the whole porch smell like summer."),
        Strain("northern", "Northern Lights", 45, 5, 110, 5, 63, 15, 0xFF3F8F55.toInt(), 0xFFB4D7A0.toInt(), 0xFFE6E0C8.toInt(),
            "Frosty indica, glows a little at night."),
        Strain("pineapple", "Pineapple Express", 52, 6, 150, 6, 79, 18, 0xFF4E8F30.toInt(), 0xFFF0C04A.toInt(), 0xFFE07A2E.toInt(),
            "Tropical, and a little bit dangerous."),
        Strain("bluedream", "Blue Dream", 60, 7, 210, 6, 100, 22, 0xFF3B7F73.toInt(), 0xFF7FB0C9.toInt(), 0xFFE58A4B.toInt(),
            "Smooth sativa with a blueberry finish."),
        Strain("jack", "Jack Herer", 75, 8, 320, 7, 140, 28, 0xFF4A8A3A.toInt(), 0xFFC4D88A.toInt(), 0xFFE6A24A.toInt(),
            "Named for a legend. Spicy and clear-headed."),
        Strain("sour", "Sour Diesel", 90, 9, 440, 7, 180, 35, 0xFF5A8F2E.toInt(), 0xFFC8D65A.toInt(), 0xFFE07B2E.toInt(),
            "Smells like a gas station. In a good way."),
        Strain("ak47", "AK-47", 105, 10, 620, 8, 240, 43, 0xFF467A36.toInt(), 0xFFD2D2A0.toInt(), 0xFFC8783A.toInt(),
            "Hits fast. One-hitter quitter."),
        Strain("purple", "Purple Haze", 120, 11, 840, 8, 300, 51, 0xFF4E6F3A.toInt(), 0xFF9B6BC8.toInt(), 0xFFF0A040.toInt(),
            "Excuse me while I kiss the sky."),
        Strain("gdp", "Granddaddy Purple", 150, 12, 1300, 8, 460, 66, 0xFF3E5E3A.toInt(), 0xFF7E4FB0.toInt(), 0xFFE8A24E.toInt(),
            "Grandpa's secret stash. Grape and couch."),
        Strain("gsc", "Girl Scout Cookies", 180, 14, 1800, 8, 640, 82, 0xFF3E6B3A.toInt(), 0xFFA88AC0.toInt(), 0xFFE8B060.toInt(),
            "Thin mints never hit like this."),
        Strain("durban", "Durban Poison", 210, 15, 2600, 9, 860, 101, 0xFF5A9A3A.toInt(), 0xFFB8E06A.toInt(), 0xFFF0D060.toInt(),
            "Pure African sativa. Rocket fuel for chores."),
        Strain("widow", "White Widow", 240, 17, 3500, 9, 1100, 122, 0xFF4C7F4A.toInt(), 0xFFECEDE0.toInt(), 0xFFD6A35A.toInt(),
            "So frosty it looks snowed on."),
        Strain("strawberry", "Strawberry Cough", 270, 18, 4700, 10, 1400, 147, 0xFF4A8A44.toInt(), 0xFFE86A7A.toInt(), 0xFFF4D08A.toInt(),
            "Sweet berries, and yes, you will cough."),
        Strain("og", "OG Kush", 300, 20, 6000, 10, 1700, 174, 0xFF2F6E3A.toInt(), 0xFF8FBF5A.toInt(), 0xFFD9772E.toInt(),
            "The legend. Pine, lemon and fuel."),
        Strain("banana", "Banana Kush", 330, 21, 7800, 10, 2300, 207, 0xFF3E7A36.toInt(), 0xFFF2E07A.toInt(), 0xFFB8783A.toInt(),
            "Smells like banana bread. Tastes like vacation."),
        Strain("gelato", "Gelato", 360, 23, 10000, 10, 3000, 242, 0xFF3A6248.toInt(), 0xFFC98FD0.toInt(), 0xFFF2B35E.toInt(),
            "Dessert in a jar."),
        Strain("tangie", "Tangie", 420, 24, 14000, 11, 4000, 299, 0xFF4A8A30.toInt(), 0xFFF29A3A.toInt(), 0xFFFFD27A.toInt(),
            "Tangerine dream with a sticky finish."),
        Strain("cake", "Wedding Cake", 480, 26, 20000, 11, 5100, 360, 0xFF3F7A4C.toInt(), 0xFFF0E2D0.toInt(), 0xFFE39A5B.toInt(),
            "Something borrowed, something stoned."),
        Strain("mimosa", "Mimosa", 540, 28, 27000, 12, 6600, 435, 0xFF4A8040.toInt(), 0xFFF6B04A.toInt(), 0xFFFFE08A.toInt(),
            "Brunch, but make it green."),
        Strain("glue", "Gorilla Glue", 600, 30, 35000, 12, 8300, 515, 0xFF356B2E.toInt(), 0xFFB9C77A.toInt(), 0xFFC96A2A.toInt(),
            "Sticks you to the couch."),
        Strain("icecream", "Ice Cream Cake", 660, 32, 46000, 13, 10000, 613, 0xFF3A6A50.toInt(), 0xFFF4ECF0.toInt(), 0xFFE8B0C8.toInt(),
            "Creamy, heavy, and gone in one evening."),
        Strain("zkittlez", "Zkittlez", 720, 34, 59000, 13, 13000, 718, 0xFF3A7A5A.toInt(), 0xFFE07AA8.toInt(), 0xFFFFC94A.toInt(),
            "Taste the rainbow."),
        Strain("cereal", "Cereal Milk", 780, 36, 77000, 14, 16000, 846, 0xFF3E6E5A.toInt(), 0xFFF0E8C8.toInt(), 0xFFFF9AC0.toInt(),
            "Saturday morning cartoons in plant form."),
        Strain("runtz", "Runtz", 840, 38, 98000, 14, 20000, 984, 0xFF2E5E4E.toInt(), 0xFF7AD0C8.toInt(), 0xFFFF8FB8.toInt(),
            "Hype in a bag."),
        Strain("biscotti", "Biscotti", 900, 40, 130000, 14, 25000, 1151, 0xFF35583E.toInt(), 0xFFC8A070.toInt(), 0xFFF0C890.toInt(),
            "Fancy cookie, fancier price tag."),
        Strain("voidkush", "Void Kush", 960, 42, 160000, 14, 32000, 1332, 0xFF24203A.toInt(), 0xFF8A5CFF.toInt(), 0xFF5CF2FF.toInt(),
            "Grown where the light doesn't reach."),
        Strain("stardawg", "Stardawg Supreme", 960, 45, 230000, 15, 44000, 1625, 0xFF2A3A5A.toInt(), 0xFFD8E0FF.toInt(), 0xFFFFE65C.toInt(),
            "Twinkles in the jar. Nobody knows why."),
        Strain("cherrypie", "Cherry Pie Royale", 960, 48, 320000, 16, 61000, 1983, 0xFF3A2A3A.toInt(), 0xFFD0304A.toInt(), 0xFFFFB0C0.toInt(),
            "Baked for kings, smoked by you."),
        Strain("galaxyglue", "Galaxy Glue", 960, 51, 460000, 17, 84000, 2419, 0xFF1E2A4A.toInt(), 0xFF5C8CFF.toInt(), 0xFFFF6CF0.toInt(),
            "Holds the universe together."),
        Strain("phoenix", "Phoenix OG", 960, 54, 650000, 18, 120000, 2951, 0xFF4A2A1E.toInt(), 0xFFFF7A2E.toInt(), 0xFFFFE04A.toInt(),
            "Rises from the ashes every harvest."),
        Strain("reserve", "Kush Acres Reserve", 960, 57, 920000, 19, 160000, 3600, 0xFF1E3A2A.toInt(), 0xFFFFD23A.toInt(), 0xFFFFFFFF.toInt(),
            "The house strain. Gold-dusted and priceless."),
        Strain("nebula", "Nebula Nectar", 960, 61, 1_300_000, 20, 220000, 4300, 0xFF2A1E4A.toInt(), 0xFFB07AFF.toInt(), 0xFFFFA0E0.toInt(),
            "Sweet as starlight, sticky as a black hole."),
        Strain("titan", "Titan Kush", 960, 66, 1_900_000, 21, 300000, 5200, 0xFF1E3A2E.toInt(), 0xFF6AC07A.toInt(), 0xFFFFC04A.toInt(),
            "Plants the size of a pickup truck."),
        Strain("elysium", "Elysium Haze", 960, 72, 2_700_000, 22, 420000, 6300, 0xFF3A5A6A.toInt(), 0xFFE8FFF4.toInt(), 0xFFFFE8A0.toInt(),
            "Smells like heaven's waiting room."),
        Strain("crown", "Crown Jewel Kush", 960, 78, 3_800_000, 24, 600000, 7700, 0xFF2A1E3A.toInt(), 0xFFE8304A.toInt(), 0xFFFFD23A.toInt(),
            "Kept under glass. Guarded by a man named Tiny."),
        Strain("ghost", "Ghost Train Haze", 40, 6, 0, 6, 260, 40, 0xFFB8C8C0.toInt(), 0xFFF4FFF8.toInt(), 0xFFA0E0FF.toInt(),
            "Pale as a ghost, strong as a freight train.", exotic = true),
        Strain("dragon", "Dragon's Breath", 180, 6, 0, 10, 2500, 300, 0xFF6A2A1E.toInt(), 0xFFFF5A2A.toInt(), 0xFFFFD040.toInt(),
            "Smoke comes out your nose on its own.", exotic = true),
        Strain("unicorn", "Unicorn Poop", 480, 6, 0, 14, 14000, 1200, 0xFFE0A0E0.toInt(), 0xFFFFFFFF.toInt(), 0xFF7AE0FF.toInt(),
            "Sparkly. Don't ask where it comes from.", exotic = true),
        Strain("moonshine", "Moonshine Haze", 720, 6, 0, 16, 60000, 3500, 0xFF2A3A6A.toInt(), 0xFFE8F0FF.toInt(), 0xFFFFF0A0.toInt(),
            "Distilled moonlight. Grown under the full moon.", exotic = true),
        Strain("kosmic", "Kosmic Kush", 960, 6, 0, 18, 250000, 9000, 0xFF10102A.toInt(), 0xFFFF4AD0.toInt(), 0xFF4AFFE0.toInt(),
            "Fell from the sky in a smoking crater.", exotic = true),
    )

    /** Holiday strains, two per holiday: a quick one and a long one, bought with that holiday's tokens. */
    private val premium = listOf(
        Strain("jahbless", "Jah Bless", 30, 1, 80, 9, 520, 70, 0xFF2E7A2E.toInt(), 0xFFFFD23A.toInt(), 0xFFE03A3A.toInt(),
            "One love, one heart, one very sticky bud.", holiday = "bucks", bucks = 40),
        Strain("zionhaze", "Zion Haze", 120, 1, 250, 12, 3200, 260, 0xFF3A8A3A.toInt(), 0xFF8AE06A.toInt(), 0xFFFFE04A.toInt(),
            "Slow, sweet and easy, like a Sunday bassline.", holiday = "bucks", bucks = 70),
        Strain("dubplate", "Dub Plate OG", 240, 1, 600, 15, 9800, 640, 0xFF1E4A2E.toInt(), 0xFF5A3AA4.toInt(), 0xFFFF8A1E.toInt(),
            "Heavy on the bass, heavy on the resin.", holiday = "bucks", bucks = 110),
        Strain("lionsmane", "Lion's Mane", 480, 1, 1500, 18, 26000, 1400, 0xFF4A6A1E.toInt(), 0xFFE0A03A.toInt(), 0xFFFF5A1E.toInt(),
            "A golden mane of orange hairs. Roars when you harvest it.", holiday = "bucks", bucks = 160),
    )

    private val festive = listOf(
        Strain("pumpkinspice", "Pumpkin Spice Kush", 45, 1, 60, 8, 420, 60, 0xFF3A4A1E.toInt(), 0xFFFF8A1E.toInt(), 0xFFFFE04A.toInt(),
            "Basic? Maybe. Delicious? Absolutely.", holiday = "halloween"),
        Strain("witchbrew", "Witch's Brew", 360, 1, 400, 14, 9000, 700, 0xFF2A1E3A.toInt(), 0xFFA43AFF.toInt(), 0xFFFF8A1E.toInt(),
            "Double, double, toil and bubble.", holiday = "halloween"),
        Strain("candycane", "Candy Cane Kush", 45, 1, 60, 8, 420, 60, 0xFF2E6A3A.toInt(), 0xFFFFF4F4.toInt(), 0xFFFF2A3A.toInt(),
            "Peppermint stripes on every bud.", holiday = "christmas"),
        Strain("mistletoe", "Mistletoe", 360, 1, 400, 14, 9000, 700, 0xFF1E5A2E.toInt(), 0xFF4AE06A.toInt(), 0xFFFF3A4A.toInt(),
            "Hang it high. Somebody might kiss you.", holiday = "christmas"),
        Strain("champagne", "Champagne Haze", 45, 1, 60, 8, 420, 60, 0xFF4A5A2E.toInt(), 0xFFFFE89A.toInt(), 0xFFFFFFFF.toInt(),
            "Pop it at midnight.", holiday = "newyear"),
        Strain("countdown", "Countdown Kush", 360, 1, 400, 14, 9000, 700, 0xFF1E1E3A.toInt(), 0xFFC0C8FF.toInt(), 0xFFFFD23A.toInt(),
            "Ten, nine, eight... wait, what year is it?", holiday = "newyear"),
        Strain("cupid", "Cupid's Arrow", 45, 1, 60, 8, 420, 60, 0xFF3A5A3A.toInt(), 0xFFFF5AA0.toInt(), 0xFFFFF0F4.toInt(),
            "Straight to the heart.", holiday = "valentine"),
        Strain("heartbreaker", "Heartbreaker", 360, 1, 400, 14, 9000, 700, 0xFF3A1E2A.toInt(), 0xFFE0103A.toInt(), 0xFFFFB0D0.toInt(),
            "It's not you. It's the terpenes.", holiday = "valentine"),
        Strain("shamrock", "Shamrock Shake", 45, 1, 60, 8, 420, 60, 0xFF1E7A2E.toInt(), 0xFF5AFF6A.toInt(), 0xFFFFFFFF.toInt(),
            "Minty green and gone by April.", holiday = "stpatrick"),
        Strain("leprechaun", "Leprechaun Gold", 360, 1, 400, 14, 9000, 700, 0xFF1E5A1E.toInt(), 0xFFFFD21E.toInt(), 0xFF3AFF5A.toInt(),
            "Found at the end of a rainbow.", holiday = "stpatrick"),
        Strain("fourtwenty", "Four Twenty Classic", 45, 1, 60, 8, 420, 60, 0xFF2E7A2E.toInt(), 0xFF9AFF3A.toInt(), 0xFFFF9A2A.toInt(),
            "The high holiday's house blend.", holiday = "fourtwenty"),
        Strain("highholy", "High Holy Haze", 360, 1, 400, 14, 9000, 700, 0xFF2A4A3A.toInt(), 0xFF3AFFD0.toInt(), 0xFFFFE03A.toInt(),
            "Blessed by every hippie in the county.", holiday = "fourtwenty"),
        Strain("jellybean", "Jelly Bean", 45, 1, 60, 8, 420, 60, 0xFF4A7A3A.toInt(), 0xFFFFA0E0.toInt(), 0xFF7AE0FF.toInt(),
            "Every bud a different flavour.", holiday = "easter"),
        Strain("peeps", "Purple Peeps", 360, 1, 400, 14, 9000, 700, 0xFF3A3A5A.toInt(), 0xFFC07AFF.toInt(), 0xFFFFF07A.toInt(),
            "Marshmallow sweet. Slightly alarming.", holiday = "easter"),
        Strain("liberty", "Liberty Haze", 45, 1, 60, 8, 420, 60, 0xFF2A4A6A.toInt(), 0xFF4A8AFF.toInt(), 0xFFFF3A3A.toInt(),
            "Red, white and dank.", holiday = "july4"),
        Strain("bottlerocket", "Bottle Rocket", 360, 1, 400, 14, 9000, 700, 0xFF3A1E1E.toInt(), 0xFFFF3A2A.toInt(), 0xFFFFFFFF.toInt(),
            "Light the fuse and stand back.", holiday = "july4"),
        Strain("cranberry", "Cranberry Sauce", 45, 1, 60, 8, 420, 60, 0xFF4A3A1E.toInt(), 0xFFD01E4A.toInt(), 0xFFFFC04A.toInt(),
            "Still has the can ridges.", holiday = "thanksgiving"),
        Strain("gravy", "Grandma's Gravy", 360, 1, 400, 14, 9000, 700, 0xFF5A3A1E.toInt(), 0xFFE0A04A.toInt(), 0xFFFFE8A0.toInt(),
            "Thick, rich and puts you to sleep at the table.", holiday = "thanksgiving"),
    )

    /** Each site's native strains, two per farm: vivid things that only grow on that land. */
    private val native: List<Strain> = run {
        class N(val site: String, val level: Int, val id: String, val name: String, val leaf: Long, val bud: Long, val hair: Long, val blurb: String, val quick: Boolean)
        val ns = listOf(
            N("redwood", 15, "redwoodrocket", "Redwood Rocket", 0xFF2E5A2A, 0xFFE0302A, 0xFFFFC04A, "Tall as the trees, red as the bark.", false),
            N("redwood", 15, "ferngully", "Fern Gully Kush", 0xFF1E6A4A, 0xFF3AF0A0, 0xFFE0FF7A, "Curls up like a fiddlehead.", true),
            N("mesa", 19, "mesasunset", "Mesa Sunset", 0xFF5A4A2A, 0xFFFF6A5A, 0xFFFFD23A, "Pink sky in a jar.", false),
            N("mesa", 19, "cactuscooler", "Cactus Cooler", 0xFF3A6A2A, 0xFFB0FF3A, 0xFFFF7AC0, "Prickly outside, juicy inside.", true),
            N("bayou", 23, "swampthing", "Swamp Thing", 0xFF2A4A1E, 0xFF7AFF1E, 0xFFC0FF5A, "Crawled out of the water on its own.", false),
            N("bayou", 23, "voodoo", "Voodoo Violet", 0xFF2A2A3A, 0xFF8A2AFF, 0xFFFF5AE0, "Nobody plants it. It just shows up.", true),
            N("coast", 27, "coralreef", "Coral Reef", 0xFF2A5A5A, 0xFFFF7A6A, 0xFFFFE0C0, "Salty, sunny, a little bit fishy.", false),
            N("coast", 27, "tidalwave", "Tidal Wave", 0xFF1E4A6A, 0xFF2AE0FF, 0xFFFFFFFF, "Comes on in sets.", true),
            N("frost", 31, "glacier", "Glacier Glow", 0xFF3A5A6A, 0xFF7AD8FF, 0xFFE8FFFF, "Grows a millimetre a year. Worth it.", false),
            N("frost", 31, "yetibreath", "Yeti Breath", 0xFF4A4A6A, 0xFFE8E0FF, 0xFFA07AFF, "Big, white and hard to find.", true),
            N("emerald", 35, "emeraldisle", "Emerald Isle", 0xFF1E5A3A, 0xFF1EFF8A, 0xFFFFE04A, "Greener than the hills.", false),
            N("emerald", 35, "jade", "Jade Empress", 0xFF2A4A3A, 0xFF5AE0B0, 0xFFFF5A7A, "Carved from the valley itself.", true),
            N("volcano", 40, "magma", "Magma Kush", 0xFF3A1E1E, 0xFFFF4A1E, 0xFFFFE01E, "Hot to the touch. Literally.", false),
            N("volcano", 40, "obsidian", "Obsidian OG", 0xFF1E1E24, 0xFF3A3A4A, 0xFFFF2AA0, "Black glass buds with pink fire inside.", true),
            N("neon", 45, "neonnights", "Neon Nights", 0xFF2A1E3A, 0xFFFF2AD0, 0xFF2AFFF0, "Buzzes like a sign at 3am.", false),
            N("neon", 45, "laserlime", "Laser Lime", 0xFF1E3A2A, 0xFFC8FF1E, 0xFFFF3AFF, "Do not look directly at the buds.", true),
            N("highland", 51, "heather", "Highland Heather", 0xFF3A4A3A, 0xFFC08AFF, 0xFFFFE0F0, "Purple hills, purple haze.", false),
            N("highland", 51, "thistle", "Thistle Haze", 0xFF2A3A2A, 0xFFE04AFF, 0xFFFFFFFF, "Prickly and proud.", true),
            N("crater", 58, "meteorite", "Meteorite", 0xFF1E2A3A, 0xFF1EE0FF, 0xFFFF9A1E, "Still warm from the fall.", false),
            N("crater", 58, "stardust", "Stardust Diesel", 0xFF2A2A3A, 0xFFE8E8FF, 0xFF5AFFC0, "Sparkles like it came from space. It did.", true),
        )
        val ladder = base.filter { !it.exotic }
        ns.map { n ->
            val l = ladder.last { it.level <= n.level }
            val minutes = if (n.quick) l.minutes / 3 else l.minutes
            val k = minutes / l.minutes.toDouble()
            // A quarter more per hour than the ladder strain of the same level, spread over one extra gram.
            Strain(n.id, n.name, minutes, n.level, (l.seed * k * 1.1).toLong(), l.yield + 1, (l.price * 1.25 * k * l.yield / (l.yield + 1)).toLong().coerceAtLeast(1),
                (l.xp * k * 1.2).toInt().coerceAtLeast(1), n.leaf.toInt(), n.bud.toInt(), n.hair.toInt(), n.blurb, site = n.site)
        }
    }

    /**
     * Each town's back-alley strain: exotic seeds only that town's dealer sells, which only take in that farm's soil.
     * Two thirds the grow time of the ladder strain at the farm's level, four more grams and half as much again per gram.
     */
    private val smuggled: List<Strain> = run {
        class S(val site: String, val level: Int, val id: String, val name: String, val leaf: Long, val bud: Long, val hair: Long, val blurb: String)
        val ss = listOf(
            S("redwood", 15, "bigfoot", "Bigfoot Kush", 0xFF3A4A2A, 0xFF8A5A3A, 0xFFE0C08A, "Blurry photos only. The dealer swears it's real."),
            S("mesa", 19, "rattler", "Rattlesnake Rush", 0xFF6A5A2A, 0xFFE0B05A, 0xFFFF5A2A, "Shakes its tail before it bites."),
            S("bayou", 23, "gatorbite", "Gator Bite", 0xFF2A4A2A, 0xFF5A8A3A, 0xFFFFE05A, "Snaps shut on your lungs."),
            S("coast", 27, "mermaid", "Mermaid Tears", 0xFF1E5A5A, 0xFF7AF0E0, 0xFFFFB0E0, "Salty, shimmering and a little sad."),
            S("frost", 31, "polarvortex", "Polar Vortex", 0xFF2A3A5A, 0xFFC0E8FF, 0xFF5AA0FF, "Freezes your face in a grin."),
            S("emerald", 35, "truffle", "Triangle Truffle", 0xFF1E4A2A, 0xFF4A3A2A, 0xFF3AFF8A, "Dug up by a pig who won't say where."),
            S("volcano", 40, "pele", "Pele's Fire", 0xFF2A1E1E, 0xFFFF2A1E, 0xFFFFA01E, "A gift from the mountain. Don't make it angry."),
            S("neon", 45, "glowstick", "Glowstick OG", 0xFF1E2A1E, 0xFF5AFF2A, 0xFFFF2AFF, "Crack it and shake it."),
            S("highland", 51, "nessie", "Loch Ness Haze", 0xFF1E3A3A, 0xFF3A7A8A, 0xFFC0FFE0, "Surfaces once in a while. Nobody believes you."),
            S("crater", 58, "abduction", "Alien Abduction", 0xFF1E1E3A, 0xFF8AFF5A, 0xFFE0E0FF, "Lost time, found bud."),
        )
        val ladder = base.filter { !it.exotic }
        ss.map { s ->
            val l = ladder.last { it.level <= s.level }
            Strain(s.id, s.name, l.minutes * 2 / 3, s.level, 0, l.yield + 4, l.price * 3 / 2, l.xp * 3 / 2,
                s.leaf.toInt(), s.bud.toInt(), s.hair.toInt(), s.blurb, exotic = true, site = s.site)
        }
    }

    private class Cross(val a: String, val b: String, val id: String, val name: String, val blurb: String)

    /** Lab recipes, in the order the parents unlock. Later crosses can use earlier hybrids. */
    private val crosses = listOf(
        Cross("ditch", "schwag", "reggie", "Roadside Reggie", "Two bad ideas that somehow worked out."),
        Cross("mids", "lemon", "lemondrop", "Lemon Drop", "Sour candy on a stem."),
        Cross("lemon", "pineapple", "tropic", "Tropic Thunder", "A whole fruit bowl went off at once."),
        Cross("northern", "bluedream", "aurora", "Aurora Blue", "Glows blue-green after dark."),
        Cross("jack", "sour", "jetfuel", "Jet Fuel Jack", "Do not smoke near open flames."),
        Cross("sour", "og", "diesel", "Diesel Kush", "Fuel up front, pine in the back."),
        Cross("ak47", "purple", "violet", "Violet Volley", "Purple, loud and fast."),
        Cross("gdp", "gsc", "grapecookie", "Grape Cookies", "Grandpa's baking got serious."),
        Cross("durban", "widow", "leopard", "Snow Leopard", "Spotted with frost. Rarely seen."),
        Cross("og", "widow", "frostedog", "Frosted OG", "The legend, iced over."),
        Cross("strawberry", "og", "berryog", "Berry OG", "Berries and pine cones in one jar."),
        Cross("banana", "gelato", "split", "Banana Split", "Three scoops and a banana."),
        Cross("tangie", "cake", "creamsicle", "Orange Creamsicle", "Tastes like the ice-cream truck."),
        Cross("mimosa", "glue", "sunrise", "Sticky Sunrise", "Brunch that never lets you leave."),
        Cross("icecream", "zkittlez", "sherbet", "Rainbow Sherbet", "Every colour, every flavour."),
        Cross("cereal", "runtz", "sugarrush", "Sugar Rush", "Pure candy. Brush your teeth."),
        Cross("biscotti", "voidkush", "darkroast", "Dark Roast", "Espresso-black buds. Wakes you up, then doesn't."),
        Cross("tropic", "split", "punch", "Paradise Punch", "A hybrid of hybrids. Tastes like a beach party."),
        Cross("stardawg", "galaxyglue", "supernova", "Supernova", "Burns bright. Burns out the jar."),
        Cross("cherrypie", "phoenix", "firecracker", "Firecracker Pie", "Pops when you break it up."),
        Cross("phoenix", "reserve", "goldphoenix", "Golden Phoenix", "Reborn in pure gold."),
        Cross("supernova", "darkroast", "bigbang", "Big Bang Kush", "Where everything started. And ended."),
        Cross("nebula", "titan", "colossus", "Cosmic Colossus", "A giant made of starlight."),
        Cross("elysium", "crown", "paradise", "Paradise Crown", "Heaven, wearing a crown."),
        // Exotic crosses: the black-market seeds bred into something you can plant from the lab.
        Cross("ghost", "northern", "phantom", "Phantom Lights", "Flickers in the dark field. Spooky."),
        Cross("dragon", "phoenix", "wyrm", "Wyrmfire", "Two firebirds walk into a lab."),
        Cross("unicorn", "zkittlez", "rainbowhorn", "Rainbow Horn", "Sparkles in every colour there is."),
        Cross("moonshine", "nebula", "moonnectar", "Moon Nectar", "Sipped by astronauts."),
        Cross("kosmic", "crown", "kosmicking", "Kosmic King", "Rules the galaxy from a jar."),
        // Native crosses: a site's two strains together.
        Cross("redwoodrocket", "ferngully", "forestfire", "Forest Fire", "Red and green, and spreading fast."),
        Cross("mesasunset", "cactuscooler", "desertbloom", "Desert Bloom", "Flowers once a year. Today's the day."),
        Cross("swampthing", "voodoo", "hoodoo", "Hoodoo Gumbo", "Stirred at midnight with a gator bone."),
        Cross("coralreef", "tidalwave", "riptide", "Riptide", "Pulls you right out to sea."),
        Cross("glacier", "yetibreath", "avalanche", "Avalanche", "Everything at once, from the top."),
        Cross("emeraldisle", "jade", "jadeisle", "Jade Isle", "Green on green on green."),
        Cross("magma", "obsidian", "eruption", "Eruption", "Black buds with lava running through them."),
        Cross("neonnights", "laserlime", "synthwave", "Synthwave", "Sounds like 1986 looks."),
        Cross("heather", "thistle", "bagpipe", "Bagpipe Haze", "Loud, purple and Scottish."),
        Cross("meteorite", "stardust", "impact", "Impact Kush", "Hits like a space rock."),
        // Holiday crosses: each holiday's pair together.
        Cross("pumpkinspice", "witchbrew", "jackolantern", "Jack-o'-Lantern", "Glows orange with a purple grin."),
        Cross("candycane", "mistletoe", "northpole", "North Pole", "Santa's personal stash."),
        Cross("champagne", "countdown", "midnight", "Midnight Toast", "Happy new year, all year."),
        Cross("cupid", "heartbreaker", "lovepotion", "Love Potion", "One puff and you're smitten."),
        Cross("shamrock", "leprechaun", "potogold", "Pot o' Gold", "Lucky all the way down."),
        Cross("fourtwenty", "highholy", "blazeit", "Blaze It", "Every day is 4/20 now."),
        Cross("jellybean", "peeps", "eggstra", "Eggstra Dank", "Hidden in the grass. Found by noon."),
        Cross("liberty", "bottlerocket", "fireworks", "Fireworks", "Ooh. Aah. Cough."),
        Cross("cranberry", "gravy", "leftovers", "Leftovers", "Somehow better the next day."),
    )

    private fun mix(a: Int, b: Int, k: Float): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) * (1 - k) + ((b shr s) and 0xFF) * k).toInt() shl s
        return (0xFF shl 24) or ch(16) or ch(8) or ch(0)
    }

    private val hybrids: List<Strain> = run {
        val known = (base + native + festive).associateBy { it.id }.toMutableMap()
        crosses.map { c ->
            val a = known.getValue(c.a); val b = known.getValue(c.b)
            Strain(c.id, c.name,
                minutes = ((a.minutes + b.minutes) / 2.0 * 0.9).toInt().coerceAtLeast(4),
                level = maxOf(a.level, b.level),
                seed0 = ((a.seed0 + b.seed0) / 2.0 * 1.2).toLong().coerceAtLeast(5),
                yield = maxOf(a.yield, b.yield) + 1,
                price0 = ((a.price0 + b.price0) / 2.0 * 1.3).toLong(),
                xp0 = ((a.xp0 + b.xp0) / 2.0 * 1.25).toInt(),
                leaf = mix(a.leaf, b.leaf, 0.5f), bud = mix(a.bud, b.bud, 0.6f), hair = a.hair,
                blurb = c.blurb, parents = c.a to c.b, inherit = a.traits + b.traits,
                edge = if (a.event || b.event) CROSS_EDGE else 0.0,
            ).also { known[it.id] = it }
        }
    }

    val all: List<Strain> = base.filter { !it.exotic } + hybrids + base.filter { it.exotic } + smuggled + native + festive + premium

    /** The strains you buy seeds for with cash, in unlock order. */
    val ladder = all.filter { !it.exotic && !it.hybrid && it.site == null && it.holiday == null }
    /** Strains that only grow on their own farm site. */
    val natives = native
    /** Town dealers' strains, one per farm town. */
    val townList = smuggled
    /** Holiday strains, kept for good once bought. */
    val holidays = festive
    /** Pot Bucks strains, kept for good once bought. */
    val bucksList = premium
    /** Lab strains: unlocked by breeding them once. */
    val hybridList = all.filter { it.hybrid }
    /** Black-market strains: planted from seeds you own, never asked for in orders. */
    val exotics = all.filter { it.exotic }

    private val byId = all.associateBy { it.id }
    operator fun get(id: String): Strain? = byId[id]

    /** Event strains earn this many times the best ladder strain's money and XP per minute; their lab crosses more. */
    const val EVENT_EDGE = 1.3
    const val CROSS_EDGE = 1.45
    /** The most a scaled event seed costs, as a share of what its harvest sells for. */
    const val EVENT_SEED = 0.15
    /** The level event strains scale to: the farm's, kept in step by [Farm.level]. */
    @Volatile var scaleLevel = 1
    private val bests = HashMap<Int, Pair<Double, Double>>()
    /** The best money and XP per minute from ladder strains unlocked at level [l]. */
    fun best(l: Int): Pair<Double, Double> = synchronized(bests) {
        bests.getOrPut(l) {
            val open = ladder.filter { it.level <= l }
            (open.maxOfOrNull { it.price0 * it.yield.toDouble() / it.minutes } ?: 0.0) to
                (open.maxOfOrNull { it.xp0.toDouble() / it.minutes } ?: 0.0)
        }
    }
}
