package dev.m4s4t0.kushacres.model

/** What a story chapter asks for; each borrows a [Deal]'s time, pay and reputation. */
enum class Want(val deal: Deal) { BIG(Deal.BULK), RUSH(Deal.RUSH), CLASS(Deal.SPECIAL), KITCHEN(Deal.KITCHEN), PAIR(Deal.PAIR), LOCAL(Deal.SPECIAL), LAB(Deal.SPECIAL) }

/** One chapter of a town's story: [line] has {who} for the town's buyer and {town} for its name. */
class Chapter(val title: String, val line: String, val want: Want, val work: Double = want.deal.work)

/** What finishing a town's story leaves you: a keepsake with a small bonus on every farm. */
class Keepsake(val name: String, val perk: Perk) {
    val desc get() = if (perk == Perk.SELL) "Everything sells ${pct}% higher" else "+${pct}% XP"
    companion object { const val AMT = 0.03; val pct = (AMT * 100).toInt() }
}

/**
 * Story chains: each town's buyer has [STEPS] linked contracts. Chapter n opens at reputation tier n+1 in that town,
 * one at a time, and the last one leaves that town's [Keepsake].
 */
object Chains {
    const val STEPS = 4
    const val PAY = 1.25
    const val BUCKS = 3L

    private val ARCS: List<List<Chapter>> = listOf(
        listOf(
            Chapter("A Foot in the Door", "{who} is opening a second shop in {town} and needs stock for the shelves. Show you can deliver.", Want.BIG, 4.0),
            Chapter("Opening Night", "The ribbon gets cut at dawn. {who} needs something quick to impress the crowd.", Want.RUSH),
            Chapter("The Connoisseurs", "Word's out, and the picky crowd is asking for something special.", Want.CLASS),
            Chapter("The Edibles Counter", "{who} wants a whole counter of cooked goods to round the shop off.", Want.KITCHEN),
        ),
        listOf(
            Chapter("Word of Mouth", "{who} is putting on the {town} festival and wants the farm everyone talks about.", Want.BIG, 4.0),
            Chapter("The Sample Tent", "Festival-goers want something grown right here. Bring the local strain.", Want.LOCAL),
            Chapter("The Headliner's Rider", "The headliner's rider asks for the good stuff, and they're on stage in hours.", Want.RUSH),
            Chapter("Main Stage", "The whole of {town} turns up. {who} needs enough for everyone.", Want.BIG),
        ),
        listOf(
            Chapter("Undercut", "A big-city supplier is undercutting {who}. Help keep the shelves full at a fair price.", Want.BIG, 4.0),
            Chapter("The Taste Test", "{who} set up a blind taste test against the rival. Bring your best of the kind they asked for.", Want.CLASS),
            Chapter("Lab Work", "The rival brags about lab genetics. Show {town} that yours are better.", Want.LAB),
            Chapter("Run Them Out", "One last big push and the rival packs up for good.", Want.BIG),
        ),
        listOf(
            Chapter("An Old Friend", "{who}'s grandpa grew around {town} for fifty years. He wants to taste what grows here now.", Want.LOCAL),
            Chapter("Remedy", "His knees aren't what they were. Something cooked, gentle and slow.", Want.KITCHEN),
            Chapter("The Old Seed", "He remembers a strain from his youth. The closest thing you've got will do.", Want.CLASS),
            Chapter("One Last Harvest", "He wants to see a real harvest come in one more time. Make it a big one.", Want.BIG, 6.0),
        ),
        listOf(
            Chapter("Joining Up", "{who} runs the growers' collective in {town}. Members pull their weight; show you can.", Want.BIG, 4.0),
            Chapter("The Shared Kitchen", "The collective cooks together on Sundays. Bring goods from your kitchens.", Want.KITCHEN),
            Chapter("Two Farms, One Order", "A member's crop failed. The collective covers it with strains from two of your farms.", Want.PAIR),
            Chapter("Market Day", "{who} wants something nobody else in {town} has: your lab strains.", Want.LAB),
        ),
    )

    private val KEEPSAKES = mapOf(
        "home" to Keepsake("Key to Kushville", Perk.SELL),
        "redwood" to Keepsake("Carved Owl Totem", Perk.XP),
        "mesa" to Keepsake("Turquoise Belt Buckle", Perk.SELL),
        "bayou" to Keepsake("Lucky Gator Tooth", Perk.XP),
        "coast" to Keepsake("Message in a Bottle", Perk.XP),
        "frost" to Keepsake("Ice-Carved Pipe", Perk.SELL),
        "emerald" to Keepsake("Emerald Grower's Badge", Perk.SELL),
        "volcano" to Keepsake("Obsidian Grinder", Perk.XP),
        "neon" to Keepsake("Neon Sign Fragment", Perk.SELL),
        "fort20" to Keepsake("Castle Keep Key", Perk.SELL),
        "highland" to Keepsake("Tartan Stash Pouch", Perk.XP),
        "crater" to Keepsake("Starfall Meteorite", Perk.XP),
        "spooky" to Keepsake("Haunted Lantern", Perk.SELL),
        "northpole" to Keepsake("Elf-Made Rolling Tray", Perk.XP),
        "timesquare" to Keepsake("Golden Countdown Clock", Perk.SELL),
        "lovers" to Keepsake("Heart Locket", Perk.XP),
        "clover" to Keepsake("Four-Leaf Clover", Perk.SELL),
        "highhill" to Keepsake("Golden Bong", Perk.XP),
        "meadow" to Keepsake("Painted Egg", Perk.SELL),
        "liberty" to Keepsake("Liberty Bell Pin", Perk.SELL),
        "harvest" to Keepsake("Golden Corncob", Perk.XP),
    )

    private fun arc(town: String) = ARCS[Math.floorMod(Towns.ids.indexOf(town).coerceAtLeast(0), ARCS.size)]
    fun chapter(town: String, i: Int): Chapter = arc(town)[i.coerceIn(0, STEPS - 1)]
    fun keepsake(town: String) = KEEPSAKES[town] ?: Keepsake("${Towns[town].town} Keepsake", Perk.SELL)
    /** The buyer the story follows: the town's bud buyer. */
    fun who(town: String) = Towns[town].names[1]
    fun line(town: String, i: Int) = chapter(town, i).line.replace("{who}", who(town)).replace("{town}", Towns[town].town)

    /** Chapters finished in [town]. */
    fun done(f: Farm, town: String) = f.chains[town] ?: 0
    /** The tier chapter [i] needs. */
    fun tierFor(i: Int) = i + 1
    fun keepsakes(f: Farm) = f.chains.filter { it.value >= STEPS }.keys.map { keepsake(it) }
    /** The farm-wide bonus of kind [p] from every keepsake. */
    fun bonus(f: Farm, p: Perk): Double {
        var n = 0
        for ((town, d) in f.chains) if (d >= STEPS && keepsake(town).perk == p) n++
        return n * Keepsake.AMT
    }
}
