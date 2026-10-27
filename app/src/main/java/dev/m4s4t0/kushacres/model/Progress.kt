package dev.m4s4t0.kushacres.model

import kotlin.math.roundToLong
import java.util.TimeZone
import kotlin.random.Random

/** Lifetime counters. They survive selling the farm, and feed quests and trophies. */
enum class Stat(val title: String) {
    HARVESTS("Plants harvested"),
    GOLDEN("Golden harvests"),
    GRAMS("Grams grown"),
    PLANTED("Seeds planted"),
    WATERED("Plants watered"),
    ORDERS("Orders filled"),
    SOLD("Grams sold"),
    EARNED("Cash earned"),
    BATCHES("Batches collected"),
    PESTS("Pests shooed"),
    RAIDS("Raids survived"),
    BRED("Hybrids bred"),
    EXOTIC("Exotic harvests"),
    QUESTS("Quests done"),
    DAYS("Daily rewards claimed"),
    EVENTS("Events seen"),
    SPENT("Cash spent"),
}

/** The real-world seasons of the farm, a week each, the same for everyone. */
enum class Season(val title: String, val desc: String) {
    SPRING("Spring", "Everything grows 15% faster"),
    SUMMER("Summer", "Watering lasts twice as long"),
    AUTUMN("Autumn", "Harvests yield 20% more"),
    WINTER("Winter", "Prices +25%, growth −10%");

    companion object {
        const val WEEK = 7 * 86_400_000L
        /** Test override (debug builds, from the launch intent). */
        var force: Season? = null
        fun at(now: Long): Season = force ?: entries[((now / WEEK) % 4).toInt()]
        /** Millis until the season turns. */
        fun left(now: Long): Long = WEEK - now % WEEK
    }
}

/** A day of the local calendar, counted from 1970. */
fun localDay(now: Long): Long = (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L

enum class QuestKind(val stat: Stat, val text: String) {
    HARVEST(Stat.HARVESTS, "Harvest %s plants"),
    PLANT(Stat.PLANTED, "Plant %s seeds"),
    WATER(Stat.WATERED, "Water %s plants"),
    ORDERS(Stat.ORDERS, "Fill %s orders"),
    EARN(Stat.EARNED, "Earn %s from sales"),
    GOLDEN(Stat.GOLDEN, "Get %s golden harvests"),
    BATCH(Stat.BATCHES, "Collect %s batches"),
    GRAMS(Stat.GRAMS, "Grow %sg of bud"),
}

/** One of the day's three jobs. Progress is how far the [stat][QuestKind.stat] has moved since [base]. */
class Quest(val kind: QuestKind, val target: Long, val base: Long, val cash: Long, val xp: Long, var claimed: Boolean = false)

/** Goals for each farm on its own: reached once per farm, for a gem and some XP. */
enum class FarmGoal(val title: String, val desc: String, val target: Long, val value: (Farm, Site) -> Long) {
    HARVEST_100("Growing Season", "Harvest 100 plants on this farm", 100, { _, s -> s.harvests }),
    HARVEST_1K("Breadbasket", "Harvest 1,000 plants on this farm", 1_000, { _, s -> s.harvests }),
    NATIVES("Local Flavour", "Harvest both of this farm's own strains", 2, { f, s -> Strains.all.count { it.site == s.def.id && !it.hybrid && it.id in f.grown }.toLong() }),
    TOWN("Company Town", "Have 5 work buildings on this farm", 5, { _, s -> s.tiles.count { it.kind == Kind.HOUSE }.toLong() }),
    CREW("Full Crew", "Hire one of every kind of crew here", Crew.entries.size.toLong(), { _, s -> s.crew.count { it > 0 }.toLong() }),
    FLEET("Fleet", "Park every vehicle on this farm", Vehicle.entries.size.toLong(), { _, s -> s.tiles.count { it.kind == Kind.VEHICLE }.toLong() });

    /** Home Acres has no strains of its own. */
    fun applies(s: Site) = this != NATIVES || Strains.all.any { it.site == s.def.id && !it.hybrid }

    companion object { fun of(id: String) = entries.firstOrNull { it.name == id } }
}

/**
 * Trophies: lifetime goals that unlock on their own. [value] reads how far along the farm is; [target] is the goal.
 */
enum class Trophy(val title: String, val desc: String, val target: Long, val value: (Farm) -> Long) {
    HARVEST_1("First Harvest", "Harvest a plant", 1, { it.stat(Stat.HARVESTS) }),
    HARVEST_100("Green Thumb", "Harvest 100 plants", 100, { it.stat(Stat.HARVESTS) }),
    HARVEST_1K("Harvest Moon", "Harvest 1,000 plants", 1_000, { it.stat(Stat.HARVESTS) }),
    HARVEST_10K("Bud Baron", "Harvest 10,000 plants", 10_000, { it.stat(Stat.HARVESTS) }),
    GOLDEN_1("Struck Gold", "Get a golden harvest", 1, { it.stat(Stat.GOLDEN) }),
    GOLDEN_50("Midas Touch", "Get 50 golden harvests", 50, { it.stat(Stat.GOLDEN) }),
    GOLDEN_500("Golden God", "Get 500 golden harvests", 500, { it.stat(Stat.GOLDEN) }),
    ORDER_10("Open for Business", "Fill 10 orders", 10, { it.stat(Stat.ORDERS) }),
    ORDER_100("Plug of the Town", "Fill 100 orders", 100, { it.stat(Stat.ORDERS) }),
    ORDER_1K("Kingpin", "Fill 1,000 orders", 1_000, { it.stat(Stat.ORDERS) }),
    EARN_10K("Pocket Money", "Earn \$10,000", 10_000, { it.stat(Stat.EARNED) }),
    EARN_1M("Millionaire", "Earn \$1M", 1_000_000, { it.stat(Stat.EARNED) }),
    EARN_1B("Billionaire", "Earn \$1B", 1_000_000_000, { it.stat(Stat.EARNED) }),
    EARN_1T("Trillionaire", "Earn \$1T", 1_000_000_000_000, { it.stat(Stat.EARNED) }),
    LEVEL_10("Getting Somewhere", "Reach level 10", 10, { it.bestLevel.toLong() }),
    LEVEL_25("Seasoned Grower", "Reach level 25", 25, { it.bestLevel.toLong() }),
    LEVEL_50("Master Grower", "Reach level 50", 50, { it.bestLevel.toLong() }),
    LEVEL_80("Legend of Kush", "Reach level 80", 80, { it.bestLevel.toLong() }),
    LAND_MAX("Lord of the Land", "Own the full ${Farm.MAX}×${Farm.MAX} farm", Farm.MAX.toLong(), { f -> f.sites.maxOf { it.land }.toLong() }),
    MANSION("Mansion Life", "Move into the Kush Mansion", 5, { it.bestShack().toLong() }),
    PALACE("Palace Dweller", "Move into the Kush Palace", 9, { it.bestShack().toLong() }),
    HYBRID_1("Mad Scientist", "Breed a hybrid", 1, { it.bred.size.toLong() }),
    HYBRID_10("Gene Splicer", "Breed 10 different hybrids", 10, { it.bred.size.toLong() }),
    HYBRID_ALL("Master Breeder", "Breed every hybrid", Hybrids.recipes.size.toLong(), { it.bred.size.toLong() }),
    BATCH_10("Small Business", "Collect 10 batches", 10, { it.stat(Stat.BATCHES) }),
    BATCH_500("Factory Floor", "Collect 500 batches", 500, { it.stat(Stat.BATCHES) }),
    STREAK_7("Week Warrior", "Claim 7 daily rewards in a row", 7, { it.bestStreak.toLong() }),
    STREAK_30("Creature of Habit", "Claim 30 daily rewards in a row", 30, { it.bestStreak.toLong() }),
    QUEST_10("Errand Runner", "Finish 10 quests", 10, { it.stat(Stat.QUESTS) }),
    QUEST_100("Task Master", "Finish 100 quests", 100, { it.stat(Stat.QUESTS) }),
    PEST_25("Bug Squasher", "Shoo 25 pests", 25, { it.stat(Stat.PESTS) }),
    RAID_10("Cool Under Pressure", "Survive 10 raids", 10, { it.stat(Stat.RAIDS) }),
    EXOTIC_10("Collector", "Harvest 10 exotic plants", 10, { it.stat(Stat.EXOTIC) }),
    DECOR_10("Interior Designer", "Have 10 decorations out", 10, { it.decorCount().toLong() }),
    CREW("Full Crew", "Hire every helper", Helper.entries.size.toLong(), { f -> f.helpers.count { it }.toLong() }),
    LEGACY_1("Fresh Start", "Sell the farm", 1, { it.legacy.toLong() }),
    LEGACY_5("Dynasty", "Sell the farm 5 times", 5, { it.legacy.toLong() });

    companion object {
        fun of(id: String) = entries.firstOrNull { it.name == id }
    }
}

/** What a day of the login streak pays out. */
class DailyReward(val day: Int, val cash: Long, val buff: Buff? = null, val minutes: Int = 0, val seeds: String? = null, val seedCount: Int = 0) {
    /** Pot Bucks, and holiday tokens when a holiday is on. */
    var bucks = 0L
    var tokens = 0L
}

/** Breeding recipes: two parents make a hybrid that grows faster and sells higher than either. */
object Hybrids {
    class Recipe(val a: String, val b: String, val id: String)

    val recipes: List<Recipe> by lazy {
        Strains.all.filter { it.parents != null }.map { Recipe(it.parents!!.first, it.parents.second, it.id) }
    }

    fun of(a: String, b: String): Recipe? = recipes.firstOrNull { (it.a == a && it.b == b) || (it.a == b && it.b == a) }
    fun from(id: String): Recipe? = recipes.firstOrNull { it.id == id }
}

/** Daily rewards, quests, trophies, breeding and selling the farm, as extensions of the farm's rules. */
object Progress {
    const val BREED_LEVEL = 8
    const val LEGACY_LEVEL = 40
    private val DAY_MULT = doubleArrayOf(1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 8.0)

    fun reward(f: Farm, day: Int): DailyReward {
        val d = (day - 1) % 7 + 1
        val cash = nice((60.0 + 30.0 * f.level * f.level) * DAY_MULT[d - 1])
        val r = when (d) {
            3 -> DailyReward(d, cash, Buff.JUICE, 30)
            5 -> DailyReward(d, cash, Buff.DUST, 20)
            7 -> {
                val ex = BmItem.entries.filter { it.strain != null && it.site == null && it.level <= maxOf(6, f.level) }.last()
                DailyReward(d, cash, seeds = ex.strain, seedCount = 3)
            }
            else -> DailyReward(d, cash)
        }
        r.bucks = if (d == 7) 10 else 2
        r.tokens = if (Holiday.at(f.lastTick) != null) ((10 + 2 * d) * f.tokenMult(f.level)).roundToLong() else 0
        return r
    }

    fun newQuests(f: Farm, day: Long): List<Quest> {
        val r = Random(day * 7919 + f.legacy * 31)
        val kinds = QuestKind.entries.filter {
            when (it) {
                QuestKind.BATCH -> f.tiles.any { t -> t.kind == Kind.FACILITY }
                QuestKind.GOLDEN -> f.level >= 3
                QuestKind.ORDERS -> true
                else -> true
            }
        }.shuffled(r).take(3)
        val l = f.level
        val top = Strains.ladder.last { f.unlocked(it) }
        return kinds.map { k ->
            val target: Long = when (k) {
                QuestKind.HARVEST -> 10L + 2 * l + r.nextInt(10)
                QuestKind.PLANT -> 10L + 2 * l + r.nextInt(10)
                QuestKind.WATER -> 8L + l + r.nextInt(8)
                QuestKind.ORDERS -> 2L + l / 10 + r.nextInt(3)
                QuestKind.EARN -> nice(top.price * top.yield * f.priceMult() * (8 + r.nextInt(8)))
                QuestKind.GOLDEN -> 1L + r.nextInt(2)
                QuestKind.BATCH -> 2L + r.nextInt(3)
                QuestKind.GRAMS -> nice(top.yield * f.yieldMult() * (12 + r.nextInt(10)).toDouble())
            }
            val cash = nice((100.0 + 20.0 * l * l) * (1.5 + r.nextDouble()))
            val xp = maxOf(5L, (Farm.xpNeed(l) * 0.06).toLong())
            Quest(k, target, f.stat(k.stat), cash, xp)
        }
    }
}
