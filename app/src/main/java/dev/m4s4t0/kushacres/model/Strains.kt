package dev.m4s4t0.kushacres.model

/**
 * One strain you can grow. [minutes] is the grow time on dry soil with no boosts; water, nutrients and lamps make it
 * faster. Each strain earns more per hour than the one before it, and gives a bit more XP per hour, so the newest
 * unlock is always worth planting, while the short ones stay best for active play (one watering covers the whole grow).
 */
class Strain(
    val id: String,
    val name: String,
    val minutes: Int,
    val level: Int,
    /** Cost to plant one plot. */
    val seed: Long,
    /** Grams per harvest before Genetics. */
    val yield: Int,
    /** Market price per gram at full demand, before Street Cred. */
    val price: Long,
    val xp: Int,
    /** Sprite colours (ARGB): leaves, buds and the little hairs on them. */
    val leaf: Int,
    val bud: Int,
    val hair: Int,
    val blurb: String,
    /** Black-market only: planted from seeds in your pocket instead of bought per plot. */
    val exotic: Boolean = false,
    /** Bred in the lab from these two strains; planted like a ladder strain once you've made it. */
    val parents: Pair<String, String>? = null,
) {
    val seconds: Long get() = minutes * 60L
    val hybrid: Boolean get() = parents != null
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
    )

    private fun mix(a: Int, b: Int, k: Float): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) * (1 - k) + ((b shr s) and 0xFF) * k).toInt() shl s
        return (0xFF shl 24) or ch(16) or ch(8) or ch(0)
    }

    private val hybrids: List<Strain> = run {
        val known = base.associateBy { it.id }.toMutableMap()
        crosses.map { c ->
            val a = known.getValue(c.a); val b = known.getValue(c.b)
            Strain(c.id, c.name,
                minutes = ((a.minutes + b.minutes) / 2.0 * 0.9).toInt().coerceAtLeast(4),
                level = maxOf(a.level, b.level),
                seed = ((a.seed + b.seed) / 2.0 * 1.2).toLong().coerceAtLeast(5),
                yield = maxOf(a.yield, b.yield) + 1,
                price = ((a.price + b.price) / 2.0 * 1.3).toLong(),
                xp = ((a.xp + b.xp) / 2.0 * 1.25).toInt(),
                leaf = mix(a.leaf, b.leaf, 0.5f), bud = mix(a.bud, b.bud, 0.6f), hair = a.hair,
                blurb = c.blurb, parents = c.a to c.b,
            ).also { known[it.id] = it }
        }
    }

    val all: List<Strain> = base.filter { !it.exotic } + hybrids + base.filter { it.exotic }

    /** The strains you buy seeds for with cash, in unlock order. */
    val ladder = all.filter { !it.exotic && !it.hybrid }
    /** Lab strains: unlocked by breeding them once. */
    val hybridList = all.filter { it.hybrid }
    /** Black-market strains: planted from seeds you own, never asked for in orders. */
    val exotics = all.filter { it.exotic }

    private val byId = all.associateBy { it.id }
    operator fun get(id: String): Strain? = byId[id]
}
