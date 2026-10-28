package dev.m4s4t0.kushacres.model

/** The three branches of the legacy perk tree. */
enum class Branch(val title: String) { GROWER("Grower"), TRADER("Trader"), SCIENTIST("Scientist") }

/**
 * Permanent perks bought with legacy points, one point for every time the farm is sold. Each needs the one before it in
 * its [branch] ([tier] 0 first).
 */
enum class LegacyPerk(val branch: Branch, val tier: Int, val title: String, val desc: String) {
    GREEN_THUMB(Branch.GROWER, 0, "Green Thumb", "Plants grow 15% faster"),
    BIG_BUDS(Branch.GROWER, 1, "Big Buds", "Every harvest is 10% bigger"),
    SEED_VAULT(Branch.GROWER, 2, "Seed Vault", "Keep 2 more heirloom strains"),
    SILVER_TONGUE(Branch.TRADER, 0, "Silver Tongue", "Everything sells 15% higher"),
    KNOWN_FACE(Branch.TRADER, 1, "Known Face", "Contracts earn 25% more reputation"),
    OLD_MONEY(Branch.TRADER, 2, "Old Money", "Start every new farm with \$25,000"),
    QUICK_STUDY(Branch.SCIENTIST, 0, "Quick Study", "+15% XP"),
    LAB_NOTES(Branch.SCIENTIST, 1, "Lab Notes", "The Lab researches 50% faster"),
    GOLDEN_TOUCH(Branch.SCIENTIST, 2, "Golden Touch", "+5% golden harvest chance");

    companion object { fun of(name: String) = entries.firstOrNull { it.name == name } }
}

/** Where a sold farm starts over: the land you pick when you sell. */
enum class StartMap(val title: String, val desc: String) {
    HOMESTEAD("Old Homestead", "The plain plot you know"),
    CREEKSIDE("Creekside", "A pond waters the plots beside it, for good"),
    QUARRY("Quarry Hill", "Bigger land to start with, but strewn with boulders"),
    GREENHOUSE("Old Greenhouse", "Two sprinklers are already in the ground");

    companion object { fun of(name: String) = entries.firstOrNull { it.name == name } ?: HOMESTEAD }
}

/** Legacy 2.0: the perk tree, heirloom strains and start maps that come with selling the farm. */
object Legacy {
    const val HEIRLOOM_YIELD = 1.1
    const val OLD_MONEY = 25_000L
    const val QUARRY_LAND = 8

    fun has(f: Farm, p: LegacyPerk) = p in f.legacyPerks
    /** Perk points not spent yet: one per sale. */
    fun points(f: Farm) = f.legacy - f.legacyPerks.size
    fun canTake(f: Farm, p: LegacyPerk) = points(f) > 0 && !has(f, p) &&
        LegacyPerk.entries.filter { it.branch == p.branch && it.tier < p.tier }.all { has(f, it) }

    fun take(f: Farm, p: LegacyPerk): Result {
        if (has(f, p)) return Result.BLOCKED
        if (!canTake(f, p)) return Result.LOCKED
        f.legacyPerks += p
        return Result.OK
    }

    fun mult(f: Farm, p: LegacyPerk, amt: Double) = if (has(f, p)) 1 + amt else 1.0

    /** How many strains a sale can keep. */
    fun slots(f: Farm) = 1 + if (has(f, LegacyPerk.SEED_VAULT)) 2 else 0
    /** Strains that can become heirlooms: the plain ladder ones you've reached, past the first. */
    fun eligible(f: Farm) = Strains.ladder.filter { it.site == null && !it.hybrid && !it.exotic && it.holiday == null && it.level in 2..f.level }
    fun heirloom(f: Farm, s: Strain) = s.id in f.heirlooms

    /** Lays out [map] on the farm's fresh land, after the shack and starting patch are down. */
    fun lay(f: Farm, map: StartMap) {
        val x = f.ox; val y = f.oy
        when (map) {
            StartMap.HOMESTEAD -> {}
            StartMap.CREEKSIDE -> for (b in y + 2..y + 3) f.tile(x + 4, b).apply { clear(); kind = Kind.WATER }
            StartMap.QUARRY -> {
                f.land = maxOf(f.land, QUARRY_LAND)
                for ((a, b) in listOf(5 to 0, 6 to 3, 1 to 5, 4 to 6, 7 to 6, 6 to 1, 2 to 7, 7 to 4)) {
                    val t = f.tile(x + a, y + b)
                    if (t.kind == Kind.GRASS) t.kind = Kind.ROCK
                }
            }
            StartMap.GREENHOUSE -> for (a in listOf(1, 4)) f.tile(x + a, y + 3).apply { if (kind == Kind.GRASS) kind = Kind.SPRINKLER }
        }
    }
}
