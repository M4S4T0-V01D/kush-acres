package dev.m4s4t0.kushacres.model

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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
    /** A quick cut of a long ladder strain: a fifth of the grow time, a bit less per hour. */
    val quick: Boolean = false,
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

    /**
     * More ladder strains in the gaps between the ones above: grow time, money and XP per minute sit [F.t] of the way
     * from [F.a] to [F.b] (past 1 for one beyond the top), so the ladder keeps climbing in order.
     */
    private val filler: List<Strain> = run {
        class F(val a: String, val b: String, val t: Double, val level: Int, val id: String, val name: String, val leaf: Long, val bud: Long, val hair: Long, val blurb: String)
        val fs = listOf(
            F("ditch", "schwag", 0.5, 1, "bammer", "Bammer", 0xFF7A8A3A, 0xFF9A8A4E, 0xFFB0703A, "Brown, crunchy and cheaper than a soda."),
            F("schwag", "mids", 0.5, 2, "brick", "Brick Weed", 0xFF5A7A36, 0xFF8A9A4A, 0xFFA06A3A, "Pressed flat in a car trunk somewhere."),
            F("mids", "lemon", 0.5, 3, "skunk", "Skunk #1", 0xFF4A8A3A, 0xFFB8C45A, 0xFFE0A03A, "The granddaddy of the garden. Smells like it."),
            F("northern", "pineapple", 0.5, 5, "blueberry", "Blueberry", 0xFF3A7A5A, 0xFF5A7AD0, 0xFFE89A5A, "Tastes like the muffin. Mostly."),
            F("bluedream", "jack", 0.5, 7, "goldengoat", "Golden Goat", 0xFF4A8A3A, 0xFFE0C05A, 0xFFE0703A, "Started as an accident in Kansas. Nobody's sorry."),
            F("jack", "sour", 0.5, 8, "trainwreck", "Trainwreck", 0xFF4A7A3A, 0xFFC0C878, 0xFFD0602A, "Hits like a freight car. Watch the tracks."),
            F("sour", "ak47", 0.5, 9, "greencrack", "Green Crack", 0xFF3A9A3A, 0xFF8AE04A, 0xFFF0A02A, "Mango and pure get-up-and-go."),
            F("ak47", "purple", 0.5, 10, "chemdawg", "Chemdawg", 0xFF4A6A3A, 0xFFB8C8A0, 0xFFC0803A, "Smells like a chemistry set. Gets the job done."),
            F("purple", "gdp", 0.5, 11, "silverhaze", "Super Silver Haze", 0xFF4A6A4A, 0xFFE0E4E8, 0xFFE8B05A, "Shiny, spicy and up all night."),
            F("gdp", "gsc", 0.5, 13, "dosido", "Do-Si-Dos", 0xFF3A5A3A, 0xFF9A7AC8, 0xFFF0B060, "Swing your partner round the grow room."),
            F("gsc", "durban", 0.5, 14, "bluecheese", "Blue Cheese", 0xFF3A6A5A, 0xFF8AA8D0, 0xFFF0E0A0, "Stinks like a fancy fridge. Smokes like a dream."),
            F("durban", "widow", 0.5, 16, "skywalker", "Skywalker OG", 0xFF3A5A4A, 0xFFA0C0E0, 0xFFE8C04A, "Use the force. Then the couch."),
            F("widow", "strawberry", 0.5, 17, "afghan", "Afghan Kush", 0xFF3A5A2E, 0xFF7A8A4A, 0xFFC0702A, "Straight from the mountains, thick with resin."),
            F("strawberry", "og", 0.5, 19, "headband", "Headband", 0xFF3A6A3A, 0xFFB0D06A, 0xFFE8904A, "Squeezes your temples like a hug."),
            F("og", "banana", 0.5, 20, "bubba", "Bubba Kush", 0xFF2E5A3A, 0xFF6A8A4A, 0xFFB8603A, "Heavy, chocolatey and not going anywhere."),
            F("banana", "gelato", 0.5, 22, "sundae", "Sundae Driver", 0xFF3E6A48, 0xFFE0A8D0, 0xFFF2C07A, "Takes the long way home. Slowly."),
            F("gelato", "tangie", 0.5, 23, "purplepunch", "Purple Punch", 0xFF3A4A3A, 0xFFA04AC8, 0xFFFF9A5A, "Grape soda with a right hook."),
            F("tangie", "cake", 0.5, 25, "macone", "MAC 1", 0xFF3E7A4E, 0xFFF0F0E8, 0xFFFFA05A, "Miracle Alien Cookies. Silver frost, sour finish."),
            F("cake", "mimosa", 0.5, 27, "fritter", "Apple Fritter", 0xFF4A7A3A, 0xFFE0B87A, 0xFFF0E0A0, "Glazed, golden and gone by ten."),
            F("mimosa", "glue", 0.5, 29, "lcg", "Lemon Cherry Gelato", 0xFF3E6A44, 0xFFF06A7A, 0xFFFFE05A, "The sweet shop at the end of the street."),
            F("glue", "icecream", 0.5, 31, "jealousy", "Jealousy", 0xFF2E6A4A, 0xFF7AE07A, 0xFFFF7AA0, "Everyone else wants your jar."),
            F("icecream", "zkittlez", 0.5, 33, "garypayton", "Gary Payton", 0xFF2E5A4A, 0xFF8AB0A0, 0xFFE8C05A, "Locks you down like a point guard."),
            F("zkittlez", "cereal", 0.5, 35, "marker", "Permanent Marker", 0xFF2E4A5A, 0xFF5A6AE0, 0xFFFF5A8A, "Smells exactly like the name. Don't ask how."),
            F("cereal", "runtz", 0.5, 37, "pinkrozay", "Pink Rozay", 0xFF3E5E4E, 0xFFFF9ACD, 0xFFFFF0F4, "Pink, fizzy and very expensive."),
            F("runtz", "biscotti", 0.5, 39, "gushers", "Gushers", 0xFF2E5E4A, 0xFFE05AE0, 0xFF7AF0C8, "Bursts when you squeeze it."),
            F("biscotti", "voidkush", 0.5, 41, "oreoz", "Oreoz", 0xFF2A2E2A, 0xFF4A3A3A, 0xFFF0F0F0, "Black and white and dunked in resin."),
            F("voidkush", "stardawg", 0.5, 43, "eclipse", "Eclipse OG", 0xFF14141E, 0xFF3A3A6A, 0xFFFFC83A, "The sun goes out when you open the jar."),
            F("galaxyglue", "phoenix", 0.5, 52, "cometkush", "Comet Kush", 0xFF1E2A3A, 0xFF8AE0FF, 0xFFFF8A3A, "Streaks across the field once every harvest."),
            F("nebula", "titan", 0.5, 64, "skyfall", "Skyfall Haze", 0xFF2A3A5A, 0xFF7AB0FF, 0xFFFFF0C0, "Drops out of the clouds and right into your bowl."),
            F("titan", "elysium", 0.5, 69, "olympus", "Olympus OG", 0xFF2E4A3A, 0xFFF0E8C0, 0xFF5AD0FF, "Grown by gods. Smoked by mortals."),
            F("elysium", "crown", 0.5, 75, "zenith", "Zenith", 0xFF2A3A5A, 0xFFFFF4E0, 0xFFFF9A3A, "As high as it gets. Almost."),
            F("elysium", "crown", 1.5, 80, "emperor", "Emperor's Stash", 0xFF1E1A2A, 0xFFFFD23A, 0xFFE8304A, "The last jar on the top shelf. Bow when you open it."),
        )
        val by = base.associateBy { it.id }
        fun profit(s: Strain) = (s.price0 * s.yield - s.seed0) / s.minutes.toDouble()
        fs.map { f ->
            val a = by.getValue(f.a); val b = by.getValue(f.b)
            fun lerp(x: Double, y: Double) = x + (y - x) * f.t
            val minutes = lerp(a.minutes.toDouble(), b.minutes.toDouble()).roundToInt()
            val seed = (a.seed0 * (b.seed0 / a.seed0.toDouble()).pow(f.t)).toLong()
            val yield = lerp(a.yield.toDouble(), b.yield.toDouble()).roundToInt()
            val price = ((lerp(profit(a), profit(b)) * minutes + seed) / yield).roundToLong()
            val xp = (lerp(a.xp0 / a.minutes.toDouble(), b.xp0 / b.minutes.toDouble()) * minutes).roundToInt()
            Strain(f.id, f.name, minutes, f.level, seed, yield, price, xp, f.leaf.toInt(), f.bud.toInt(), f.hair.toInt(), f.blurb)
        }
    }

    /** The cash ladder, in unlock order: shortest grow first, ties (the day-long ones) by level. */
    private val climb = (base.filter { !it.exotic } + filler).sortedWith(compareBy({ it.minutes }, { it.level }))

    /**
     * Quick cuts of the longest ladder strains: a fifth of the grow time and half the bud, for four fifths of the
     * parent's money per hour and a bit less XP. Unlocked at the parent's level.
     */
    private val express: List<Strain> = run {
        class Q(val of: String, val id: String, val name: String, val blurb: String)
        val qs = listOf(
            Q("cereal", "cerealbar", "Cereal Bar", "Breakfast on the go."),
            Q("biscotti", "biscottibite", "Biscotti Bites", "Same fancy cookie, snack size."),
            Q("voidkush", "voidblink", "Void Blink", "Gone before you notice it was there."),
            Q("stardawg", "shootingstar", "Shooting Star", "Make a wish. Make it quick."),
            Q("cherrypie", "cherrytart", "Cherry Tart", "All the royal flavour, half the bake."),
            Q("galaxyglue", "pocketgalaxy", "Pocket Galaxy", "The universe, travel size."),
            Q("phoenix", "phoenixspark", "Phoenix Spark", "Just the first flicker of the fire."),
            Q("reserve", "housepour", "House Pour", "The Reserve, poured short."),
            Q("nebula", "nebulashot", "Nebula Shot", "One gulp of starlight."),
            Q("titan", "minititan", "Mini Titan", "Pickup-truck genes in a hatchback."),
            Q("elysium", "daytrip", "Elysium Day Trip", "Heaven, back by dinner."),
            Q("crown", "tiara", "Tiara Kush", "Not the crown jewels, but it still sparkles."),
            Q("emperor", "royalflash", "Royal Flash", "An emperor's stash, in a hurry."),
        )
        val by = climb.associateBy { it.id }
        qs.map { q ->
            val p = by.getValue(q.of)
            val minutes = p.minutes / 25 * 5
            val k = minutes / p.minutes.toDouble()
            val seed = (p.seed0 * k).toLong()
            val yield = (p.yield + 1) / 2
            val profit = (p.price0 * p.yield - p.seed0) / p.minutes.toDouble() * 0.8
            Strain(q.id, q.name, minutes, p.level, seed, yield, ((profit * minutes + seed) / yield).roundToLong(), (p.xp0 * k * 0.85).roundToInt(),
                p.leaf, mix(p.bud, 0xFFFFFFFF.toInt(), 0.25f), p.hair, q.blurb, quick = true)
        }
    }

    /** Holiday strains, five per holiday from a quick one to a day-long one, bought with that holiday's tokens. */
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
        // Three more each, slower and richer, to save up for.
        Strain("ghostberry", "Ghost Berry", 150, 1, 180, 11, 2400, 260, 0xFF3A4A3A.toInt(), 0xFFE8F0F4.toInt(), 0xFF9AE0FF.toInt(),
            "Pale as a sheet and twice as spooky.", holiday = "halloween"),
        Strain("jackolantern", "Jack-o'-Lantern OG", 720, 1, 900, 18, 16000, 1400, 0xFF2E3A1E.toInt(), 0xFFFF7A10.toInt(), 0xFFFFE04A.toInt(),
            "Carved, lit and glowing from the inside.", holiday = "halloween"),
        Strain("graveyardgas", "Graveyard Gas", 960, 1, 2000, 22, 40000, 2800, 0xFF1A1A24.toInt(), 0xFF6A3AA4.toInt(), 0xFF7AFF6A.toInt(),
            "Dug up at midnight. Smells like it, too.", holiday = "halloween"),
        Strain("gingerbread", "Gingerbread Haze", 150, 1, 180, 11, 2400, 260, 0xFF3A4A1E.toInt(), 0xFFC0783A.toInt(), 0xFFFFF4F4.toInt(),
            "Iced, spiced and a little bit crumbly.", holiday = "christmas"),
        Strain("northpolekush", "North Pole Kush", 720, 1, 900, 18, 16000, 1400, 0xFF2A4A3A.toInt(), 0xFFE8F4FF.toInt(), 0xFF7AC8FF.toInt(),
            "Frosted so thick it squeaks.", holiday = "christmas"),
        Strain("krampus", "Krampus Kush", 960, 1, 2000, 22, 40000, 2800, 0xFF2A1414.toInt(), 0xFF8A1E1E.toInt(), 0xFF1E1E1E.toInt(),
            "For the naughty list only.", holiday = "christmas"),
        Strain("sparkler", "Sparkler", 150, 1, 180, 11, 2400, 260, 0xFF3A4A2E.toInt(), 0xFFFFF0A0.toInt(), 0xFFFFFFFF.toInt(),
            "Fizzes when you break it up.", holiday = "newyear"),
        Strain("auldlang", "Auld Lang Syne", 720, 1, 900, 18, 16000, 1400, 0xFF2A3A2E.toInt(), 0xFFD0A060.toInt(), 0xFFFFE8C0.toInt(),
            "Should old acquaintance be forgot? You will.", holiday = "newyear"),
        Strain("balldrop", "Ball Drop", 960, 1, 2000, 22, 40000, 2800, 0xFF14142A.toInt(), 0xFFE0E8FF.toInt(), 0xFFFFD23A.toInt(),
            "A crystal-coated bud that only lands once a year.", holiday = "newyear"),
        Strain("chocobox", "Chocolate Box", 150, 1, 180, 11, 2400, 260, 0xFF3A2A1E.toInt(), 0xFF8A4A2A.toInt(), 0xFFFF8AB0.toInt(),
            "You never know which one you're gonna get.", holiday = "valentine"),
        Strain("lovepotion", "Love Potion No. 9", 720, 1, 900, 18, 16000, 1400, 0xFF3A1E3A.toInt(), 0xFFFF3AA0.toInt(), 0xFFC07AFF.toInt(),
            "One whiff and you're smitten.", holiday = "valentine"),
        Strain("rosegold", "Rose Gold", 960, 1, 2000, 22, 40000, 2800, 0xFF2E3A2A.toInt(), 0xFFF0A0A0.toInt(), 0xFFFFD27A.toInt(),
            "Petal-pink buds with a gold sheen.", holiday = "valentine"),
        Strain("luckycharm", "Lucky Charm", 150, 1, 180, 11, 2400, 260, 0xFF2E7A3A.toInt(), 0xFFFF8AD0.toInt(), 0xFFFFE04A.toInt(),
            "Magically delicious.", holiday = "stpatrick"),
        Strain("irishcream", "Irish Cream", 720, 1, 900, 18, 16000, 1400, 0xFF2A5A2A.toInt(), 0xFFF0E0C0.toInt(), 0xFF8A5A2A.toInt(),
            "Smooth, creamy and a bit boozy.", holiday = "stpatrick"),
        Strain("potogold", "Pot o' Gold", 960, 1, 2000, 22, 40000, 2800, 0xFF1E4A1E.toInt(), 0xFFFFC81E.toInt(), 0xFFFFF4A0.toInt(),
            "The real treasure at the end of the rainbow.", holiday = "stpatrick"),
        Strain("wakebake", "Wake & Bake", 150, 1, 180, 11, 2400, 260, 0xFF3A6A2A.toInt(), 0xFFFFC07A.toInt(), 0xFFFFFFFF.toInt(),
            "Breakfast of champions.", holiday = "fourtwenty"),
        Strain("hotbox", "Hotbox Haze", 720, 1, 900, 18, 16000, 1400, 0xFF2A3A2A.toInt(), 0xFFC0C0C0.toInt(), 0xFF9AFF3A.toInt(),
            "Roll the windows up.", holiday = "fourtwenty"),
        Strain("blazeit", "Blaze It OG", 960, 1, 2000, 22, 40000, 2800, 0xFF1E3A1E.toInt(), 0xFF5AE02A.toInt(), 0xFFFF6A1E.toInt(),
            "The one they write songs about.", holiday = "fourtwenty"),
        Strain("bunnyhop", "Bunny Hop", 150, 1, 180, 11, 2400, 260, 0xFF4A7A3A.toInt(), 0xFFF4F0F4.toInt(), 0xFFFFA0C0.toInt(),
            "Quick on its feet and soft to the touch.", holiday = "easter"),
        Strain("chocobunny", "Chocolate Bunny", 720, 1, 900, 18, 16000, 1400, 0xFF3A2A1E.toInt(), 0xFF7A4A2A.toInt(), 0xFFF0E0C0.toInt(),
            "Ears first, always.", holiday = "easter"),
        Strain("goldenegg", "Golden Egg", 960, 1, 2000, 22, 40000, 2800, 0xFF3A4A1E.toInt(), 0xFFFFD84A.toInt(), 0xFFFFFFFF.toInt(),
            "Laid once a year by a very special goose.", holiday = "easter"),
        Strain("firecracker", "Firecracker", 150, 1, 180, 11, 2400, 260, 0xFF3A2A1E.toInt(), 0xFFFF4A2A.toInt(), 0xFFFFE04A.toInt(),
            "Snap, crackle, pop.", holiday = "july4"),
        Strain("unclesam", "Uncle Sam's Stash", 720, 1, 900, 18, 16000, 1400, 0xFF1E2A4A.toInt(), 0xFFF4F4F4.toInt(), 0xFFE03A3A.toInt(),
            "He wants YOU to grow it.", holiday = "july4"),
        Strain("starsstripes", "Stars & Stripes", 960, 1, 2000, 22, 40000, 2800, 0xFF1A2A4A.toInt(), 0xFF3A6AE0.toInt(), 0xFFFFFFFF.toInt(),
            "Fifty stars on every cola.", holiday = "july4"),
        Strain("pumpkinpie", "Pumpkin Pie", 150, 1, 180, 11, 2400, 260, 0xFF4A3A1E.toInt(), 0xFFE08A2A.toInt(), 0xFFFFF0D0.toInt(),
            "Whipped cream not included.", holiday = "thanksgiving"),
        Strain("turkeyleg", "Turkey Leg Kush", 720, 1, 900, 18, 16000, 1400, 0xFF4A2E1A.toInt(), 0xFFA0602A.toInt(), 0xFFE0A04A.toInt(),
            "Couch-lock, guaranteed.", holiday = "thanksgiving"),
        Strain("cornucopia", "Cornucopia", 960, 1, 2000, 22, 40000, 2800, 0xFF5A4A1E.toInt(), 0xFFE0B03A.toInt(), 0xFFD03A2A.toInt(),
            "Horn of plenty. Plenty of everything.", holiday = "thanksgiving"),
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
        // The in-between ladder strains, each crossed with a neighbour.
        Cross("bammer", "brick", "dirtcheap", "Dirt Cheap", "Two bargain bags, one surprisingly decent smoke."),
        Cross("skunk", "lemon", "skunkade", "Skunkade", "Lemonade with a stink."),
        Cross("blueberry", "bluedream", "bbmuffin", "Blueberry Muffin", "Fresh out of the oven, still steaming."),
        Cross("goldengoat", "jack", "goldenjack", "Golden Jack", "A legend with a lucky streak."),
        Cross("trainwreck", "greencrack", "derailed", "Derailed", "Fast, loud and off the tracks."),
        Cross("chemdawg", "sour", "chemsour", "Chem Sour", "Fuel and bleach. Somehow it works."),
        Cross("silverhaze", "purple", "silverlining", "Silver Lining", "Every purple cloud has one."),
        Cross("dosido", "gsc", "cookiejar", "Cookie Jar", "Hand caught in it, every time."),
        Cross("bluecheese", "durban", "cheeseboard", "Cheese Board", "Fancy enough for a party, strong enough to end it."),
        Cross("skywalker", "og", "jedi", "Jedi Kush", "These are the buds you're looking for."),
        Cross("afghan", "widow", "silkroad", "Silk Road", "Travelled a long way to reach your jar."),
        Cross("headband", "strawberry", "halo", "Halo Berry", "Sits on your head like a crown of berries."),
        Cross("bubba", "banana", "bubbagum", "Bubba Gum", "Blow a bubble, sink into the couch."),
        Cross("sundae", "gelato", "funday", "Sundae Funday", "Every scoop on the menu at once."),
        Cross("purplepunch", "gdp", "grapeape", "Grape Ape", "Big, purple and beats its chest."),
        Cross("macone", "cake", "weddingmac", "Wedding MAC", "Cookies at the reception, aliens at the afterparty."),
        Cross("fritter", "mimosa", "brunchbox", "Brunch Box", "Pastry and bubbly in one sticky jar."),
        Cross("lcg", "glue", "cherryglue", "Cherry Glue", "Sweet at first, then you can't get up."),
        Cross("jealousy", "garypayton", "fastbreak", "Fast Break", "Steals the ball and dunks it."),
        Cross("marker", "pinkrozay", "pinkink", "Pink Ink", "Writes its name all over your evening."),
        Cross("gushers", "oreoz", "snackattack", "Snack Attack", "The munchies, bred on purpose."),
        Cross("eclipse", "voidkush", "totality", "Totality", "Not a speck of light left."),
        Cross("cometkush", "galaxyglue", "cometglue", "Comet Glue", "Sticks to the sky as it flies by."),
        Cross("skyfall", "olympus", "thunderbolt", "Thunderbolt", "Thrown down from the mountain."),
        Cross("zenith", "emperor", "apex", "Apex Emperor", "Nothing grows higher. Nothing ever will."),
        // Quick crosses: two quick cuts together, for a lab strain that's done in a few hours.
        Cross("cerealbar", "biscottibite", "dunkers", "Dunkers", "Cookies and cereal, dipped in milk."),
        Cross("voidblink", "shootingstar", "warpspeed", "Warp Speed", "Gone before the light gets here."),
        Cross("cherrytart", "phoenixspark", "poptart", "Pop Tart", "Out of the toaster, still on fire."),
        Cross("housepour", "nebulashot", "lastcall", "Last Call", "One more round, quick, before they close."),
        Cross("minititan", "daytrip", "weekender", "Weekender", "Big trip, small bag."),
        Cross("tiara", "royalflash", "flashcrown", "Flash Crown", "Royalty, on the clock."),
    )

    private fun mix(a: Int, b: Int, k: Float): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) * (1 - k) + ((b shr s) and 0xFF) * k).toInt() shl s
        return (0xFF shl 24) or ch(16) or ch(8) or ch(0)
    }

    private val hybrids: List<Strain> = run {
        val known = (base + filler + express + native + festive).associateBy { it.id }.toMutableMap()
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

    val all: List<Strain> = climb + express + hybrids + base.filter { it.exotic } + smuggled + native + festive + premium

    /** The strains you buy seeds for with cash, in unlock order. */
    val ladder = all.filter { !it.exotic && !it.hybrid && !it.quick && it.site == null && it.holiday == null }
    /** Quick cuts of the long ladder strains. */
    val quickList = express
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
